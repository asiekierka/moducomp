package pl.asie.moducomp.block;

import java.util.Random;
import java.util.logging.Level;

import pl.asie.moducomp.ModularComputing;
import pl.asie.moducomp.item.ItemPaperTape;
import pl.asie.moducomp.lib.Helper;
import pl.asie.moducomp.lib.ITileEntityOwner;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mods.immibis.redlogic.api.wiring.IBundledEmitter;
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

public class BlockTapeReader extends BlockMachine implements ITileEntityOwner {
	private Icon iconMG, iconMT;
	
	public Class<? extends TileEntity> getTileEntityClass() { return TileEntityTapeReader.class; }
	
    public BlockTapeReader(int id, String name) 
    {
        	super(id, name);
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons (IconRegister iconRegister)
    {
    	this.iconMG = iconRegister.registerIcon("moducomp:machine_generic");
    	this.iconMT = iconRegister.registerIcon("moducomp:tape_reader_top");
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public Icon getIcon (int side, int metadata)
    {
        if(side == 1) return this.iconMT;
        else return this.iconMG;
    }
    
	@Override
	public TileEntity createNewTileEntity(World world) {
		return new TileEntityTapeReader();
	}
	
	// For RedLogic support
	@Override
    public void onNeighborBlockChange(World world, int x, int y, int z, int par5)
    {
    	if(world.isRemote) return;
    	
        boolean isPowered = world.isBlockIndirectlyGettingPowered(x, y, z) || world.isBlockIndirectlyGettingPowered(x, y + 1, z);
        boolean wasPowered = world.getBlockMetadata(x, y, z) > 0;

        if (isPowered && !wasPowered)
        {
            world.scheduleBlockUpdate(x, y, z, this.blockID, 4);
            TileEntityTapeReader tileEntity = (TileEntityTapeReader)world.getBlockTileEntity(x,y,z);
            tileEntity.nextByte();
            world.setBlockMetadataWithNotify(x, y, z, 1, 4);
        }
        else if (wasPowered && !isPowered)
        {
            world.setBlockMetadataWithNotify(x, y, z, 0, 4);
        }
    }
}
