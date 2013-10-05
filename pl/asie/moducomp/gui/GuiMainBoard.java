package pl.asie.moducomp.gui;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

import cpw.mods.fml.common.network.PacketDispatcher;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.network.packet.Packet250CustomPayload;
import pl.asie.moducomp.NetworkHandler;
import pl.asie.moducomp.lib.ContainerInventory;
import pl.asie.moducomp.lib.GuiInventory;
import pl.asie.moducomp.lib.TileEntityInventory;

public class GuiMainBoard extends GuiInventory {

	public GuiMainBoard(InventoryPlayer inventoryPlayer,
			TileEntityInventory tileEntity, ContainerInventory inventory,
			int xs, int ys, String textureName) {
		super(inventoryPlayer, tileEntity, inventory, xs, ys, textureName);
	}

    @Override
    protected void keyTyped(char keyChar, int keyCode)
    {
    	super.keyTyped(keyChar, keyCode);
        if(keyCode == 200) { // Press UP to play (TODO MAKE THIS REAL)
        	PacketDispatcher.sendPacketToServer(sendTurnOnPacket());
        }
    }
    
    private Packet250CustomPayload sendTurnOnPacket() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(32);
        DataOutputStream os = new DataOutputStream(bos);
        try {
        	NetworkHandler.prefixTileEntity(this.tileEntity, os);
            os.writeByte(1);
            os.writeBoolean(true);
        } catch(Exception e) { e.printStackTrace(); }
        return new Packet250CustomPayload("ModularC", bos.toByteArray());
    }
    
}
