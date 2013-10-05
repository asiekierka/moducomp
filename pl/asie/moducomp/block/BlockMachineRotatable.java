package pl.asie.moducomp.block;

import java.util.logging.Level;

import pl.asie.moducomp.ModularComputing;
import pl.asie.moducomp.lib.Helper;
import pl.asie.moducomp.lib.ITileEntityOwner;
import pl.asie.moducomp.lib.TileEntityInventory;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Icon;
import net.minecraft.world.World;

public abstract class BlockMachineRotatable extends BlockMachine {
    public BlockMachineRotatable(int id, String name) {
    	super(id, name);
    }
    
	@Override
	public void onBlockAdded(World world, int x, int y, int z) {
        super.onBlockAdded(world,x,y,z);
		Helper.setDefaultDirection(world,x,y,z);
	}
	@Override
	public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase el, ItemStack is) {
		world.setBlockMetadataWithNotify(x,y,z, Helper.placedDirection(el), 2);
	}
}
