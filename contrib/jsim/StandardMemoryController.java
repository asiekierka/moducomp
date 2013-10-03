public class StandardMemoryController extends Memory
{
	protected Memory slots[] = new Memory[15];
	protected Memory rom;
	protected Memory sys_slot;

	public void setSlot(int idx, Memory slot)
	{
		this.slots[idx] = slot;
	}

	public void setROM(Memory rom)
	{
		this.rom = rom;
	}

	public void setSysSlot(Memory sys_slot)
	{
		this.sys_slot = sys_slot;
	}

	public byte read8(CPU cpu, int addr)
	{
		addr &= 0xFFFFF;

		// 48KB mirror
		if(addr >= 0xF0000 && addr <= 0xFBFFF)
			addr &= 0xFFFF;

		int bank = (addr>>16);

		if(bank < 0xF)
		{
			Memory slot = this.slots[bank];
			return (slot == null ? (byte)0xFF : slot.read8(cpu, addr & 0xFFFF));
		} else if(addr <= 0xFCFFF) {
			return (byte)0xFF;
		} else if(addr <= 0xFDFFF) {
			int subbank = (addr>>12) & 15;

			if(subbank < 0xF)
			{
				Memory slot = this.slots[subbank];
				return (slot == null ? (byte)0xFF : slot.read8(cpu, (addr & 0xFF) | 0x200000));
			} else {
				Memory slot = this.sys_slot;
				return (slot == null ? (byte)0xFF : slot.read8(cpu, (addr & 0xFF) | 0x200000));
			}
		} else {
			Memory slot = this.rom;
			return (slot == null ? (byte)0xFF : slot.read8(cpu, addr & 0x1FFF));
		}
	}

	public void write8(CPU cpu, int addr, byte val)
	{
		addr &= 0xFFFFF;

		// 48KB mirror
		if(addr >= 0xF0000 && addr <= 0xFBFFF)
			addr &= 0xFFFF;

		int bank = (addr>>16);

		if(bank < 0xF)
		{
			Memory slot = this.slots[bank];
			if(slot != null)
				slot.write8(cpu, addr & 0xFFFF, val);
		} else if(addr <= 0xFCFFF) {
			// do nothing
		} else if(addr <= 0xFDFFF) {
			int subbank = (addr>>12) & 15;

			Memory slot = null;
			if(subbank < 0xF)
			{
				slot = this.slots[subbank];
			} else {
				slot = this.sys_slot;
			}

			if(slot != null)
				slot.write8(cpu, (addr & 0xFF) | 0x200000, val);
		} else {
			Memory slot = this.rom;
			if(slot != null)
				slot.write8(cpu, addr & 0x1FFF, val);
		}
	}
}

