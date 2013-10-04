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
		this.deviceSlots[idx] = slot;
	}

	public byte read8(ICPU cpu, int addr)
	{
		addr &= 0xFFFFF;

		if(addr < 0xFC000) {
			IMemory slot = this.memorySlots[(addr >> 16) & 0xF];
			return (slot == null ? (byte)0xFF : slot.read8(cpu, addr & 0xFFFF));
		} else if(addr <= 0xFCFFF) { // Reserved
			return (byte)0xFF;
		} else if(addr <= 0xFDFFF) { // Devices
			IMemory slot = this.deviceSlots[(addr>>8) & 15];
			return (slot == null ? (byte)0xFF : slot.read8(cpu, addr & 0xFF));
		} else { // ROM
			IMemory slot = this.slots[15];
			return (slot == null ? (byte)0xFF : slot.read8(cpu, addr & 0x1FFF));
		}
	}

	public void write8(ICPU cpu, int addr, byte val)
	{
		addr &= 0xFFFFF;
		
		if(addr < 0xFC000) {
			IMemory slot = this.memorySlots[(addr >> 16) & 0xF];
			if(slot != null)
				slot.write8(cpu, addr & 0xFFFF, val);
		} else if(addr <= 0xFCFFF) { // Reserved
			// Do nothing
		} else if(addr <= 0xFDFFF) { // Devices
			IMemory slot = this.deviceSlots[(addr>>8) & 15];
			if(slot != null)
				slot.write8(cpu, addr & 0xFF, val);
		} else { // ROM
			IMemory slot = this.slots[15];
			if(slot != null)
				slot.write8(cpu, addr & 0x1FFF, val);
		}
	}
	
	public int length() {
		return 0x100000; // 1MB
	}
}

