public abstract class Memory
{
	public static final int MEM_OPEN = -1;
	public static final int MEM_DYNA = -2;
	public static final int MEM_RECALC = -3;

	public byte read8(CPU cpu, int addr)
	{
		return (byte)0xFF;
	}

	public void write8(CPU cpu, int addr, byte val)
	{
		// Do nothing.
	}

	public int getShadowAddress(int addr)
	{
		return MEM_DYNA;
	}

	public int getShadowSize()
	{
		return 0;
	}
}

