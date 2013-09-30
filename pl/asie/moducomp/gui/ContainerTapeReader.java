package pl.asie.moducomp.gui;

import java.util.logging.Level;

import pl.asie.moducomp.block.TileEntityTapeReader;
import pl.asie.moducomp.item.ItemPaperTape;
import pl.asie.moducomp.lib.ContainerInventory;
import pl.asie.moducomp.lib.SlotTyped;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;

public class ContainerTapeReader extends ContainerInventory {
	protected TileEntityTapeReader tileEntity;

	public ContainerTapeReader(InventoryPlayer inventoryPlayer, TileEntityTapeReader te) {
		super(1);
		tileEntity = te;
		addSlotToContainer(new SlotTyped(ItemPaperTape.class, tileEntity, 0, 25, 55));
		bindPlayerInventory(inventoryPlayer, 8, 85);
	}
	
	@Override
	public boolean canInteractWith(EntityPlayer player) {
		return tileEntity.isUseableByPlayer(player);
	}
}
