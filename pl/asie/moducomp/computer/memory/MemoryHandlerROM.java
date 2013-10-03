package pl.asie.moducomp.computer.memory;

import pl.asie.moducomp.api.computer.ICPU;
import pl.asie.moducomp.api.computer.IMemory;

public class MemoryHandlerROM implements IMemory {
	private final byte[] data;
	private final int size;
	
	public MemoryHandlerROM(int size, byte[] data) {
		this.size = size;
		this.data = data;
	}
	
	@Override
	public byte read8(ICPU cpu, int addr) {
		if(addr < 0 || addr >= this.size) return 0;
		else return data[addr];
	}

	@Override
	public void write8(ICPU cpu, int addr, byte val) { }

	@Override
	public int length() {
		return this.size;
	}

}
