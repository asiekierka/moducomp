package pl.asie.moducomp.api;

import pl.asie.moducomp.api.computer.IMemoryController;

public interface IMemoryControllerProvider {
	public IMemoryController getMemoryController();
	public void reset();
}