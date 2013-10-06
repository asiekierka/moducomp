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
	
	public void drawLetter(Gui gui, TextureManager tm, TextWindow window, int x, int y, int color, short chr) {
		if(chr == 0) return;
		color &= 32767;
		// BG
		int realColor = window.getColor((byte)(color >> 8));
		int red = (int)(((realColor >> 10) & 31));
		int green = (int)(((realColor >> 5) & 31));
		int blue = (int)((realColor & 31));		
        GL11.glColor4f(red/31.0f, green/31.0f, blue/31.0f, 1.0f);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
		gui.drawTexturedModalRect(x, y, 0, 0, 8, 8); // Cheat!
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		// FG
		realColor = window.getColor((byte)(color & 0xFF));
		red = (int)(((realColor >> 10) & 31));
		green = (int)(((realColor >> 5) & 31));
		blue = (int)((realColor & 31));			
        GL11.glColor4f(red/31.0f, green/31.0f, blue/31.0f, 1.0f);
		tm.bindTexture(this.texture);
		gui.drawTexturedModalRect(x, y, (chr & 31)*8, (chr >> 5)*8, 8, 8);
	}
	
	public void renderWindow(Gui gui, TextureManager tm, TextWindow window, int xp, int yp, int color) {
		short[] display = window.getCharArray();
		for(int y = 0; y < window.height; y++) {
			for(int x = 0; x < window.width; x++) {
				drawLetter(gui, tm, window, xp + (x*8), yp + (y*8), display[x + ((y+window.height)*window.width)], display[x + (y*window.width)]);
			}
		}
	}
}
