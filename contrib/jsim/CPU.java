import java.util.*;

public class CPU
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

	public static final int UOP_NOP = 0;
	public static final int UOP_MOVE = 1;
	public static final int UOP_CMP = 2;
	public static final int UOP_ADD = 3;
	public static final int UOP_SUB = 4;
	public static final int UOP_XOR = 5;
	public static final int UOP_OR = 6;
	public static final int UOP_AND = 7;
	public static final int UOP_ASL = 8;
	public static final int UOP_ASR = 9;
	public static final int UOP_LSR = 10;
	public static final int UOP_ROL = 11;
	public static final int UOP_ROR = 12;
	public static final int UOP_RCL = 13;
	public static final int UOP_RCR = 14;
	public static final int UOP_JZ = 15;
	public static final int UOP_JNZ = 16;
	public static final int UOP_JC = 17;
	public static final int UOP_JNC = 18;
	public static final int UOP_JV = 19;
	public static final int UOP_JNV = 20;
	public static final int UOP_JS = 21;
	public static final int UOP_JNS = 22;
	public static final int UOP_JMP = 23;
	public static final int UOP_JSR = 24;
	public static final int UOP_RET = 25;
	public static final int UOP_POPF = 26;
	public static final int UOP_PUSHF = 27;
	public static final int UOP_CLI = 28;
	public static final int UOP_SEI = 29;
	public static final int UOP_HLT = 30;

	public static final int UOP_LD = 64;
	public static final int UOP_ST = 96;

	private Memory memctl;
	private int pc;
	private short flags;
	private boolean halted;
	private boolean open_hatch;
	private short regs[] = new short[16];
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

	public CPU(Memory memctl)
	{
		this.memctl = memctl;
		this.halted = false;
		this.cold_reset();
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
			// TODO: find specific triggers we can use
			this.intregs[addr] = val;
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

	public void warm_reset()
	{
		this.flags = 0;
		for(int i = 0; i < 16; i++)
			this.regs[i] = 0;

		int pc_low = 0xFFFF & (int)this.read16(0xFFFFC);
		int pc_high = 0xFF & (int)this.read8(0xFFFFE);
		this.pc = (pc_low | (pc_high<<16)) & 0xFFFFF;
	}

	public void cold_reset()
	{
		for(int i = 0; i < 128; i++)
			this.intregs[i] = 0;
		
		this.flags = 0;

		this.open_hatch = true;
		for(int i = 0; i < 3; i++)
			this.write8(0xFFFFC+i, this.read8(0xFFFFC+i));
		this.open_hatch = false;

		this.warm_reset();
	}

	public boolean isHalted()
	{
		return this.halted;
	}

	public void runUntilHalt()
	{
		this.doCycles(1000000000);

		if(false)
		{
			System.out.printf("HALT %05X:", this.pc);
			System.out.printf(" %04X", 0xFFFF & (int)this.flags);
			for(int i = 1; i < 16; i++)
				System.out.printf(" %04X", 0xFFFF & (int)this.regs[i]);
			System.out.println();
		}
	}

	public void clearUops()
	{
		for(int i = 0; i < uop_cache.length; i++)
			uop_cache[i].pc_start = -1;
	}

	private int loadUop()
	{
		int op = 0xFF & (int)this.fetch8();

		int rop = -1;
		int rx = 0;
		int ry = 0;
		int rimm = 0;
		int rsize = 0;

		if((op & 0xE0) == 0xE0)
		{
			int extra = 0xFF & (int)this.fetch8();
			int ex0 = (extra & 15);
			int ex1 = (extra >> 4);

			if((op & 0x07) == 0x07)
			{
				// OP3/4
				if((op & 0x08) == 0)
				{
					// OP3
					if((op & 0x10) == 0)
					{
						rsize = 1;
						rx = ex0;
						rimm = 0xFF & (int)this.fetch8();
						ry = (rimm>>4);
						rimm <<= 12;
						rimm &= 0xFFFF;
					} else {
						rsize = 2;
						rx = 0;
						ry = ex0;
						rimm = 0xFFFF & (int)this.fetch16();
					}

					if(ex1 < 10)
						rop = UOP_JZ + ex1;
				} else {
					// OP4
					rx = ex0;
					rop = ((ex1 & 1) == 0 ? UOP_LD : UOP_ST);
					int meep = 0;
					ry = 0;
					rsize = (op & 0x10) != 0 ? 2 : 1;
					switch(ex1>>1)
					{
						case 1:
							rimm = 0xFFFF & (int)this.fetch16();
							rop |= 0x0F;
							break;
						case 2:
							meep = 0xFF & (int)this.fetch8();
							rimm = 0xFF & (int)this.fetch8();
							rimm <<= 8;
							rop |= meep & 0x0F;
							ry = meep>>4;
							break;
						case 3:
							meep = 0xFF & (int)this.fetch8();
							rimm = 0xFFFF & (int)this.fetch16();
							rop |= 0x10;
							rop |= meep & 0x0F;
							ry = meep>>4;
							break;
						default:
							rop = -1;
					}
				}
			} else {
				// OP1/2 @y
				rx = ex0;
				ry = ex1;
				switch((op>>3)&3)
				{
					case 0:
						// OP1.b @y
						rop = UOP_MOVE + (op & 7);
						rsize = 1;
						break;
					case 1:
						// OP2.w #y
						rop = UOP_ASL + (op & 7);
						rsize = 2;
						rimm = ry;
						ry = 0;
						break;
					case 2:
						// OP1.w @y
						rop = UOP_MOVE + (op & 7);
						rsize = 2;
						break;
					case 3:
						// OP2.w @y
						rop = UOP_ASL + (op & 7);
						rsize = 2;
						break;
				}
			}
		} else if((op & 0x0F) == 0x00) {
			// Special ops
			switch(op>>4)
			{
				case 0:
					rop = UOP_NOP;
					break;
				case 2:
					rop = UOP_RET;
					break;
				case 4:
					rop = UOP_POPF;
					break;
				case 6:
					rop = UOP_PUSHF;
					break;
				case 8:
					rop = UOP_CLI;
					break;
				case 10:
					rop = UOP_SEI;
					break;
				case 12:
					rop = UOP_HLT;
					break;
			}
		} else {
			// OP1 immediate
			rop = (op>>5) + UOP_MOVE;
			rx = (op & 0x0F);
			ry = 0;
			if((op & 0x10) == 0)
			{
				rimm = 0xFF & (int)this.fetch8();
				rsize = 1;
			} else {
				rimm = 0xFFFF & (int)this.fetch16();
				rsize = 2;
			}
		}

		if(rop == -1)
			throw new RuntimeException(String.format("unsupported op %02X", op));

		int ret = 0;
		ret |= (rsize == 2 ? 0x80000000 : 0x00000000);
		ret |= (rop & 0x7F)<<24;
		ret |= (ry & 0x0F)<<20;
		ret |= (rx & 0x0F)<<16;
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
		v ^= (v>>4);
		v ^= (v>>2);
		v ^= (v>>1);
		this.setFlag(F_OVERFLOW, (v & 1) != 0);
	}

	private short doUopStep(int size, int op, int rx, int ry, int imm, short fmask) throws HaltCPU
	{
		if(op >= UOP_JZ && op <= UOP_JSR)
		{
			imm = 0xFFFFF & (imm + (ry<<16) + (0xFFFF & (int)this.regs[rx]));
		}

		switch(op)
		{
			case UOP_NOP:
				break;
			case UOP_MOVE:
				return (short)(ry != 0 ? this.regs[ry] : imm);
			case UOP_CMP:
			case UOP_SUB: {
				int vx = 0xFFFF & (int)this.regs[rx];
				int vy = (ry == 0 ? imm : 0xFFFF & (int)this.regs[ry]);
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
				int vx = 0xFFFF & (int)this.regs[rx];
				int vy = (ry == 0 ? imm : 0xFFFF & (int)this.regs[ry]);
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
				int vx = this.regs[rx];
				int vy = (ry == 0 ? imm : this.regs[ry]);
				int ret = vx ^ vy;
				if((fmask & F_OVERFLOW) != 0) this.setParityFlag((short)(ret & (size == 2 ? 0xFFFF : 0xFF)));
				if((fmask & F_SIGNED) != 0) this.setFlag(F_SIGNED, (ret & (size == 2 ? 0x8000 : 0x0080)) != 0);
				if((fmask & F_ZERO) != 0) this.setFlag(F_ZERO, (ret & (size == 2 ? 0xFFFF : 0x00FF)) == 0);
				return (short)ret;
			} //break;
			case UOP_OR: {
				int vx = this.regs[rx];
				int vy = (ry == 0 ? imm : this.regs[ry]);
				int ret = vx | vy;
				if((fmask & F_OVERFLOW) != 0) this.setParityFlag((short)(ret & (size == 2 ? 0xFFFF : 0xFF)));
				if((fmask & F_SIGNED) != 0) this.setFlag(F_SIGNED, (ret & (size == 2 ? 0x8000 : 0x0080)) != 0);
				if((fmask & F_ZERO) != 0) this.setFlag(F_ZERO, (ret & (size == 2 ? 0xFFFF : 0x00FF)) == 0);
				return (short)ret;
			} //break;
			case UOP_AND: {
				int vx = this.regs[rx];
				int vy = (ry == 0 ? imm : this.regs[ry]);
				if(size == 1)
					vy |= 0xFF00;
				int ret = vx & vy;
				if((fmask & F_OVERFLOW) != 0) this.setParityFlag((short)(ret & (size == 2 ? 0xFFFF : 0xFF)));
				if((fmask & F_SIGNED) != 0) this.setFlag(F_SIGNED, (ret & (size == 2 ? 0x8000 : 0x0080)) != 0);
				if((fmask & F_ZERO) != 0) this.setFlag(F_ZERO, (ret & (size == 2 ? 0xFFFF : 0x00FF)) == 0);
				return (short)ret;
			} //break;
			case UOP_ASL: {
				int vx = 0xFFFF & (int)this.regs[rx];
				int vy = (ry == 0 ? imm : this.regs[ry]) & 0xF;
				int ret = vx << vy;
				if((fmask & F_OVERFLOW) != 0) this.setParityFlag((short)(ret & (size == 2 ? 0xFFFF : 0xFF)));
				if((fmask & F_SIGNED) != 0) this.setFlag(F_SIGNED, (ret & (size == 2 ? 0x8000 : 0x0080)) != 0);
				if((fmask & F_ZERO) != 0) this.setFlag(F_ZERO, (ret & (size == 2 ? 0xFFFF : 0x00FF)) == 0);
				if((fmask & F_CARRY) != 0) this.setFlag(F_CARRY, (ret & 0x10000) != 0);
				return (short)ret;
			} //break;
			case UOP_ASR: {
				int vx = this.regs[rx];
				int vy = (ry == 0 ? imm : this.regs[ry]) & 0xF;
				int ret = vx >> vy;
				int retpre = (vy == 0 ? vx << 1 : vx >> (vy-1));
				if((fmask & F_OVERFLOW) != 0) this.setParityFlag((short)(ret & (size == 2 ? 0xFFFF : 0xFF)));
				if((fmask & F_SIGNED) != 0) this.setFlag(F_SIGNED, (ret & (size == 2 ? 0x8000 : 0x0080)) != 0);
				if((fmask & F_ZERO) != 0) this.setFlag(F_ZERO, (ret & (size == 2 ? 0xFFFF : 0x00FF)) == 0);
				if((fmask & F_CARRY) != 0) this.setFlag(F_CARRY, (retpre & 0x0001) != 0);
				return (short)ret;
			} //break;
			case UOP_LSR: {
				int vx = 0xFFFF & (int)this.regs[rx];
				int vy = (ry == 0 ? imm : this.regs[ry]) & 0xF;
				int ret = vx >>> vy;
				int retpre = (vy == 0 ? vx << 1 : vx >>> (vy-1));
				if((fmask & F_OVERFLOW) != 0) this.setParityFlag((short)(ret & (size == 2 ? 0xFFFF : 0xFF)));
				if((fmask & F_SIGNED) != 0) this.setFlag(F_SIGNED, (ret & (size == 2 ? 0x8000 : 0x0080)) != 0);
				if((fmask & F_ZERO) != 0) this.setFlag(F_ZERO, (ret & (size == 2 ? 0xFFFF : 0x00FF)) == 0);
				if((fmask & F_CARRY) != 0) this.setFlag(F_CARRY, (retpre & 0x0001) != 0);
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
			default:
				if((op & 0x40) != 0)
				{
					int top = (op & 0x0F);
					int vy = 0xFFFF & (int)this.regs[ry];
					int offs = (vy + (top<<16) + imm);

					if((op & 0x20) == 0)
					{
						// LD
						//System.out.printf("%05X %d\n", this.pc, size);
						//System.out.printf("LD %05X\n", offs);
						if(size == 2)
							return this.read16(offs);
						else
							return this.read8(offs);
					} else {
						// ST
						//System.out.printf("ST %05X\n", offs);
						if(size == 2)
							this.write16(offs, this.regs[rx]);
						else
							this.write8(offs, (byte)this.regs[rx]);
					}
				} else {
					throw new RuntimeException(String.format("unsupported uop: %d %d %d %d %04X", op, size, rx, ry, imm));
				}
		}

		return this.regs[rx];
	}

	private void doUop(int uop_data, short fmask, boolean use_ret) throws HaltCPU
	{
		int size = ((uop_data>>31) & 1) + 1;
		int op = (uop_data>>24) & 0x7F;
		int rx = (uop_data>>16) & 0x0F;
		int ry = (uop_data>>20) & 0x0F;
		int imm = (uop_data) & 0xFFFF;

		short ret = doUopStep(size, op, rx, ry, imm, fmask);

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

		if(op >= UOP_MOVE && op <= UOP_RCL)
		{
			return true; // TODO: actually check these
		} else if(op >= UOP_JZ && op <= UOP_HLT) {
			return false;
		} else {
			return true;
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

		int op = (uop_data>>24) & 0x7F;
		int rx = (uop_data>>16) & 0x0F;

		SavedUop sop = new SavedUop(uop_data, ncyc - ocyc, this.pc, (short)0xFFFF, opReturns(op, rx), opCanJump(op));

		return sop;
	}

	// Under profiling, Java says this is the bottleneck.
	// doUop/doUopStep/loadUop might be getting inlined, though.
	public int doCycles(int count)
	{
		int cyc_end = count + this.cycles;

		try
		{
			while((cyc_end - this.cycles) > 0)
			{
				int lpc = this.pc;
				SavedUopBank bank = fetchUopChain();
				SavedUop[] uop_chain = bank.chain;
				this.pc = bank.pc_end;

				for(int i = 0; i < uop_chain.length; i++)
				{
					SavedUop sop = uop_chain[i];

					if(false)
					{
						System.out.printf("%05X %08X:", sop.new_pc, sop.uop);
						System.out.printf(" %04X", 0xFFFF & (int)this.flags);
						for(int j = 1; j < 16; j++)
							System.out.printf(" %04X", 0xFFFF & (int)this.regs[j]);
						System.out.println();
					}

					this.cycles += sop.load_cycles;

					doUop(sop.uop, sop.fmask, sop.use_ret);

					if(sop.can_jump)
						break;
				}
			}
		} catch(HaltCPU _) {}

		return cyc_end - this.cycles;
	}
}

