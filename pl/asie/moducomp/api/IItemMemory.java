package pl.asie.moducomp.api;

import pl.asie.moducomp.api.computer.IMemory;
import net.minecraft.item.ItemStack;

public interface IItemMemory {
	public int getLength(ItemStack stack);
	public IMemory createNewMemoryHandler(ItemStack stack);
}
