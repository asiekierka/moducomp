package pl.asie.moducomp;

import java.io.*;
import java.util.*;

import pl.asie.moducomp.block.TileEntityMainBoard;
import pl.asie.moducomp.block.TileEntityTapeReader;
import pl.asie.moducomp.gui.GuiMainBoard;
import pl.asie.moducomp.gui.text.TextWindow;
import net.minecraft.src.*;
import net.minecraft.client.Minecraft;
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
					World world = DimensionManager.getWorld(packetData.readInt());
	                int x = packetData.readInt();
	                int y = packetData.readInt();
	                int z = packetData.readInt();
	                if(world == null) return;
	                TileEntity tileEntity = world.getBlockTileEntity(x, y, z);
	                parseTEPacket(manager, packetData, player, tileEntity);
				} break;
				default: break; // No command
			}
		} catch(Exception e) { e.printStackTrace(); }
	}
	
	public void parseTEPacket(INetworkManager manager, DataInputStream packetData, Player player, TileEntity tileEntity) {
		try {
	        if(tileEntity instanceof TileEntityTapeReader) {
	        	TileEntityTapeReader tapeReader = (TileEntityTapeReader)tileEntity;
	        	int commandID = packetData.readUnsignedByte();
	        	switch(commandID) {
	        		case 1: { // Set bit
	                	tapeReader.setBit(packetData.readInt(), packetData.readByte(), packetData.readByte());
	        		} break;
	        		case 2: { // Set position
	        			tapeReader.setPosition(packetData.readInt());
	        		} break;
	        	}
	        } else if(tileEntity instanceof TileEntityMainBoard) {
	        	TileEntityMainBoard mainBoard = (TileEntityMainBoard)tileEntity;
	        	int commandID = packetData.readUnsignedByte();
	        	ModularComputing.instance.logger.info("Received Mainboard command #"+commandID+" on "+(!(player instanceof EntityPlayerMP) ? "client" : "server"));
				GuiMainBoard gmb = GuiMainBoard.instance;
	        	if(!(player instanceof EntityPlayerMP)) { // Server -> Client
	        		switch(commandID) {
	        			case 1: { // Print character
	        				gmb.window.print(packetData.readShort());
	        			} break;
	        			case 2: { // Get initial data
	        				int width = packetData.readShort();
	        				int height = packetData.readShort();
	        				int x = packetData.readShort();
	        				int y = packetData.readShort();
	        				short[] chars = new short[width*height];
	        				for(int i = 0; i < width*height; i++) {
	        					chars[i] = packetData.readShort();
	        				}
	        				gmb.window = new TextWindow(width, height);
	        				gmb.window.x = x;
	        				gmb.window.y = y;
	        				gmb.window.setCharArray(chars);
	        			} break;
	        			case 3: { // Clear window
	        				int width = packetData.readShort();
	        				int height = packetData.readShort();
	        				gmb.window = new TextWindow(width, height);
	        			} break;
	        		}
	        	} else { // Client -> Server
	        		switch(commandID) {
	        			case 1: { // Start/stop CPU
	        				boolean should = packetData.readBoolean();
	        				if(should) {
	        					mainBoard.begin();
	        				} else mainBoard.end();
	        			} break;
	        			case 2: { // Request initial data
	        				mainBoard.sendInitialWindowPacket(player);
	        			} break;
	        		}
	        	}
	        }
		} catch(Exception e) { e.printStackTrace(); }
	}
}