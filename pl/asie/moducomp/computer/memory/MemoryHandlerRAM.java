package pl.asie.moducomp.computer.memory;

import pl.asie.moducomp.api.computer.ICPU;
import pl.asie.moducomp.api.computer.IMemory;

public class MemoryHandlerRAM implements IMemory {
	private byte[] data;
	private final int size;
	
	public MemoryHandlerRAM(int size) {
		this.size = size;
		data = new byte[size];
	}
	
	@Override
	public byte read8(ICPU cpu, int addr) {
		if(addr < 0 || addr >= this.size) return 0;
		else return data[addr];
	}

	@Override
	public void write8(ICPU cpu, int addr, byte val) {
		if(addr < 0 || addr >= this.size) return;
		data[addr] = val;
	}

	@Override
	public int length() {
		return this.size;
	}

}
