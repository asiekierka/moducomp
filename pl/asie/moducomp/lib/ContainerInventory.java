package pl.asie.moducomp.lib;

import net.minecraft.util.*;
import net.minecraft.block.*;
import net.minecraft.world.World;
import net.minecraft.inventory.*;
import net.minecraft.entity.player.*;
import net.minecraft.item.*;
import net.minecraft.tileentity.TileEntity;

public class ContainerInventory extends Container {
	protected int iSize;
	protected TileEntity tileEntity;
	public ContainerInventory(int size, TileEntity entity) {
		iSize = size;
		this.tileEntity = entity;
	}
	@Override
	public boolean canInteractWith(EntityPlayer player) { return true; }
	@Override
	public ItemStack transferStackInSlot(EntityPlayer player, int slot) {
		ItemStack stack = null;
		Slot slotObject = (Slot)inventorySlots.get(slot);
		if(slotObject != null && slotObject.getHasStack()) {
			ItemStack stackInSlot = slotObject.getStack();
			stack = stackInSlot.copy();
			if(slot < this.iSize) {
				if(!this.mergeItemStack(stackInSlot, this.iSize, inventorySlots.size(), true)) {
					return null;
				}
			}
			else if(!this.mergeItemStack(stackInSlot, 0, this.iSize, false)) {
				return null;
			}
			if(stackInSlot.stackSize == 0) {
				slotObject.putStack(null);
			} else {
				slotObject.onSlotChanged();
			}
		}
		return stack;
	}

	protected void bindPlayerInventory(InventoryPlayer inventoryPlayer, int startX, int startY) {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                addSlotToContainer(new Slot(inventoryPlayer, j + i * 9 + 9,
                        startX + j * 18, startY + i * 18));
            }
        }
        for (int i = 0; i < 9; i++) {
            addSlotToContainer(new Slot(inventoryPlayer, i, startX + i * 18, startY + 58));
        }
	}
}