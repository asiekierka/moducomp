package pl.asie.moducomp;

import java.util.Random;
import java.util.logging.Level;

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

public class BlockMusicBox extends BlockContainer {
	private Icon iconMG, iconMT;
	
    public BlockMusicBox(int id, Material material) 
    {
            super(id, material);
    		this.setHardness(4.5F);
    		this.setUnlocalizedName("block.moducomp.music_box");
    		this.setCreativeTab(CreativeTabs.tabRedstone);
    		this.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.875F, 1.0F); // Aesthetics.
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons (IconRegister iconRegister)
    {
    	this.iconMG = iconRegister.registerIcon("minecraft:noteblock");
    	this.iconMT = iconRegister.registerIcon("moducomp:musicbox_top");
    }

    public boolean isOpaqueCube() { return false; }
    public boolean renderAsNormalBlock() { return false; }
    
    @Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int par6, float par7, float par8, float par9) {
		if(!world.isRemote || player.isSneaking()) {
			player.openGui(ModularComputing.instance, 1, world, x, y, z);
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
	public TileEntity createNewTileEntity(World world) {
		return new TileEntityMusicBox();
	}
	
	public int getMusicSpeed() { return 3; }
	
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
            TileEntityMusicBox temb = (TileEntityMusicBox)world.getBlockTileEntity(x, y, z);
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
	
	private static final String[] noteMapping = {"abOne", "bbOne", "cOne", "dbOne", "ebOne", "fOne", "gOne", "abTwo", "none", "bbTwo", "cTwo", "dbTwo", "ebTwo", "fTwo", "gTwo", "abThree"};

	protected static void addSounds(SoundManager manager) {
		for(String note: noteMapping) {
			if(note.equals("none")) continue;
			manager.addSound("moducomp:musicbox_"+note+".ogg");
		}
		ModularComputing.logger.info("Initialized MusicBox sounds.");
	}
	
    @Override
    public void updateTick(World world, int x, int y, int z, Random random)
    {
        if (!world.isRemote)
        {
        	// Get the tape and the relevant handler.
            TileEntityMusicBox temb = (TileEntityMusicBox)world.getBlockTileEntity(x, y, z);
            ItemStack stack = temb.getStackInSlot(0);
            if(stack != null && stack.getItem() instanceof ItemPaperTape) {
            	ItemPaperTape tapeHandler = (ItemPaperTape)stack.getItem();
            	ModularComputing.debug("Tick, position "+ tapeHandler.getPosition(stack));
            	int seek = tapeHandler.seek(stack, 2);
            	if(seek < 2) return; // End of tape
            	int b1 = 0xFF & tapeHandler.getByte(stack, -2);
            	int b2 = 0xFF & tapeHandler.getByte(stack, -1);
            	int values = (b1 << 8) | b2;
            	for(int i = 0; i < 16; i++) {
            		boolean is = (values & (1<<(15-i))) > 0;
            		if(!is) continue;
            		String note = noteMapping[i];
            		if(note.equals("none")) continue;
            		ModularComputing.debug("Triggering note "+note);
            		world.playSoundEffect(x, y, z, "moducomp:musicbox_"+note, 1.0F, 1.0F);
            		//PacketDispatcher.sendPacketToAllAround(x, y, z, 16.0D, world.provider.dimensionId,
            		//		new Packet62LevelSound("moducomp:musicbox_"+note, x, y, z, 1.0F, 1.0F));
            	}
            	world.scheduleBlockUpdate(x, y, z, this.blockID, getMusicSpeed());
            }
        }
    }
}
