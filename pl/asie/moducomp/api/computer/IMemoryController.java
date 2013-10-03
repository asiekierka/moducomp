package pl.asie.moducomp.api.computer;

public interface IMemoryController extends IMemory {
	public void setDeviceSlot(int idx, IMemory device);
}