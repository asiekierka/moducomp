package pl.asie.moducomp.lib;

import net.minecraft.tileentity.TileEntity;

public interface ITileEntityOwner {
	public Class<? extends TileEntity> getTileEntityClass();
}
