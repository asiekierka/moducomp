package pl.asie.moducomp;

import java.awt.Container;

import pl.asie.moducomp.lib.TileEntityInventory;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class TileEntityTapeReader extends TileEntityInventory {
	public TileEntityTapeReader() {
		super(1, 1, "block.moducomp.tape_reader");
	}
	
}
