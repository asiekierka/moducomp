package pl.asie.moducomp.integration;

public interface IModIntegration {
	public String[] getDependencies();
	public String getName();
	public void init();
}
