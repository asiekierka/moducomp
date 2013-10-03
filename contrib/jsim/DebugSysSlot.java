public class DebugSysSlot extends Memory
{
	protected byte data[];
	protected int size;

	private byte config[] = {(byte)0xDF, (byte)0x99, 0x01, (byte)0xFF};

	public DebugSysSlot()
	{
		//
	}

	public byte read8(CPU cpu, int addr)
	{
		if((addr & 0x200000) != 0)
		{
			return this.config[addr & 3];
		} else {
			return (byte)0xFF;
		}
	}
	
	public void write8(CPU cpu, int addr, byte val)
	{
		if((addr & 0x200000) == 0)
			/* do nothing */;
		else {
			addr &= 0xFF;
			//System.out.printf("debug %02X %02X\n", addr, 0xFF & (int)val);
			if(addr == 0xFE)
			{
				System.out.printf("%c", (char)(0xFF & (int)val));
				System.out.flush();
			}
		}
	}
}


