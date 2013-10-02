package pl.asie.moducomp.computer.memory;

import pl.asie.moducomp.api.IMemoryHandler;

public class MemoryHandlerROM implements IMemoryHandler {
	private final byte[] data;
	private final int size;
	
	public MemoryHandlerROM(int size, byte[] data) {
		this.size = size;
		this.data = data;
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
	public void write8(int addr, byte val) { }

	@Override
	public void write16(int addr, short val) { }

	@Override
	public int length() {
		return this.size;
	}

}
