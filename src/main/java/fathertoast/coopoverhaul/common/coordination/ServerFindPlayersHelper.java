package fathertoast.coopoverhaul.common.coordination;

import fathertoast.coopoverhaul.common.config.Config;
import fathertoast.coopoverhaul.common.network.PacketHandler;
import fathertoast.coopoverhaul.common.network.message.ClientboundFindPlayersDataPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.Team;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Performs the server-side find player functions.
 * Provides player data to clients when requested to enable finding players beyond render distance.
 *
 * @see fathertoast.coopoverhaul.client.coordination.FindPlayersManager
 */
public final class ServerFindPlayersHelper {
    
    /** Players that are currently subscribed to server-side "find players" data. */
    private static final Set<UUID> TRACKING_PLAYERS = new HashSet<>();
    /**
     * Players queued for tracking removal. This prevents the server from sending out packets faster
     * than the update rate to players who enable & disable tracking very frequently.
     */
    private static final Set<UUID> REMOVE_PLAYERS = new HashSet<>();
    
    /** Number of ticks until we are allowed to send another ping. */
    private static int updateCooldown;
    
    /** Handles the tracking request from a player. */
    public static void setTracker( ServerPlayer player, boolean enable ) {
        if( enable ) addTracker( player );
        else removeTracker( player );
    }
    
    /** Subscribes the player to "find players" data. */
    public static void addTracker( ServerPlayer player ) {
        if( TRACKING_PLAYERS.add( player.getUUID() ) ) update( player ); // Send initial update immediately
        else REMOVE_PLAYERS.remove( player.getUUID() ); // Remove from unsubscribe queue
    }
    
    /** Queues the player to be unsubscribed from "find players" data. */
    public static void removeTracker( ServerPlayer player ) { REMOVE_PLAYERS.add( player.getUUID() ); }
    
    /** Called at the end of each server tick to update logic. */
    public static void onTick( MinecraftServer server ) {
        updateCooldown++;
        if( updateCooldown > Config.MAIN.GENERAL.findPlayersUpdateCooldown.get() ) {
            updateCooldown = 0;
            PlayerList players = server.getPlayerList();
            TRACKING_PLAYERS.removeAll( REMOVE_PLAYERS );
            REMOVE_PLAYERS.clear();
            TRACKING_PLAYERS.forEach( uuid -> update( players.getPlayer( uuid ) ) );
        }
    }
    
    /** Sends an update packet to the player. */
    private static void update( @Nullable ServerPlayer player ) {
        if( player == null ) return;
        
        Map<UUID, BlockPos> playerPositions = new HashMap<>();
        for( Player otherPlayer : player.level().players() ) {
            if( canFind( player, otherPlayer ) ) {
                playerPositions.put( otherPlayer.getUUID(), otherPlayer.blockPosition() );
            }
        }
        PacketHandler.sendFindPlayersData( new ClientboundFindPlayersDataPacket( playerPositions ), player );
    }
    
    /** @return True if the player is allowed to find a particular entity. */
    public static boolean canFind( Player player, Entity entity ) {
        return canFind( player, entity, Config.MAIN.GENERAL.maxFindPlayersRange.get() *
                Config.MAIN.GENERAL.maxFindPlayersRange.get() );
    }
    
    /** @return True if the player is allowed to find a particular entity. */
    public static boolean canFind( Player player, Entity entity, double rangeSq ) {
        return !entity.isSpectator() && !entity.isRemoved() && !player.isSpectator() && !entity.equals( player ) &&
                /* Comment this out for easier testing -> */ entity instanceof Player &&
                !entity.isInvisibleTo( player ) && notOnOpposingTeams( player, entity ) &&
                player.distanceToSqr( entity ) < rangeSq;
    }
    
    /** @return False only if both the player and entity are in teams and those teams are not allied. */
    //TODO should we wrap this into the friendly fire setting as a global "when to say a player is friendly" setting?
    private static boolean notOnOpposingTeams( Player player, Entity entity ) {
        Team playerTeam = player.getTeam();
        Team otherTeam = entity.getTeam();
        return playerTeam == null || otherTeam == null || playerTeam.isAlliedTo( otherTeam );
    }
    
    
    private ServerFindPlayersHelper() {}
}