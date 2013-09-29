package pl.asie.moducomp;

import java.util.logging.Level;

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
		// Logging stuff!
		ItemStack stack = te.getStackInSlot(0);
		if(stack != null && stack.getItem() instanceof ItemPaperTape) {
			ItemPaperTape ipt = (ItemPaperTape)stack.getItem();
			ModularComputing.logger.log(Level.INFO, "Byte is " + ipt.getByte(stack, 0));
		}
	}
	
	@Override
	public boolean canInteractWith(EntityPlayer player) {
		return tileEntity.isUseableByPlayer(player);
	}
}
