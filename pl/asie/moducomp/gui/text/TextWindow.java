package pl.asie.moducomp.gui.text;

import pl.asie.moducomp.ModularComputing;

public class TextWindow {
	public static final int TAB_SIZE = 4;
	public final int width, height;
	private short[] display;
	public int x,y;
	private short lastColor = 32767;
	
	public TextWindow(int width, int height) {
		this.width = width;
		this.height = height;
		this.display = new short[width*height*2];
		this.x = 0;
		this.y = 0;
	}
	
	private void scrollUp() {
		System.arraycopy(this.display, width, this.display, 0, (width*(height-1)));
		System.arraycopy(this.display, width*(height+1), this.display, width*height, (width*(height-1)));
		for(int i = (width*(height-1)); i<(width*height); i++) {
			this.display[i] = 0;
			this.display[width*height + i] = 0;
		}
		this.y--;
	}
	
	public void newline() {
		this.x = 0;
		this.y++;
		if(this.y == this.height) {
			this.scrollUp();
		}
	}
	
	private void advance() {
		this.x++;
		if(this.x == this.width) newline();
	}
	
	public void key(int key) {
		if(key == 13) { // Enter
			newline();
		} else if(key == 8 || key == 127) { // Backspace
			if(this.x > 0) {
				this.x--;
			} else if(this.y > 0) {
				this.x = this.width - 1;
				this.y--;
			} else return;
			this.display[this.x + (this.y * this.width)] = 0;
		} else if(key == 9) { // Tab
			for(int i = 0; i < TAB_SIZE; i++) {
				print(lastColor, (short)32);
			}
		} else if(key >= 32 && key < 127) print(lastColor, (short)key);
	}
	
	public void print(short color, short chr) {
		this.lastColor = color;
		ModularComputing.instance.logger.info("Printing character " + chr + "(x is "+this.x+", y is "+this.y+", color is "+this.lastColor+", x0y0 is "+this.display[0]+")");
		this.display[this.x + (this.y * this.width)] = chr;
		this.display[this.x + ((this.y + this.height) * this.width)] = color;
		this.advance();
	}
	
	public void print(String string) { // WARNING: Only supports ASCII chars (0-255). PLEASE USE ONLY FOR DEBUGGING
		for(int i = 0; i < string.length(); i++) {
			print((short)32767, (short)string.charAt(i));
		}
	}
	
	public short[] getCharArray() {
		return this.display;
	}
	
	public void setCharArray(short[] arr) {
		this.display = arr;
	}
}
