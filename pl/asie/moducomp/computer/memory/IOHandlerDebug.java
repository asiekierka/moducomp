package pl.asie.moducomp.computer.memory;

import pl.asie.moducomp.api.computer.ICPU;
import pl.asie.moducomp.api.computer.IMemory;

public class IOHandlerDebug implements IMemory
{
	protected byte data[];
	protected int size;

	private byte config[] = {(byte)0xDF, (byte)0x99, 0x01, (byte)0xFF};

	public byte read8(ICPU cpu, int addr)
	{
		if((addr & 0x200000) != 0)
		{
			return this.config[addr & 3];
		} else {
			return (byte)0xFF;
		}
	}
	
	public void write8(ICPU cpu, int addr, byte val)
	{
		if(addr == 0xFE)
		{
			System.out.write(val);
		}
	}

	@Override
	public int length() {
		return 0x100;
	}
}


