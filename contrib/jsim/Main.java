import java.io.*;

public class Main
{
	public static void main(String[] args) throws IOException
	{
		StandardMemoryController memctl = new StandardMemoryController();
		memctl.setSlot(0, new RandomAccessMemory(4096));
		byte rom[] = new byte[8192];
		int v = new FileInputStream(new File("../bios.rom")).read(rom, 0, rom.length);
		System.out.printf("%d\n", v);
		memctl.setROM(new ReadOnlyMemory(rom.length, rom));
		memctl.setSysSlot(new DebugSysSlot());

		CPU cpu = new CPU(memctl);
		cpu.cold_reset();
		cpu.run_until_halt();
	}
}

