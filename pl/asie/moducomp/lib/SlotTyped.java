package pl.asie.moducomp.lib;

import net.minecraft.inventory.*;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class SlotTyped extends Slot {
	public Class<?> tclass;

	public SlotTyped(Class<?> target, IInventory iinv, int par3, int par4, int par5) {
		super(iinv,par3,par4,par5);
		tclass = target;
	}
	public boolean isItemValid(ItemStack is) {
		if(is != null && tclass.isInstance(is.getItem())) return true;
		return false;
	}
}