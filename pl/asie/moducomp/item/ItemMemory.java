package pl.asie.moducomp.item;

import java.util.List;

import pl.asie.moducomp.ModularComputing;
import pl.asie.moducomp.api.IItemMemory;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public abstract class ItemMemory extends Item implements IItemMemory {
	
	public ItemMemory(int id, String name) {
		super(id);
		this.setUnlocalizedName(name);
		this.setTextureName("moducomp:generic_chip");
		this.setCreativeTab(ModularComputing.instance.tab);
	}
	
    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean par4)
    {
    	int length = getLength(stack);
    	if(length >= 1024) {
    		list.add((length >> 10) + "KB");
    	} else list.add(length + "B");
    }
}
