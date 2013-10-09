package pl.asie.moducomp.item;

import java.util.ArrayList;
import java.util.List;

import pl.asie.moducomp.ModularComputing;
import pl.asie.moducomp.api.IItemMemory;
import pl.asie.moducomp.api.computer.IMemory;
import pl.asie.moducomp.computer.memory.MemoryHandlerRAM;
import pl.asie.moducomp.computer.memory.MemoryHandlerROM;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class ItemROM extends ItemMemory implements IItemMemory {
	private ArrayList<IMemory> roms;
	private ArrayList<String> romNames;
	public ItemROM(int id, String name) {
		super(id, name);
		roms = new ArrayList<IMemory>();
		romNames = new ArrayList<String>();
	}

	public int getLength(ItemStack stack) {
		return 8192;
	}
	
	@Override
    public void getSubItems(int id, CreativeTabs tab, List items)
    {
		for(String name: romNames) {
			items.add(new ItemStack(id, 1, romNames.indexOf(name)));
		}
    }
	
	public int registerROM(String filename, String name) {
		byte[] romData = new byte[8192];
		try {
			ModularComputing.class.getClassLoader().getResourceAsStream("assets/moducomp/"+filename).read(romData);
		} catch(Exception e) { e.printStackTrace(); return -1; }
		MemoryHandlerROM rom = new MemoryHandlerROM(romData);
		roms.add(rom);
		int romID = roms.indexOf(rom);
		romNames.add(romID, name);
		return romID;
	}
	
    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean par4)
    {
    	int romID = stack.getItemDamage();
    	if(romNames.get(romID) != null)
    		list.add(romNames.get(romID));
    	else list.add("Unknown");
    }
    
	@Override
	public IMemory createNewMemoryHandler(ItemStack stack) {
		int romID = stack.getItemDamage();
		return roms.get(romID);
	}
}
