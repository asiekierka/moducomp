package pl.asie.moducomp.api;

import net.minecraft.item.ItemStack;

public interface ITape {
	public int seek(ItemStack stack, int bytes);
	public byte getByte(ItemStack stack, int offset);
	public void setByte(ItemStack stack, byte value, int offset);
	public boolean isValid(ItemStack stack, int offset);
	public int getLength(ItemStack stack);
	public void reset(ItemStack stack);
	public float getSpeed();
}
