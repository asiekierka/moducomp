package pl.asie.moducomp.peripheral;

import pl.asie.moducomp.api.computer.ICPU;
import pl.asie.moducomp.api.computer.IMemory;
import pl.asie.moducomp.block.TileEntityMainBoard;
import pl.asie.moducomp.gui.text.TextWindow;

public class IOHandlerTerminal implements IMemory
{	
	public static final int FLAG_HARDWARE_ECHO = 0;
	public static final int FLAG_INTTERUPT = 1;
	public static final int FLAG_OUTPUT_SIZE = 2;
	private TileEntityMainBoard board;
	
	public IOHandlerTerminal(TileEntityMainBoard board) {
		this.board = board;
		this.flags = new boolean[8];
	}
	
	protected byte data[];
	protected int size;
	protected boolean flags[];
	protected short lastKeys[];
	
	private byte config[] = {(byte)0x1E, (byte)0xA5, 0x01, (byte)0xFF};

	public byte read8(ICPU cpu, int addr)
	{
		if(addr < 4) {
			return this.config[addr & 3];
		} else if(addr == 0x08) { // Flags
			int value = 0;
			for(int i = 0; i < 8; i++)
				value |= (flags[i]?1:0)<<i;
			return (byte)value;
		} else if(addr >= 0x10 && addr < 0x3F) { // Last 12 keys
			return (byte)(lastKeys[addr>>1] >> ((addr & 1) > 0 ? 8 : 0));
		} else return (byte)0;
	}
	
	private byte lowChar;
	
	public void write8(ICPU cpu, int addr, byte val)
	{
		if(addr == 0x0A) { // Send low
			lowChar = val;
			if(flags[FLAG_OUTPUT_SIZE]) // Use bytes
				board.sendAndPrint((short)((short)lowChar & 0xFF));
		} else if(addr == 0x0B) { // Send high
			int chr = (((int)0xFF & val) << 8) | ((int)0xFF & lowChar);
			board.sendAndPrint((short)chr);
		} else if(addr == 0x08) { // Flags
			int value = (int)0xFF & val;
			for(int i = 0; i < 8; i++)
				flags[i] = (val&(1<<i)) > 0;
		}
	}
	
	public boolean addKey(ICPU cpu, short key) {
		System.arraycopy(lastKeys, 0, lastKeys, 1, lastKeys.length - 1);
		lastKeys[0] = key;
		return flags[FLAG_HARDWARE_ECHO];
	}

	@Override
	public int length() {
		return 0x100;
	}
}


