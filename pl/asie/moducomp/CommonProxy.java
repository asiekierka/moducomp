package pl.asie.moducomp;

import cpw.mods.fml.common.registry.LanguageRegistry;

public class CommonProxy {
	public void addNames() {
		LanguageRegistry lr = LanguageRegistry.instance();
		lr.addStringLocalization("tile.moducomp.tape_reader.name", "Tape Reader");
		lr.addStringLocalization("tile.moducomp.music_box.name", "Music Box");
		lr.addStringLocalization("item.moducomp.paper_tape.name", "Paper Tape");
	}
	
	public void setupEvents() { }
}
