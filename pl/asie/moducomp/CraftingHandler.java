package pl.asie.moducomp;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import cpw.mods.fml.common.ICraftingHandler;

public class CraftingHandler implements ICraftingHandler {

	@Override
	public void onCrafting(EntityPlayer player, ItemStack item,
			IInventory craftMatrix) {
        if (!player.worldObj.isRemote)
        {
        	String paperTapeRecipe = " x xyx x ";
        	boolean isPaperTape = true;
        	for(int i = 0; i < 9; i++) {
        		int itemID = 0;
        		char currentChar = paperTapeRecipe.charAt(i);
        		if(currentChar == 'x') itemID = Item.paper.itemID;
        		else if(currentChar == 'y') itemID = ModularComputing.instance.itemPaperTape.itemID;
        		ItemStack slot = craftMatrix.getStackInSlot(i);
        		int targetID = (slot != null ? slot.itemID : 0);
        		if(itemID != targetID) { isPaperTape = false; break; }
        	}
        	if(isPaperTape && item.getItem() instanceof ItemPaperTape) { // Extend paper tape's length.
        		ItemPaperTape tapeHandler = (ItemPaperTape)item.getItem();
        		item = tapeHandler.extend(item);
        		return;
        	}
        }
	}

	@Override
	public void onSmelting(EntityPlayer player, ItemStack item) {
		
	}

	
}
