package pl.asie.moducomp.block;

import java.awt.Container;

import pl.asie.moducomp.api.IItemMemory;
import pl.asie.moducomp.api.IMemoryControllerProvider;
import pl.asie.moducomp.api.computer.ICPU;
import pl.asie.moducomp.api.computer.IMemory;
import pl.asie.moducomp.api.computer.IMemoryController;
import pl.asie.moducomp.computer.memory.MemoryControllerSlot;
import pl.asie.moducomp.item.ItemPaperTape;
import pl.asie.moducomp.lib.Helper;
import pl.asie.moducomp.lib.TileEntityInventory;
import pl.asie.moducomp.peripheral.IOHandlerDebugMC;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class TileEntityMainBoard extends TileEntityInventory implements Runnable
{
	public TileEntityMainBoard() {
		super(1, 1, "block.moducomp.main_board");
	}
	
	public IMemoryController getMemoryController() {
		for(int[] dir : Helper.DIRECTIONS) {
			TileEntity te = worldObj.getBlockTileEntity(this.xCoord + dir[0], this.yCoord + dir[1], this.zCoord + dir[2]);
			if(te instanceof IMemoryControllerProvider) {
				IMemoryControllerProvider mem = (IMemoryControllerProvider)te;
				return mem.getMemoryController();
			}
		}
		return null;
	}
	
	private boolean isRunning = false;
	private ICPU cpu;
	private IMemoryController memory;
	
	public void run() { // Thread
		while(isRunning) {
			long t_start = System.nanoTime() / 1000000;
			int cyclesLeft = cpu.run(250000 / 20); // 250KHz TODO changeable
			if(cyclesLeft > 0) isRunning = false;
			long t_end = System.nanoTime() / 1000000;
			try {
				Thread.sleep(50 - (t_end - t_start));
			} catch(Exception e) { }
		}
	}
	
	public void begin() {
		if(isRunning) return;
		// Get memory
		memory = getMemoryController();
		if(memory == null) return;
		// Initialize peripherals
		memory.setDeviceSlot(15, new IOHandlerDebugMC());
		// Get CPU
		ItemStack cpuStack = this.getStackInSlot(0);
		if(cpuStack == null || !(cpuStack.getItem() instanceof ICPU)) return;
		cpu = (ICPU)cpuStack.getItem();
		cpu.setMemoryHandler(memory);
		cpu.resetCold();
		isRunning = true;
		new Thread(this).start();
	}
	
	public void end() {
		isRunning = false;
	}
}
