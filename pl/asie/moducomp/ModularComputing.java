package pl.asie.moducomp;

import java.util.logging.Logger;

import pl.asie.moducomp.block.BlockMusicBox;
import pl.asie.moducomp.block.BlockTapeReader;
import pl.asie.moducomp.block.TileEntityMusicBox;
import pl.asie.moducomp.block.TileEntityTapeReader;
import pl.asie.moducomp.integration.IntegrationOpenPeripheral;
import pl.asie.moducomp.integration.ModIntegration;
import pl.asie.moducomp.item.ItemPaperTape;
import net.minecraft.block.material.Material;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.Configuration;
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
    	
    	Configuration config = new Configuration(event.getSuggestedConfigurationFile());
    	config.load();
    	
    	blockTapeReader = new BlockTapeReader(config.getBlock("moducomp.tape_reader", 1920).getInt(), Material.circuits);
    	blockMusicBox = new BlockMusicBox(config.getBlock("moducomp.music_box", 1921).getInt(), Material.circuits);
    	itemPaperTape = new ItemPaperTape(config.getItem("moducomp.paper_tape", 19200).getInt());

    	GameRegistry.registerBlock(blockTapeReader, "moducomp.tape_reader");
    	GameRegistry.registerBlock(blockMusicBox, "moducomp.music_box");
    	
    	GameRegistry.registerTileEntity(TileEntityTapeReader.class, "moducomp.tape_reader");
    	GameRegistry.registerTileEntity(TileEntityMusicBox.class, "moducomp.music_box");
    	
    	GameRegistry.registerItem(itemPaperTape, "moducomp.paper_tape");

    	config.save();
    	proxy.setupEvents();
    }
	
	@EventHandler
	public void init(FMLInitializationEvent event) {
    	proxy.addNames();

    	ModIntegration integration = new ModIntegration();
    	integration.addModIntegrator(new IntegrationOpenPeripheral());
    	
    	integration.init();
    	
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
