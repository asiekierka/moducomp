public class RandomAccessMemory extends Memory
{
	protected byte data[];
	protected int size;

	private byte config[] = {(byte)0xDF, (byte)0x99, 0x00, (byte)0xFF, 0x10, (byte)0xFF, (byte)0xFF, (byte)0xFF};

	public RandomAccessMemory(int size)
	{
		// TODO: assert power of two
		this.size = size;
		this.data = new byte[size];
		this.config[4] = (byte)(0x10 | 0x00); // TODO: get log2 of size
	}

	public byte read8(CPU cpu, int addr)
	{
		if((addr & 0x200000) != 0)
		{
			return this.config[addr & 7];
		} else {
			return this.data[addr & (this.size-1)];
		}
	}
	
	public void write8(int addr, byte val)
	{
		if((addr & 0x200000) == 0)
			this.data[addr & (this.size-1)] = val;
	}
}

