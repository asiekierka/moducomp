package pl.asie.moducomp.api;

import pl.asie.moducomp.api.computer.ICPU;
import pl.asie.moducomp.api.computer.IMemory;
import pl.asie.moducomp.api.computer.IMemoryController;

public interface IEntityPeripheral {
	public IMemory init(ICPU cpu, IMemoryController memoryController);
	public void deinit(ICPU cpu);
}