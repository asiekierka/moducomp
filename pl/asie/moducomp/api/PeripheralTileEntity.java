package pl.asie.moducomp.api;

import net.minecraft.tileentity.TileEntity;
import pl.asie.moducomp.ModularComputing;
import pl.asie.moducomp.api.computer.ICPU;
import pl.asie.moducomp.api.computer.IMemory;
import pl.asie.moducomp.api.computer.PeripheralBasic;

public abstract class PeripheralTileEntity extends PeripheralBasic {

	private TileEntity tileEntity;
	private IEntityPeripheral peripheral;
	
	public PeripheralTileEntity(IEntityPeripheral tileEntity, short author, byte id, byte deviceClass,
			byte[] memoryMap) {
		super(author, id, deviceClass, memoryMap);
		this.peripheral = tileEntity;
		if(tileEntity instanceof TileEntity)
			this.tileEntity = (TileEntity)tileEntity;
	}

	public void onWriteByte(ICPU cpu, int addr) {
	}
	
	public void onWriteShortEnd(ICPU cpu, int addr) {
	}
}
