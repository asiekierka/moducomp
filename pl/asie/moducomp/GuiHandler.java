package pl.asie.moducomp;

import pl.asie.moducomp.block.TileEntityMusicBox;
import pl.asie.moducomp.block.TileEntityTapeReader;
import pl.asie.moducomp.gui.ContainerMusicBox;
import pl.asie.moducomp.gui.ContainerTapeReader;
import pl.asie.moducomp.gui.GuiMusicBox;
import pl.asie.moducomp.gui.GuiTapeReader;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import cpw.mods.fml.common.network.IGuiHandler;

public class GuiHandler implements IGuiHandler {
    //returns an instance of the Container you made earlier
    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
            TileEntity tileEntity = world.getBlockTileEntity(x, y, z);
            if(tileEntity instanceof TileEntityTapeReader){
                    return new ContainerTapeReader(player.inventory, (TileEntityTapeReader) tileEntity);
            }
            if(tileEntity instanceof TileEntityMusicBox){
                return new ContainerMusicBox(player.inventory, (TileEntityMusicBox) tileEntity);
        }
            return null;
    }

    //returns an instance of the Gui you made earlier
    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
            TileEntity tileEntity = world.getBlockTileEntity(x, y, z);
            if(tileEntity instanceof TileEntityTapeReader){
                    return new GuiTapeReader(player.inventory, (TileEntityTapeReader) tileEntity);
            }
            if(tileEntity instanceof TileEntityMusicBox){
                return new GuiMusicBox(player.inventory, (TileEntityMusicBox) tileEntity);
            }
            return null;

    }
}
