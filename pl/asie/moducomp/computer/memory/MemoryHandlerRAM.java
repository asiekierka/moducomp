package pl.asie.moducomp.computer.memory;

import pl.asie.moducomp.api.IMemoryHandler;

public class MemoryHandlerRAM implements IMemoryHandler {
	private byte[] data;
	private final int size;
	
	public MemoryHandlerRAM(int size) {
		this.size = size;
		data = new byte[size];
	}
	
	@Override
	public int read8(int addr) {
		if(addr < 0 || addr >= this.size) return 0;
		else return 0xFF & data[addr];
	}

	@Override
	public int read16(int addr) {
		if(addr < 0 || addr >= this.size) return 0;
		else return read8(addr) | (read8(addr+1)<<8);
	}

	@Override
	public void write8(int addr, byte val) {
		if(addr < 0 || addr >= this.size) return;
		data[addr] = val;
	}

	@Override
	public void write16(int addr, short val) {
		if(addr < 0 || addr >= this.size) return;
		data[addr] = (byte)(val&0xFF);
		data[addr+1] = (byte)(val>>8);
	}

	@Override
	public int length() {
		return this.size;
	}

}
