package pl.asie.moducomp.gui;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.logging.Level;

import org.lwjgl.opengl.GL11;

import pl.asie.moducomp.ModularComputing;
import pl.asie.moducomp.NetworkHandler;
import pl.asie.moducomp.api.IGUIText;
import pl.asie.moducomp.api.IItemTape;
import pl.asie.moducomp.block.TileEntityMainBoard;
import pl.asie.moducomp.block.TileEntityMusicBox;
import pl.asie.moducomp.gui.text.TextRenderer;
import pl.asie.moducomp.gui.text.TextWindow;
import pl.asie.moducomp.item.ItemPaperTape;
import pl.asie.moducomp.lib.ContainerInventory;
import pl.asie.moducomp.lib.GuiInventory;
import pl.asie.moducomp.lib.TileEntityInventory;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.Packet250CustomPayload;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import cpw.mods.fml.common.network.PacketDispatcher;
import cpw.mods.fml.relauncher.SideOnly;
import cpw.mods.fml.relauncher.Side;

@SideOnly(Side.CLIENT)
public class GuiMainBoard extends GuiInventory implements IGUIText
{
	private TextRenderer textRenderer = new TextRenderer();
	private TextWindow window;
	private boolean hardwareEcho;
	
	public TextWindow getWindow() { return window; }
	public void setWindow(TextWindow window) {
		this.window = window;
	}
	public boolean getHardwareEcho() { return hardwareEcho; }
	public void setHardwareEcho(boolean echo) {
		this.hardwareEcho = echo;
	}
	
	public GuiMainBoard(InventoryPlayer inventoryPlayer, TileEntityInventory tileEntity, ContainerInventory inventory, int xs, int ys, String textureName) {
		super(inventoryPlayer, tileEntity, inventory, xs, ys, textureName);
		PacketDispatcher.sendPacketToServer(sendGetWindowPacket());
	}

    /**
     * Draw the foreground layer for the GuiContainer (everything in front of the items)
     */
    protected void drawGuiContainerForegroundLayer(int par1, int par2)
    {
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
    
    private Packet250CustomPayload sendGetWindowPacket() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(32);
        DataOutputStream os = new DataOutputStream(bos);
        try {
        	NetworkHandler.prefixTileEntity(this.tileEntity, os);
            os.writeByte(2);
        } catch(Exception e) { e.printStackTrace(); }
        return new Packet250CustomPayload("ModularC", bos.toByteArray());
    }
    
    @Override
    protected void keyTyped(char keyChar, int keyCode)
    {
        if (keyCode == 1) {
            this.mc.displayGuiScreen((GuiScreen)null);
            this.mc.setIngameFocus();
        } else if(keyCode == 200) { // Press UP to play (TODO MAKE THIS REAL)
        	PacketDispatcher.sendPacketToServer(sendTurnOnPacket());
        } else {
        	int key = (int)keyChar;
        	ModularComputing.instance.logger.info("Pressed key " + key);
            ByteArrayOutputStream bos = new ByteArrayOutputStream(32);
            DataOutputStream os = new DataOutputStream(bos);
            try {
            	NetworkHandler.prefixTileEntity(this.tileEntity, os);
                os.writeByte(3);
                os.writeShort((short)key);
            } catch(Exception e) { e.printStackTrace(); }
            PacketDispatcher.sendPacketToServer(new Packet250CustomPayload("ModularC", bos.toByteArray()));
            if(hardwareEcho) window.key(key);
        }
    }
    
    /**
     * Draw the background layer for the GuiContainer (everything behind the items)
     */
    protected void drawGuiContainerBackgroundLayer(float par1, int par2, int par3)
    {
    	if(!(this.tileEntity instanceof TileEntityMainBoard)) return; // wat
    	TileEntityMainBoard mb = (TileEntityMainBoard)this.tileEntity;
        super.drawGuiContainerBackgroundLayer(par1, par2, par3);
        int xo = (this.width - this.xSize) / 2;
        int yo = (this.height - this.ySize) / 2;
        if(window != null) textRenderer.renderWindow(this, this.mc.getTextureManager(), window, xo+11, yo+43, TextRenderer.COLOR_WHITE);
    }
}
