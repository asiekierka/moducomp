package pl.asie.moducomp.gui;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.logging.Level;

import org.lwjgl.opengl.GL11;

import pl.asie.moducomp.NetworkHandler;
import pl.asie.moducomp.api.IItemTape;
import pl.asie.moducomp.block.TileEntityTapeReader;
import pl.asie.moducomp.item.ItemPaperTape;
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
public class GuiTapeReader extends GuiContainer
{
    private static final ResourceLocation tapeTexture = new ResourceLocation("moducomp", "textures/gui/tape_reader.png");
    private ContainerTapeReader tapeReaderInventory;
    private TileEntityTapeReader tapeReaderEntity;

	public GuiTapeReader(InventoryPlayer inventoryPlayer, TileEntityTapeReader tileEntity) {
		super(new ContainerTapeReader(inventoryPlayer, tileEntity));
		tapeReaderEntity = tileEntity;
		xSize = 176;
		ySize = 166;
	}

    /**
     * Draw the foreground layer for the GuiContainer (everything in front of the items)
     */
    protected void drawGuiContainerForegroundLayer(int par1, int par2)
    {
    }
    
    private static final int TAPE_HOLES_Y = 172;
    private static final int TAPE_OUTLINES_Y = 185;
    private static final int TAPE_Y = 38;
    private static final int TAPE_YOFF = 14;
    private static final int[][] TAPE_INFO = {
    		{62, 0},
    		{74, 12},
    		{86, 24},
    		{109, 47},
    		{121, 59},
    		{133, 71},
    		{145, 83},
    		{157, 95},
    		{98, 36} // clock
    };

    protected void drawTape(ItemStack tape, IItemTape tapeHandler, int offset, int xo, int yo) {
    	if(tapeHandler.isValid(tape, offset)) {
    		int value = tapeHandler.getByte(tape, offset) & 0xFF;
    		for(int i = 0; i <= 8; i++) { // bit 8 is clock, <= is VALID
    			if(i == 8 || (value & (1 << (7 - i))) > 0) {
    				this.drawTexturedModalRect(xo+TAPE_INFO[i][0], yo+TAPE_Y + (offset*TAPE_YOFF), TAPE_INFO[i][1], TAPE_HOLES_Y, 9, 9);
    			} else {
    				this.drawTexturedModalRect(xo+TAPE_INFO[i][0], yo+TAPE_Y + (offset*TAPE_YOFF), TAPE_INFO[i][1], TAPE_OUTLINES_Y, 9, 9);
    			}
    		}
    	}
    }
    
    @Override
    protected void keyTyped(char keyChar, int key)
    {
    	super.keyTyped(keyChar, key);
        ItemStack tape = tapeReaderEntity.getStackInSlot(0);
        if(tape != null && tape.getItem() instanceof ItemPaperTape) {
        	ItemPaperTape tapeHandler = (ItemPaperTape)tape.getItem();
        	boolean changed = true;
        	if (key == 200) {
        		tapeHandler.seek(tape, -1);
        	} else if(key == 208) {
        		tapeHandler.seek(tape, 1);
        	} else changed = false;
        	if(changed) PacketDispatcher.sendPacketToServer(sendPositionPacket()); // HACK!
        }
    }
   
    private Packet250CustomPayload sendPositionPacket() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(32);
        DataOutputStream os = new DataOutputStream(bos);
        try {
        	NetworkHandler.prefixTileEntity(this.tapeReaderEntity, os);
            os.writeByte(2);
            os.writeInt(this.tapeReaderEntity.getPosition());
        } catch(Exception e) { e.printStackTrace(); }
        return new Packet250CustomPayload("ModularC", bos.toByteArray());
    }
    
    private Packet250CustomPayload sendBitSetPacket(byte offset, byte shift) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(32);
        DataOutputStream os = new DataOutputStream(bos);
        try {
        	NetworkHandler.prefixTileEntity(this.tapeReaderEntity, os);
            os.writeByte(1);
            os.writeInt(this.tapeReaderEntity.getPosition());
            os.writeByte(offset); os.writeByte(shift);
        } catch(Exception e) { e.printStackTrace(); }
        return new Packet250CustomPayload("ModularC",bos.toByteArray());
    }
    @Override
    protected void mouseClicked(int x, int y, int button) {
    	super.mouseClicked(x, y, button);
        if (button == 0)
        {
            int xo = (this.width - this.xSize) / 2;
            int yo = (this.height - this.ySize) / 2;
            int bit = -1;
            for(int i = 0; i <= 7; i++) {
            	int xpos = xo+TAPE_INFO[i][0];
            	if(x >= xpos && x < xpos+9) { bit = 7-i; break; }
            }
            if(bit >= 0) { // X found
            	int offset = -3;
            	for(int i = -2; i <= 2; i++) {
            		int ypos = yo + TAPE_Y + (i*TAPE_YOFF);
            		if(y >= ypos && y < ypos+9) { offset = i; break; }
            	}
            	if(offset >= -3) { // Y found
            		PacketDispatcher.sendPacketToServer(sendBitSetPacket((byte)offset, (byte)bit));
            	}
            }
        }
    }
    
    public void onGuiClosed() {
    	PacketDispatcher.sendPacketToServer(sendPositionPacket());
    }
    
    /**
     * Draw the background layer for the GuiContainer (everything behind the items)
     */
    protected void drawGuiContainerBackgroundLayer(float par1, int par2, int par3)
    {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(tapeTexture);
        int xo = (this.width - this.xSize) / 2;
        int yo = (this.height - this.ySize) / 2;
        ItemStack tape = tapeReaderEntity.getStackInSlot(0);
        if(tape != null && tape.getItem() instanceof ItemPaperTape) {
        	ItemPaperTape tapeHandler = (ItemPaperTape)tape.getItem();
        	this.drawTexturedModalRect(xo+60, yo+14, 0, 199, 108, 57);
        	for(int i = -2; i <= 2; i++) {
        		drawTape(tape, tapeHandler, i, xo, yo);
        	}
        } else this.drawTexturedModalRect(xo+60, yo+14, 108, 199, 108, 57);
        this.drawTexturedModalRect(xo, yo, 0, 0, this.xSize, this.ySize);
    }
}
