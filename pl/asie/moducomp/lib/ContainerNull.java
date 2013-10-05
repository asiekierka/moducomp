package pl.asie.moducomp.lib;

import net.minecraft.util.*;
import net.minecraft.block.*;
import net.minecraft.world.World;
import net.minecraft.inventory.*;
import net.minecraft.entity.player.*;
import net.minecraft.item.*;
import net.minecraft.tileentity.TileEntity;

public class ContainerNull extends Container {
	public ContainerNull() {}
	@Override
	public boolean canInteractWith(EntityPlayer player) { return true; }
	@Override
	public ItemStack transferStackInSlot(EntityPlayer player, int slot) {
		return null;
	}
}