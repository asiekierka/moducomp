package pl.asie.moducomp.gui;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.logging.Level;

import org.lwjgl.opengl.GL11;

import pl.asie.moducomp.api.ITape;
import pl.asie.moducomp.block.TileEntityMusicBox;
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
public class GuiMusicBox extends GuiContainer
{
    private static final ResourceLocation texture = new ResourceLocation("moducomp", "textures/gui/one_slot.png");
    private ContainerMusicBox musicBoxInventory;
    private TileEntityMusicBox musicBoxEntity;

	public GuiMusicBox(InventoryPlayer inventoryPlayer, TileEntityMusicBox tileEntity) {
		super(new ContainerMusicBox(inventoryPlayer, tileEntity));
		musicBoxEntity = tileEntity;
		xSize = 176;
		ySize = 134;
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
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(texture);
        int xo = (this.width - this.xSize) / 2;
        int yo = (this.height - this.ySize) / 2;
        this.drawTexturedModalRect(xo, yo, 0, 0, this.xSize, this.ySize);
    }
}
