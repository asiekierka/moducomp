package pl.asie.moducomp.block;

import java.awt.Container;

import pl.asie.moducomp.api.IItemMemory;
import pl.asie.moducomp.api.IMemoryControllerProvider;
import pl.asie.moducomp.api.computer.IMemory;
import pl.asie.moducomp.api.computer.IMemoryController;
import pl.asie.moducomp.computer.memory.MemoryControllerSlot;
import pl.asie.moducomp.item.ItemPaperTape;
import pl.asie.moducomp.lib.Helper;
import pl.asie.moducomp.lib.TileEntityInventory;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class TileEntityMainBoard extends TileEntityInventory
{
	public TileEntityMainBoard() {
		super(1, 1, "block.moducomp.main_board");
	}
	
	public IMemoryController getMemoryController() {
		for(int[] dir : Helper.DIRECTIONS) {
			TileEntity te = worldObj.getBlockTileEntity(this.xCoord + dir[0], this.yCoord + dir[1], this.zCoord + dir[2]);
			if(te instanceof IMemoryControllerProvider) {
				IMemoryControllerProvider mem = (IMemoryControllerProvider)te;
				return mem.getMemoryController();
			}
		}
		return null;
	}
}
