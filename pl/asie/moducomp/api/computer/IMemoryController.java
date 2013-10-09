package pl.asie.moducomp.api.computer;

public interface IMemoryController extends IMemory {
	public void setSlot(int idx, IMemory device);
	public void setDeviceSlot(int idx, IMemory device);
	public IMemory getDeviceSlot(int idx);
}