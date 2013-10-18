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
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Icon;
import net.minecraft.world.World;

public class BlockMainBoard extends BlockMachineRotatable implements ITileEntityOwner {
	private Icon iconMG, iconMT;

	public Class<? extends TileEntity> getTileEntityClass() { return TileEntityMainBoard.class; }
	
    public BlockMainBoard(int id, String name) {
    	super(id, name);
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons (IconRegister iconRegister)
    {
    	this.iconMG = iconRegister.registerIcon("moducomp:machine_generic");
    	this.iconMT = iconRegister.registerIcon("moducomp:mainboard_front");
    }
	
    @Override
    @SideOnly(Side.CLIENT)
    public Icon getIcon (int side, int metadata)
    {
        if(side == (metadata&7) || (side == 3 && (metadata&7) == 0)) return this.iconMT;
        else return this.iconMG;
    }
    
	// Here comes the musicbox logic.
	@Override
    public void onNeighborBlockChange(World world, int x, int y, int z, int par5)
    {
    	if(world.isRemote) return;
    	int metadata = world.getBlockMetadata(x, y, z);
    	
        boolean isPowered = world.isBlockIndirectlyGettingPowered(x, y, z) || world.isBlockIndirectlyGettingPowered(x, y + 1, z);
        boolean wasPowered = world.getBlockMetadata(x, y, z) >= 8;

    	TileEntity te = world.getBlockTileEntity(x, y, z);
    	if(!(te instanceof TileEntityMainBoard)) return;
        TileEntityMainBoard board = (TileEntityMainBoard)te;
        
        if (isPowered && !wasPowered)
        {
            world.setBlockMetadataWithNotify(x, y, z, 8 | (metadata&7), 4);
            board.begin();
        }
        else if (wasPowered && !isPowered)
        {
            world.setBlockMetadataWithNotify(x, y, z, (metadata&7), 4);
            board.end();
        }
    }
    
	@Override
	public TileEntity createNewTileEntity(World world) {
		return new TileEntityMainBoard();
	}
}
