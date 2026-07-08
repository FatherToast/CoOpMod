package fathertoast.coopmod.common.compat.naturalabsorption;

import fathertoast.naturalabsorption.api.INaturalAbsorption;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public class CMNaturalAbsorptionPlugin {
    
    public static final Function<INaturalAbsorption, Void> RECEIVER = ( apiInstance ) -> {
        API_INSTANCE = apiInstance;
        return null;
    };
    
    public static INaturalAbsorption API_INSTANCE;
    
    /**
     * @return The player's max absorption, from all sources combined.
     * In other words, the actual limit on absorption recovery.
     */
    public static double getMaxAbsorption( Player player ) {
        return API_INSTANCE.getAbsorptionAccessor().getMaxAbsorption( player );
    }
}