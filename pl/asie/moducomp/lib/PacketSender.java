package pl.asie.moducomp.lib;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

import cpw.mods.fml.common.network.PacketDispatcher;
import cpw.mods.fml.common.network.Player;
import net.minecraft.network.packet.Packet250CustomPayload;
import net.minecraft.tileentity.TileEntity;
import pl.asie.moducomp.NetworkHandler;

public class PacketSender {
	public DataOutputStream stream;
	private ByteArrayOutputStream bos;
	
	public PacketSender() {
		this.bos = new ByteArrayOutputStream();
        stream = new DataOutputStream(this.bos);
	}
	
	public PacketSender(TileEntity te) {
		this.bos = new ByteArrayOutputStream();
        stream = new DataOutputStream(this.bos);
		prefixTileEntity(te);
	}
	
	public PacketSender(TileEntity te, int command) {
		this.bos = new ByteArrayOutputStream();
        stream = new DataOutputStream(this.bos);
		prefixTileEntity(te);
		try {
			stream.writeByte(command);
		} catch(Exception e) { e.printStackTrace(); }
	}
	
	public Packet250CustomPayload getPacket() {
		return new Packet250CustomPayload("ModularC", bos.toByteArray());
	}
	
	public void prefixTileEntity(TileEntity te) {
        try {
        	NetworkHandler.prefixTileEntity(te, this.stream);
        } catch(Exception e) { e.printStackTrace(); }
	}
	
	public void sendToPlayer(Player player) {
		PacketDispatcher.sendPacketToPlayer(this.getPacket(), player);
	}
	
	public void sendAround(TileEntity te) {
        PacketDispatcher.sendPacketToAllAround(te.xCoord, te.yCoord, te.zCoord, 24.0D,
        		te.worldObj.provider.dimensionId, this.getPacket());
	}
	
	public void sendServer() {
		PacketDispatcher.sendPacketToServer(this.getPacket());
	}
}
