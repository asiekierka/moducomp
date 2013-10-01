package pl.asie.moducomp.block;

import java.awt.Container;

import pl.asie.moducomp.item.ItemPaperTape;
import pl.asie.moducomp.lib.TileEntityInventory;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class TileEntityRAMBoard extends TileEntityInventory {
	public TileEntityRAMBoard() {
		super(16, 1, "block.moducomp.ram_board");
	}
}
