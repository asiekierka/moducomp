package pl.asie.moducomp.gui;

public class TextWindow {
	public static final int TAB_SIZE = 4;
	public final int width, height;
	private short[] display;
	private int x,y;
	
	public TextWindow(int width, int height) {
		this.width = width;
		this.height = height;
		this.display = new short[width*height];
		this.x = 0;
		this.y = 0;
	}
	
	private void scrollUp() {
		System.arraycopy(this.display, width, this.display, 0, (width*(height-1)));
		for(int i = (width*(height-1)); i<(width*height); i++) {
			this.display[i] = 0;
		}
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
		} else if(key == 8) { // Backspace
			if(this.x > 0) {
				this.x--;
				this.display[this.x + (this.y * this.width)] = 0;
			}
		} else if(key == 9) { // Tab
			for(int i = 0; i < TAB_SIZE; i++) {
				print((short)32);
			}
		} else if(key >= 32 && key < 127) print((short)key);
	}
	
	public void print(short chr) {
		this.display[this.x + (this.y * this.width)] = chr;
		this.advance();
	}
	
	public void print(String string) { // WARNING: Only supports ASCII chars (0-255). PLEASE USE ONLY FOR DEBUGGING
		for(int i = 0; i < string.length(); i++) {
			print((short)string.charAt(i));
		}
	}
	
	public short[] getCharArray() {
		return this.display;
	}
}
