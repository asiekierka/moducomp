package pl.asie.moducomp.api.computer;

import pl.asie.moducomp.ModularComputing;

public abstract class PeripheralBasic implements IMemory {
	public static final int MAP_NONE = 0;
	public static final int MAP_BYTE = 1;
	public static final int MAP_SHORT = 2;
	public static final int MAP_SIZE = 3;
	public static final int MAP_NO_READ = 4;
	public static final int MAP_NO_WRITE = 8;
	public static final int MAP_SYNC_CLIENT = 16;
	
	public PeripheralBasic(short author, byte id, byte deviceClass, byte[] memoryMap) {
		this.intregs = new byte[256];
		this.intregs[0] = (byte)(author&0xFF);
		this.intregs[1] = (byte)(author>>8);
		this.intregs[2] = id;
		this.intregs[3] = deviceClass;
		this.mapAddress = new short[memoryMap.length];
		this.memoryMap = new byte[256];
		for(int i = 0; i < 4; i++) {
			this.memoryMap[i] = MAP_BYTE | MAP_NO_WRITE;
		}
		int j = 4;
		for(int i = 0; i < memoryMap.length; i++) {
			if((memoryMap[i] & MAP_SIZE) == MAP_SHORT && ((j&1) == 1)) {
				// Align
				j++;
			}
			this.mapAddress[i] = (short)j;
			this.memoryMap[j] = memoryMap[i]; j++;
			if((memoryMap[i] & MAP_SIZE) == MAP_SHORT) {
				this.memoryMap[j] = memoryMap[i]; j++;
			}
		}
		if(j < 256) {
			for(; j < 256; j++) {
				this.memoryMap[j] = MAP_NONE | MAP_NO_READ | MAP_NO_WRITE;
			}
		}
	}
	
	protected final byte[] memoryMap;
	protected final short[] mapAddress;
	protected byte[] intregs;
	
	public short readShort(int addr) {
		return (short)((((int)0xFF & intregs[addr+1]) << 8) | ((int)0xFF & intregs[addr]));
	}
	
	public void writeShort(int addr, short val) {
		intregs[addr] = (byte)(0xFF & val);
		intregs[addr+1] = (byte)(0xFF & (val>>8));
	}
	
	// Executed when a byte is being read
	public void onReadByte(ICPU cpu, int addr) {
	}
	
	// Executed when first byte of short is read
	public void onReadShortBegin(ICPU cpu, int addr) {
	}
	
	// Executed when second byte of short is read
	public void onReadShortEnd(ICPU cpu, int addr) {
	}
	
	// Executed after a byte is written
	public void onWriteByte(ICPU cpu, int addr) {
	}
	
	// Executed after a byte is written
	public void onWriteShortBegin(ICPU cpu, int addr) {
	}
	
	// Executed after a byte is written
	public void onWriteShortEnd(ICPU cpu, int addr) {
	}
	
	public byte read8(ICPU cpu, int addr) {
		byte mapData = memoryMap[addr];
		if((mapData & MAP_NO_READ) != 0 || (mapData & MAP_SIZE) == MAP_NONE) return (byte)0xFF;
		else {
			if((mapData & MAP_SIZE) == MAP_BYTE) onReadByte(cpu, addr);
			else if((mapData & MAP_SIZE) == MAP_SHORT) {
				switch(addr&1) {
					case 0:
						onReadShortBegin(cpu, addr);
						break;
					case 1:
						onReadShortEnd(cpu, addr);
						break;
				}
			}
			return intregs[addr];
		}
	}
	
	public void write8(ICPU cpu, int addr, byte data) {
		byte mapData = memoryMap[addr];
		if((mapData & MAP_NO_WRITE) != 0 || (mapData & MAP_SIZE) == MAP_NONE) return;
		else {
			intregs[addr] = data;
			if((mapData & MAP_SIZE) == MAP_BYTE) onWriteByte(cpu, addr);
			else if((mapData & MAP_SIZE) == MAP_SHORT) {
				switch(addr&1) {
					case 0:
						onWriteShortBegin(cpu, addr);
						break;
					case 1:
						onWriteShortEnd(cpu, addr);
						break;
				}
			}
		}
	}
	
	public int length() {
		return 0x100;
	}
}
