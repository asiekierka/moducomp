package pl.asie.moducomp.lib;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.*;
import net.minecraft.nbt.*;
import net.minecraft.tileentity.TileEntity;

public class TileEntityInventory extends TileEntity implements IInventory {
	protected ItemStack[] inventory;
	protected int maxStackSize;
	protected String inventoryName;

	public TileEntityInventory(int size, int mss, String name) {
		inventory = new ItemStack[size];
		maxStackSize = size;
		if(maxStackSize < 1) maxStackSize = 1;
		if(maxStackSize > 64) maxStackSize = 64;
		inventoryName = name;
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

	public int getSizeInventory() { return inventory.length; }
	public String getInvName() { return inventoryName; }
	public int getInventoryStackLimit() { return maxStackSize; }
	
	public boolean isInvNameLocalized() {
		// Checks for the occurence of any uppercase letters and/or the lack of dots.
		boolean hasUpper = !inventoryName.equals(inventoryName.toLowerCase());
		boolean hasDots = inventoryName.indexOf(".") >= 0;
		return hasUpper || !hasDots;
	}
	public ItemStack getStackInSlot(int slot) {
		if(slot < 0 || slot >= inventory.length) return null;
		return inventory[slot];
	}
	public ItemStack getStackInSlotOnClosing(int slot) {
		ItemStack stack = getStackInSlot(slot);
		if(stack == null) return null;
		inventory[slot] = null;
		return stack;
	}
    public ItemStack decrStackSize(int par1, int par2)
    {
        if (this.inventory[par1] != null)
        {
            ItemStack itemstack;

            if (this.inventory[par1].stackSize <= par2)
            {
                itemstack = this.inventory[par1];
                this.inventory[par1] = null;
                this.onInventoryChanged();
                return itemstack;
            }
            else
            {
                itemstack = this.inventory[par1].splitStack(par2);

                if (this.inventory[par1].stackSize == 0)
                {
                    this.inventory[par1] = null;
                }

                this.onInventoryChanged();
                return itemstack;
            }
        }
        else
        {
            return null;
        }
    }
    /*
	public ItemStack decrStackSize(int slot, int count) {
		ItemStack stack = getStackInSlot(slot);
		ItemStack stackO = stack;
		if(inventory[slot] != null) {
			if(stack.stackSize > count)
				stack = stack.splitStack(count);
			if(stack.stackSize <= count || stack.stackSize == 0)
				stack = null;
		}
		if(stack != stackO) { setInventorySlotContents(slot,stack); }
		return stack;
	}
	*/
	public boolean isItemValidForSlot(int i, ItemStack stack) {
		if(i < 0 || i >= inventory.length) return false;
		if(stack != null && stack.stackSize > maxStackSize) return false;
		return true;
	}
	public void setInventorySlotContents(int slot, ItemStack stack) {
		inventory[slot] = stack;
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
                        if (slot >= 0 && slot < inventory.length) {
                                inventory[slot] = ItemStack.loadItemStackFromNBT(tag);
                        }
                }
        }

        @Override
        public void writeToNBT(NBTTagCompound tagCompound) {
                super.writeToNBT(tagCompound);
                NBTTagList itemList = new NBTTagList();
                for (int i = 0; i < inventory.length; i++) {
                        ItemStack stack = inventory[i];
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