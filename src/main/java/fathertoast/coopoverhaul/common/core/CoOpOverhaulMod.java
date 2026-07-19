package fathertoast.coopoverhaul.common.core;

import fathertoast.coopoverhaul.common.config.Config;
import fathertoast.coopoverhaul.common.network.PacketHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.javafmlmod.FMLModContainer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;

/**
 * The core of the mod. Contains basic info about the mod, initializes configs, and hooks into FML.
 */
@Mod( CoOpOverhaulMod.MOD_ID )
public final class CoOpOverhaulMod {
    
    /* Features list:
     * (KEY: - = complete in current version, o = incomplete feature from previous version,
     *       + = incomplete new feature, ? = feature to consider adding)
     *  - general features
     *      - Jade integration (tooltip for inspect target)
     *          ? figure out why distant entities don't display tooltips
     *      ? multiplayer pause ("Multiplayer Server Pause" does this, maybe ditch)
     *          ? automatic when no players online
     *          ? all/enough online players requested
     *  + social features
     *      ? player inspect
     *      + parties (Maybe hook into or rely on HQM party system)
     *      ? enable chat over (most) screens
     *      - chat item linking
     *      ? chat emojis
     *      ? salute/emotes
     *  - co-ordination features
     *      - inspect
     *      - ping
     *          - sounds
     *          - nameplate
     *          + a way to cancel pings
     *          + more automatic/detected ping color & sound assignment
     *          ? HUD arrows pointing to off-screen pings
     *      - find players
     *          ? HUD arrows pointing to off-screen players
     *      - party status GUI overlay
     *          + status for remote players in party
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
    public static final String MOD_ID = "coopoverhaul";
    
    /** The logger used by this mod. */
    public static final Logger LOG = LogManager.getLogger( MOD_ID );
    
    /** Mod instance. */
    public static CoOpOverhaulMod INSTANCE;
    
    /** True if Natural Absorption is installed. */
    public static boolean NA_INSTALLED;
    
    /** Mod container. */
    public final FMLModContainer CONTAINER;
    /** Packet handler instance */
    public final PacketHandler PACKET_HANDLER = new PacketHandler();
    
    
    public CoOpOverhaulMod( FMLJavaModLoadingContext context ) {
        INSTANCE = this;
        CONTAINER = context.getContainer();
        PACKET_HANDLER.registerMessages();
        
        IEventBus eventBus = context.getModEventBus();
        
        eventBus.addListener( this::onInterModEnqueue );
        
        Config.initializeEarly();
        
        COSoundEvents.register( eventBus );
        COAttributes.register( eventBus );
    }
    
    public void onInterModEnqueue( InterModEnqueueEvent event ) {
        // Temporarily disabled until NA syncs its own max absorption to other clients
        //        NA_INSTALLED = InterModComms.sendTo( "naturalabsorption", "getNaturalAbsorptionAPI",
        //                () -> CMNaturalAbsorptionPlugin.RECEIVER );
    }
    
    /** @return A ResourceLocation with the mod's modid. */
    public static ResourceLocation rl( String path ) { return ResourceLocation.fromNamespaceAndPath( MOD_ID, path ); }
    
    public static String logPrefix( Class<?> clazz ) {
        return "[" + MOD_ID + "/" + clazz.getSimpleName() + "] ";
    }
    
    /** @return Returns the resource location as a string, or "null" if it is null. */
    public static String toString( @Nullable ResourceLocation res ) { return res == null ? "null" : res.toString(); }
}