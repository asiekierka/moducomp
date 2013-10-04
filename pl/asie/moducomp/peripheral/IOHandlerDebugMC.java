package pl.asie.moducomp.peripheral;

import pl.asie.moducomp.api.computer.ICPU;
import pl.asie.moducomp.api.computer.IMemory;
import pl.asie.moducomp.gui.text.TextWindow;

public class IOHandlerDebugMC implements IMemory
{	
	public IOHandlerDebugMC() {
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
		/*
			if(addr == 0xFE) lowChar = val;
			else if(addr == 0xFF) {
			int chr = (((int)0xFF & val) << 8) | ((int)0xFF & lowChar);
			window.print((short)chr);
			}
		TODO - wait for GM to add asm.hrl */
		//if(addr == 0xFE) window.print((short)((int)0xFF & val));
	}

	@Override
	public int length() {
		return 0x100;
	}
}


