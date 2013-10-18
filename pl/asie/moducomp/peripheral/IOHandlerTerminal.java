package pl.asie.moducomp.peripheral;

import java.util.Random;

import pl.asie.moducomp.ModularComputing;
import pl.asie.moducomp.api.computer.ICPU;
import pl.asie.moducomp.api.computer.IMemory;
import pl.asie.moducomp.api.computer.PeripheralBasic;
import pl.asie.moducomp.block.TileEntityMainBoard;
import pl.asie.moducomp.block.TileEntityTerminal;
import pl.asie.moducomp.gui.text.TextWindow;

public class IOHandlerTerminal extends PeripheralBasic implements IMemory
{	
	public static final int FLAG_HARDWARE_ECHO = 0;
	public static final int FLAG_INTERRUPT = 1;
	public static final int FLAG_OUTPUT_SIZE = 2;
	private TileEntityTerminal board;
	
	private static final byte memoryMapFinal[] = {
		MAP_BYTE, MAP_NONE, // 0x04: FLAGS
		MAP_BYTE, // 0x06: INTERRUPT LANE
		MAP_NONE, // 0x07: RESERVED
		MAP_SHORT, // 0x08-0x09: WRITE CHARACTER
		MAP_SHORT, // 0x0A-0x0B: SET CHARACTER COLOR (NO CHAR IS TYPED OUT)
		MAP_SHORT | MAP_NO_WRITE, // 0x0C-0x0D: GET LAST KEY TYPED
		MAP_BYTE | MAP_NO_READ, // 0x0E: SEND NEWLINE
		MAP_NONE, // 0x0F: RESERVED
		MAP_SHORT, // 0x10-0x11: SET COLOR IN PALETTE
		MAP_BYTE // 0x12: SET PALETTE NUMBER TO EDIT
	};
	
	public IOHandlerTerminal(TileEntityTerminal tileEntityTerminal) {
		super((short)0xA51E, (byte)0x01, (byte)0x01, memoryMapFinal);
		this.board = tileEntityTerminal;
		this.flags = new boolean[8];
		this.lastKeys = new short[0x18];
		intregs[0x0A] = 15; // Set FG
		intregs[0x0B] = 0; // Set BG
	}
	
	protected boolean flags[];
	protected short lastKeys[];
	protected short lastKeyPos = 0;

	@Override
	public void onReadByte(ICPU cpu, int addr) {
		switch(addr) {
			case 0x04: // FLAGS
				int value = 0;
				for(int i = 0; i < 8; i++)
					value |= (flags[i]?1:0)<<i;
				intregs[addr] = (byte)value;
				break;
		}
	}
	
	@Override
	public void onReadShortBegin(ICPU cpu, int addr) {
		switch(addr) {
			case 0x0C: // BEGIN KEY READ
				synchronized(lastKeys) {
					short key = lastKeys[0];
					intregs[0x0C] = (byte)(key & 0xFF);
					intregs[0x0D] = (byte)((key >> 8) & 0xFF);
					if(lastKeyPos > 0) {
						System.arraycopy(lastKeys, 1, lastKeys, 0, lastKeys.length - 1);
						lastKeyPos--;
					}
					lastKeys[lastKeyPos] = 0;
					break;
				}
		}
	}
	
	@Override
	public void onWriteByte(ICPU cpu, int addr) {
		switch(addr) {
			case 0x04: // FLAGS
				int value = (int)0xFF & intregs[addr];
				for(int i = 0; i < 8; i++)
					flags[i] = (value&(1<<i)) > 0;
				board.setHardwareEcho(flags[FLAG_HARDWARE_ECHO]);
				break;
			case 0x0E: // NEWLINE
				board.newline(true);
				break;
		}
	}
	
	@Override
	public void onWriteShortBegin(ICPU cpu, int addr) {
		if(!flags[FLAG_OUTPUT_SIZE]) return;
		switch(addr) {
			case 0x08: // FINISH CHARACTER AS BYTE
				board.print(getColor(), (short)((short)intregs[addr]&0xFF), true);
				break;
		}
	}
	
	public short getColor() {
		return (short)((((int)0xFF & intregs[0x0B]) << 8) | ((int)0xFF & intregs[0x0A]));
	}
	
	@Override
	public void onWriteShortEnd(ICPU cpu, int addr) {
		switch(addr) {
			case 0x09: // FINISH CHARACTER
				board.print(getColor(), readShort(0x08), true);
				break;
			case 0x11: // FINISH COLOR
				board.setPalette((int)0xFF & intregs[0x12], readShort(0x10));
				break;
		}
	}
	
	public boolean addKey(ICPU cpu, short key) {
		int interruptLane = (int)0xFF & intregs[0x06];
		synchronized(lastKeys) {
			System.arraycopy(lastKeys, 0, lastKeys, 1, lastKeys.length - 1);
			lastKeys[0] = key;
			lastKeyPos++;
		}
		if(flags[FLAG_INTERRUPT] && interruptLane >= 0 && interruptLane < 28)
			cpu.interrupt(interruptLane);
		return flags[FLAG_HARDWARE_ECHO];
	}
}


