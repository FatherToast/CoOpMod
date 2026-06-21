package fathertoast.coopmod.common.config.dimension;

import fathertoast.crust.api.config.common.ConfigManager;
import fathertoast.coopmod.common.config.ConfigGroup;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * Groups together every config file used for a single dimension.
 */
public class DimensionConfigGroup extends ConfigGroup {
    public final ResourceKey<Level> DIMENSION;
    
    public final GeneralConfig GENERAL;
    
    public DimensionConfigGroup( ConfigManager manager, ResourceKey<Level> dimension ) {
        DIMENSION = dimension;
        
        // Organized in folder: configs/CoOpMod/dimension/<modid>/<dimension>/
        final String dir = "dimension/" + dimension.location().getNamespace() + "/" + dimension.location().getPath() + "/";
        
        GENERAL = group( new GeneralConfig( manager, dir, this ) );
    }
    
    /** @return The short name for this dimension (e.g. "'the_nether' dimension"). */
    public String dimensionName() { return "'" + DIMENSION.location().getPath() + "' dimension"; }
    
    /** @return The long name for this dimension (e.g. "'the_nether' dimension from 'minecraft'"). */
    public String longDimensionName() {
        return dimensionName() + " from '" + DIMENSION.location().getNamespace() + "'";
    }
}