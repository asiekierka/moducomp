package pl.asie.moducomp.block;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.List;

import cpw.mods.fml.common.network.PacketDispatcher;
import cpw.mods.fml.common.network.Player;
import pl.asie.moducomp.ModularComputing;
import pl.asie.moducomp.NetworkHandler;
import pl.asie.moducomp.api.IEntityPeripheral;
import pl.asie.moducomp.api.IItemCPU;
import pl.asie.moducomp.api.IItemMemory;
import pl.asie.moducomp.api.IMemoryControllerProvider;
import pl.asie.moducomp.api.computer.ICPU;
import pl.asie.moducomp.api.computer.IMemory;
import pl.asie.moducomp.api.computer.IMemoryController;
import pl.asie.moducomp.computer.memory.MemoryControllerSlot;
import pl.asie.moducomp.computer.memory.MemoryHandlerROM;
import pl.asie.moducomp.gui.text.TextWindow;
import pl.asie.moducomp.item.ItemPaperTape;
import pl.asie.moducomp.lib.Helper;
import pl.asie.moducomp.lib.TileEntityInventory;
import pl.asie.moducomp.peripheral.IOHandlerDebugMC;
import pl.asie.moducomp.peripheral.IOHandlerTerminal;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.Packet250CustomPayload;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class TileEntityMainBoard extends TileEntityInventory {
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
    
	public List<IEntityPeripheral> getPeripherals() {
		ArrayList<IEntityPeripheral> list = new ArrayList<IEntityPeripheral>(5);
		for(int[] dir: Helper.DIRECTIONS) {
			TileEntity te = worldObj.getBlockTileEntity(this.xCoord + dir[0], this.yCoord + dir[1], this.zCoord + dir[2]);
			if(te instanceof IEntityPeripheral)
				list.add((IEntityPeripheral)te);
		}
		return list;
	}
	
	private void unloadPeripherals() {
		for(IEntityPeripheral peripheral: getPeripherals()) {
			peripheral.deinit(cpu);
		}
	}
	
	private void unload() {
		unloadPeripherals();
	}
	
	private ICPU cpu;
	private IMemoryController memory;
	
	public boolean isCPUInserted() {
		ItemStack cpuStack = this.getStackInSlot(0);
		return cpuStack != null && (cpuStack.getItem() instanceof IItemCPU);
	}
	
	private CPUThreadMainBoard currentThread;
	
	public void begin() {
		if(!isCPUInserted()) return;
		// Turn off previous CPU
		end();
		// Get memory
		this.memory = getMemoryController();
		if(this.memory == null) return;
		// Get CPU
		ItemStack cpuStack = this.getStackInSlot(0);
		IItemCPU itemCPU = (IItemCPU)cpuStack.getItem();
		cpu = itemCPU.createNewCPUHandler(cpuStack);
		if(cpu == null) return;
		// Initialize peripherals
		this.memory.setDeviceSlot(15, new IOHandlerDebugMC(this));
		int i = 0;
		for(IEntityPeripheral peripheral: getPeripherals()) {
			ModularComputing.instance.logger.info("Adding peripheral: " + peripheral.toString() + ", slot "+i);
			if(peripheral.getPreferredDeviceID() >= 0 && peripheral.getPreferredDeviceID() <= 15) {
				int id = peripheral.getPreferredDeviceID();
				if(this.memory.getDeviceSlot(id) == null)
					this.memory.setDeviceSlot(id, peripheral.init(this.cpu, this.memory));
			} else {
				while(this.memory.getDeviceSlot(i) != null) i++;
				if(i >= 15) break;
				this.memory.setDeviceSlot(i, peripheral.init(this.cpu, this.memory));
				i++;
				if(i >= 15) break; // All slots taken!
			}
		}
		// Reset
		cpu.setMemoryHandler(memory);
		cpu.resetCold();
		ModularComputing.instance.debug("Starting emulation!");
		currentThread = new CPUThreadMainBoard(cpu, 250000); // TODO change clock speed
		new Thread(currentThread).start();
	}
	
	public void end() {
		if(currentThread != null) {
			currentThread.kill();
			currentThread = null;
			unload();
		}
	}
}
