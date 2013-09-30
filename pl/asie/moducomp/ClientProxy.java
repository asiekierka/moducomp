package pl.asie.moducomp;

import pl.asie.moducomp.block.TileEntityMusicBox;
import net.minecraft.client.audio.SoundManager;
import net.minecraftforge.client.event.sound.SoundLoadEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ForgeSubscribe;

public class ClientProxy extends CommonProxy {
	private boolean initSounds;
	
    @ForgeSubscribe
    public void onSound (SoundLoadEvent event)
    {
        if(!initSounds) {
            initSounds = true;
            try {
                SoundManager soundManager = event.manager;
                TileEntityMusicBox.addSounds(soundManager);
            }
            catch (Exception e) { e.printStackTrace(); }
        }
    }
    
    public void setupEvents() {
    	super.setupEvents();
    	MinecraftForge.EVENT_BUS.register(this);
    }
}
