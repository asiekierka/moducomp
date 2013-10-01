package pl.asie.moducomp.lib;

import java.io.*;
import java.util.*;

import pl.asie.moducomp.ModularComputing;
import net.minecraft.block.*;
import net.minecraft.block.material.*;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.*;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.network.*;
import net.minecraft.network.packet.*;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.util.MathHelper;

public class Helper {
        public static final int[][] DIRECTIONS = {
            {-1,0,0}, {0,0,-1}, {1,0,0}, {0,0,1}, {0,-1,0}, {0,1,0}
        };

        public static int[] fileToIntArray(String fn) {
            InputStream is = ModularComputing.class.getClassLoader().getResourceAsStream(fn);
            if(is == null) return null;
            int[] romData = new int[65536]; // No ROMs above 64K!
            int lastByte = 0;
            int i = 0;
            try {
                while(lastByte>=0) {
                    lastByte = is.read();
                    romData[i++] = lastByte;
                }
                return Arrays.copyOfRange(romData,0,i-1);
            } catch(Exception e) { e.printStackTrace(); return null; }
        }
        
        public static NBTTagCompound getTagCompound(ItemStack stack) {
            if(stack.getTagCompound() == null) return new NBTTagCompound();
            else return stack.getTagCompound();
        }
        
	public static void dropItems(World world, int x, int y, int z) {
		Random rand = new Random();
		TileEntity tileEntity = world.getBlockTileEntity(x, y, z);
		if (tileEntity == null || !(tileEntity instanceof IInventory)) {
			return;
		}
		IInventory inventory = (IInventory) tileEntity;

                for (int i = 0; i < inventory.getSizeInventory(); i++) {
                        ItemStack item = inventory.getStackInSlot(i);

                        if (item != null && item.stackSize > 0) {
                        		inventory.setInventorySlotContents(i, null);
                                float rx = rand.nextFloat() * 0.8F + 0.1F;
                                float ry = rand.nextFloat() * 0.8F + 0.1F;
                                float rz = rand.nextFloat() * 0.8F + 0.1F;

                                EntityItem entityItem = new EntityItem(world,
                                                x + rx, y + ry, z + rz,
                                                new ItemStack(item.itemID, item.stackSize, item.getItemDamage()));

                                if (item.hasTagCompound()) {
                                        entityItem.getEntityItem().setTagCompound((NBTTagCompound)item.getTagCompound().copy());
                                }

                                float factor = 0.05F;
                                entityItem.motionX = rand.nextGaussian() * factor;
                                entityItem.motionY = rand.nextGaussian() * factor + 0.2F;
                                entityItem.motionZ = rand.nextGaussian() * factor;
                                world.spawnEntityInWorld(entityItem);
                                item.stackSize = 0;
                        }
		}
	}
    // source: BlockFurnace
    public static void setDefaultDirection(World par1World, int par2, int par3, int par4)
    {
        if (!par1World.isRemote)
        {
            int l = par1World.getBlockId(par2, par3, par4 - 1);
            int i1 = par1World.getBlockId(par2, par3, par4 + 1);
            int j1 = par1World.getBlockId(par2 - 1, par3, par4);
            int k1 = par1World.getBlockId(par2 + 1, par3, par4);
            byte b0 = 3;

            if (Block.opaqueCubeLookup[l] && !Block.opaqueCubeLookup[i1]) b0 = 3;
            if (Block.opaqueCubeLookup[i1] && !Block.opaqueCubeLookup[l]) b0 = 2;
            if (Block.opaqueCubeLookup[j1] && !Block.opaqueCubeLookup[k1]) b0 = 5;
            if (Block.opaqueCubeLookup[k1] && !Block.opaqueCubeLookup[j1]) b0 = 4;

            par1World.setBlockMetadataWithNotify(par2, par3, par4, b0, 2);
        }
    }

    private static final int[] PLACEMENT_DIRECTIONS = {2,5,3,4};
    // source: BlockFurnace
    public static int placedDirection(EntityLiving par5EntityLiving)
    {
        int l = MathHelper.floor_double((double)(par5EntityLiving.rotationYaw * 4.0F / 360.0F) + 0.5D) & 3;
        return PLACEMENT_DIRECTIONS[l];
    }
}