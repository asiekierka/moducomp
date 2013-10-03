import java.io.*;

public class Main
{
	public static void main(String[] args) throws IOException
	{
		StandardMemoryController memctl = new StandardMemoryController();
		byte rom[] = new byte[8192];
		int v = new FileInputStream(new File("../bios.rom")).read(rom, 0, rom.length);
		System.out.printf("%d\n", v);
		memctl.setROM(new ReadOnlyMemory(rom.length, rom));
		memctl.setSysSlot(new DebugSysSlot());
		Memory ram = new RandomAccessMemory(4096);
		memctl.setSlot(0, ram);

		double est_mhz = 0.0;
		int runs = 100;
		for(int i = 0; i < runs; i++)
		{
			System.out.printf("\nrun #%d\n", i);
			for(int j = 0; j < 4096; j++)
				ram.write8(null, j, (byte)0);
				
			CPU cpu = new CPU(memctl);
			cpu.cold_reset();
			cpu.cycles = 0;
			long t_start = System.nanoTime();
			cpu.run_until_halt();
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

