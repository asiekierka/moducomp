package pl.asie.moducomp.gui.text;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;

public class TextRenderer {
	private static final ResourceLocation[] textures = {
		new ResourceLocation("moducomp", "textures/fonts/font_black.png"),
		new ResourceLocation("moducomp", "textures/fonts/font_white.png")
	};
	
	public static final int COLOR_BLACK = 0;
	public static final int COLOR_WHITE = 1;
	
	public void drawLetter(Gui gui, TextureManager tm, int x, int y, int color, short chr) {
		ResourceLocation texture = textures[color % textures.length];
		tm.bindTexture(texture);
		gui.drawTexturedModalRect(x, y, (chr & 31)*8, (chr >> 5)*8, 8, 8);
	}
	
	public void renderWindow(Gui gui, TextureManager tm, TextWindow window, int xp, int yp, int color) {
		ResourceLocation texture = textures[color % textures.length];
		short[] display = window.getCharArray();
		for(int y = 0; y < window.height; y++) {
			for(int x = 0; x < window.width; x++) {
				drawLetter(gui, tm, xp + (x*8), yp + (y*8), color, display[x + (y*window.width)]);
			}
		}
	}
}
