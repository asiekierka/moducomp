package pl.asie.moducomp.api.computer;

public interface IComputerState {
	// Getters/Setters
	public ICPU getCPU();
	public IMemory getMemory();
	public void setCPU(ICPU cpu);
	public void setMemory(IMemory memory);
	
	// Handlers
	public boolean run(int cycles);
}