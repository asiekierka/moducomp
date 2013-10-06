package pl.asie.moducomp.item;

import java.util.List;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import pl.asie.moducomp.ModularComputing;
import pl.asie.moducomp.api.IItemTape;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class ItemPaperTape extends Item implements IItemTape {
	public static final int MAX_TAPE_LENGTH = (16384/32);
	
	public ItemPaperTape(int id, String name) {
		super(id);
		this.setUnlocalizedName(name);
		this.setTextureName("moducomp:paper_tape");
   		this.setCreativeTab(ModularComputing.instance.tab);
	}
	
	private boolean check(ItemStack stack) {
		if(stack.itemID != this.itemID) return false;
		if(stack.getTagCompound() == null) {
			NBTTagCompound compound = new NBTTagCompound();
			compound.setInteger("TapePosition", 0);
			compound.setByteArray("TapeData", new byte[getLength(stack)]);
			stack.setTagCompound(compound);
		} else {
			NBTTagCompound compound = stack.getTagCompound();
			if(!compound.hasKey("TapePosition") || !compound.hasKey("TapeData")) {
				compound.setInteger("TapePosition", 0);
				compound.setByteArray("TapeData", new byte[getLength(stack)]);
			}
			if(compound.getByteArray("TapeData").length != getLength(stack))
				compound.setByteArray("TapeData", new byte[getLength(stack)]);
			stack.setTagCompound(compound);
		}
		return true;
	}
	
	public int seek(ItemStack stack, int bytes) {
		if(check(stack)) {
			NBTTagCompound compound = stack.getTagCompound();
			int currentPosition = compound.getInteger("TapePosition");
			int targetPosition = currentPosition + bytes;
			if(targetPosition < 0) targetPosition = 0;
			else if(targetPosition >= getLength(stack)) targetPosition = getLength(stack) - 1;
			compound.setInteger("TapePosition", targetPosition);
			stack.setTagCompound(compound);
			return Math.abs(currentPosition - targetPosition);
		} else return 0;
	}
	
	public int getPosition(ItemStack stack) {
		if(check(stack)) {
			return stack.getTagCompound().getInteger("TapePosition");
		} else return 0;
	}
	
	public void setPosition(ItemStack stack, int pos) {
		if(check(stack)) {
			NBTTagCompound compound = stack.getTagCompound();
			compound.setInteger("TapePosition", pos);
			stack.setTagCompound(compound);
		}
	}
	
	public ItemStack extend(ItemStack stack, ItemStack oldStack, boolean copyData) {
		if(check(stack) && check(oldStack) && oldStack.getItemDamage() < MAX_TAPE_LENGTH) {
			stack.setItemDamage(oldStack.getItemDamage()+1);
			if(copyData) {
				NBTTagCompound compound = stack.getTagCompound();
				byte[] newData = new byte[getLength(stack)];
				byte[] oldData = oldStack.getTagCompound().getByteArray("TapeData");
				System.arraycopy(oldData, 0, newData, 0, oldData.length);
				compound.setByteArray("TapeData", newData);
				stack.setTagCompound(compound);
			}
			return stack;
		} else return null;
	}

	public void setByte(ItemStack stack, byte val, int offset) {
		if(check(stack)) {
			NBTTagCompound compound = stack.getTagCompound();
			byte[] data = compound.getByteArray("TapeData");
			int pos = (compound.getInteger("TapePosition") + offset);
			if(pos < 0 || pos >= getLength(stack)) return;
			data[pos] = val;
			compound.setByteArray("TapeData", data);
			stack.setTagCompound(compound);
		}
	}
	
	public byte getByte(ItemStack stack, int offset) {
		if(check(stack)) {
			NBTTagCompound compound = stack.getTagCompound();
			int pos = (compound.getInteger("TapePosition") + offset);
			if(pos < 0 || pos >= getLength(stack)) return (byte)0;
			return compound.getByteArray("TapeData")[pos];
		} else return (byte)0;
	}
	
	public boolean isValid(ItemStack stack, int offset) {
		if(check(stack)) {
			NBTTagCompound compound = stack.getTagCompound();
			int target = (compound.getInteger("TapePosition") + offset);
			int length = getLength(stack);
			return (target >= 0 && target < length);
		} else return false;
	}
	public int getLength(ItemStack stack) {
		if(stack.itemID != this.itemID) return 0;
		return (1+((int)0xFFFF&stack.getItemDamage()))*32;
	}
	
	public NBTTagCompound reset(NBTTagCompound compound) {
		compound.setInteger("TapePosition", 0);
		return compound;
	}
	
	public void reset(ItemStack stack) {
		if(check(stack)) {
			stack.setTagCompound(reset(stack.getTagCompound()));
		}
	}
	
	public float getSpeed() {
		return 0.5F;
	}
	
    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean par4)
    {
        list.add(getLength(stack) + " bytes");
    }
}
