package pl.asie.moducomp.item;

import java.util.List;

import pl.asie.moducomp.ModularComputing;
import pl.asie.moducomp.api.IItemCPU;
import pl.asie.moducomp.api.computer.ICPU;
import pl.asie.moducomp.computer.cpu.CPUAreia;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class ItemCPUAreia extends Item implements IItemCPU {
	
	public ItemCPUAreia(int id, String name) {
		super(id);
		this.setUnlocalizedName(name);
		this.setTextureName("moducomp:cpu_areia");
		this.setCreativeTab(ModularComputing.instance.tab);
	}
	
	@Override
	public ICPU createNewCPUHandler(ItemStack stack) {
		return new CPUAreia();
	}
}
