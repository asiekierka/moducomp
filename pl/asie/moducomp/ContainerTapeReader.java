package pl.asie.moducomp;

import pl.asie.moducomp.lib.ContainerInventory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;

public class ContainerTapeReader extends ContainerInventory {
	protected TileEntityTapeReader tileEntity;

	public ContainerTapeReader(InventoryPlayer inventoryPlayer, TileEntityTapeReader te) {
		super(1);
		tileEntity = te;
		//addSlotToContainer(new SlotModular(IPeripheralClock.class, tileEntity, 0, 16, 16)); // Clock
		bindPlayerInventory(inventoryPlayer, 8, 148);
	}
	
	@Override
	public boolean canInteractWith(EntityPlayer player) {
		return tileEntity.isUseableByPlayer(player);
	}
}
