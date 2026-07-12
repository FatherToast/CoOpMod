package fathertoast.coopoverhaul.client.coordination;

import fathertoast.coopoverhaul.client.config.ClientConfig;
import fathertoast.coopoverhaul.client.vfx.HighlightManager;
import fathertoast.coopoverhaul.common.coordination.ServerFindPlayersHelper;
import fathertoast.coopoverhaul.common.network.PacketHandler;
import fathertoast.coopoverhaul.common.network.message.ClientboundFindPlayersDataPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Controls the 'find players' function. This causes all friendly players within the configured distance
 * to be highlighted, such that they are easier to find.
 * <p>
 * Cannot help finding players beyond your render distance, for now.
 *
 * @see HighlightManager
 * @see ServerFindPlayersHelper
 */
public final class FindPlayersManager {
    
    /** Previous tick's last known block positions of trackable players, if different. */
    private static final Map<UUID, BlockPos> PREVIOUS_PLAYER_POSITIONS = new HashMap<>();
    /** Last known block positions of trackable players. */
    private static final Map<UUID, BlockPos> PLAYER_POSITIONS = new HashMap<>();
    
    public static Set<Map.Entry<UUID, BlockPos>> getPlayerPositions() { return PLAYER_POSITIONS.entrySet(); }
    
    public static Vec3 getPos( UUID uuid, BlockPos pos, float partialTick ) {
        // Convert block pos to expected name tag position
        BlockPos previousPos = PREVIOUS_PLAYER_POSITIONS.get( uuid );
        if( previousPos != null ) { // lerp between last tick's pos and the new one
            return new Vec3(
                    Mth.lerp( partialTick, previousPos.getX(), pos.getX() ) + 0.5F,
                    Mth.lerp( partialTick, previousPos.getY(), pos.getY() ) + EntityType.PLAYER.getHeight() + 0.5F,
                    Mth.lerp( partialTick, previousPos.getZ(), pos.getZ() ) + 0.5F );
        }
        return Vec3.atLowerCornerWithOffset( pos,
                0.5F, EntityType.PLAYER.getHeight() + 0.5F, 0.5F );
    }
    
    /** Number of ticks until we disable ourselves. */
    private static int enabledDuration;
    
    /** True while the player is using find players mode. */
    private static boolean enabled;
    /** The max squared distance that we can highlight friendly players. */
    private static double rangeSq;
    
    /** Turns on "find players" mode, which highlights nearby friendly players. */
    public static void enable() { setEnabled( true ); }
    
    /** Turns off "find players" mode, which highlights nearby friendly players. */
    public static void disable() { setEnabled( false ); }
    
    /** Sets "find players" mode on or off, which highlights nearby friendly players. */
    public static void setEnabled( boolean on ) {
        if( enabled != on ) PacketHandler.requestFindPlayersData( on );
        
        enabled = on;
        enabledDuration = 0;
    }
    
    /** Flags "find players" mode to disable after the configured delay. */
    public static void delayDisable() {
        enabledDuration = ClientConfig.PREFS.PLAYER_FINDER.lingerDuration.get();
        if( enabledDuration == 0 ) enabled = false;
    }
    
    /** @return Whether "find players" mode is on or off. */
    public static boolean isEnabled() { return enabled; }
    
    /** Updates the find players range based on current settings. */
    public static void updateRange() {
        rangeSq = Math.min( ClientConfig.PREFS.PLAYER_FINDER.range.get(), ClientConfig.getMaxFindPlayersRange() );
        rangeSq *= rangeSq;
    }
    
    /** @return True if the entity should be highlighted. */
    public static boolean shouldHighlight( Entity entity ) {
        LocalPlayer player = Minecraft.getInstance().player;
        return isEnabled() && player != null && ServerFindPlayersHelper.canFind( player, entity, rangeSq );
    }
    
    /** @return True if the block position is within find players range. */
    public static boolean isInRange( LocalPlayer player, BlockPos pos ) {
        Vec3 playerPos = player.position();
        double dX = pos.getX() + 0.5 - playerPos.x;
        double dY = pos.getY() - playerPos.y;
        double dZ = pos.getZ() + 0.5 - playerPos.z;
        return dX * dX + dY * dY + dZ * dZ < rangeSq;
    }
    
    public static void updatePlayerPositions( ClientboundFindPlayersDataPacket message ) {
        message.playerPositions().forEach( ( uuid, pos ) -> {
            BlockPos previousPos = PLAYER_POSITIONS.get( uuid );
            if( previousPos != null ) PREVIOUS_PLAYER_POSITIONS.put( uuid, previousPos );
        } );
        PLAYER_POSITIONS.clear();
        PLAYER_POSITIONS.putAll( message.playerPositions() );
    }
    
    /** Called at the end of each tick to update logic. */
    public static void onTick() {
        if( enabledDuration > 0 ) {
            enabledDuration--;
            if( enabledDuration == 0 ) disable();
        }
        PREVIOUS_PLAYER_POSITIONS.clear();
    }
    
    
    private FindPlayersManager() {}
}