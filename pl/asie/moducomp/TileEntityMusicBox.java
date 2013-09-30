package pl.asie.moducomp;

import net.minecraft.client.audio.SoundManager;
import net.minecraft.item.ItemStack;
import pl.asie.moducomp.lib.TileEntityInventory;

public class TileEntityMusicBox extends TileEntityInventory {	
	// Goes from Ab0 to Ab2 in Ab major scale.
	private static final String[] noteMapping = {"abOne", "bbOne", "cOne", "dbOne", "ebOne", "fOne", "gOne", "abTwo", "none", "bbTwo", "cTwo", "dbTwo", "ebTwo", "fTwo", "gTwo", "abThree"};

	public TileEntityMusicBox() {
		super(1, 1, "block.moducomp.music_box");
	}

	protected static void addSounds(SoundManager manager) {
		for(String note: noteMapping)
			if(!note.equals("none")) manager.addSound("moducomp:musicbox_"+note+".ogg");
		ModularComputing.logger.info("Initialized MusicBox sounds.");
	}
	
	protected boolean playNote() {
		ItemStack stack = this.getStackInSlot(0);
		if(stack != null && stack.getItem() instanceof ItemPaperTape) {
	    	ItemPaperTape tapeHandler = (ItemPaperTape)stack.getItem();
	    	int seek = tapeHandler.seek(stack, 2);
	    	if(seek < 2) return false; // End of tape
	    	int values = ((0xFF & tapeHandler.getByte(stack, -2)) << 8) | (0xFF & tapeHandler.getByte(stack, -1));
	    	for(int i = 0; i < 16; i++) {
	    		if((values & (1<<(15-i))) == 0) continue;
	    		String note = noteMapping[i];
	    		if(!note.equals("none"))
	    			this.worldObj.playSoundEffect(this.xCoord, this.yCoord, this.zCoord, "moducomp:musicbox_"+note, 1.0F, 1.0F);
	    	}
	    	return true;
		} else return false;
	}
}
