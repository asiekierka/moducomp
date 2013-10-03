package pl.asie.moducomp.block;

import java.awt.Container;

import pl.asie.moducomp.api.IItemMemory;
import pl.asie.moducomp.api.IMemoryControllerProvider;
import pl.asie.moducomp.api.computer.IMemory;
import pl.asie.moducomp.api.computer.IMemoryController;
import pl.asie.moducomp.computer.memory.MemoryControllerSlot;
import pl.asie.moducomp.item.ItemPaperTape;
import pl.asie.moducomp.lib.TileEntityInventory;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class TileEntityRAMBoard extends TileEntityInventory implements IMemoryControllerProvider
{
	public TileEntityRAMBoard() {
		super(16, 1, "block.moducomp.ram_board");
	}
	
	public IMemoryController getMemoryController() {
		MemoryControllerSlot memory = new MemoryControllerSlot();
		for(int i = 0; i < 16; i++) { // 16 slots
			ItemStack stack = this.getStackInSlot(i);
			if(stack != null && stack.getItem() instanceof IItemMemory) {
				IItemMemory stackHandler = (IItemMemory)stack.getItem();
				memory.setSlot(i, stackHandler.createNewMemoryHandler(stack));
			}
		}
		return (IMemoryController) memory;
	}
}
