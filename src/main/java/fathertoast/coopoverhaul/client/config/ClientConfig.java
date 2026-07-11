package fathertoast.coopoverhaul.client.config;

import fathertoast.coopoverhaul.client.coordination.FindPlayersManager;
import fathertoast.coopoverhaul.client.coordination.InspectManager;
import fathertoast.coopoverhaul.common.core.CoOpOverhaulMod;
import fathertoast.coopoverhaul.common.network.message.ClientboundMainConfigSyncPacket;
import fathertoast.crust.api.config.common.ConfigManager;

/**
 * Used as the sole hub for all client-side config access from outside the config package.
 * <p>
 * Contains references to all client-side config files used in this mod, which in turn provide direct
 * 'getter' access to each configurable value.
 * <p>
 * Also, this contains the main client-side config spec itself.
 */
public class ClientConfig {
    
    public static ClientPreferences PREFS;
    
    /** Performs loading of configs in this mod. Added to deferred work queue at common setup. */
    public static void initialize() {
        ConfigManager manager = ConfigManager.getRequired( CoOpOverhaulMod.MOD_ID );
        
        PREFS = new ClientPreferences( manager, "_client_prefs" );
        PREFS.SPEC.initialize();
    }
    
    private static double maxInspectRange;
    
    private static int pingDuration;
    private static int pingCooldown = Integer.MAX_VALUE;
    
    private static double maxFindPlayersRange;
    
    private static boolean trollHiddenBlocks = true;
    
    /** Updates all fields set by the logical server. */
    public static void sync( ClientboundMainConfigSyncPacket message ) {
        AttributeModUtil.syncModifiers( message );
        // Inspect feature
        maxInspectRange = message.maxInspectRange();
        // Ping feature
        pingDuration = message.pingDuration();
        pingCooldown = message.pingCooldown();
        // Find Players feature
        maxFindPlayersRange = message.maxFindPlayersRange();
        FindPlayersManager.updateRange();
        // Highlight feature
        trollHiddenBlocks = !message.allowRecoloringHidden();
    }
    
    /** The max value allowed for inspect range. Set by logical server. */
    public static double getMaxInspectRange() { return maxInspectRange; }
    
    /** Ticks before pings fade. Set by logical server. */
    public static int getPingDuration() { return pingDuration; }
    
    /** Minimum ticks required between pings. Set by logical server. */
    public static int getPingCooldown() { return pingCooldown; }
    
    /** The max value allowed for find players range. Set by logical server. */
    public static double getMaxFindPlayersRange() { return maxFindPlayersRange; }
    
    /** True if we should not allow identifying hidden blocks (e.g, infested). Set by logical server. */
    public static boolean trollHiddenBlocks() { return trollHiddenBlocks; }
}