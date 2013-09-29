package pl.asie.moducomp;

import cpw.mods.fml.common.registry.LanguageRegistry;

public class CommonProxy {
	public void addNames() {
		LanguageRegistry lr = LanguageRegistry.instance();
		lr.addStringLocalization("block.moducomp.tape_reader", "Tape Reader");
		lr.addStringLocalization("item.moducomp.paper_tape", "Paper Tape");
	}
}
