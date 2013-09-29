package pl.asie.moducomp.api;

import net.minecraft.item.ItemStack;

public interface ITape {
	public int seek(ItemStack stack, int bytes, TapeDirection direction);
	public byte getByte(ItemStack stack);
	public int getLength(ItemStack stack);
	public void reset(ItemStack stack);
	public float getSpeed();
}
