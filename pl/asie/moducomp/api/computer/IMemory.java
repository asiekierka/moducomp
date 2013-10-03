package pl.asie.moducomp.api.computer;

public interface IMemory {
	public byte read8(ICPU cpu, int addr);
	public void write8(ICPU cpu, int addr, byte data);
	public int length();
}
