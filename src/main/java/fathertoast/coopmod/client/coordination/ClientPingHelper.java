package fathertoast.coopmod.client.coordination;

import fathertoast.coopmod.client.config.ClientConfig;
import fathertoast.coopmod.common.coordination.PingManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;

public final class ClientPingHelper {
    
    /** Number of ticks until we are allowed to send another ping. */
    private static int localPingCooldown;
    
    /** @return The client's ping manager. Only returns null when not in game. */
    @Nullable
    public static PingManager pingManager() {
        Level level = Minecraft.getInstance().level;
        return level == null ? null : PingManager.get( level );
    }
    
    /** Pings the target, if possible. */
    public static void ping( @Nullable HitResult hitResult ) {
        if( localPingCooldown <= 0 ) {
            localPingCooldown = ClientConfig.getPingCooldown();
            PingManager.ping( Minecraft.getInstance().player, hitResult, ClientConfig.getPingDuration() );
        }
    }
    
    /** Pings the current inspect target, if any. */
    public static void ping() { ping( InspectManager.target() ); }
    
    /** Pings whatever the player is currently looking at. Performs a ray trace if not already inspecting something. */
    public static void quickPing() {
        if( InspectManager.isEnabled() ) { ping(); }
        else {
            Minecraft client = Minecraft.getInstance();
            if( client.player != null && !client.player.isSpectator() ) {
                ping( InspectManager.rayCast( client, client.player, 1.0F ) );
            }
        }
    }
    
    /** Called at the end of each tick to update logic. */
    public static void onTick() {
        localPingCooldown = Math.max( 0, localPingCooldown - 1 );
    }
    
    
    private ClientPingHelper() {}
}