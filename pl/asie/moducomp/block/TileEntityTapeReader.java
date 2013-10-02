package pl.asie.moducomp.block;

import java.awt.Container;

import pl.asie.moducomp.item.ItemPaperTape;
import pl.asie.moducomp.lib.TileEntityInventory;
import mods.immibis.redlogic.api.wiring.IBundledEmitter;
import mods.immibis.redlogic.api.wiring.IBundledWire;
import mods.immibis.redlogic.api.wiring.IConnectable;
import mods.immibis.redlogic.api.wiring.IWire;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class TileEntityTapeReader extends TileEntityInventory implements IBundledEmitter, IConnectable {
	public TileEntityTapeReader() {
		super(1, 1, "block.moducomp.tape_reader");
	}
	
	public void setBit(int position, byte offset, byte shift) {
		ItemStack stack = this.getStackInSlot(0);
		if(stack != null && stack.getItem() instanceof ItemPaperTape) {
			ItemPaperTape tapeHandler = (ItemPaperTape)stack.getItem();
			tapeHandler.setPosition(stack, position);
			byte value = tapeHandler.getByte(stack, offset);
			value |= (byte)1<<shift;
			tapeHandler.setByte(stack, value, offset);
		}
	}
	
	public int getPosition() {
		ItemStack stack = this.getStackInSlot(0);
		if(stack != null && stack.getItem() instanceof ItemPaperTape) {
			ItemPaperTape tapeHandler = (ItemPaperTape)stack.getItem();
			return tapeHandler.getPosition(stack);
		}
		return 0;
	}
	
	public void setPosition(int position) {
		ItemStack stack = this.getStackInSlot(0);
		if(stack != null && stack.getItem() instanceof ItemPaperTape) {
			ItemPaperTape tapeHandler = (ItemPaperTape)stack.getItem();
			tapeHandler.setPosition(stack, position);
		}
	}

	@Override
	public byte[] getBundledCableStrength(int blockFace, int toDirection) {
		ItemStack stack = this.getStackInSlot(0);
		if(stack != null && stack.getItem() instanceof ItemPaperTape) {
			ItemPaperTape tapeHandler = (ItemPaperTape)stack.getItem();
			byte[] data = new byte[16];
			int realValue = 0xFF & tapeHandler.getByte(stack, 0);
			for(int i = 0; i < 8; i++) {
				data[i] = (realValue & (1<<i)) > 0 ? (byte)255 : (byte)0;
			}
			return data;
		} else return null;
	}

	@Override
	public boolean connects(IWire wire, int blockFace, int fromDirection) {
		return (wire instanceof IBundledWire);
	}

	@Override
	public boolean connectsAroundCorner(IWire wire, int blockFace,
			int fromDirection) {
		return false;
	}
}
