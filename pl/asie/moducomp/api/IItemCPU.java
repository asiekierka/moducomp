package pl.asie.moducomp.api;

import pl.asie.moducomp.api.computer.ICPU;
import net.minecraft.item.ItemStack;

public interface IItemCPU {
	public ICPU createNewCPUHandler(ItemStack stack);
}