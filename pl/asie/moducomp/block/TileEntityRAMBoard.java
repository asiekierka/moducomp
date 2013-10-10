package pl.asie.moducomp.block;

import java.awt.Container;

import pl.asie.moducomp.api.IItemMemory;
import pl.asie.moducomp.api.IMemoryControllerProvider;
import pl.asie.moducomp.api.ITileEntityPeripheral;
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
	private MemoryControllerSlot memory;
	public TileEntityRAMBoard() {
		super(16, 1, "block.moducomp.ram_board");
		memory = new MemoryControllerSlot();
		reset();
	}
	
	public void reset() {
		for(int i = 0; i < 16; i++)
			updateMemorySlot(i);
	}
	
	public void updateMemorySlot(int slot) {
		ItemStack stack = this.getStackInSlot(slot);
		IMemory memorySlot = null;
		if(stack != null && stack.getItem() instanceof IItemMemory) {
			IItemMemory stackHandler = (IItemMemory)stack.getItem();
			memorySlot = stackHandler.createNewMemoryHandler(stack);
		}
		memory.setSlot(slot, memorySlot);
	}

	@Override
	public void onInventoryChanged(int slot) {
		super.onInventoryChanged(slot);
		updateMemorySlot(slot);
	}

	@Override
	public IMemoryController getMemoryController() {
		return memory;
	}
}
