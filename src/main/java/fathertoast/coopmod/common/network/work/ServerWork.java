package fathertoast.coopmod.common.network.work;

import fathertoast.coopmod.common.config.Config;
import fathertoast.coopmod.common.coordination.PingManager;
import fathertoast.coopmod.common.network.message.ServerboundBlockPingPacket;
import fathertoast.coopmod.common.network.message.ServerboundEntityPingPacket;
import fathertoast.crust.api.lib.DeferredAction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

public final class ServerWork {
    
    /** All players currently unable to send pings. */
    public static final Set<Player> PING_ON_COOLDOWN = new HashSet<>();
    
    public static void handlePing( ServerboundEntityPingPacket message, @Nullable ServerPlayer sender ) {
        if( sender == null || isPingOnCooldown( sender ) ) return;
        putPingOnCooldown( sender );
        PingManager.get( sender ).receivePing( message, sender );
    }
    
    public static void handlePing( ServerboundBlockPingPacket message, @Nullable ServerPlayer sender ) {
        if( sender == null || isPingOnCooldown( sender ) ) return;
        putPingOnCooldown( sender );
        PingManager.get( sender ).receivePing( message, sender );
    }
    
    /** @return True if the player's ping is on cooldown. */
    private static boolean isPingOnCooldown( Player player ) { return PING_ON_COOLDOWN.contains( player ); }
    
    /** Starts the player's ping cooldown timer. */
    private static void putPingOnCooldown( Player player ) {
        PING_ON_COOLDOWN.add( player );
        DeferredAction.queue( Config.MAIN.GENERAL.pingCooldown.get() - 5, // Some wiggle room for variance in latency
                () -> putPingOffCooldown( player ) );
    }
    
    /** Ends the player's ping cooldown timer. */
    private static boolean putPingOffCooldown( Player player ) {
        PING_ON_COOLDOWN.remove( player );
        return true;
    }
    
    
    private ServerWork() {}
}