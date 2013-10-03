package pl.asie.moducomp;

import cpw.mods.fml.common.registry.LanguageRegistry;

public class CommonProxy {
	public void addNames() {
		LanguageRegistry lr = LanguageRegistry.instance();
		lr.addStringLocalization("tile.moducomp.tape_reader.name", "Tape Reader");
		lr.addStringLocalization("tile.moducomp.music_box.name", "Music Box");
		lr.addStringLocalization("item.moducomp.paper_tape.name", "Paper Tape");
		lr.addStringLocalization("item.moducomp.ram.name", "Random Access Memory");
		lr.addStringLocalization("item.moducomp.cpu_areia.name", "Areia-1 Central Processing Unit");
		lr.addStringLocalization("itemGroup.moducomp.name", "Modular Computing");
	}
	
	public void setupEvents() { }
}
