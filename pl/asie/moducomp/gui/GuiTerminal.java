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
import pl.asie.moducomp.block.TileEntityTerminal;
import pl.asie.moducomp.gui.text.TextRenderer;
import pl.asie.moducomp.gui.text.TextWindow;
import pl.asie.moducomp.item.ItemPaperTape;
import pl.asie.moducomp.lib.ContainerInventory;
import pl.asie.moducomp.lib.ContainerNull;
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
public class GuiTerminal extends GuiContainer implements IGUIText
{
	private TextRenderer textRenderer = new TextRenderer();
	private TextWindow window;
	private TileEntity tileEntity;
	private boolean hardwareEcho;
	private ResourceLocation texture;
	
	public TextWindow getWindow() { return window; }
	public void setWindow(TextWindow window) {
		this.window = window;
	}
	public boolean getHardwareEcho() { return hardwareEcho; }
	public void setHardwareEcho(boolean echo) {
		this.hardwareEcho = echo;
	}
	
	public TileEntity getTileEntity() {
		return this.tileEntity;
	}
	
	public GuiTerminal(InventoryPlayer inventoryPlayer, TileEntityTerminal tileEntity) {
		super(new ContainerNull());
		this.tileEntity = tileEntity;
		this.texture = new ResourceLocation("moducomp", "textures/gui/terminal.png");
		this.xSize = 256;
		this.ySize = 196;
		PacketDispatcher.sendPacketToServer(sendGetWindowPacket());
	}

    /**
     * Draw the foreground layer for the GuiContainer (everything in front of the items)
     */
    protected void drawGuiContainerForegroundLayer(int par1, int par2)
    {
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
    
    int keysTyped = 0;
    
    @Override
    protected void keyTyped(char keyChar, int keyCode)
    {
        if (keyCode == 1) {
            this.mc.displayGuiScreen((GuiScreen)null);
            this.mc.setIngameFocus();
        } else {
        	try {
        		while(keysTyped > 0) { Thread.sleep(5); }
        	} catch(Exception e) { }
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
            if(hardwareEcho) {
            	try {
            		keysTyped++;
            		window.key(key);
            		Thread.sleep(5);
            		keysTyped--;
            	} catch(Exception e) { }
            }
        }
    }
    
    /**
     * Draw the background layer for the GuiContainer (everything behind the items)
     */
    protected void drawGuiContainerBackgroundLayer(float par1, int par2, int par3)
    {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(texture);
        int xo = (this.width - this.xSize) / 2;
        int yo = (this.height - this.ySize) / 2;
        this.drawTexturedModalRect(xo, yo, 0, 0, this.xSize, this.ySize);
        if(window != null) textRenderer.renderWindow(this, this.mc.getTextureManager(), window, xo+8, yo+12, TextRenderer.COLOR_WHITE);
    }
}
