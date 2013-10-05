package pl.asie.moducomp.block;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

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

public class TileEntityTerminal extends TileEntityInventory implements IEntityPeripheral
{
	public TextWindow window;
	private IOHandlerTerminal terminal;
	private ICPU cpu;
	
	public TileEntityTerminal() {
		super(1, 1, "block.moducomp.terminal");
	}
	
	public void print(short chr) { print(chr, false); }
	
    public void print(short chr, boolean send) {
    	this.window.print(chr);
    	if(send) {
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
    }
    
    public void newline(boolean send) {
    	this.window.newline();
    	if(send) {
	        ByteArrayOutputStream bos = new ByteArrayOutputStream(32);
	        DataOutputStream os = new DataOutputStream(bos);
	        try {
	        	NetworkHandler.prefixTileEntity(this, os);
	            os.writeByte(4);
	        } catch(Exception e) { e.printStackTrace(); }
	        PacketDispatcher.sendPacketToAllAround(this.xCoord, this.yCoord, this.zCoord, Math.sqrt(this.getMaxRenderDistanceSquared()),
	        		this.worldObj.provider.dimensionId, new Packet250CustomPayload("ModularC", bos.toByteArray()));
    	}
    }
    
    public void key(short key) {
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
    
    public void onPlayerOpen(Player player) {
    	if(this.window == null) clear(false);
        ByteArrayOutputStream bos = new ByteArrayOutputStream(24 + (this.window.width * this.window.height));
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
    
    public IMemory init(ICPU cpu, IMemoryController memoryController) {
    	//if(this.cpu != null) return null;
    	this.cpu = cpu;
		this.terminal = new IOHandlerTerminal(this);
		clear(true);
        return this.terminal;
    }
    
	private void clear(boolean send) {
		this.window = new TextWindow(30, 22);
		if(send) {
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
    }
    
    public void deinit(ICPU cpu) {
    	if(this.cpu != cpu) return;
    	this.cpu = null;
    	this.terminal = null;
    	clear(true);
    }
}
