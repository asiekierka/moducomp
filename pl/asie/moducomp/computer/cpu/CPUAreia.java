package pl.asie.moducomp.computer.cpu;

import java.util.*;
import pl.asie.moducomp.api.computer.ICPU;
import pl.asie.moducomp.api.computer.IMemory;
import pl.asie.moducomp.api.computer.IMemoryController;

public class CPUAreia implements ICPU
{
	private class HaltCPU extends Exception {}

	// RULES FOR ACCESSING THE CYCLES VARIABLE:
	// - Increment this if you need extra wait states for read access.
	//   The CPU will increment it by 1 each read.
	// - You can read this at any time.
	public int cycles;

	public static final int F_ZERO = 0x0001;
	public static final int F_CARRY = 0x0002;
	public static final int F_OVERFLOW = 0x0004;
	public static final int F_SIGNED = 0x0008;
	public static final int F_INTERRUPT = 0x0010;

	// NOTE: uop format differs slightly:
	//
	// lmbboooo xxxxyyyy iiiiiiii iiiiiiii
	//
	// where:
	// l = size (0 = byte, 1 = word)
	// m = mode (depends on the op)
	// b = op bank
	// o = opcode
	// x = register x
	// y = register y OR in some cases the top nybble of a 20-bit address
	// i = immediate value
	
	// OP0
	public static final int UOP_NOP = 0;
	public static final int UOP_RET = 1;
	public static final int UOP_POPF = 2;
	public static final int UOP_PUSHF = 3;
	public static final int UOP_CLI = 4;
	public static final int UOP_SEI = 5;
	public static final int UOP_HLT = 6;
	// unused op 7
	public static final int UOP_GSEG = 8; // we just map these here to make our lives easier
	public static final int UOP_SSEG = 9;
	// unused ops 12-15

	// OP1
	public static final int UOP_MOVE = 16;
	public static final int UOP_ADD = 17;
	public static final int UOP_CMP = 18;
	public static final int UOP_SUB = 19;
	public static final int UOP_XOR = 20;
	public static final int UOP_OR = 21;
	public static final int UOP_AND = 22;
	// unused op 23
	public static final int UOP_ROL = 24;
	public static final int UOP_ROR = 25;
	public static final int UOP_RCL = 26;
	public static final int UOP_RCR = 27;
	public static final int UOP_ASR = 28;
	public static final int UOP_ASL = 29;
	public static final int UOP_LSR = 30;
	// unused op 31
	
	// OP2
	public static final int UOP_LD = 32;
	public static final int UOP_ST = 40;
	
	// OP3
	public static final int UOP_JZ = 48;
	public static final int UOP_JNZ = 49;
	public static final int UOP_JC = 50;
	public static final int UOP_JNC = 51;
	public static final int UOP_JV = 52;
	public static final int UOP_JNV = 53;
	public static final int UOP_JS = 54;
	public static final int UOP_JNS = 55;
	public static final int UOP_JMP = 56;
	public static final int UOP_JSR = 57;
	// unused ops 58-63

	private IMemory memctl;
	private int pc;
	private short flags;
	private boolean halted;
	private boolean open_hatch;
	private short regs[] = new short[16];
	private byte segs[] = new byte[4];
	private byte intregs[] = new byte[128];

	private class SavedUop
	{
		public int uop;
		public byte load_cycles;
		public short fmask;
		public int new_pc;
		public boolean use_ret;
		public boolean can_jump;

		public SavedUop(int uop, int load_cycles, int new_pc, short fmask, boolean use_ret, boolean can_jump)
		{
			this.uop = uop;
			this.load_cycles = (byte)load_cycles;
			this.new_pc = new_pc;
			this.fmask = fmask;
			this.use_ret = use_ret;
			this.can_jump = can_jump;
		}
	}

	private class SavedUopBank
	{
		public int pc_start, pc_end;
		public int load_cycles;
		public SavedUop[] chain;
	}

	private SavedUopBank[] uop_cache = new SavedUopBank[8];
	{ for(int i = 0; i < uop_cache.length; i++) { uop_cache[i] = new SavedUopBank(); uop_cache[i].chain = new SavedUop[64]; uop_cache[i].pc_start = -1; } }
	private int uop_cache_ptr = 0;

	public CPUAreia()
	{
		this.halted = false;
	}
	
	public int getAddressBitLength() { return 20; }
	
	public IMemory getMemoryHandler() {
		return this.memctl;
	}
	
	public void setMemoryHandler(IMemory ctl) {
		this.memctl = ctl;
	}

	private byte read8(int addr)
	{
		addr &= 0xFFFFF;
		byte val = (byte)0xFF;

		if(open_hatch || addr <= 0xFFF7F)
			val = this.memctl.read8(this, addr);
		else
			val = this.intregs[addr & 127];

		this.cycles++;
		return val;
	}

	private short read16(int addr)
	{
		int v0 = 0xFF & (int)this.read8(addr);
		int v1 = 0xFF & (int)this.read8((addr+1) & 0xFFFFF);

		return (short)(v0 | (v1<<8));
	}

	private void write8(int addr, byte val)
	{
		addr &= 0xFFFFF;
		if(addr <= 0xFFF7F)
		{
			this.memctl.write8(this, addr, val);
		} else {
			addr &= 0x7F;
			if(addr >= 4 && addr < 8) { // Interrupt line area
				this.intregs[addr] &= val;
			} else this.intregs[addr] = val;
		}

		this.cycles++;
	}

	private void write16(int addr, short val)
	{
		int vi = 0xFFFF & (int)val;
		this.write8(addr, (byte)(vi & 0xFF));
		this.write8((addr+1) & 0xFFFFF, (byte)((vi>>8) & 0xFF));
	}

	public byte fetch8()
	{
		byte val = this.read8(this.pc);
		this.pc++;
		return val;
	}

	public short fetch16()
	{
		short val = this.read16(this.pc);
		this.pc += 2;
		return val;
	}

	public void resetWarm()
	{
		this.flags = 0;
		for(int i = 0; i < 16; i++)
			this.regs[i] = 0;
		for(int i = 0; i < 4; i++)
			this.segs[i] = 0;

		int pc_low = 0xFFFF & (int)this.read16(0xFFFFC);
		int pc_high = 0xFF & (int)this.read8(0xFFFFE);
		this.pc = (pc_low | (pc_high<<16)) & 0xFFFFF;
	}

	public void resetCold()
	{
		for(int i = 0; i < 128; i++)
			this.intregs[i] = 0;
		
		this.flags = 0;

		this.open_hatch = true;
		for(int i = 0; i < 3; i++)
			this.write8(0xFFFFC+i, this.read8(0xFFFFC+i));
		this.open_hatch = false;

		this.resetWarm();
	}

	public boolean isHalted()
	{
		return this.halted;
	}

	public void debugPC(int pc) {
		System.out.printf("%05X:", pc);
		System.out.printf(" %04X", 0xFFFF & (int)this.flags);
		for(int i = 0; i < 4; i++)
			System.out.printf(" %02X", 0xFF & (int)this.segs[i]);
		for(int i = 1; i < 16; i++)
			System.out.printf(" %04X", 0xFFFF & (int)this.regs[i]);
		System.out.println();
	}
	
	public void debugPC() { debugPC(this.pc); }

	public void clearUops()
	{
		for(int i = 0; i < uop_cache.length; i++)
			uop_cache[i].pc_start = -1;
	}

	private int loadUop()
	{
		int op = 0xFF & (int)this.fetch8();

		int rop = -1;
		int rmode = 0;
		int rx = 0;
		int ry = 0;
		int rimm = 0;
		int rsize = 0;

		switch((op >> 6) & 3)
		{
			case 0:
				// OP0
				rsize = 1;
				if(op < 7) {
					rop = (op & 63);
				} else if(op >= 16 && op <= 17) {
					rop = (op & 7) + 8;
					int rpair = 0xFF & (int)this.fetch8();
					rx = rpair >> 4;
					ry = rpair & 15;
					assert(ry <= 3);
				}
				break;

			case 1:
				// OP1
				if((op & 7) != 7)
				{
					rsize = 1 + ((op>>4) & 1);
					rmode = (op>>5) & 1;
					rop = (op & 15) + 16;

					int rpair = 0xFF & (int)this.fetch8();
					rx = rpair >> 4;
					ry = rpair & 15;

					if(rmode == 0)
					{
						// regs only, load from x,y
					} else if(rsize == 2) {
						// immediate word, load from y,i
						rimm = 0xFFFF & (int)this.fetch16();
					} else {
						// immediate byte, load from y,i
						rimm = 0xFF & (int)this.fetch8();
					}
				}
				break;

			case 2: {
				// OP2
				rsize = 1 + ((op>>2) & 1);
				rmode = ((op>>4) & 3) == 1 ? 1 : 0;
				rop = (op & 0x0B) + 32;

				// If mode == 1: an extra top nybble is given by Y field, plus when seg == 3, we treat it as a seg of $00
				// Low two bits of opcode denote which segment is used

				int rpair = 0xFF & (int)this.fetch8();
				rx = (rpair >> 4);
				ry = (rpair & 15);

				switch((op>>4) & 3)
				{
					case 0:
						break;
					case 1:
					case 3:
						rimm = 0xFFFF & (int)this.fetch16();
						break;
					case 2:
						rimm = 0xFFFF & (int)(byte)this.fetch8();
						break;
				}

			} break;

			case 3:
				// OP3
				rsize = 1 + ((op>>4) & 1);

				if((op & 15) < 10)
					rop = (op & 15) + 48;

				rmode = (op>>5) & 1;

				// If mode == 1, then size is size, and this jump is relative.
				// Otherwise,
				//   if size == 0: top nybble is Y
				//   if size == 1: top nybble is (pc>>16)
				switch((op>>4) & 3)
				{
					case 0: {
						int rpair = 0xFF & (int)this.fetch8();
						rx = rpair >> 4;
						ry = rpair & 15; // actually top nybble of the 20-bit PC
					} // follow through
					case 1:
						rimm = 0xFFFF & (int)this.fetch16();
						break;
					case 2:
						rimm = 0xFFFF & (int)(byte)this.fetch8(); // note, SIGNED
						break;
					case 3:
						rimm = 0xFFFF & (int)this.fetch16(); // note, SIGNED
						break;
				}
				break;
		}

		if(rop == -1)
			throw new RuntimeException(String.format("unsupported op %02X at position %05X", op, this.pc));

		int ret = 0;
		ret |= (rsize == 2 ? 0x80000000 : 0x00000000);
		ret |= (rmode & 1)<<30;
		ret |= (rop & 0x7F)<<24;
		ret |= (ry & 0x0F)<<16;
		ret |= (rx & 0x0F)<<20;
		ret |= (rimm & 0xFFFF);

		return ret;
	}

	private void setFlag(int flag, boolean check)
	{
		if(check)
			this.flags |= flag;
		else
			this.flags &= ~flag;
	}

	private void setParityFlag(short val)
	{
		byte v = (byte)(val^(val>>8));
		this.setFlag(F_OVERFLOW, ((0x6996 >> ((v^(v>>4)) & 0xf)) & 1) != 0);
	}

	private short doUopStep(int size, int mode, int op, int rx, int ry, int imm, short fmask) throws HaltCPU
	{
		if((op & 0x30) == 0x30)
		{
			// set stuff up for our jumps
			if(mode == 1)
			{
				// relative
				imm = (this.pc + (int)(short)imm);
			} else if(size == 1) {
				// absolute + @x
				imm = (ry<<16) + imm + (0xFFFF & (int)this.regs[rx]);
				// This op might still be broken. Tread with caution.
				//throw new RuntimeException(String.format("address %05X @%i abs", imm, rx));
			} else {
				// relative to current pc bank (whyyyyy)
				imm = (this.pc & 0xF0000) | imm;
			}
			imm &= 0xFFFFF;
			//System.out.printf("JUMP: %05X -> %05X [%d]\n", this.pc, imm, op);
		}

		switch(op)
		{
			case UOP_NOP:
				break;
			case UOP_MOVE:
				return (short)(mode == 0 ? this.regs[ry] : imm);
			case UOP_CMP:
			case UOP_SUB: {
				int vx = 0xFFFF & (int)this.regs[(mode == 0 ? rx : ry)];
				int vy = (mode != 0 ? imm : 0xFFFF & (int)this.regs[ry]);
				int ret = vx - vy;
				if((fmask & F_CARRY) != 0) this.setFlag(F_CARRY, vx < vy);
				if((fmask & F_ZERO) != 0) this.setFlag(F_ZERO, (ret & (size == 2 ? 0xFFFF : 0x00FF)) == 0);
				if((fmask & F_SIGNED) != 0) this.setFlag(F_SIGNED, (ret & (size == 2 ? 0x8000 : 0x0080)) != 0);
				if((fmask & F_OVERFLOW) != 0)
				{
					if (((vx ^ vy) & (size == 2 ? 0x8000 : 0x0080)) == 0)
					{
						// same signs
						int aimm = (vy < 0x8000 ? vy :  0x10000-vy);
						int axval = (vx < 0x8000 ? vx : 0x10000-vx);
						if(size == 2)
							if((fmask & F_OVERFLOW) != 0) this.setFlag(F_OVERFLOW, aimm + axval >= 0x8000);
						else
							if((fmask & F_OVERFLOW) != 0) this.setFlag(F_OVERFLOW, aimm + axval >= 0x80);
					} else {
						// differing signs
						if((fmask & F_OVERFLOW) != 0) this.setFlag(F_OVERFLOW, false);
					}
				}

				if(op != UOP_CMP)
					return (short)ret;
			} break;
			case UOP_ADD: {
				int vx = 0xFFFF & (int)this.regs[(mode == 0 ? rx : ry)];
				int vy = (mode != 0 ? imm : 0xFFFF & (int)this.regs[ry]);
				int ret = vx + vy;
				if((fmask & F_CARRY) != 0) this.setFlag(F_CARRY, (size == 2 ? ret >= 0x10000 : ret >= 0x100));
				if((fmask & F_ZERO) != 0) this.setFlag(F_ZERO, (ret & (size == 2 ? 0xFFFF : 0x00FF)) == 0);
				if((fmask & F_SIGNED) != 0) this.setFlag(F_SIGNED, (ret & (size == 2 ? 0x8000 : 0x0080)) != 0);
				if((fmask & F_OVERFLOW) != 0)
				{
					if (((vx ^ vy) & (size == 2 ? 0x8000 : 0x0080)) == 0)
					{
						// same signs
						int aimm = (vy < 0x8000 ? vy :  0x10000-vy);
						int axval = (vx < 0x8000 ? vx : 0x10000-vx);
						if(size == 2)
							if((fmask & F_OVERFLOW) != 0) this.setFlag(F_OVERFLOW, aimm + axval >= 0x8000);
						else
							if((fmask & F_OVERFLOW) != 0) this.setFlag(F_OVERFLOW, aimm + axval >= 0x80);
					} else {
						// differing signs
						if((fmask & F_OVERFLOW) != 0) this.setFlag(F_OVERFLOW, false);
					}
				}

				if(op != UOP_CMP)
					return (short)ret;
			} break;
			case UOP_XOR: {
				int vx = 0xFFFF & (int)this.regs[(mode == 0 ? rx : ry)];
				int vy = (mode != 0 ? imm : 0xFFFF & (int)this.regs[ry]);
				int ret = vx ^ vy;
				if((fmask & F_OVERFLOW) != 0) this.setParityFlag((short)(ret & (size == 2 ? 0xFFFF : 0xFF)));
				if((fmask & F_SIGNED) != 0) this.setFlag(F_SIGNED, (ret & (size == 2 ? 0x8000 : 0x0080)) != 0);
				if((fmask & F_ZERO) != 0) this.setFlag(F_ZERO, (ret & (size == 2 ? 0xFFFF : 0x00FF)) == 0);
				return (short)ret;
			} //break;
			case UOP_OR: {
				int vx = 0xFFFF & (int)this.regs[(mode == 0 ? rx : ry)];
				int vy = (mode != 0 ? imm : 0xFFFF & (int)this.regs[ry]);
				int ret = vx | vy;
				if((fmask & F_OVERFLOW) != 0) this.setParityFlag((short)(ret & (size == 2 ? 0xFFFF : 0xFF)));
				if((fmask & F_SIGNED) != 0) this.setFlag(F_SIGNED, (ret & (size == 2 ? 0x8000 : 0x0080)) != 0);
				if((fmask & F_ZERO) != 0) this.setFlag(F_ZERO, (ret & (size == 2 ? 0xFFFF : 0x00FF)) == 0);
				return (short)ret;
			} //break;
			case UOP_AND: {
				int vx = 0xFFFF & (int)this.regs[(mode == 0 ? rx : ry)];
				int vy = (mode != 0 ? imm : 0xFFFF & (int)this.regs[ry]);
				if(size == 1)
					vy |= 0xFF00;
				int ret = vx & vy;
				if((fmask & F_OVERFLOW) != 0) this.setParityFlag((short)(ret & (size == 2 ? 0xFFFF : 0xFF)));
				if((fmask & F_SIGNED) != 0) this.setFlag(F_SIGNED, (ret & (size == 2 ? 0x8000 : 0x0080)) != 0);
				if((fmask & F_ZERO) != 0) this.setFlag(F_ZERO, (ret & (size == 2 ? 0xFFFF : 0x00FF)) == 0);
				return (short)ret;
			} //break;
			case UOP_ASL: {
				int vx = 0xFFFF & (int)this.regs[(mode == 0 ? rx : ry)];
				int vy = (mode != 0 ? imm : 0xFFFF & (int)this.regs[ry]);
				int ret = vx << vy;
				if((fmask & F_OVERFLOW) != 0) this.setParityFlag((short)(ret & (size == 2 ? 0xFFFF : 0xFF)));
				if((fmask & F_SIGNED) != 0) this.setFlag(F_SIGNED, (ret & (size == 2 ? 0x8000 : 0x0080)) != 0);
				if((fmask & F_ZERO) != 0) this.setFlag(F_ZERO, (ret & (size == 2 ? 0xFFFF : 0x00FF)) == 0);
				if((fmask & F_CARRY) != 0) this.setFlag(F_CARRY, (ret & 0x10000) != 0);
				return (short)ret;
			} //break;
			case UOP_ASR: {
				int vx = 0xFFFF & (int)this.regs[(mode == 0 ? rx : ry)];
				int vy = (mode != 0 ? imm : 0xFFFF & (int)this.regs[ry]);
				int ret = vx >> vy;
				int retpre = (vy == 0 ? vx << 1 : vx >> (vy-1));
				if((fmask & F_OVERFLOW) != 0) this.setParityFlag((short)(ret & (size == 2 ? 0xFFFF : 0xFF)));
				if((fmask & F_SIGNED) != 0) this.setFlag(F_SIGNED, (ret & (size == 2 ? 0x8000 : 0x0080)) != 0);
				if((fmask & F_ZERO) != 0) this.setFlag(F_ZERO, (ret & (size == 2 ? 0xFFFF : 0x00FF)) == 0);
				if((fmask & F_CARRY) != 0) this.setFlag(F_CARRY, (retpre & 0x0001) != 0);
				return (short)ret;
			} //break;
			case UOP_LSR: {
				int vx = 0xFFFF & (int)this.regs[(mode == 0 ? rx : ry)];
				int vy = (mode != 0 ? imm : 0xFFFF & (int)this.regs[ry]);
				int ret = vx >>> vy;
				int retpre = (vy == 0 ? vx << 1 : vx >>> (vy-1));
				if((fmask & F_OVERFLOW) != 0) this.setParityFlag((short)(ret & (size == 2 ? 0xFFFF : 0xFF)));
				if((fmask & F_SIGNED) != 0) this.setFlag(F_SIGNED, (ret & (size == 2 ? 0x8000 : 0x0080)) != 0);
				if((fmask & F_ZERO) != 0) this.setFlag(F_ZERO, (ret & (size == 2 ? 0xFFFF : 0x00FF)) == 0);
				if((fmask & F_CARRY) != 0) this.setFlag(F_CARRY, (retpre & 0x0001) != 0);
				return (short)ret;
			} //break;
			case UOP_ROL: {
				int vx = 0xFFFF & (int)this.regs[(mode == 0 ? rx : ry)];
				int vy = (mode != 0 ? imm : 0xFFFF & (int)this.regs[ry]);
				int ret = (size == 2 ? (vx << (vy&15) | vx >>> (16-(vy&15))) & 0xFFFF
						: (vx << (vy&7) | vx >>> (8-(vy&7))) & 0xFF);
				if((fmask & F_OVERFLOW) != 0) this.setParityFlag((short)(ret & (size == 2 ? 0xFFFF : 0xFF)));
				if((fmask & F_SIGNED) != 0) this.setFlag(F_SIGNED, (ret & (size == 2 ? 0x8000 : 0x0080)) != 0);
				if((fmask & F_ZERO) != 0) this.setFlag(F_ZERO, (ret & (size == 2 ? 0xFFFF : 0x00FF)) == 0);
				return (short)ret;
			} //break;
			case UOP_ROR: {
				int vx = 0xFFFF & (int)this.regs[(mode == 0 ? rx : ry)];
				int vy = (mode != 0 ? imm : 0xFFFF & (int)this.regs[ry]);
				int ret = (size == 2 ? (vx >>> (vy&15) | vx << (16-(vy&15))) & 0xFFFF
						: (vx >>> (vy&7) | vx << (8-(vy&7))) & 0xFF);
				if((fmask & F_OVERFLOW) != 0) this.setParityFlag((short)(ret & (size == 2 ? 0xFFFF : 0xFF)));
				if((fmask & F_SIGNED) != 0) this.setFlag(F_SIGNED, (ret & (size == 2 ? 0x8000 : 0x0080)) != 0);
				if((fmask & F_ZERO) != 0) this.setFlag(F_ZERO, (ret & (size == 2 ? 0xFFFF : 0x00FF)) == 0);
				return (short)ret;
			} //break;
			case UOP_RCL: {
				int vx = 0xFFFF & (int)this.regs[(mode == 0 ? rx : ry)];
				int vy = (mode != 0 ? imm : 0xFFFF & (int)this.regs[ry]) % (size == 2 ? 17 : 9);
				int ret = (size == 2 ? vx | (((this.flags & F_CARRY) > 0) ? 0x10000 : 0)
						: vx | (((this.flags & F_CARRY) > 0) ? 0x100 : 0));
				ret = (size == 2 ? (ret << vy | ret >>> (17-vy))
						: (ret << vy | ret >>> (9-vy)));
				if((fmask & F_OVERFLOW) != 0) this.setParityFlag((short)(ret & (size == 2 ? 0xFFFF : 0xFF)));
				if((fmask & F_SIGNED) != 0) this.setFlag(F_SIGNED, (ret & (size == 2 ? 0x8000 : 0x0080)) != 0);
				if((fmask & F_ZERO) != 0) this.setFlag(F_ZERO, (ret & (size == 2 ? 0xFFFF : 0x00FF)) == 0);
				if((fmask & F_CARRY) != 0) this.setFlag(F_CARRY, (ret & (size == 2 ? 0x10000 : 0x100)) != 0);
				return (short)ret;
			} //break;
			case UOP_RCR: {
				int vx = 0xFFFF & (int)this.regs[(mode == 0 ? rx : ry)];
				int vy = (mode != 0 ? imm : 0xFFFF & (int)this.regs[ry]) % (size == 2 ? 17 : 9);
				int ret = (size == 2 ? vx | (((this.flags & F_CARRY) > 0) ? 0x10000 : 0)
						: vx | (((this.flags & F_CARRY) > 0) ? 0x100 : 0));
				ret = (size == 2 ? (ret >>> vy | ret << (17-vy))
						: (ret >>> vy | ret << (9-vy)));
				if((fmask & F_OVERFLOW) != 0) this.setParityFlag((short)(ret & (size == 2 ? 0xFFFF : 0xFF)));
				if((fmask & F_SIGNED) != 0) this.setFlag(F_SIGNED, (ret & (size == 2 ? 0x8000 : 0x0080)) != 0);
				if((fmask & F_ZERO) != 0) this.setFlag(F_ZERO, (ret & (size == 2 ? 0xFFFF : 0x00FF)) == 0);
				if((fmask & F_CARRY) != 0) this.setFlag(F_CARRY, (ret & (size == 2 ? 0x10000 : 0x100)) != 0);
				return (short)ret;
			} //break;
			case UOP_JZ:
				if((this.flags & F_ZERO) != 0)
					this.pc = imm;
				break;
			case UOP_JNZ:
				if((this.flags & F_ZERO) == 0)
					this.pc = imm;
				break;
			case UOP_JC:
				if((this.flags & F_CARRY) != 0)
					this.pc = imm;
				break;
			case UOP_JNC:
				if((this.flags & F_CARRY) == 0)
					this.pc = imm;
				break;
			case UOP_JS:
				if((this.flags & F_SIGNED) != 0)
					this.pc = imm;
				break;
			case UOP_JNS:
				if((this.flags & F_SIGNED) == 0)
					this.pc = imm;
				break;
			case UOP_JV:
				if((this.flags & F_OVERFLOW) != 0)
					this.pc = imm;
				break;
			case UOP_JNV:
				if((this.flags & F_OVERFLOW) == 0)
					this.pc = imm;
				break;
			case UOP_JMP:
				this.pc = imm;
				break;
			case UOP_JSR:
				this.regs[15] -= 1;
				this.write8(0xF0000 | (0xFFFF & (int)this.regs[15]), (byte)(this.pc>>16));
				this.regs[15] -= 2;
				this.write16(0xF0000 | (0xFFFF & (int)this.regs[15]), (short)this.pc);
				this.pc = imm;
				break;
			case UOP_PUSHF: {
				this.regs[15] -= 2;
				this.write16(0xF0000 | (0xFFFF & (int)this.regs[15]), (short)this.flags);
				break;
			}
			case UOP_POPF: {
				this.flags = (short)(0xFFFF & (int)this.read16(0xF0000 | (0xFFFF & (int)this.regs[15])));
				this.regs[15] += 2;
				break;
			}
			case UOP_RET: {
				int pc_low = 0xFFFF & (int)this.read16(0xF0000 | (0xFFFF & (int)this.regs[15]));
				this.regs[15] += 2;
				int pc_high = 0xF & (int)this.read8(0xF0000 | (0xFFFF & (int)this.regs[15]));
				this.regs[15] += 1;
				this.pc = pc_low + (pc_high<<16);
			} break;
			case UOP_CLI:
				this.setFlag(F_INTERRUPT, false);
				break;
			case UOP_SEI:
				this.setFlag(F_INTERRUPT, true);
				break;
			case UOP_HLT:
				this.halted = true;
				throw new HaltCPU();
				//break;
			case UOP_GSEG:
				return this.segs[ry & 3];
			case UOP_SSEG:
				this.segs[ry & 3] = (byte)this.regs[rx];
				break;
			case UOP_LD+0:
			case UOP_LD+1:
			case UOP_LD+2:
			case UOP_LD+3: {
				// LD
				int seg = (op & 3);
				int offs = ((mode == 0
					? ((0xFF & (int)this.segs[seg])<<12) + (0xFFFF & (int)this.regs[ry])
					: (seg == 3 ? 0 : ((0xFF & (int)this.segs[seg])<<12)) + (ry<<16))
						+ imm) & 0xFFFFF;
				//System.out.printf("%05X %d\n", this.pc, size);
				//System.out.printf("LD %05X @%01X %04X\n", offs, ry, (0xFFFF & (int)this.regs[14]));
				if(size == 2)
					return this.read16(offs);
				else
					return this.read8(offs);
			} //break;
			case UOP_ST+0:
			case UOP_ST+1:
			case UOP_ST+2:
			case UOP_ST+3: {
				// ST
				int seg = (op & 3);
				int offs = ((mode == 0
					? ((0xFF & (int)this.segs[seg])<<12) + (0xFFFF & (int)this.regs[ry])
					: (seg == 3 ? 0 : ((0xFF & (int)this.segs[seg])<<12)) + (ry<<16))
						+ imm) & 0xFFFFF;
				//System.out.printf("ST %05X\n", offs);
				if(size == 2)
					this.write16(offs, this.regs[rx]);
				else
					this.write8(offs, (byte)this.regs[rx]);
			} break;
			default:
				throw new RuntimeException(String.format("unsupported uop: %d %d %d %d %04X", op, size, rx, ry, imm));
		}

		return this.regs[rx];
	}

	private void doUop(int uop_data, short fmask, boolean use_ret) throws HaltCPU
	{
		int size = ((uop_data>>31) & 1) + 1;
		int mode = ((uop_data>>30) & 1);
		int op = (uop_data>>24) & 0x3F;
		int rx = (uop_data>>20) & 0x0F;
		int ry = (uop_data>>16) & 0x0F;
		int imm = (uop_data) & 0xFFFF;

		short ret = doUopStep(size, mode, op, rx, ry, imm, fmask);

		if(use_ret)
		{
			if(size == 2)
				this.regs[rx] = ret;
			else
				this.regs[rx] = (short)((this.regs[rx] & 0xFF00) | (0xFF & (int)ret));
		}
	}

	private short opFlagWrites(int op)
	{
		if(op >= UOP_CMP && op <= UOP_SUB)
			return (short)(F_ZERO | F_SIGNED | F_OVERFLOW | F_CARRY);
		else if(op >= UOP_XOR && op <= UOP_AND)
			return (short)(F_ZERO | F_SIGNED | F_OVERFLOW);
		else if(op >= UOP_ASL && op <= UOP_RCR)
			return (short)(F_ZERO | F_SIGNED | F_OVERFLOW | F_CARRY);
		else if(op == UOP_POPF)
			return (short)0xFFFF;
		else
			return 0;
	}

	private boolean opCanJump(int op)
	{
		return (op >= UOP_JZ && op <= UOP_JSR) || op == UOP_RET || op == UOP_HLT;
	}

	private boolean opReturns(int op, int rx)
	{
		if(rx == 0)
			return false;

		switch((op>>4) & 3)
		{
			case 0:
				return op == UOP_GSEG;
			case 1:
				return op != UOP_CMP;
			case 2:
				return op < UOP_ST;
			default:
				return false;
		}
	}

	private SavedUopBank fetchUopChain()
	{
		SavedUopBank bank = null;
		int lpc = this.pc;
		int lcyc = this.cycles;

		for(int i = 0; i < uop_cache.length; i++)
		{
			bank = uop_cache[i];
			if(bank.pc_start == lpc)
				return bank;
		}

		bank = uop_cache[this.uop_cache_ptr];
		this.uop_cache_ptr = (this.uop_cache_ptr+1) & (uop_cache.length-1);
		SavedUop[] chain = bank.chain;
		bank.pc_start = lpc;

		for(int i = 0; i < chain.length; i++)
		{
			SavedUop sop = fetchUop();

			chain[i] = sop;

			if(sop.can_jump)
				break;
		}

		bank.pc_end = this.pc;
		bank.load_cycles = this.cycles - lcyc;

		this.pc = lpc;
		this.cycles = lcyc;

		tweakUopChain(lpc, bank.chain);

		return bank;
	}

	private void tweakUopChain(int base_pc, SavedUop[] ul)
	{
		int[] pc_fw = new int[] {-1, -1, -1, -1};
		int pc = base_pc;

		// Get flag writes
		for(int i = 0; i < ul.length; i++)
		{
			SavedUop sop = ul[i];

			if(sop.can_jump)
				break; // No jump operations write any flags

			// Check for our two carry-reading opcodes
			int op = (sop.uop>>24) & 0x7F;
			if(op == UOP_RCL || op == UOP_RCR)
				pc_fw[1] = -1;

			short fm = opFlagWrites(op);

			// Clear any previous writes that haven't been read
			if((fm & (1<<0)) != 0) { if(pc_fw[0] != -1) { ul[pc_fw[0]].fmask &= ~(1<<0); } pc_fw[0] = i; }
			if((fm & (1<<1)) != 1) { if(pc_fw[1] != -1) { ul[pc_fw[1]].fmask &= ~(1<<1); } pc_fw[1] = i; }
			if((fm & (1<<2)) != 2) { if(pc_fw[2] != -1) { ul[pc_fw[2]].fmask &= ~(1<<2); } pc_fw[2] = i; }
			if((fm & (1<<3)) != 3) { if(pc_fw[3] != -1) { ul[pc_fw[3]].fmask &= ~(1<<3); } pc_fw[3] = i; }

			pc = sop.new_pc;
		}

		// Clear all
	}

	private SavedUop fetchUop()
	{
		int lpc = this.pc;
			
		int ocyc = this.cycles;
		int uop_data = loadUop();
		int ncyc = this.cycles;

		int op = (uop_data>>24) & 0x3F;
		int rx = (uop_data>>20) & 0x0F;

		SavedUop sop = new SavedUop(uop_data, ncyc - ocyc, this.pc, (short)0xFFFF, opReturns(op, rx), opCanJump(op));

		return sop;
	}

	private int waitCycles;
	private int interruptVector = -1;
	
	public void wait(int cycles) {
		waitCycles += cycles;
	}
	
	public boolean interrupt(int line) {
		if(this.cycles < 2) return false; // Give CPU time to ramp up
		if((this.flags & F_INTERRUPT) == 0) return false; // Interrupts off
		// Set the line
		int addr = 4 + (line>>3);
		int pos = 1 << (line&7);
		this.intregs[addr] |= (byte)pos;
		// Get interrupt vector
		int vector = (int)(0xFF & intregs[0]);
		vector |= (int)(0xFF & intregs[1]) << 8;
		vector |= (int)(0x0F & intregs[2]) << 16;
		interruptVector = vector;
		return true;
	}
	
	// Under profiling, Java says this is the bottleneck.
	// doUop/doUopStep/loadUop might be getting inlined, though.
	public int run(int count)
	{
		int cyc_end = count + this.cycles;

		try
		{
			while((cyc_end - this.cycles) > 0)
			{
				if(interruptVector >= 0) {
					this.regs[15] -= 1;
					this.write8(0xF0000 | (0xFFFF & (int)this.regs[15]), (byte)(this.pc>>16));
					this.regs[15] -= 2;
					this.write16(0xF0000 | (0xFFFF & (int)this.regs[15]), (short)this.pc);
					this.regs[15] -= 2;
					this.write16(0xF0000 | (0xFFFF & (int)this.regs[15]), (short)this.flags);
					this.pc = interruptVector;
					interruptVector = -1;
				}
				if(waitCycles > 0) {
					if((cyc_end - this.cycles) <= waitCycles) {
						this.cycles += waitCycles;
						waitCycles = 0;
					} else {
						int diff = cyc_end - this.cycles;
						this.cycles = cyc_end;
						waitCycles -= diff;
					}
				} else {
					int lpc = this.pc;
					SavedUopBank bank = fetchUopChain();
					SavedUop[] uop_chain = bank.chain;
					this.pc = bank.pc_end;
	
					for(int i = 0; i < uop_chain.length; i++)
					{
						SavedUop sop = uop_chain[i];
	
						this.cycles += sop.load_cycles;
	
						//if(this.pc != 0xFE09B && this.pc != 0xFE09E)
						//	this.debugPC(this.pc);

						doUop(sop.uop, sop.fmask, sop.use_ret);
						if(sop.can_jump)
							break;
					}
				}
			}
		} catch(HaltCPU _) {}

		return cyc_end - this.cycles;
	}
}

