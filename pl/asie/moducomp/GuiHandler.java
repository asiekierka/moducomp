package pl.asie.moducomp;

import pl.asie.moducomp.block.TileEntityMainBoard;
import pl.asie.moducomp.block.TileEntityMusicBox;
import pl.asie.moducomp.block.TileEntityRAMBoard;
import pl.asie.moducomp.block.TileEntityTapeReader;
import pl.asie.moducomp.block.TileEntityTerminal;
import pl.asie.moducomp.gui.ContainerMainBoard;
import pl.asie.moducomp.gui.ContainerMusicBox;
import pl.asie.moducomp.gui.ContainerRAMBoard;
import pl.asie.moducomp.gui.ContainerTapeReader;
import pl.asie.moducomp.gui.GuiMainBoard;
import pl.asie.moducomp.gui.GuiTerminal;
import pl.asie.moducomp.gui.GuiTapeReader;
import pl.asie.moducomp.lib.ContainerInventory;
import pl.asie.moducomp.lib.ContainerNull;
import pl.asie.moducomp.lib.GuiInventory;
import pl.asie.moducomp.lib.TileEntityInventory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import cpw.mods.fml.common.network.IGuiHandler;

public class GuiHandler implements IGuiHandler {
    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
            TileEntity tileEntity = world.getBlockTileEntity(x, y, z);
            if(tileEntity instanceof TileEntityTapeReader){
             	return new ContainerTapeReader(player.inventory, (TileEntityTapeReader) tileEntity);
            }
            else if(tileEntity instanceof TileEntityMusicBox){
                return new ContainerMusicBox(player.inventory, (TileEntityMusicBox) tileEntity);
            }
            else if(tileEntity instanceof TileEntityRAMBoard) {
            	return new ContainerRAMBoard(player.inventory, (TileEntityRAMBoard) tileEntity);
            }
            else if(tileEntity instanceof TileEntityMainBoard) {
            	return new ContainerMainBoard(player.inventory, (TileEntityMainBoard) tileEntity);
            }
            else if(tileEntity instanceof TileEntityTerminal) {
            	return new ContainerNull();
            }
            return null;
    }

    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
            TileEntity tileEntity = world.getBlockTileEntity(x, y, z);
            if(tileEntity instanceof TileEntityTapeReader){
            	return new GuiTapeReader(player.inventory, (TileEntityTapeReader) tileEntity);
            }
            else if(tileEntity instanceof TileEntityMusicBox){
                return new GuiInventory(player.inventory, (TileEntityInventory) tileEntity, 
                		(ContainerInventory) new ContainerMusicBox(player.inventory, (TileEntityMusicBox) tileEntity),
                		176, 134, "one_slot");
            }
            else if(tileEntity instanceof TileEntityRAMBoard) {
                return new GuiInventory(player.inventory, (TileEntityInventory) tileEntity, 
                		(ContainerInventory) new ContainerRAMBoard(player.inventory, (TileEntityRAMBoard) tileEntity),
                		176, 185, "ram_board");
            }
            else if(tileEntity instanceof TileEntityMainBoard) {
                return new GuiMainBoard(player.inventory, (TileEntityInventory) tileEntity, 
                		(ContainerInventory) new ContainerMainBoard(player.inventory, (TileEntityMainBoard) tileEntity),
                		176, 134, "one_slot");
            }
            else if(tileEntity instanceof TileEntityTerminal) {
                return new GuiTerminal(player.inventory, (TileEntityTerminal) tileEntity);
            }
            return null;

    }
}
