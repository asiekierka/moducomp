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
import pl.asie.moducomp.lib.PacketSender;
import pl.asie.moducomp.lib.TileEntityInventory;

public class GuiRAMBoard extends GuiInventory {

	public GuiRAMBoard(InventoryPlayer inventoryPlayer,
			TileEntityInventory tileEntity, ContainerRAMBoard inventory,
			int xs, int ys, String textureName) {
		super(inventoryPlayer, tileEntity, inventory, xs, ys, textureName);
	}

}
