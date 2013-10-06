package pl.asie.moducomp.block;

import pl.asie.moducomp.ModularComputing;
import pl.asie.moducomp.lib.Helper;
import pl.asie.moducomp.lib.ITileEntityOwner;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public abstract class BlockMachine extends BlockContainer implements ITileEntityOwner {
	private int guiID;
	
    public BlockMachine(int id, String name) 
    {
            super(id, Material.circuits);
    		this.setHardness(4.5F);
    		this.setUnlocalizedName(name);
    		this.setCreativeTab(ModularComputing.instance.tab);
    		this.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
    		this.guiID = BlockMachine.getUniqueGuiID();
    		ModularComputing.instance.logger.info("Gui ID for "+name+" is "+this.guiID);
    }
    
    public static int nextGuiID = 0;
    
    public static int getUniqueGuiID() {
    	nextGuiID++;
    	return nextGuiID;
    }
    
    public boolean isOpaqueCube() { return false; }
    public boolean renderAsNormalBlock() { return false; }
    
    @Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int par6, float par7, float par8, float par9) {
    	// HACK HACK HACK HACK HACK HACK HACK
    	if(!world.getBlockTileEntity(x, y, z).getClass().equals(this.getTileEntityClass())) {
    		// Invalidate invalid TileEntity
    		world.getBlockTileEntity(x, y, z).invalidate();
    		world.setBlockTileEntity(x, y, z, this.createNewTileEntity(world));
    	}
    	if(!world.isRemote || player.isSneaking()) {
			player.openGui(ModularComputing.instance, this.guiID, world, x, y, z);
		}
		return true;
	}
    
	@Override
    public void breakBlock(World world, int x, int y, int z, int id, int meta) {
    	Helper.dropItems(world, x, y, z);
    }
	
	public abstract TileEntity createNewTileEntity(World world);
}
