package pl.asie.moducomp;

import pl.asie.moducomp.api.ITape;
import pl.asie.moducomp.api.TapeDirection;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class ItemPaperTape extends Item implements ITape {
	public ItemPaperTape(int id) {
		super(id);
		this.setUnlocalizedName("moducomp.paper_tape");
		this.setTextureName("moducomp:paper_tape");
	}
	
	private boolean check(ItemStack stack) {
		if(stack.itemID != this.itemID) return false;
		if(stack.getTagCompound() == null) {
			NBTTagCompound compound = new NBTTagCompound();
			compound.setInteger("TapePosition", 0);
			compound.setByteArray("TapeData", new byte[getLength(stack)]);
			stack.setTagCompound(compound);
		}
		return true;
	}
	
	public int seek(ItemStack stack, int bytes, TapeDirection DIRECTION) {
		if(DIRECTION == TapeDirection.BACKWARD) bytes = 0 - bytes; // invert
		if(check(stack)) {
			NBTTagCompound compound = stack.getTagCompound();
			int currentPosition = compound.getInteger("TapePosition");
			int targetPosition = currentPosition + bytes;
			if(targetPosition < 0) targetPosition = 0;
			else if(targetPosition >= getLength(stack)) targetPosition = getLength(stack);
			compound.setInteger("TapePosition", targetPosition);
			stack.setTagCompound(compound);
			return Math.abs(currentPosition - targetPosition);
		} else return 0;
	}
	
	public byte getByte(ItemStack stack) {
		if(check(stack)) {
			NBTTagCompound compound = stack.getTagCompound();
			return compound.getByteArray("TapeData")[compound.getInteger("TapePosition") & getLength(stack)];
		} else return (byte)0;
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
}
