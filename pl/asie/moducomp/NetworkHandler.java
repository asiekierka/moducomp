package pl.asie.moducomp;

import java.io.*;
import java.util.*;
import net.minecraft.src.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraft.tileentity.TileEntity;
import cpw.mods.fml.common.*;
import cpw.mods.fml.common.network.*;
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
	        	ModularComputing.debug("Received command ID #"+commandID+"!");
	        	switch(commandID) {
	        		case 1: { // Set bit
	                	tapeReader.setBit(packetData.readInt(), packetData.readByte(), packetData.readByte());
	        		} break;
	        		case 2: { // Set position
	        			tapeReader.setPosition(packetData.readInt());
	        		} break;
	        	}
	        }
		} catch(Exception e) { e.printStackTrace(); }
	}
}