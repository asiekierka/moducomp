package pl.asie.moducomp.peripheral;

import pl.asie.moducomp.api.computer.ICPU;
import pl.asie.moducomp.api.computer.IMemory;
import pl.asie.moducomp.block.TileEntityMainBoard;
import pl.asie.moducomp.gui.text.TextWindow;

public class IOHandlerDebugMC implements IMemory
{	
	private TileEntityMainBoard board;
	
	public IOHandlerDebugMC(TileEntityMainBoard board) {
		this.board = board;
	}
	
	protected byte data[];
	protected int size;

	private byte config[] = {(byte)0x1E, (byte)0xA5, 0x01, (byte)0xFF};

	public byte read8(ICPU cpu, int addr)
	{
		return this.config[addr & 3];
	}
	
	private byte lowChar;
	
	public void write8(ICPU cpu, int addr, byte val)
	{
		if(addr == 0xFE) {
			System.out.write(val);
		}
	}

	@Override
	public int length() {
		return 0x100;
	}
}


