package pl.asie.moducomp;

import cpw.mods.fml.common.registry.LanguageRegistry;

public class CommonProxy {
	public void addNames() {
		LanguageRegistry lr = LanguageRegistry.instance();
		lr.addStringLocalization("tile.block.moducomp.tape_reader.name", "Tape Reader");
		lr.addStringLocalization("item.moducomp.paper_tape.name", "Paper Tape");
	}
}
