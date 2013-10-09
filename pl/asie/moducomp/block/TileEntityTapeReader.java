package pl.asie.moducomp.block;

import java.awt.Container;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import pl.asie.moducomp.api.IEntityPeripheral;
import pl.asie.moducomp.api.IItemTape;
import pl.asie.moducomp.api.computer.ICPU;
import pl.asie.moducomp.api.computer.IMemory;
import pl.asie.moducomp.api.computer.IMemoryController;
import pl.asie.moducomp.item.ItemPaperTape;
import pl.asie.moducomp.lib.TileEntityInventory;
import pl.asie.moducomp.peripheral.IOHandlerTapeReader;
import mods.immibis.redlogic.api.wiring.IBundledEmitter;
import mods.immibis.redlogic.api.wiring.IBundledWire;
import mods.immibis.redlogic.api.wiring.IConnectable;
import mods.immibis.redlogic.api.wiring.IWire;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class TileEntityTapeReader extends TileEntityInventory implements IBundledEmitter, IConnectable, IEntityPeripheral {
	private IOHandlerTapeReader handler;
	
	public TileEntityTapeReader() {
		super(1, 1, "block.moducomp.tape_reader");
		handler = new IOHandlerTapeReader(this);
	}
	
	public IItemTape getHandler() {
		ItemStack stack = getTape();
		if(stack != null && stack.getItem() instanceof IItemTape)
			return (IItemTape)stack.getItem();
		else return null;
	}
	
	public ItemStack getTape() {
		return this.getStackInSlot(0);
	}
	
	public void setBit(int position, byte offset, byte shift) {
		ItemStack stack = getTape();
		if(stack != null && stack.getItem() instanceof ItemPaperTape) {
			ItemPaperTape tapeHandler = (ItemPaperTape)stack.getItem();
			tapeHandler.setPosition(stack, position);
			byte value = tapeHandler.getByte(stack, offset);
			value |= (byte)1<<shift;
			tapeHandler.setByte(stack, value, offset);
			this.worldObj.notifyBlockChange(this.xCoord, this.yCoord, this.zCoord, 2);
		}
	}
	
	public int getPosition() {
		ItemStack stack = getTape();
		if(stack != null && stack.getItem() instanceof ItemPaperTape) {
			ItemPaperTape tapeHandler = (ItemPaperTape)stack.getItem();
			return tapeHandler.getPosition(stack);
		}
		return 0;
	}
	
	public void setPosition(int position) {
		ItemStack stack = getTape();
		if(stack != null && stack.getItem() instanceof ItemPaperTape) {
			ItemPaperTape tapeHandler = (ItemPaperTape)stack.getItem();
			tapeHandler.setPosition(stack, position);
			this.worldObj.notifyBlockChange(this.xCoord, this.yCoord, this.zCoord, 2);
		}
	}
	
	protected void nextByte() {
		ItemStack stack = getTape();
		if(stack != null && stack.getItem() instanceof ItemPaperTape) {
			ItemPaperTape tapeHandler = (ItemPaperTape)stack.getItem();
			tapeHandler.seek(stack, 1);
			this.worldObj.notifyBlockChange(this.xCoord, this.yCoord, this.zCoord, 2);
		}		
	}

	@Override
	public byte[] getBundledCableStrength(int blockFace, int toDirection) {
		ItemStack stack = getTape();
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

	@Override
	public IMemory init(ICPU cpu, IMemoryController memoryController) {
		return handler;
	}

	@Override
	public void deinit(ICPU cpu) {
	}
	
	@Override
	public int getPreferredDeviceID() {
		return -1;
	}
	
	
	@Override
	@SideOnly(Side.CLIENT)
	public void onPeripheralWriteClient(GuiScreen gui, int addr, int val) { }
}
