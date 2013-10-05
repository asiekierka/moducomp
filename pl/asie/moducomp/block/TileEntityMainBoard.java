package pl.asie.moducomp.block;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

import cpw.mods.fml.common.network.PacketDispatcher;
import cpw.mods.fml.common.network.Player;
import pl.asie.moducomp.ModularComputing;
import pl.asie.moducomp.NetworkHandler;
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

public class TileEntityMainBoard extends TileEntityInventory implements Runnable
{
	public TextWindow window;
	private IOHandlerTerminal terminal;
	
	public TileEntityMainBoard() {
		super(1, 1, "block.moducomp.main_board");
		this.window = new TextWindow(19, 7);
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
	
    public void sendAndPrint(short chr) {
    	this.window.print(chr);
        ByteArrayOutputStream bos = new ByteArrayOutputStream(32);
        DataOutputStream os = new DataOutputStream(bos);
        try {
        	NetworkHandler.prefixTileEntity(this, os);
            os.writeByte(1);
            os.writeShort(chr);
        } catch(Exception e) { e.printStackTrace(); }
        PacketDispatcher.sendPacketToAllAround(this.xCoord, this.yCoord, this.zCoord, Math.sqrt(this.getMaxRenderDistanceSquared()),
        		this.worldObj.provider.dimensionId, new Packet250CustomPayload("ModularC", bos.toByteArray()));
    }
    
    public void sendNewline() {
    	this.window.newline();
        ByteArrayOutputStream bos = new ByteArrayOutputStream(32);
        DataOutputStream os = new DataOutputStream(bos);
        try {
        	NetworkHandler.prefixTileEntity(this, os);
            os.writeByte(4);
        } catch(Exception e) { e.printStackTrace(); }
        PacketDispatcher.sendPacketToAllAround(this.xCoord, this.yCoord, this.zCoord, Math.sqrt(this.getMaxRenderDistanceSquared()),
        		this.worldObj.provider.dimensionId, new Packet250CustomPayload("ModularC", bos.toByteArray()));
    }
    
    public void handleKey(short key) {
    	if(this.cpu == null || this.terminal == null) return;
    	if(terminal.addKey(this.cpu, key)) { // Echo
    		this.window.key(key);
    	}
    }
    
    private boolean hardwareEcho;
    
    public void setHardwareEcho(boolean is) {
    	hardwareEcho = is;
        ByteArrayOutputStream bos = new ByteArrayOutputStream(32);
        DataOutputStream os = new DataOutputStream(bos);
        try {
        	NetworkHandler.prefixTileEntity(this, os);
            os.writeByte(5);
            os.writeBoolean(is);
        } catch(Exception e) { e.printStackTrace(); }
        PacketDispatcher.sendPacketToAllAround(this.xCoord, this.yCoord, this.zCoord, Math.sqrt(this.getMaxRenderDistanceSquared()),
        		this.worldObj.provider.dimensionId, new Packet250CustomPayload("ModularC", bos.toByteArray()));
    }
    
    public void sendInitialWindowPacket(Player player) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(32);
        DataOutputStream os = new DataOutputStream(bos);
        try {
        	NetworkHandler.prefixTileEntity(this, os);
            os.writeByte(2);
            os.writeShort(this.window.width);
            os.writeShort(this.window.height);
            os.writeShort(this.window.x);
            os.writeShort(this.window.y);
            os.writeBoolean(this.hardwareEcho);
            short[] chars = this.window.getCharArray();
            for(int i = 0; i < this.window.width * this.window.height; i++) {
            	os.writeShort(chars[i]);
            }
        } catch(Exception e) { e.printStackTrace(); }
        PacketDispatcher.sendPacketToPlayer(new Packet250CustomPayload("ModularC", bos.toByteArray()), player);
    }
    
    public void sendClearPacket() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(32);
        DataOutputStream os = new DataOutputStream(bos);
        try {
        	NetworkHandler.prefixTileEntity(this, os);
            os.writeByte(3);
            os.writeShort(this.window.width);
            os.writeShort(this.window.height);
        } catch(Exception e) { e.printStackTrace(); }
        PacketDispatcher.sendPacketToAllAround(this.xCoord, this.yCoord, this.zCoord, Math.sqrt(this.getMaxRenderDistanceSquared()),
        		this.worldObj.provider.dimensionId, new Packet250CustomPayload("ModularC", bos.toByteArray()));
    }
    
	private boolean isRunning = false;
	private ICPU cpu;
	private IMemoryController memory;
	
	public void run() { // Thread
		while(isRunning) {
			long t_start = System.nanoTime() / 1000000;
			int cyclesLeft = cpu.run(250000 / 20); // 250KHz TODO changeable
			long t_end = System.nanoTime() / 1000000;
			try {
				Thread.sleep(50 - (t_end - t_start));
			} catch(Exception e) { }
		}
	}
	
	public void begin() {
		isRunning = false;
		// Reset window
		this.window = new TextWindow(19, 7);
		this.sendClearPacket();
		// Get memory
		this.memory = getMemoryController();
		if(this.memory == null) return;
		// Initialize peripherals
		terminal = new IOHandlerTerminal(this);
		this.memory.setDeviceSlot(0, terminal);
		this.memory.setDeviceSlot(15, new IOHandlerDebugMC(this));
		// TODO: TEMPORARY CODE - ROM
		byte[] romData = new byte[8192];
		try {
			ModularComputing.class.getClassLoader().getResourceAsStream("assets/moducomp/bios.rom").read(romData);
		} catch(Exception e) { e.printStackTrace(); return; }
		MemoryHandlerROM rom = new MemoryHandlerROM(romData);
		this.memory.setSlot(15, rom);
		// Get CPU
		ItemStack cpuStack = this.getStackInSlot(0);
		if(cpuStack == null || !(cpuStack.getItem() instanceof IItemCPU)) return;
		IItemCPU itemCPU = (IItemCPU)cpuStack.getItem();
		cpu = itemCPU.createNewCPUHandler(cpuStack);
		if(cpu == null) return;
		cpu.setMemoryHandler(memory);
		cpu.resetCold();
		isRunning = true;
		ModularComputing.instance.debug("Starting emulation!");
		new Thread(this).start();
	}
	
	public void end() {
		isRunning = false;
	}
}
