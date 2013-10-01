package pl.asie.moducomp.block;

import java.util.logging.Level;

import pl.asie.moducomp.ModularComputing;
import pl.asie.moducomp.lib.Helper;
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

public class BlockRAMBoard extends BlockMachine implements ITileEntityOwner {
	private Icon iconMG, iconMT;

	public Class<? extends TileEntity> getTileEntityClass() { return TileEntityRAMBoard.class; }
	
    public BlockRAMBoard(int id, String name) {
    	super(id, name);
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons (IconRegister iconRegister)
    {
    	this.iconMG = iconRegister.registerIcon("moducomp:machine_generic");
    	this.iconMT = iconRegister.registerIcon("moducomp:machine_generic"); // TODO
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
		return new TileEntityRAMBoard();
	}
}
