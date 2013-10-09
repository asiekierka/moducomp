package pl.asie.moducomp.block;

import pl.asie.moducomp.api.computer.ICPU;
import pl.asie.moducomp.api.computer.IMemory;
import pl.asie.moducomp.api.computer.IMemoryController;

public class CPUThreadMainBoard implements Runnable {

	private ICPU cpu;
	private final int clock;
	private boolean isRunning;
	
	public CPUThreadMainBoard(ICPU cpu, int clock) {
		this.cpu = cpu;
		this.clock = clock;
	}
	
	public void kill() {
		isRunning = false;
	}
	
	@Override
	public void run() {
		isRunning = true;
		while(isRunning) {
			long t_start = System.nanoTime() / 1000000;
			int cyclesLeft = cpu.run(clock / 200); // 250KHz TODO changeable
			long t_end = System.nanoTime() / 1000000;
			try {
				Thread.sleep(5 - (t_end - t_start));
			} catch(Exception e) { }
		}
	}

}
