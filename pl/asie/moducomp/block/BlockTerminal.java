package pl.asie.moducomp.block;

import java.util.logging.Level;

import pl.asie.moducomp.ModularComputing;
import pl.asie.moducomp.item.ItemPaperTape;
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
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Icon;
import net.minecraft.world.World;

public class BlockTerminal extends BlockMachineRotatable implements ITileEntityOwner {
	private Icon iconMG, iconMT;

	public Class<? extends TileEntity> getTileEntityClass() { return TileEntityTerminal.class; }
	
    public BlockTerminal(int id, String name) {
    	super(id, name);
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons (IconRegister iconRegister)
    {
    	this.iconMG = iconRegister.registerIcon("moducomp:machine_generic");
    	this.iconMT = iconRegister.registerIcon("moducomp:terminal");
    }
	
    @Override
    @SideOnly(Side.CLIENT)
    public Icon getIcon (int side, int metadata)
    {
        if(side == metadata || (metadata == 0 && side == 3)) return this.iconMT;
        else return this.iconMG;
    }
    
	@Override
	public TileEntity createNewTileEntity(World world) {
		return new TileEntityTerminal();
	}
}
