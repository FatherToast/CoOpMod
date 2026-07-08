package fathertoast.coopmod.common.util;

import fathertoast.coopmod.common.config.Config;
import fathertoast.coopmod.common.network.message.ClientboundMainConfigSyncPacket;
import fathertoast.crust.api.util.OnClient;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.UUID;

/**
 * Contains various attribute modifier related utilities and constants.
 */
public final class AttributeModUtil {
    
    /**
     * An inspection range modifier used for granting players the base/starting amount of inspection range.
     * This modifier is created after startup on the server, and is reconstructed on the client when logging inn.
     */
    private static AttributeModifier BASE_INSPECTION_RANGE_MODIFIER;
    
    /** An inspection range modifier associated with the spyglass item. */
    private static AttributeModifier SPYGLASS_INSPECTION_RANGE_MODIFIER;
    
    
    // -------------------- Utility methods --------------------
    
    /** @return The modifier used to give players their base amount of inspection range. */
    public static AttributeModifier getBaseInspectionRangeMod() {
        return BASE_INSPECTION_RANGE_MODIFIER;
    }
    
    /** @return The modifier used to give players additional range when holding/using a spyglass. */
    public static AttributeModifier getSpyglassInspectionRangeMod() {
        return SPYGLASS_INSPECTION_RANGE_MODIFIER;
    }
    
    /**
     * Called on the client when receiving a main config sync packet.
     * Rebuilds attribute modifiers with configurable values.
     */
    @OnClient
    public static void syncModifiers( ClientboundMainConfigSyncPacket message ) {
        createBaseInspectionRangeMod( message.defaultInspectRange() );
        createSpyglassInspectionRangeMod( message.spyglassInspectRange() );
    }
    
    /** Helper method for rebuilding the base inspection range modifier, with the given value. */
    private static void createBaseInspectionRangeMod( double value ) {
        BASE_INSPECTION_RANGE_MODIFIER = new AttributeModifier(
                UUID.fromString( "6d5293f0-4793-42d2-a256-78ff233d2e32" ),
                "Base inspection range modifier",
                value,
                AttributeModifier.Operation.ADDITION
        );
    }
    
    /** Helper method for rebuilding the spyglass inspection range modifier, with the given value. */
    private static void createSpyglassInspectionRangeMod( double value ) {
        SPYGLASS_INSPECTION_RANGE_MODIFIER = new AttributeModifier(
                UUID.fromString( "409c1960-50f3-4df5-b8bc-3a0400e5d97f" ),
                "Spyglass inspection range boost",
                value,
                AttributeModifier.Operation.ADDITION
        );
    }
    
    
    // -------------------- Event listener --------------------
    
    /**
     * Added as a listener from {@link fathertoast.coopmod.common.core.CoOpMod#CoOpMod(FMLJavaModLoadingContext)}.
     * <br><br>
     * Called on the server when the server has finished loading and is ready for playing.
     */
    @SuppressWarnings( "unused" )
    public static void onServerStarted( ServerStartedEvent event ) {
        createBaseInspectionRangeMod( Config.MAIN.GENERAL.defaultInspectRange.get() );
        createSpyglassInspectionRangeMod( Config.MAIN.GENERAL.spyglassInspectRange.get() );
    }
    
    
    private AttributeModUtil() { }
}
