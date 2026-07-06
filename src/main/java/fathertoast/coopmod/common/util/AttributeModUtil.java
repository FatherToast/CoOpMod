package fathertoast.coopmod.common.util;

import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.UUID;

/**
 * Contains various attribute modifier related utilities and constants.
 */
public final class AttributeModUtil {
    
    /** An inspection range modifier associated with the spyglass item. */
    public static final AttributeModifier SPYGLASS_MOD = new AttributeModifier(
            UUID.fromString( "409c1960-50f3-4df5-b8bc-3a0400e5d97f" ),
            "Spyglass inspection range boost",
            64.0,
            AttributeModifier.Operation.ADDITION
    );
    
    
    private AttributeModUtil() { }
}
