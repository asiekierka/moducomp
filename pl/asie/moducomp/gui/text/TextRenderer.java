package pl.asie.moducomp.gui.text;

import org.lwjgl.opengl.GL11;

import pl.asie.moducomp.ModularComputing;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;

public class TextRenderer {
	private static final ResourceLocation[] textures = {
		new ResourceLocation("moducomp", "textures/fonts/font_black.png"),
		new ResourceLocation("moducomp", "textures/fonts/font_white.png")
	};
	
	public void drawLetter(Gui gui, TextureManager tm, int x, int y, int color, short chr) {
		if(chr == 0) return;
		ResourceLocation texture = textures[color % textures.length];
		float red = ((color >> 10) & 31) / 31.0F;
		float green = ((color >> 5) & 31) / 31.0F;
		float blue = (color & 31) / 31.0F;
        GL11.glColor4f(red, green, blue, 1.0F);
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
