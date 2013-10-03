public class ReadOnlyMemory extends Memory
{
	protected byte data[];
	protected int size;
	
	// Java sucks balls
	private byte config[] = {(byte)0xDF, (byte)0x99, 0x00, (byte)0xFF, 0x00, (byte)0xFF, (byte)0xFF, (byte)0xFF};

	public ReadOnlyMemory(int size, byte[] data)
	{
		// TODO: assert power of two
		assert size == data.length;
		this.size = size;
		this.data = new byte[size];
		this.config[4] = (byte)(0x00 | 0x00); // TODO: get log2 of size

		for(int i = 0; i < size; i++)
			this.data[i] = data[i];
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
}


