package pl.asie.moducomp.api;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.gui.GuiScreen;
import pl.asie.moducomp.api.computer.ICPU;
import pl.asie.moducomp.api.computer.IMemory;
import pl.asie.moducomp.api.computer.IMemoryController;

public interface ITileEntityPeripheral {
	public IMemory init(ICPU cpu, IMemoryController memoryController);
	public void deinit(ICPU cpu);
	
	public int getPreferredDeviceID();
}