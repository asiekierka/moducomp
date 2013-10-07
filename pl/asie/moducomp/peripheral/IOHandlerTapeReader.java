package pl.asie.moducomp.peripheral;

import net.minecraft.item.ItemStack;
import pl.asie.moducomp.ModularComputing;
import pl.asie.moducomp.api.IItemTape;
import pl.asie.moducomp.api.computer.ICPU;
import pl.asie.moducomp.api.computer.IMemory;
import pl.asie.moducomp.api.computer.PeripheralBasic;
import pl.asie.moducomp.block.TileEntityTapeReader;

public class IOHandlerTapeReader extends PeripheralBasic implements IMemory, Runnable {

	private static final int SPEED = 10; // in milliseconds, per byte
	private static final byte memoryMapFinal[] = {
		MAP_BYTE, // 0x04: FLAGS
		MAP_BYTE, // 0x05: INTERRUPT LANE
		MAP_SHORT, // 0x06-0x07: SEEK (SIGNED) - SENDS INTERRUPT, ON READ - AMOUNT OF BYTES LAST SEEKED (UNSIGNED)
		MAP_BYTE, // 0x08: CURRENT BYTE - WRITE TO PUNCH HOLES
		MAP_BYTE | MAP_NO_WRITE // 0x09: IS READING?
	};
	
	public IOHandlerTapeReader(TileEntityTapeReader tileEntity) {
		super((short)0xA51E, (byte)0x02, (byte)0x02, memoryMapFinal);
		this.tapeReader = tileEntity;
		this.intregs[0x05] = 1; // Default
		this.flags = new boolean[8];
	}
	
	private boolean[] flags;
	private TileEntityTapeReader tapeReader;
	
	private void setReadByte() {
		IItemTape handler = tapeReader.getHandler();
		if(handler == null) {
			intregs[0x08] = (byte)0xFF;
		} else {
			ItemStack tape = tapeReader.getTape();
			intregs[0x08] = handler.getByte(tape, 0);
		}
	}
	
	@Override
	public void onReadByte(ICPU cpu, int addr) {
		switch(addr) {
			case 0x04: // FLAGS
				int value = 0;
				for(int i = 0; i < 8; i++)
					value |= (flags[i]?1:0)<<i;
				intregs[addr] = (byte)value;
				break;
			case 0x08: // CURRENT BYTE
				setReadByte();
				break;
		}
	}

	@Override
	public void onWriteByte(ICPU cpu, int addr) {
		switch(addr) {
			case 0x04: // FLAGS
				int value = (int)0xFF & intregs[addr];
				for(int i = 0; i < 8; i++)
					flags[i] = (value&(1<<i)) > 0;
				break;
			case 0x08: // BYTE
				ItemStack tape = tapeReader.getTape();
				IItemTape handler = tapeReader.getHandler();
				if(handler == null) return;
				byte current = handler.getByte(tape, 0);
				handler.setByte(tape, (byte)(current | intregs[addr]), 0);
				break;
		}
	}
	
	@Override
	public void onWriteShortEnd(ICPU cpu, int addr) {
		switch(addr) {
			case 0x07: // SEEK
				this.seekCPU = cpu;
				this.seekBytes = readShort(0x06);
				synchronized(this) {
					intregs[0x09] = 1;
					writeShort(0x06, (short)0);
					new Thread(this).start();
				}
				break;
		}
	}

	private int seekBytes = 0;
	private ICPU seekCPU;
	
	@Override
	public void run() {
		ItemStack tape = tapeReader.getTape();
		IItemTape handler = tapeReader.getHandler();
		if(handler == null) return;
		
		int bytes = Math.abs(handler.getSeekLength(tape, seekBytes));
		long time = bytes * SPEED;
		try { Thread.sleep(time); }
		catch(Exception e) { e.printStackTrace(); }

		int interruptLane = intregs[0x05]&31;
		synchronized(this) {
			tape = tapeReader.getTape();
			if(tape != null) {
				handler.seek(tape, seekBytes);
				setReadByte();
				writeShort(0x06, (short)bytes);
			} else writeShort(0x06, (short)0);
			intregs[0x09] = 0;
		}
		if(interruptLane >= 0 && interruptLane < 28)
			seekCPU.interrupt(interruptLane);
	}

}
