package pl.asie.moducomp.api;

import net.minecraft.item.ItemStack;

public interface IItemMemory {
	public int getLength(ItemStack stack);
	public IMemoryHandler createNewMemoryHandler(ItemStack stack);
}
