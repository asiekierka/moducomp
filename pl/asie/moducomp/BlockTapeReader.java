package pl.asie.moducomp;

import java.util.logging.Level;

import pl.asie.moducomp.lib.Helper;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Icon;
import net.minecraft.world.World;

public class BlockTapeReader extends BlockContainer {
	private Icon iconMG, iconMT;
	
    public BlockTapeReader(int id, Material material) 
    {
            super(id, material);
    		this.setHardness(4.5F);
    		this.setUnlocalizedName("block.moducomp.tape_reader");
    		this.setCreativeTab(CreativeTabs.tabRedstone);
    		this.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.875F, 1.0F); // Aesthetics.
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons (IconRegister iconRegister)
    {
    	this.iconMG = iconRegister.registerIcon("moducomp:machine_generic");
    	this.iconMT = iconRegister.registerIcon("moducomp:tape_reader_top");
    }

    public boolean isOpaqueCube() { return false; }
    public boolean renderAsNormalBlock() { return false; }
    
    @Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int par6, float par7, float par8, float par9) {
		if(!world.isRemote || player.isSneaking()) {
			player.openGui(ModularComputing.instance, 0, world, x, y, z);
		}
		return true;
	}
	
    @Override
    @SideOnly(Side.CLIENT)
    public Icon getIcon (int side, int metadata)
    {
        if(side == 1) return this.iconMT;
        else return this.iconMG;
    }
    
	@Override
    public void breakBlock(World world, int x, int y, int z, int id, int meta) {
    	Helper.dropItems(world, x, y, z);
    }
    
	@Override
	public TileEntity createNewTileEntity(World world) {
		return new TileEntityTapeReader();
	}
}
