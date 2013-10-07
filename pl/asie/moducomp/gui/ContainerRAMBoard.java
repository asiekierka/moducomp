package pl.asie.moducomp.gui;

import java.util.logging.Level;

import pl.asie.moducomp.api.IItemMemory;
import pl.asie.moducomp.block.TileEntityRAMBoard;
import pl.asie.moducomp.lib.ContainerInventory;
import pl.asie.moducomp.lib.SlotTyped;
import pl.asie.moducomp.lib.TileEntityInventory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;

public class ContainerRAMBoard extends ContainerInventory {
	protected TileEntityRAMBoard tileEntity;

	public ContainerRAMBoard(InventoryPlayer inventoryPlayer, TileEntityRAMBoard te) {
		super(16, te);
		tileEntity = te;
		for(int i=0; i<16; i++) {
			addSlotToContainer(new SlotTyped(IItemMemory.class, tileEntity, i, 53 + (18 * (i&3)), 17 + (18 * (i>>2))));
		}
		bindPlayerInventory(inventoryPlayer, 8, 103);
	}
	
	@Override
	public boolean canInteractWith(EntityPlayer player) {
		return tileEntity.isUseableByPlayer(player);
	}
}
