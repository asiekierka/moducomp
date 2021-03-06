package pl.asie.moducomp;

import java.io.*;
import java.util.*;

import pl.asie.moducomp.api.ITileEntityPeripheral;
import pl.asie.moducomp.api.IGUIText;
import pl.asie.moducomp.block.TileEntityMainBoard;
import pl.asie.moducomp.block.TileEntityTapeReader;
import pl.asie.moducomp.block.TileEntityTerminal;
import pl.asie.moducomp.gui.GuiTerminal;
import pl.asie.moducomp.gui.text.TextWindow;
import pl.asie.moducomp.lib.Helper;
import pl.asie.moducomp.lib.IGUITileEntity;
import net.minecraft.src.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;
import net.minecraft.tileentity.TileEntity;
import cpw.mods.fml.common.*;
import cpw.mods.fml.common.network.*;
import cpw.mods.fml.relauncher.Side;
import net.minecraftforge.common.*;
import net.minecraft.network.*;
import net.minecraft.network.packet.*;
import net.minecraft.network.*;

public class NetworkHandler implements IPacketHandler {
	
	public static void prefixTileEntity(TileEntity entity, DataOutputStream os) {
		try {
            os.writeByte(1);
			os.writeInt(entity.worldObj.provider.dimensionId);
            os.writeInt(entity.xCoord);
            os.writeInt(entity.yCoord);
            os.writeInt(entity.zCoord);
		} catch(Exception e) { e.printStackTrace(); }
	}
	
	@Override
	public void onPacketData(INetworkManager manager, Packet250CustomPayload packet, Player player) {
		ModularComputing.debug("Received packet data: " + packet.data.toString());
		DataInputStream packetData = new DataInputStream(new ByteArrayInputStream(packet.data));
		try {
			int commandType = packetData.readByte();
			switch(commandType) {
				case 1: { // TileEntity related
					World world;
					if(!(player instanceof EntityPlayerMP)) { // Client
						world = ((EntityLivingBase)player).worldObj;
						if(world.provider.dimensionId != packetData.readInt()) return;
					} else world = DimensionManager.getWorld(packetData.readInt()); // Server
	                int x = packetData.readInt();
	                int y = packetData.readInt();
	                int z = packetData.readInt();
	                if(world == null) return;
	                TileEntity tileEntity = world.getBlockTileEntity(x, y, z);
	                int block = world.getBlockId(x,  y,  z);
	                parseTEPacket(manager, packetData, player, block, tileEntity);
				} break;
				default: break; // No command
			}
		} catch(Exception e) { e.printStackTrace(); }
	}
	
	public void parseTEPacket(INetworkManager manager, DataInputStream packetData, Player player, int block, TileEntity tileEntity) {
		try {
			int commandID = packetData.readUnsignedByte();
        	if(!(player instanceof EntityPlayerMP)) { // Server -> Client
        		GuiScreen display = Minecraft.getMinecraft().currentScreen;
        		IGUITileEntity gui = null;
        		if(display instanceof IGUITileEntity) {
        			gui = (IGUITileEntity)display;
        			TileEntity checkedTileEntity = gui.getTileEntity();
					ModularComputing.instance.logger.info("Received Terminal command #"+commandID+" on "+(!(player instanceof EntityPlayerMP) ? "client" : "server"));
        			if(Helper.equalTileEntities(tileEntity, checkedTileEntity)) {
        				if(tileEntity instanceof TileEntityTerminal && gui instanceof IGUIText) {
        					IGUIText textGui = (IGUIText)gui;
        	        		TextWindow window = textGui.getWindow();
        	        		switch(commandID) {
                			case 1: { // Print character
                				short color = packetData.readShort();
                				window.print(color, packetData.readShort());
                			} break;
                			case 2: { // Get initial data
                				int width = packetData.readShort();
                				int height = packetData.readShort();
                				int x = packetData.readShort();
                				int y = packetData.readShort();
                				textGui.setHardwareEcho(packetData.readBoolean());
                				short[] chars = new short[width*height*2];
                				for(int i = 0; i < width*height*2; i++)
                					chars[i] = packetData.readShort();
                				window = new TextWindow(width, height);
                				window.x = x;
                				window.y = y;
                				window.setCharArray(chars);
                				for(int i = 0; i < 64; i++)
                					window.setPaletteColor(i, packetData.readShort());
                				textGui.setWindow(window);
                			} break;
                			case 3: { // Clear window
                				int width = packetData.readShort();
                				int height = packetData.readShort();
                				textGui.setWindow(new TextWindow(width, height));
                			} break;
                			case 4: { // Newline
                				window.newline();
                			} break;
                			case 5: { // Hardware echo
                				textGui.setHardwareEcho(packetData.readBoolean());
                			} break;
                			case 6: { // Echo key
                				short key = packetData.readShort();
                				int excludedID = packetData.readInt();
                				if(player instanceof EntityPlayer) {
                					EntityPlayer entity = (EntityPlayer)player;
                					if(entity.entityId != excludedID && window != null) {
                						window.key(key);
                					}
                				}
                			} break;
                			case 7: { // Set palette color
                				int number = packetData.readUnsignedByte();
                				window.setPaletteColor(number, packetData.readShort());
                			} break;
        	        		}
        	 
        				}
        			}
        		}
        	}
	        if(tileEntity instanceof TileEntityTapeReader) {
	        	TileEntityTapeReader tapeReader = (TileEntityTapeReader)tileEntity;
	        	switch(commandID) {
	        		case 1: { // Set bit
	                	tapeReader.setBit(packetData.readInt(), packetData.readByte(), packetData.readByte());
	        		} break;
	        		case 2: { // Set position
	        			tapeReader.setPosition(packetData.readInt());
	        		} break;
	        	}
	        } else if(tileEntity instanceof TileEntityTerminal) {
	        	TileEntityTerminal terminal = (TileEntityTerminal)tileEntity;
	        	if(player instanceof EntityPlayerMP) { // Client -> Server
	        		switch(commandID) {
	        			case 2: { // Request initial data
	        				terminal.onPlayerOpen(player);
	        			} break;
	        			case 3: { // Key typed
	        				terminal.key(packetData.readShort(), player);
	        			} break;
	        		}
	        	}
	        }
		} catch(Exception e) { e.printStackTrace(); }
	}
}