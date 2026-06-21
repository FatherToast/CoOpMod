package fathertoast.coopmod.common.config.dimension;

import fathertoast.coopmod.common.config.Config;
import fathertoast.crust.api.config.common.AbstractConfigCategory;
import fathertoast.crust.api.config.common.AbstractConfigFile;
import fathertoast.crust.api.config.common.ConfigManager;
import fathertoast.crust.api.config.common.field.BooleanField;
import fathertoast.coopmod.common.util.DimensionConfigHelper;
import net.minecraft.world.level.Level;

public class GeneralConfig extends AbstractConfigFile {
    
    /** The parent group containing this feature config. */
    public final DimensionConfigGroup DIMENSION_CONFIGS;
    
    public final General GENERAL;
    
    /** Builds the config spec that should be used for this config. */
    GeneralConfig( ConfigManager manager, String dir, DimensionConfigGroup dimConfigs ) {
        super( manager, "dungeon",
                "This config contains general and/or miscellaneous options specific to the " +
                        dimConfigs.longDimensionName() + "." );
        DIMENSION_CONFIGS = dimConfigs;
        
        if( Level.OVERWORLD.equals( dimConfigs.DIMENSION ) ) {
            SPEC.decreaseIndent();
            SPEC.newLine();
            SPEC.comment( "This config also functions as the default settings for features in any extra " +
                    "dimensions that do not have world gen configs (all dimensions not included in the \"" +
                    Config.MAIN.GENERAL.extraDimensions.getKey() + "\" list within the mod's main config file, \"" +
                    Config.MAIN.SPEC.NAME + "\")." );
            SPEC.increaseIndent();
        }
        
        GENERAL = new General( this, "general" );
    }
    
    
    public static class General extends AbstractConfigCategory<GeneralConfig> {
        
        public final BooleanField bink;
        
        General( GeneralConfig parent, String name ) {
            super( parent, name, "General and/or miscellaneous options." );
            
            SPEC.newLine();
            
            bink = SPEC.define( new BooleanField( "bink_bonk", false,
                    "If true, the big happens.",
                    DimensionConfigHelper.MESSAGE_NO_OVERRIDE ) );
        }
    }
}