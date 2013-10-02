package pl.asie.moducomp.item;

import java.util.List;

import pl.asie.moducomp.api.IItemMemory;
import pl.asie.moducomp.api.IMemoryHandler;
import pl.asie.moducomp.computer.memory.MemoryHandlerRAM;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class ItemRAM extends ItemMemory implements IItemMemory {
	private static final int MIN_LENGTH = 7;
	
	public ItemRAM(int id, String name) {
		super(id, name);
	}

	public int getLength(ItemStack stack) {
		return 1<<(stack.getItemDamage()+MIN_LENGTH);
	}
	
	@Override
	public IMemoryHandler createNewMemoryHandler(ItemStack stack) {
		return new MemoryHandlerRAM(this.getLength(stack));
	}
}
