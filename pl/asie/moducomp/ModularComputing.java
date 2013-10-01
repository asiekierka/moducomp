package pl.asie.moducomp;

import java.util.logging.Logger;

import pl.asie.moducomp.block.BlockMachine;
import pl.asie.moducomp.block.BlockMusicBox;
import pl.asie.moducomp.block.BlockRAMBoard;
import pl.asie.moducomp.block.BlockTapeReader;
import pl.asie.moducomp.block.ITileEntityOwner;
import pl.asie.moducomp.block.TileEntityMusicBox;
import pl.asie.moducomp.block.TileEntityTapeReader;
import pl.asie.moducomp.integration.IntegrationOpenPeripheral;
import pl.asie.moducomp.integration.ModIntegration;
import pl.asie.moducomp.item.ItemPaperTape;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.Configuration;
import cpw.mods.fml.common.*;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.Mod.EventHandler;
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
	public BlockRAMBoard blockRAMBoard;
	public ItemPaperTape itemPaperTape;
	
	private Configuration config;
	
	@SidedProxy(clientSide="pl.asie.moducomp.ClientProxy", serverSide="pl.asie.moducomp.CommonProxy")
	public static CommonProxy proxy;
	
	public static Logger logger;
    public static void debug(String string) {
        if(DEBUG) logger.info(string);
    }
    
    public Block registerBlock(Class<? extends Block> blockClass, String name, int defaultID) {
    	int id = config.getBlock(name, defaultID).getInt();
    	try {
    		Block block;
    		if(blockClass.isInstance(BlockMachine.class)) {
    			block = blockClass.getConstructor(Integer.TYPE, String.class).newInstance(id, name);
    		} else {
    			block = blockClass.getConstructor(Integer.TYPE).newInstance(id);
    		}
    		GameRegistry.registerBlock(block, name);
    		if(block instanceof ITileEntityOwner) {
    			ITileEntityOwner teOwner = (ITileEntityOwner)block;
    			GameRegistry.registerTileEntity(teOwner.getTileEntityClass(), name);
    		}
    		return block;
    	} catch(Exception e) { e.printStackTrace(); return null; }
    }
    
    public Item registerItem(Class<? extends Item> itemClass, String name, int defaultID) {
    	try {
    		Item item = itemClass.getConstructor(Integer.TYPE).newInstance(config.getItem(name, defaultID).getInt());
    		GameRegistry.registerItem(item, name);
    		return item;
    	} catch(Exception e) { e.printStackTrace(); return null; }
    }
    
    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
    	logger = Logger.getLogger("moducomp");
    	logger.setParent(FMLLog.getLogger());
    	
    	config = new Configuration(event.getSuggestedConfigurationFile());
    	config.load();
    	
    	blockTapeReader = (BlockTapeReader) registerBlock(BlockTapeReader.class, "moducomp.tape_reader", 1920);
    	blockMusicBox = (BlockMusicBox) registerBlock(BlockMusicBox.class, "moducomp.music_box", 1921);
    	blockRAMBoard = (BlockRAMBoard) registerBlock(BlockRAMBoard.class, "moducomp.ram_board", 1922);
    	
    	itemPaperTape = (ItemPaperTape) registerItem(ItemPaperTape.class, "moducomp.paper_tape", 19200);

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
