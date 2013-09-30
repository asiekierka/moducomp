package pl.asie.moducomp.integration.openperipheral;

import dan200.computer.api.IComputerAccess;
import openperipheral.api.Arg;
import openperipheral.api.IPeripheralAdapter;
import openperipheral.api.LuaMethod;
import openperipheral.api.LuaType;
import net.minecraft.item.ItemStack;
import pl.asie.moducomp.block.TileEntityMusicBox;
import pl.asie.moducomp.item.ItemPaperTape;

public class AdapterMusicBox implements IPeripheralAdapter {
	@Override
	public Class getTargetClass() {
		return TileEntityMusicBox.class;
	}
	
	@LuaMethod(returnType = LuaType.BOOLEAN, onTick = false, description = "Play a single note.", args = {})
	public boolean playNote(IComputerAccess access, TileEntityMusicBox musicBox) {
		return musicBox.playNote();
	}
	
	@LuaMethod(returnType = LuaType.VOID, onTick = false, description = "Seek the tape.",
			args = {@Arg(name = "notes", type = LuaType.NUMBER, description = "Notes to seek")}
		)
	public void seek(IComputerAccess access, TileEntityMusicBox musicBox, int notes) {
		ItemStack tape = musicBox.getStackInSlot(0);
		if(tape != null && tape.getItem() instanceof ItemPaperTape) {
			ItemPaperTape tapeHandler = (ItemPaperTape)tape.getItem();
			tapeHandler.seek(tape, notes*2);
		}
	}
	
	@LuaMethod(returnType = LuaType.BOOLEAN, onTick = false, description = "Check if tape is inserted.", args = {})
	public boolean isTapeInserted(IComputerAccess access, TileEntityMusicBox musicBox) {
		ItemStack tape = musicBox.getStackInSlot(0);
		return (tape != null && tape.getItem() instanceof ItemPaperTape);
	}
	
	@LuaMethod(returnType = LuaType.VOID, onTick = false, description = "Rewind the tape.", args = {})
	public void rewind(IComputerAccess access, TileEntityMusicBox musicBox) {
		// Hack to get around API limitations
		seek(access, musicBox, (1<<31));
	}
	
	
}
