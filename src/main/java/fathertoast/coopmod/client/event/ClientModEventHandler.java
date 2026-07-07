package fathertoast.coopmod.client.event;

import fathertoast.coopmod.client.config.ClientConfig;
import fathertoast.coopmod.client.coordination.PartyStatusGuiOverlay;
import fathertoast.coopmod.common.core.CoOpMod;
import fathertoast.crust.api.config.client.ClientConfigUtil;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Contains and automatically registers all client-side mod events.
 */
@Mod.EventBusSubscriber( value = Dist.CLIENT, modid = CoOpMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD )
public final class ClientModEventHandler {
    
    /** Called after common setup to perform client-side-only setup. */
    @SubscribeEvent
    static void onClientSetup( FMLClientSetupEvent event ) {
        ClientConfig.initialize();
        
        ClientConfigUtil.registerConfigButtonAsEditScreen( CoOpMod.INSTANCE.CONTAINER );
    }
    
    /** Registers this mod's additional key bindings. */
    @SubscribeEvent
    static void onRegisterKeyMappings( RegisterKeyMappingsEvent event ) {
        KeyBindingEvents.register( event );
    }
    
    /** Registers this mod's GUI overlays. */
    @SubscribeEvent
    static void onRegisterGuiOverlays( RegisterGuiOverlaysEvent event ) {
        event.registerBelow( VanillaGuiOverlay.SCOREBOARD.id(), "party_status",
                PartyStatusGuiOverlay::render );
    }
}