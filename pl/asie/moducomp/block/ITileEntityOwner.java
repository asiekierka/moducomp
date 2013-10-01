package pl.asie.moducomp.block;

import net.minecraft.tileentity.TileEntity;

public interface ITileEntityOwner {
	public Class<? extends TileEntity> getTileEntityClass();
}
