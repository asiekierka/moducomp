package pl.asie.moducomp.api;

public interface IMemoryHandler {
	public int read8(int addr);
	public int read16(int addr);
	public void write8(int addr, byte data);
	public void write16(int addr, short data);
	public int length();
}
