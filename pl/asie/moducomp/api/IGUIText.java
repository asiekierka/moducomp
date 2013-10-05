package pl.asie.moducomp.api;

import net.minecraft.tileentity.TileEntity;
import pl.asie.moducomp.gui.text.TextWindow;

public interface IGUIText {
	public TextWindow getWindow();
	public void setWindow(TextWindow window);
	public boolean getHardwareEcho();
	public void setHardwareEcho(boolean echo);
	public TileEntity getTileEntity();
}
