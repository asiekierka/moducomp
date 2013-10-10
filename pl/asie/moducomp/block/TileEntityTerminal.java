package pl.asie.moducomp.block;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

import cpw.mods.fml.common.network.PacketDispatcher;
import cpw.mods.fml.common.network.Player;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import pl.asie.moducomp.ModularComputing;
import pl.asie.moducomp.NetworkHandler;
import pl.asie.moducomp.api.ITileEntityPeripheral;
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
import pl.asie.moducomp.lib.PacketSender;
import pl.asie.moducomp.lib.TileEntityInventory;
import pl.asie.moducomp.peripheral.IOHandlerDebugMC;
import pl.asie.moducomp.peripheral.IOHandlerTerminal;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.Packet250CustomPayload;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class TileEntityTerminal extends TileEntityInventory implements ITileEntityPeripheral
{
	public TextWindow window;
	private IOHandlerTerminal terminal;
	private ICPU cpu;
	
	public TileEntityTerminal() {
		super(1, 1, "block.moducomp.terminal");
		this.terminal = new IOHandlerTerminal(this);
	}
	
	public void print(short color, short chr) { print(color, chr, false); }
	
    public void print(short color, short chr, boolean send) {
    	this.window.print(color, chr);
    	if(send) {
            PacketSender sender = new PacketSender(this, 1);
	        try {
	            sender.stream.writeShort(color);
	            sender.stream.writeShort(chr);
	        } catch(Exception e) { e.printStackTrace(); }
	        sender.sendAround(this);
    	}
    }
    
    public void setPalette(int number, short col) {
    	this.window.setPaletteColor(number, col);
        PacketSender sender = new PacketSender(this, 7);
        try {
            sender.stream.writeByte((byte)number);
            sender.stream.writeShort(col);
        } catch(Exception e) { e.printStackTrace(); }
        sender.sendAround(this);
    }
    
    public void keyExcludingPlayer(short chr, int excludedID) {
        PacketSender sender = new PacketSender(this, 6);
        try {
            sender.stream.writeShort(chr);
            sender.stream.writeInt(excludedID);
        } catch(Exception e) { e.printStackTrace(); }
        sender.sendAround(this);
    }
    
    public void newline(boolean send) {
    	this.window.newline();
    	if(send) {
	        PacketSender sender = new PacketSender(this, 4);
	        sender.sendAround(this);
    	}
    }
    
    public void key(short key, Player sender) {
    	if(sender instanceof EntityPlayer) {
    		EntityPlayer player = (EntityPlayer) sender;
    	}
    	if(this.cpu == null || this.terminal == null) return;
    	if(terminal.addKey(this.cpu, key)) { // Echo
    		this.window.key(key);
    	   	if(sender instanceof EntityPlayer) {
        		EntityPlayer player = (EntityPlayer) sender;
        		keyExcludingPlayer(key, player.entityId);
        	}
    	}
    }
    
    private boolean hardwareEcho;
    
    public void setHardwareEcho(boolean is) {
    	hardwareEcho = is;
    	PacketSender sender = new PacketSender(this, 5);
        try {
            sender.stream.writeBoolean(is);
        } catch(Exception e) { e.printStackTrace(); }
        sender.sendAround(this);
    }
    
    public void onPlayerOpen(Player player) {
    	if(this.window == null) clear(false);
    	PacketSender sender = new PacketSender(this, 2);
        try {
            sender.stream.writeShort(this.window.width);
            sender.stream.writeShort(this.window.height);
            sender.stream.writeShort(this.window.x);
            sender.stream.writeShort(this.window.y);
            sender.stream.writeBoolean(this.hardwareEcho);
            short[] chars = this.window.getCharArray();
            for(int i = 0; i < this.window.width * this.window.height * 2; i++) {
            	sender.stream.writeShort(chars[i]);
            }
            short[] colors = this.window.getPalette();
            for(int i = 0; i < 64; i++) {
            	sender.stream.writeShort(colors[i]);
            }
        } catch(Exception e) { e.printStackTrace(); }
        sender.sendToPlayer(player);
    }
    
    public IMemory init(ICPU cpu, IMemoryController memoryController) {
    	this.cpu = cpu;
		clear(true);
        return this.terminal;
    }
    
	private void clear(boolean send) {
		this.window = new TextWindow(30, 22);
		if(send) {
	        PacketSender sender = new PacketSender(this, 3);
	        try {
	            sender.stream.writeShort(this.window.width);
	            sender.stream.writeShort(this.window.height);
	        } catch(Exception e) { e.printStackTrace(); }
	        sender.sendAround(this);
		}
    }
    
    public void deinit(ICPU cpu) {
    	this.cpu = null;
    	this.terminal = null;
    	clear(true);
    }

	@Override
	public int getPreferredDeviceID() {
		return -1;
	}
}
