package pl.asie.moducomp.gui.text;

import org.lwjgl.opengl.GL11;

import pl.asie.moducomp.ModularComputing;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;

public class TextRenderer {
	private static final ResourceLocation texture = new ResourceLocation("moducomp", "textures/fonts/font_white.png");
	
	public int[] calculateColor(int color, short[] tint) {
		int[] colors = new int[3];
		colors[0] = (color >> 10) & 31;
		colors[1] = (color >> 5) & 31;
		colors[2] = color & 31;
		int gray = (30*colors[0] + 60*colors[1] + 11*colors[2]) / 100; 
		// Mix with grayscale
		if(tint[3] > 0) {
			colors[0] = ((colors[0] * (32 - tint[3])) + (gray * tint[3])) >> 5;
			colors[1] = ((colors[1] * (32 - tint[3])) + (gray * tint[3])) >> 5;
			colors[2] = ((colors[2] * (32 - tint[3])) + (gray * tint[3])) >> 5;
		}
		colors[0] = colors[0] * tint[0] >> 5;
		colors[1] = colors[1] * tint[1] >> 5;
		colors[2] = colors[2] * tint[2] >> 5;
		return colors;
	}
	
	public void drawLetter(Gui gui, TextureManager tm, TextWindow window, int x, int y, int color, short chr, short[] tint) {
		if(chr == 0) return;
		int[] colors;
		// BG
		colors = calculateColor(window.getColor((byte)(color >> 8)), tint);
        GL11.glColor4f(colors[0]/31.0f, colors[1]/31.0f, colors[2]/31.0f, 1.0f);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
		gui.drawTexturedModalRect(x, y, 0, 0, 8, 8); // Cheat!
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		// FG
		colors = calculateColor(window.getColor((byte)(color & 0xFF)), tint);
        GL11.glColor4f(colors[0]/31.0f, colors[1]/31.0f, colors[2]/31.0f, 1.0f);
		tm.bindTexture(this.texture);
		gui.drawTexturedModalRect(x, y, (chr & 31)*8, (chr >> 5)*8, 8, 8);
	}
	
	public void renderWindow(Gui gui, TextureManager tm, TextWindow window, int xp, int yp, short[] tint) {
		short[] display = window.getCharArray();
		for(int y = 0; y < window.height; y++) {
			for(int x = 0; x < window.width; x++) {
				drawLetter(gui, tm, window, xp + (x*8), yp + (y*8), display[x + ((y+window.height)*window.width)], display[x + (y*window.width)], tint);
			}
		}
	}
}
