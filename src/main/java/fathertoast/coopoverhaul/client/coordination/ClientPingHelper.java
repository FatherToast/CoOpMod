package fathertoast.coopoverhaul.client.coordination;

import fathertoast.coopoverhaul.client.config.ClientConfig;
import fathertoast.coopoverhaul.common.coordination.PingManager;
import fathertoast.coopoverhaul.common.network.message.ClientboundBlockPingPacket;
import fathertoast.coopoverhaul.common.network.message.ClientboundEntityPingPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Performs the client-side ping functions. Acts as the bridge between the 'inspect' function (which only exists on
 * the client side) and the 'ping' function (which exists on both sides).
 *
 * @see InspectManager
 * @see PingManager
 */
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
            playSound( hitResult );
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
    
    public static void receivePing( ClientboundEntityPingPacket message ) {
        PingManager manager = pingManager();
        if( manager != null ) {
            manager.receivePing( message );
            playSound( message.entityId() );
        }
    }
    
    public static void receivePing( ClientboundBlockPingPacket message ) {
        PingManager manager = pingManager();
        if( manager != null ) {
            manager.receivePing( message );
            playSound( message.blockPos() );
        }
    }
    
    /** Plays the appropriate ping sound for the target. */
    private static void playSound( @Nullable HitResult target ) {
        if( target instanceof EntityHitResult entityTarget )
            playSound( entityTarget.getEntity() );
        else if( target instanceof BlockHitResult blockTarget )
            playSound( blockTarget.getBlockPos() );
    }
    
    /** Plays the appropriate ping sound for the entity. */
    private static void playSound( int entityId ) {
        Level level = Minecraft.getInstance().level;
        if( level != null ) {
            Entity entity = level.getEntity( entityId );
            if( entity != null ) playSound( entity );
        }
    }
    
    /** Plays the appropriate ping sound for the entity. */
    private static void playSound( Entity entity ) {
        SoundEvent sound = ClientConfig.PREFS.HIGHLIGHT_SETTINGS.getSound( entity );
        if( sound == null ) return;
        // noinspection resource
        entity.level().playLocalSound( entity.getX(), entity.getY( 0.5 ), entity.getZ(),
                sound, SoundSource.PLAYERS, randomVolume( entity.level() ), randomPitch( entity.level() ), false );
    }
    
    /** Plays the appropriate ping sound for the block position. */
    private static void playSound( BlockPos pos ) {
        Level level = Minecraft.getInstance().level;
        if( level != null && level.isLoaded( pos ) ) {
            BlockState block = level.getBlockState( pos );
            SoundEvent sound = ClientConfig.PREFS.HIGHLIGHT_SETTINGS.getSound( block );
            if( sound == null ) return;
            
            level.playLocalSound( pos, sound, SoundSource.PLAYERS,
                    randomVolume( level ), randomPitch( level ), false );
        }
    }
    
    private static float randomVolume( Level level ) { return 0.4F + level.random.nextFloat() * 0.05F; }
    
    private static float randomPitch( Level level ) { return 0.9F + level.random.nextFloat() * 0.2F; }
    
    
    private ClientPingHelper() {}
}