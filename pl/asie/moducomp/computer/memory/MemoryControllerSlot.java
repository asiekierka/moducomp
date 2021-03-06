package pl.asie.moducomp.computer.memory;

import pl.asie.moducomp.api.computer.ICPU;
import pl.asie.moducomp.api.computer.IMemory;
import pl.asie.moducomp.api.computer.IMemoryController;

public class MemoryControllerSlot implements IMemoryController
{
	protected IMemory memorySlots[] = new IMemory[16];
	protected IMemory slots[] = new IMemory[16];
	protected IMemory deviceSlots[] = new IMemory[16];

	public MemoryControllerSlot() {
	}

	public void setSlot(int idx, IMemory slot) {
		if(idx < 0 && idx > 15) return;
		this.slots[idx] = slot;
		if(idx == 0) {
			this.memorySlots[0] = slot;
			this.memorySlots[15] = slot;
		} else if(idx <= 14) {
			this.memorySlots[idx] = slot;
		}
	}
	
	public void setROM(IMemory slot) {
		setSlot(15, slot);
	}
	
	public void setDeviceSlot(int idx, IMemory slot) {
		if(idx < 0 && idx > 15) return;
		this.deviceSlots[idx] = slot;
	}

	public IMemory getDeviceSlot(int idx) {
		if(idx < 0 && idx > 15) return null;
		return this.deviceSlots[idx];
	}

	public int setAddress(ICPU cpu, int addr) {
		addr &= 0xFFFFF;
		switch(cpu.getAddressBitLength()) {
			default:
				return 0;
			case 20:
				return addr;
			case 16:
				return (addr & 0xFFFF) | 0xF0000;
		}
	}
	public byte read8(ICPU cpu, int addr)
	{
		addr = setAddress(cpu, addr);

		if(addr < 0xFC000) {
			IMemory slot = this.memorySlots[(addr >> 16) & 0xF];
			if(slot == null) return (byte)0xFF;
			synchronized(slot) {
				return slot.read8(cpu, addr & 0xFFFF);
			}
		} else if(addr <= 0xFCFFF) { // Reserved
			return (byte)0xFF;
		} else if(addr <= 0xFDFFF) { // Devices
			IMemory slot = this.deviceSlots[(addr>>8) & 15];
			if(slot != null) synchronized(slot) {
				return slot.read8(cpu, addr & 0xFF);
			} else return (byte)0xFF;
		} else { // ROM
			IMemory slot = this.slots[15];
			if(slot != null) synchronized(slot) {
				return slot.read8(cpu, addr & 0x1FFF);
			} else return (byte)0xFF;
		}
	}

	public void write8(ICPU cpu, int addr, byte val)
	{
		addr = setAddress(cpu, addr);
		
		if(addr < 0xFC000) {
			IMemory slot = this.memorySlots[(addr >> 16) & 0xF];
			if(slot != null)
				synchronized(slot) {
					slot.write8(cpu, addr & 0xFFFF, val);
				}
		} else if(addr <= 0xFCFFF) { // Reserved
			// Do nothing
		} else if(addr <= 0xFDFFF) { // Devices
			IMemory slot = this.deviceSlots[(addr>>8) & 15];
			if(slot != null)
				synchronized(slot) {
					slot.write8(cpu, addr & 0xFF, val);
				}
		} else { // ROM
			IMemory slot = this.slots[15];
			if(slot != null)
				synchronized(slot) {
					slot.write8(cpu, addr & 0x1FFF, val);
				}
		}
	}
	
	public int length() {
		return -1;
	}
}

