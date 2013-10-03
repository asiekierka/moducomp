package pl.asie.moducomp.computer;

import java.io.*;

import pl.asie.moducomp.api.computer.IMemory;
import pl.asie.moducomp.computer.cpu.CPUAreia;
import pl.asie.moducomp.computer.memory.IOHandlerDebug;
import pl.asie.moducomp.computer.memory.MemoryControllerSlot;
import pl.asie.moducomp.computer.memory.MemoryHandlerRAM;
import pl.asie.moducomp.computer.memory.MemoryHandlerROM;

public class Tester
{
	public static void main(String[] args) throws IOException
	{
		MemoryControllerSlot memctl = new MemoryControllerSlot();
		byte rom[] = new byte[8192];
		FileInputStream fis = new FileInputStream(new File("bios.rom"));
		int v = fis.read(rom, 0, rom.length);
		fis.close();
		System.out.printf("%d\n", v);
		memctl.setROM(new MemoryHandlerROM(rom.length, rom));
		memctl.setDeviceSlot(15, new IOHandlerDebug());
		IMemory ram = new MemoryHandlerRAM(4096);
		memctl.setSlot(0, ram);

		double est_mhz = 0.0;
		int runs = 100;
		for(int i = 0; i < runs; i++)
		{
			System.out.printf("\nrun #%d\n", i);
			for(int j = 0; j < 4096; j++)
				ram.write8(null, j, (byte)0);
				
			CPUAreia cpu = new CPUAreia();
			cpu.setMemoryHandler(memctl);
			cpu.resetCold();
			cpu.cycles = 0;
			long t_start = System.nanoTime();
			cpu.runUntilHalt();
			long t_end = System.nanoTime();
			int t_total = (int)((t_end - t_start)/1000);
			double t_total_f = t_total/1000000.0;
			System.out.printf("cycles: %d\n", cpu.cycles);
			System.out.printf("time: %f\n", t_total_f);
			double mhz = (cpu.cycles/(1000000*t_total_f));
			System.out.printf("MHz: %.6f\n", mhz);
			est_mhz += mhz;
		}
		System.out.println();
		est_mhz /= runs;
		System.out.printf("average MHz: %.6f\n", est_mhz);
	}
}

