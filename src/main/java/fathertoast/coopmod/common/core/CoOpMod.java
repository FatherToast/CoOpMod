package fathertoast.coopmod.common.core;

import fathertoast.coopmod.common.config.Config;
import fathertoast.coopmod.common.network.PacketHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DeferredWorkQueue;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingStage;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.javafmlmod.FMLModContainer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * The core of the mod. Contains basic info about the mod, initializes configs, and hooks into FML.
 */
@Mod( CoOpMod.MOD_ID )
public final class CoOpMod {
    
    /* Features list:
     * (KEY: - = complete in current version, o = incomplete feature from previous version,
     *       + = incomplete new feature, ? = feature to consider adding)
     *  - general features
     *      ? Jade integration (tooltip for inspect target)
     *      ? multiplayer pause ("Multiplayer Server Pause" does this, maybe ditch)
     *          ? automatic when no players online
     *          ? all/enough online players requested
     *  + social features
     *      ? player inspect
     *      ? chat item linking ("Show Me What You Got" does this, maybe ditch)
     *      ? chat emojis
     *      ? salute/emotes
     *  - co-ordination features
     *      - inspect
     *      - ping
     *      - find players
     *          ? render marker for players beyond render distance
     *      + nameplate for highlights
     *      + HUD element showing nearby players' statuses (health, etc.)
     *      ? HUD arrows pointing to off-screen highlights
     *      ? revive
     *  - protection features
     *      - friendly fire
     *      + chunk
     *      + inventory
     *          ? keep items
     *          ? gravestone
     *      ? action log
     *
     * Possible future additions:
     *  - tbd
     */
    
    /** The mod ID used by this mod. */
    public static final String MOD_ID = "coopmod";
    
    /** The logger used by this mod. */
    public static final Logger LOG = LogManager.getLogger( MOD_ID );
    
    /** Mod instance. */
    public static CoOpMod INSTANCE;
    
    /** Mod container. */
    public final FMLModContainer CONTAINER;
    /** Packet handler instance */
    public final PacketHandler PACKET_HANDLER = new PacketHandler();
    
    
    public CoOpMod( FMLJavaModLoadingContext context ) {
        INSTANCE = this;
        CONTAINER = context.getContainer();
        PACKET_HANDLER.registerMessages();
        
        IEventBus eventBus = context.getModEventBus();
        
        eventBus.addListener( this::onCommonSetup );
        
        //CMSoundEvents.REGISTRY.register( eventBus );
        
        Config.initializeEarly();
        DeferredWorkQueue.lookup( Optional.of( ModLoadingStage.COMMON_SETUP ) ).ifPresent(
                ( workQueue ) -> workQueue.enqueueWork( ModList.get().getModContainerById( MOD_ID ).orElseThrow(),
                        Config::initialize )
        );
    }
    
    public void onCommonSetup( FMLCommonSetupEvent event ) {
        //event.enqueueWork( () -> {
        //} );
    }
    
    /** @return A ResourceLocation with the mod's modid. */
    public static ResourceLocation rl( String path ) { return ResourceLocation.fromNamespaceAndPath( MOD_ID, path ); }
    
    public static String logPrefix( Class<?> clazz ) {
        return "[" + MOD_ID + "/" + clazz.getSimpleName() + "] ";
    }
    
    /** @return Returns the resource location as a string, or "null" if it is null. */
    public static String toString( @Nullable ResourceLocation res ) { return res == null ? "null" : res.toString(); }
}