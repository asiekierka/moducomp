package pl.asie.moducomp.gui;

import java.util.logging.Level;

import pl.asie.moducomp.block.TileEntityMusicBox;
import pl.asie.moducomp.item.ItemPaperTape;
import pl.asie.moducomp.lib.ContainerInventory;
import pl.asie.moducomp.lib.SlotTyped;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

public class ContainerMusicBox extends ContainerInventory {
	protected TileEntityMusicBox tileEntity;

	public ContainerMusicBox(InventoryPlayer inventoryPlayer, TileEntityMusicBox tileEntity) {
		super(1, tileEntity);
		this.tileEntity = tileEntity;
		addSlotToContainer(new SlotTyped(ItemPaperTape.class, tileEntity, 0, 80, 20));
		bindPlayerInventory(inventoryPlayer, 8, 52);
	}
	
	@Override
	public boolean canInteractWith(EntityPlayer player) {
		return tileEntity.isUseableByPlayer(player);
	}
}
