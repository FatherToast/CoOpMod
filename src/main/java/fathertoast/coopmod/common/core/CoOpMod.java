package fathertoast.coopmod.common.core;

import fathertoast.coopmod.common.config.Config;
import fathertoast.coopmod.common.network.PacketHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingStage;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.javafmlmod.FMLModContainer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;

/**
 * The core of the mod. Contains basic info about the mod, initializes configs, and hooks into FML.
 */
@Mod( CoOpMod.MOD_ID )
public final class CoOpMod {
    
    /* Features list:
     * (KEY: - = complete in current version, o = incomplete feature from previous version,
     *       + = incomplete new feature, ? = feature to consider adding)
     *  - general features
     *      - Jade integration (tooltip for inspect target)
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
     *          - sounds
     *              + better config capability
     *          + a way to cancel pings
     *          - nameplate
     *          ? HUD arrows pointing to off-screen pings
     *      - find players
     *          + render marker for players beyond render distance
     *          ? HUD arrows pointing to off-screen players
     *      + HUD element showing nearby players' statuses (health, etc.)
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
        
        ModLoadingStage.CONSTRUCT.getDeferredWorkQueue().enqueueWork( CONTAINER, Config::initializeEarly );
        
        CMSoundEvents.register( eventBus );
        CMAttributes.register( eventBus );
    }
    
    /** @return A ResourceLocation with the mod's modid. */
    public static ResourceLocation rl( String path ) { return ResourceLocation.fromNamespaceAndPath( MOD_ID, path ); }
    
    public static String logPrefix( Class<?> clazz ) {
        return "[" + MOD_ID + "/" + clazz.getSimpleName() + "] ";
    }
    
    /** @return Returns the resource location as a string, or "null" if it is null. */
    public static String toString( @Nullable ResourceLocation res ) { return res == null ? "null" : res.toString(); }
}