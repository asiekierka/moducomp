package pl.asie.moducomp.integration;

import openperipheral.api.IntegrationRegistry;
import pl.asie.moducomp.integration.openperipheral.*;

public class IntegrationOpenPeripheral implements IModIntegration {
	private String[] DEPENDENCIES = {"ComputerCraft", "OpenPeripheral"};
	
	public String[] getDependencies() { return DEPENDENCIES; }
	public String getName() { return "OpenPeripheral"; }
	
	public void init() {
		IntegrationRegistry integrationRegistry = new IntegrationRegistry();
		integrationRegistry.registerAdapter(new AdapterTapeReader());
	}
}
