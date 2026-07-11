package fathertoast.coopoverhaul.common.config;

import fathertoast.coopoverhaul.common.core.CoOpOverhaulMod;
import fathertoast.crust.api.config.common.ConfigManager;

/**
 * Used as the sole hub for all common-side config access from outside the config package.
 * <p>
 * Contains references to all common-side config files used in this mod, which in turn provide direct
 * 'getter' access to each configurable value.
 */
public class Config {
    private static final ConfigManager MANAGER = ConfigManager.create( "CoOpOverhaul", CoOpOverhaulMod.MOD_ID );
    
    public static MainConfig MAIN = new MainConfig( MANAGER, "_main" );
    
    /**
     * Performs loading of configs in this mod with values that are needed early in the mod loading cycle.
     * Called by the mod's constructor.
     */
    public static void initializeEarly() {
        MAIN.SPEC.initialize();
    }
    
    /** Performs loading of configs in this mod. Added to deferred work queue at common setup. */
    public static void initialize() { }
}