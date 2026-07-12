package fathertoast.coopoverhaul.common.util;

import fathertoast.coopoverhaul.api.common.util.CoOpOverhaulObjects;
import fathertoast.coopoverhaul.common.config.Config;
import fathertoast.coopoverhaul.common.core.CoOpOverhaulMod;
import fathertoast.coopoverhaul.common.network.message.ClientboundMainConfigSyncPacket;
import fathertoast.crust.api.util.OnClient;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.UUID;

/**
 * Contains various attribute modifier related utilities and constants.
 */
public final class AttributeModUtil {
    
    /**
     * An inspect range modifier used for granting players the base/starting amount of inspect range.
     * Only used and applied on the logical server, relies on the built-in attribute sync to update clients.
     */
    private static AttributeModifier BASE_INSPECT_RANGE_MODIFIER;
    
    /**
     * An inspect range modifier associated with the spyglass item.
     * Actual application performed by the logical server, but this is synced to the client for tooltip info.
     */
    private static AttributeModifier SPYGLASS_INSPECT_RANGE_MODIFIER;
    
    
    // -------------------- Utility methods --------------------
    
    /**
     * @return The modifier used to give players their base amount of inspect range.
     * Only used and applied on the logical server, relies on the built-in attribute sync to update clients.
     */
    public static AttributeModifier getBaseInspectRangeMod() { return BASE_INSPECT_RANGE_MODIFIER; }
    
    /** @return The modifier used to give players additional inspect range when holding/using a spyglass. */
    public static AttributeModifier getSpyglassInspectRangeMod() { return SPYGLASS_INSPECT_RANGE_MODIFIER; }
    
    /**
     * Called on the server when the config is updated to rebuild attribute modifiers and update existing players.
     */
    public static void updateServerModifiers() {
        createBaseInspectionRangeMod( Config.MAIN.GENERAL.baseInspectRange.get() );
        createSpyglassInspectionRangeMod( Config.MAIN.GENERAL.spyglassInspectRange.get() );
        
        // If the server is running, update modifiers on existing players
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if( server != null ) {
            for( ServerPlayer player : server.getPlayerList().getPlayers() ) updateModifiers( player );
        }
    }
    
    /**
     * Called on the client when receiving a main config sync packet.
     * Rebuilds necessary attribute modifiers with configured values.
     */
    @OnClient
    public static void updateClientModifiers( ClientboundMainConfigSyncPacket message ) {
        createSpyglassInspectionRangeMod( message.spyglassInspectRange() );
    }
    
    /** Refresh or add all config-based modifiers to the player. */
    public static void updateModifiers( ServerPlayer player ) {
        final Attribute attribute = CoOpOverhaulObjects.Attributes.INSPECT_RANGE.get();
        final AttributeInstance instance = player.getAttribute( attribute );
        if( instance == null ) {
            CoOpOverhaulMod.LOG.warn( "Somehow, player '{}' does not have the inspection range attribute! :(",
                    player.getGameProfile().getName() );
            return;
        }
        
        instance.removePermanentModifier( getBaseInspectRangeMod().getId() );
        instance.addPermanentModifier( getBaseInspectRangeMod() );
        
        if( player.getAttributes().hasModifier( attribute, getSpyglassInspectRangeMod().getId() ) ) {
            instance.removeModifier( getSpyglassInspectRangeMod().getId() );
            instance.addTransientModifier( getSpyglassInspectRangeMod() );
        }
    }
    
    /** Helper method for rebuilding the base inspection range modifier, with the given value. */
    private static void createBaseInspectionRangeMod( double value ) {
        BASE_INSPECT_RANGE_MODIFIER = new AttributeModifier(
                UUID.fromString( "6d5293f0-4793-42d2-a256-78ff233d2e32" ),
                "Base inspection range modifier",
                value,
                AttributeModifier.Operation.ADDITION
        );
    }
    
    /** Helper method for rebuilding the spyglass inspection range modifier, with the given value. */
    private static void createSpyglassInspectionRangeMod( double value ) {
        SPYGLASS_INSPECT_RANGE_MODIFIER = new AttributeModifier(
                UUID.fromString( "409c1960-50f3-4df5-b8bc-3a0400e5d97f" ),
                "Spyglass inspection range boost",
                value,
                AttributeModifier.Operation.ADDITION
        );
    }
    
    
    private AttributeModUtil() {}
}