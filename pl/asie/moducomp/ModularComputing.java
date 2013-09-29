package pl.asie.moducomp;

import java.util.logging.Logger;

import net.minecraft.block.material.Material;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import cpw.mods.fml.common.*;
import cpw.mods.fml.common.Mod.*;
import cpw.mods.fml.common.event.*;
import cpw.mods.fml.common.network.*;
import cpw.mods.fml.common.registry.GameRegistry;

@Mod(modid="ModularComputing", name="Modular Computing", version="0.0.1")
@NetworkMod(clientSideRequired=true)
public class ModularComputing {
	public static final boolean DEBUG = true;
	@Instance(value = "ModularComputing")
	public static ModularComputing instance;
	
	public BlockTapeReader blockTapeReader;
	public ItemPaperTape itemPaperTape;
	
	@SidedProxy(clientSide="pl.asie.moducomp.ClientProxy", serverSide="pl.asie.moducomp.CommonProxy")
	public static CommonProxy proxy;
	
	public static Logger logger;
    public static void debug(String string) {
        if(DEBUG) logger.info(string);
    }
    
    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
    	logger = Logger.getLogger("ModularComputing");
    	logger.setParent(FMLLog.getLogger());
    	
    	blockTapeReader = new BlockTapeReader(1920, Material.circuits);
    	itemPaperTape = new ItemPaperTape(19200);

    	GameRegistry.registerBlock(blockTapeReader, "moducomp.tape_reader");
    	
    	GameRegistry.registerTileEntity(TileEntityTapeReader.class, "moducomp.tape_reader");
    	
    	GameRegistry.registerItem(itemPaperTape, "moducomp.paper_tape");
    }
	
	@EventHandler
	public void init(FMLInitializationEvent event) {
    	proxy.addNames();
    	
    	NetworkRegistry.instance().registerGuiHandler(this, new GuiHandler());
    	
    	GameRegistry.addShapedRecipe(new ItemStack(itemPaperTape), " x ", "x x", " x ", 'x', Item.paper);
	}
	
	@EventHandler
	public void postInit(FMLPostInitializationEvent event) {
		
	}
}
