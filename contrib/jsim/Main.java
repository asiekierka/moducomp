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
		int runs = 1000;
		for(int i = 0; i < runs; i++)
		{
			if(false)
				System.out.printf("\nrun #%d\n", i);
			for(int j = 0; j < 4096; j++)
				ram.write8(null, j, (byte)0);
				
			CPU cpu = new CPU(memctl);
			cpu.cold_reset();
			cpu.cycles = 0;
			cpu.clearUops();
			long t_start = System.nanoTime();
			long t_next = t_start;
			//int cycs_add = 2000000/20; // 2MHz
			int cycs_add = 1000000000; // Pretty much as fast as you like.
			int cycs = 0;
			while(true)
			{
				cycs += cycs_add;
				int cycoffs = cpu.doCycles(cycs);
				cycs = cycoffs;

				if(cpu.isHalted())
					break;

				long t_now = System.nanoTime();
				t_next += 1000000000L/20L;
				int t_diff = (int)((t_next - t_now) / 1000000L);

				if(t_diff > 0)
					try{Thread.sleep(t_diff);}catch(Exception _){}
			}
			long t_end = System.nanoTime();
			int t_total = (int)((t_end - t_start)/1000);
			double t_total_f = t_total/1000000.0;
			double mhz = (cpu.cycles/(1000000*t_total_f));
			est_mhz += mhz;
			if(i+1 == runs)
			{
				System.out.printf("cycles: %d\n", cpu.cycles);
				System.out.printf("time: %f\n", t_total_f);
				System.out.printf("MHz: %.6f\n", mhz);
				System.out.printf("average MHz: %.6f\n", est_mhz/(i+1));
			}
		}
		System.out.println();
		est_mhz /= runs;
		System.out.printf("average MHz: %.6f\n", est_mhz);
	}
}

