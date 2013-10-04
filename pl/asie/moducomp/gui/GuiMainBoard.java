package pl.asie.moducomp.gui;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.logging.Level;

import org.lwjgl.opengl.GL11;

import pl.asie.moducomp.api.IItemTape;
import pl.asie.moducomp.block.TileEntityMainBoard;
import pl.asie.moducomp.block.TileEntityMusicBox;
import pl.asie.moducomp.gui.text.TextRenderer;
import pl.asie.moducomp.lib.ContainerInventory;
import pl.asie.moducomp.lib.GuiInventory;
import pl.asie.moducomp.lib.TileEntityInventory;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.Packet250CustomPayload;
import net.minecraft.util.ResourceLocation;
import cpw.mods.fml.common.network.PacketDispatcher;
import cpw.mods.fml.relauncher.SideOnly;
import cpw.mods.fml.relauncher.Side;

@SideOnly(Side.CLIENT)
public class GuiMainBoard extends GuiInventory
{
	private TextRenderer textRenderer = new TextRenderer();
	
	public GuiMainBoard(InventoryPlayer inventoryPlayer, TileEntityInventory tileEntity, ContainerInventory inventory, int xs, int ys, String textureName) {
		super(inventoryPlayer, tileEntity, inventory, xs, ys, textureName);
	}

    /**
     * Draw the foreground layer for the GuiContainer (everything in front of the items)
     */
    protected void drawGuiContainerForegroundLayer(int par1, int par2)
    {
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
        textRenderer.renderWindow(this, this.mc.getTextureManager(), mb.window, xo+11, yo+43, TextRenderer.COLOR_WHITE);
    }
}
