package pl.asie.moducomp;

import java.util.logging.Level;

import pl.asie.moducomp.lib.ContainerInventory;
import pl.asie.moducomp.lib.SlotTyped;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;

public class ContainerMusicBox extends ContainerInventory {
	protected TileEntityMusicBox tileEntity;

	public ContainerMusicBox(InventoryPlayer inventoryPlayer, TileEntityMusicBox te) {
		super(1);
		tileEntity = te;
		addSlotToContainer(new SlotTyped(ItemPaperTape.class, tileEntity, 0, 80, 20));
		bindPlayerInventory(inventoryPlayer, 8, 52);
	}
	
	@Override
	public boolean canInteractWith(EntityPlayer player) {
		return tileEntity.isUseableByPlayer(player);
	}
}