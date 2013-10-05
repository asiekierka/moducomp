package pl.asie.moducomp.block;

import java.util.Random;
import java.util.logging.Level;

import pl.asie.moducomp.ModularComputing;
import pl.asie.moducomp.item.ItemPaperTape;
import pl.asie.moducomp.lib.Helper;
import pl.asie.moducomp.lib.ITileEntityOwner;
import cpw.mods.fml.common.network.PacketDispatcher;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.audio.SoundManager;
import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.Packet62LevelSound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Icon;
import net.minecraft.world.World;

public class BlockMusicBox extends BlockMachine implements ITileEntityOwner {
	
	private Icon iconMG, iconMT;
	
	public Class<? extends TileEntity> getTileEntityClass() { return TileEntityMusicBox.class; }

	public BlockMusicBox(int id, String name) {
		super(id, name);
	}

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons (IconRegister iconRegister)
    {
    	this.iconMG = iconRegister.registerIcon("minecraft:noteblock");
    	this.iconMT = iconRegister.registerIcon("moducomp:musicbox_top");
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
		return new TileEntityMusicBox();
	}
	
	public int getMusicSpeed() { return 5; }
	
	// Here comes the musicbox logic.
	@Override
    public void onNeighborBlockChange(World world, int x, int y, int z, int par5)
    {
    	if(world.isRemote) return;
    	
        boolean isPowered = world.isBlockIndirectlyGettingPowered(x, y, z) || world.isBlockIndirectlyGettingPowered(x, y + 1, z);
        boolean wasPowered = world.getBlockMetadata(x, y, z) > 0;

        if (isPowered && !wasPowered)
        {
            world.scheduleBlockUpdate(x, y, z, this.blockID, 4);
            world.setBlockMetadataWithNotify(x, y, z, 1, 4);
            // Rewind paper tape.
        	TileEntity te = world.getBlockTileEntity(x, y, z);
        	if(!(te instanceof TileEntityMusicBox)) return;
            TileEntityMusicBox temb = (TileEntityMusicBox)te;
            ItemStack stack = temb.getStackInSlot(0);
            if(stack != null && stack.getItem() instanceof ItemPaperTape) {
            	ItemPaperTape tapeHandler = (ItemPaperTape)stack.getItem();
            	tapeHandler.setPosition(stack, 0);
            }
        }
        else if (wasPowered && !isPowered)
        {
            world.setBlockMetadataWithNotify(x, y, z, 0, 4);
        }
    }

    @Override
    public void updateTick(World world, int x, int y, int z, Random random)
    {
        if (!world.isRemote) {
        	TileEntity te = world.getBlockTileEntity(x, y, z);
        	if(!(te instanceof TileEntityMusicBox)) return;
            TileEntityMusicBox temb = (TileEntityMusicBox)te;
            if(temb.playNote()) world.scheduleBlockUpdate(x, y, z, this.blockID, getMusicSpeed()); // Try playing another note.
        }
    }
}
