package pl.asie.moducomp.api.computer;

public interface ICPU {
	public void setMemoryHandler(IMemory handler);

	public void resetWarm();
	public void resetCold();

	public boolean interrupt(int line);

	public void doCycle();
	
	public void run(int cycles);
	
    public void wait(int cycles);
}