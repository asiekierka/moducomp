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

@Mod(modid="moducomp", name="Modular Computing", version="0.0.1")
@NetworkMod(clientSideRequired=true)
public class ModularComputing {
	public static final boolean DEBUG = true;
	@Instance(value = "moducomp")
	public static ModularComputing instance;
	
	public BlockTapeReader blockTapeReader;
	public BlockMusicBox blockMusicBox;
	public ItemPaperTape itemPaperTape;
	
	@SidedProxy(clientSide="pl.asie.moducomp.ClientProxy", serverSide="pl.asie.moducomp.CommonProxy")
	public static CommonProxy proxy;
	
	public static Logger logger;
    public static void debug(String string) {
        if(DEBUG) logger.info(string);
    }
    
    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
    	logger = Logger.getLogger("moducomp");
    	logger.setParent(FMLLog.getLogger());
    	
    	blockTapeReader = new BlockTapeReader(1920, Material.circuits);
    	blockMusicBox = new BlockMusicBox(1921, Material.circuits);
    	itemPaperTape = new ItemPaperTape(19200);

    	GameRegistry.registerBlock(blockTapeReader, "moducomp.tape_reader");
    	GameRegistry.registerBlock(blockMusicBox, "moducomp.music_box");
    	
    	GameRegistry.registerTileEntity(TileEntityTapeReader.class, "moducomp.tape_reader");
    	GameRegistry.registerTileEntity(TileEntityMusicBox.class, "moducomp.music_box");
    	
    	GameRegistry.registerItem(itemPaperTape, "moducomp.paper_tape");
    	
    	proxy.setupEvents();
    }
	
	@EventHandler
	public void init(FMLInitializationEvent event) {
    	proxy.addNames();
    	
    	GameRegistry.registerCraftingHandler(new CraftingHandler());
    	
    	NetworkRegistry.instance().registerGuiHandler(this, new GuiHandler());
    	NetworkRegistry.instance().registerChannel(new NetworkHandler(), "ModularC");
    	
    	GameRegistry.addShapedRecipe(new ItemStack(itemPaperTape), " x ", "x x", " x ", 'x', Item.paper);
    	for(int i = 0; i < ItemPaperTape.MAX_TAPE_LENGTH; i++) { // TERIRBLE, TERRIBLE HACK! Adds 2048 recipes
    		GameRegistry.addShapedRecipe(new ItemStack(itemPaperTape, 1, i+1), " x ", "xyx", " x ", 'x', Item.paper, 'y', new ItemStack(itemPaperTape, 1, i));
    	}
	}
	
	@EventHandler
	public void postInit(FMLPostInitializationEvent event) {
		
	}
}
