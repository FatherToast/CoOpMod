package fathertoast.coopmod.common.util;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;

/**
 * Contains helper methods for determining what things clients should be tracking.
 * <p>
 * Used to filter out irrelevant data to reduce size and quantity of clientbound packets.
 * <p>
 * In general, tracking is managed on the server side by {@link ChunkMap}.
 */
public final class TrackingHelper {
    /**
     * @return True if the player is on the same functional side as us.
     * Note that this NOT equivalent to either the logical side or the physical side.<p>
     * Logical client: Always true.<p>
     * Logical server: True only for the server owner on an integrated server; never true on a dedicated server.
     */
    public static boolean isLocalPlayer( Player player ) {
        if( player.isLocalPlayer() ) return true;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if( server == null ) return true;
        if( server.isDedicatedServer() ) return false;
        GameProfile profile = server.getSingleplayerProfile();
        return profile != null && player.getGameProfile().getName().equalsIgnoreCase( profile.getName() );
    }
    
    /**
     * @return True if the entity is within a player's tracking range. Calculation based on
     * {@link ChunkMap.TrackedEntity#updatePlayer(ServerPlayer)}.
     */
    @SuppressWarnings( "JavadocReference" )
    public static boolean isEntityInTrackingRange( ServerPlayer player, @Nullable Entity entity ) {
        MinecraftServer server = player.getServer();
        if( server == null || entity == null || player == entity ) return false; // Don't track self
        
        // Calculate effective tracking range
        int trackingRange = entity.getType().clientTrackingRange();
        if( trackingRange == 0 ) return false; // Looks silly, but this is how the game does things
        for( Entity e : entity.getIndirectPassengers() ) {
            int passengerTrackingRange = e.getType().clientTrackingRange();
            if( passengerTrackingRange > trackingRange ) {
                trackingRange = passengerTrackingRange;
            }
        }
        trackingRange = Math.min( server.getScaledTrackingDistance( SectionPos.sectionToBlockCoord( trackingRange ) ),
                SectionPos.sectionToBlockCoord( server.getPlayerList().getViewDistance() ) );
        
        // Finally, do the distance check
        double dX = entity.position().x() - player.position().x();
        double dZ = entity.position().z() - player.position().z();
        return dX * dX + dZ * dZ <= trackingRange * trackingRange;
    }
    
    /**
     * @return True if the block position is within a player's tracking range. Calculation based on
     * {@link ChunkMap#move(ServerPlayer)}.
     */
    public static boolean isBlockInTrackingRange( ServerPlayer player, BlockPos pos ) {
        return isChunkInTrackingRange( player,
                SectionPos.blockToSectionCoord( pos.getX() ),
                SectionPos.blockToSectionCoord( pos.getZ() ) );
    }
    
    /**
     * @return True if the chunk position is within a player's tracking range. Calculation based on
     * {@link ChunkMap#move(ServerPlayer)}.
     */
    public static boolean isChunkInTrackingRange( ServerPlayer player, int chunkX, int chunkZ ) {
        return isChunkInRange(
                SectionPos.blockToSectionCoord( player.getBlockX() ),
                SectionPos.blockToSectionCoord( player.getBlockZ() ),
                chunkX, chunkZ, player.serverLevel().getServer().getPlayerList().getViewDistance() );
    }
    
    /** @return True if the two chunk coordinates are within a certain range from each other. */
    public static boolean isChunkInRange( int x0, int z0, int x1, int z1, int range ) {
        return ChunkMap.isChunkInRange( x0, z0, x1, z1, range );
    }
    
    
    private TrackingHelper() {}
}