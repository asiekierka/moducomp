package pl.asie.moducomp.lib;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.*;
import net.minecraft.nbt.*;
import net.minecraft.tileentity.TileEntity;

public class TileEntityInventory extends TileEntity implements IInventory {
	protected ItemStack[] inv;
	protected int maxStackSize;
	protected String invName;

	public TileEntityInventory(int size, int mss, String name) {
		inv = new ItemStack[size];
		maxStackSize = size;
		if(maxStackSize < 1) maxStackSize = 1;
		if(maxStackSize > 64) maxStackSize = 64;
		invName = name;
	}
	public TileEntityInventory(int size, int mss) {
		this(size, mss, "modular.inventory");
	}
	public TileEntityInventory(int size, String name) {
		this(size, 64, name);
	}
	public TileEntityInventory(int size) {
		this(size, 64);
	}

	// Called every time a slot in inventory is changed.
	// I recommend you override the latter version, which is called as well but includes the slot, if you're
	// looking for specific ones.
	public void onInventoryChanged() { }
	public void onInventoryChanged(int slot) { }

	public void openChest() {}
	public void closeChest() {}

	public int getSizeInventory() { return inv.length; }
	public String getInvName() { return invName; }
	public int getInventoryStackLimit() { return maxStackSize; }
	
	public boolean isInvNameLocalized() {
		// Checks for the occurence of any uppercase letters and/or the lack of dots.
		boolean hasUpper = !invName.equals(invName.toLowerCase());
		boolean hasDots = invName.indexOf(".") >= 0;
		return hasUpper || !hasDots;
	}
	public ItemStack getStackInSlot(int slot) {
		if(slot < 0 || slot >= inv.length) return null;
		return inv[slot];
	}
	public ItemStack getStackInSlotOnClosing(int slot) {
		ItemStack stack = getStackInSlot(slot);
		if(stack == null) return null;
		inv[slot] = null;
		return stack;
	}
	public ItemStack decrStackSize(int slot, int count) {
		ItemStack stack = getStackInSlot(slot);
		ItemStack stackO = stack;
		if(inv[slot] != null) {
			if(stack.stackSize > count)
				stack = stack.splitStack(count);
			if(stack.stackSize <= count || stack.stackSize == 0)
				stack = null;
		}
		if(stack != stackO) { setInventorySlotContents(slot,stack); }
		return stack;
	}
	
	public boolean isItemValidForSlot(int i, ItemStack stack) {
		if(i < 0 || i >= inv.length) return false;
		if(stack != null && stack.stackSize > maxStackSize) return false;
		return true;
	}
	public void setInventorySlotContents(int slot, ItemStack stack) {
		inv[slot] = stack;
		if(stack != null && stack.stackSize > this.getInventoryStackLimit()) {
			stack.stackSize = this.getInventoryStackLimit();
		}
		this.onInventoryChanged();
		this.onInventoryChanged(slot);
	}
	public boolean isUseableByPlayer(EntityPlayer player) {
		return this.worldObj.getBlockTileEntity(this.xCoord, this.yCoord, this.zCoord) != this
			? false : player.getDistanceSq( (double)this.xCoord+0.5D,
							(double)this.yCoord+0.5D,
							(double)this.zCoord+0.5D ) <= 64.0D;
	}

	// http://www.minecraftforge.net/wiki/Containers_and_GUIs
        @Override
        public void readFromNBT(NBTTagCompound tagCompound) {
                super.readFromNBT(tagCompound);
                NBTTagList tagList = tagCompound.getTagList("Inventory");
                for (int i = 0; i < tagList.tagCount(); i++) {
                        NBTTagCompound tag = (NBTTagCompound) tagList.tagAt(i);
                        byte slot = tag.getByte("Slot");
                        if (slot >= 0 && slot < inv.length) {
                                inv[slot] = ItemStack.loadItemStackFromNBT(tag);
                        }
                }
        }

        @Override
        public void writeToNBT(NBTTagCompound tagCompound) {
                super.writeToNBT(tagCompound);
                NBTTagList itemList = new NBTTagList();
                for (int i = 0; i < inv.length; i++) {
                        ItemStack stack = inv[i];
                        if (stack != null) {
                                NBTTagCompound tag = new NBTTagCompound();
                                tag.setByte("Slot", (byte) i);
                                stack.writeToNBT(tag);
                                itemList.appendTag(tag);
                        }
                }
                tagCompound.setTag("Inventory", itemList);
        }
}