package pl.asie.moducomp.integration;

import java.util.ArrayList;

import pl.asie.moducomp.ModularComputing;
import cpw.mods.fml.common.Loader;

public class ModIntegration {
	private ArrayList<IModIntegration> modIntegrators;
	
	public ModIntegration() {
		modIntegrators = new ArrayList<IModIntegration>();
	}
	
	public void addModIntegrator(IModIntegration integrator) {
		modIntegrators.add(integrator);
	}
	
	public boolean checkMods(String[] deps) {
		for(String mod: deps) {
			if(!Loader.isModLoaded(mod)) return false;
		}
		return true;
	}
	
	public void init() {
		for(IModIntegration integrator: modIntegrators) {
			if(checkMods(integrator.getDependencies())) {
				ModularComputing.logger.info("Loading "+integrator.getName()+" ModIntegrator...");
				integrator.init();	
			}
		}
	}
}
