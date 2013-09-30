package pl.asie.moducomp.integration.openperipheral;

import net.minecraft.item.ItemStack;
import net.minecraftforge.common.ForgeDirection;
import dan200.computer.api.IComputerAccess;
import openperipheral.api.Arg;
import openperipheral.api.IPeripheralAdapter;
import openperipheral.api.LuaMethod;
import openperipheral.api.LuaType;
import pl.asie.moducomp.block.TileEntityMusicBox;
import pl.asie.moducomp.block.TileEntityTapeReader;
import pl.asie.moducomp.item.ItemPaperTape;

public class AdapterTapeReader implements IPeripheralAdapter {
	@Override
	public Class getTargetClass() {
		return TileEntityTapeReader.class;
	}
	
	@LuaMethod(returnType = LuaType.BOOLEAN, onTick = false, description = "Check if tape is inserted.", args = {})
	public boolean isTapeInserted(IComputerAccess access, TileEntityTapeReader tapeReader) {
		ItemStack tape = tapeReader.getStackInSlot(0);
		return (tape != null && tape.getItem() instanceof ItemPaperTape);
	}
	
	@LuaMethod(returnType = LuaType.NUMBER, onTick = false, description = "Seek the tape - returns number of bytes seeked.",
				args = {@Arg(name = "bytes", type = LuaType.NUMBER, description = "Bytes to seek")}
			)
	public int seek(IComputerAccess computer, TileEntityTapeReader tapeReader, int bytes) {
		ItemStack tape = tapeReader.getStackInSlot(0);
		if(tape != null && tape.getItem() instanceof ItemPaperTape) {
			ItemPaperTape tapeHandler = (ItemPaperTape)tape.getItem();
			return tapeHandler.seek(tape, bytes);
		} else return -1;
	}
	
	@LuaMethod(returnType = LuaType.VOID, onTick = false, description = "Punch holes in the tape - cannot remove existing holes!",
			args = {@Arg(name = "value", type = LuaType.NUMBER, description = "Value (0-255)")}
		)
	public void punch(IComputerAccess computer, TileEntityTapeReader tapeReader, int value) {
		ItemStack tape = tapeReader.getStackInSlot(0);
		if(tape != null && tape.getItem() instanceof ItemPaperTape) {
			ItemPaperTape tapeHandler = (ItemPaperTape)tape.getItem();
			int newValue = tapeHandler.getByte(tape, 0) | (value&255);
			tapeHandler.setByte(tape, (byte)newValue, 0);
		}
	}
	
	@LuaMethod(returnType = LuaType.NUMBER, onTick = false, description = "Read current byte.", args = {})
	public int read(IComputerAccess computer, TileEntityTapeReader tapeReader) {
		ItemStack tape = tapeReader.getStackInSlot(0);
		if(tape != null && tape.getItem() instanceof ItemPaperTape) {
			ItemPaperTape tapeHandler = (ItemPaperTape)tape.getItem();
			return 0xFF & tapeHandler.getByte(tape, 0);
		} else return -1;
	}
	
	@LuaMethod(returnType = LuaType.NUMBER, onTick = false, description = "Get current tape length.", args = {})
	public int length(IComputerAccess computer, TileEntityTapeReader tapeReader) {
		ItemStack tape = tapeReader.getStackInSlot(0);
		if(tape != null && tape.getItem() instanceof ItemPaperTape) {
			ItemPaperTape tapeHandler = (ItemPaperTape)tape.getItem();
			return tapeHandler.getLength(tape);
		} else return 0;
	}
}
