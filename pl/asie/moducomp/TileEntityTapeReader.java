package pl.asie.moducomp;

import java.awt.Container;

import pl.asie.moducomp.lib.TileEntityInventory;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class TileEntityTapeReader extends TileEntityInventory {
	public TileEntityTapeReader() {
		super(1, 1, "block.moducomp.tape_reader");
	}
	
	protected void setBit(int position, byte offset, byte shift) {
		ItemStack stack = this.getStackInSlot(0);
		if(stack != null && stack.getItem() instanceof ItemPaperTape) {
			ItemPaperTape tapeHandler = (ItemPaperTape)stack.getItem();
			tapeHandler.setPosition(stack, position);
			byte value = tapeHandler.getByte(stack, offset);
			value |= (byte)1<<shift;
			tapeHandler.setByte(stack, value, offset);
		}
	}
	
	protected int getPosition() {
		ItemStack stack = this.getStackInSlot(0);
		if(stack != null && stack.getItem() instanceof ItemPaperTape) {
			ItemPaperTape tapeHandler = (ItemPaperTape)stack.getItem();
			return tapeHandler.getPosition(stack);
		}
		return 0;
	}
	
	protected void setPosition(int position) {
		ItemStack stack = this.getStackInSlot(0);
		if(stack != null && stack.getItem() instanceof ItemPaperTape) {
			ItemPaperTape tapeHandler = (ItemPaperTape)stack.getItem();
			tapeHandler.setPosition(stack, position);
		}
	}
}
