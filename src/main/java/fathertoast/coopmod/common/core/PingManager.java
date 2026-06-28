package fathertoast.coopmod.common.core;

import fathertoast.coopmod.common.config.Config;
import fathertoast.coopmod.common.network.PacketHandler;
import fathertoast.coopmod.common.network.message.*;
import fathertoast.crust.api.util.OnClient;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * TODO
 */
public class PingManager {
    /** Mapping of each level key (dimension) to its ping manager instance. */
    private static final Map<ResourceKey<Level>, PingManager> MANAGERS = new HashMap<>();
    
    /** @return The ping manager for the player's current dimension. */
    public static PingManager get( Player player ) { return get( player.level() ); }
    
    /** @return The ping manager for the level's dimension. */
    public static PingManager get( Level level ) {
        PingManager manager = MANAGERS.get( level.dimension() );
        if( manager == null ) MANAGERS.put( level.dimension(), manager = new PingManager() );
        return manager;
    }
    
    /** Deletes the ping manager for the level's dimension. */
    public static void destroy( Level level ) { MANAGERS.remove( level.dimension() ); }
    
    /** Deletes all ping managers. */
    public static void reset() { MANAGERS.clear(); }
    
    
    public static boolean areAnyPingsActive( @Nullable Level level ) { return level != null && get( level ).areAnyPingsActive(); }
    
    public static boolean isPinged( Entity entity ) { return get( entity.level() ).getIsPinged( entity ); }
    
    public static boolean isPinged( Level level, BlockPos pos ) { return get( level ).getIsPinged( pos ); }
    
    @Nullable
    public static Ping.EntityData getPingData( Entity entity ) { return get( entity.level() ).ENTITY_PINGS.get( entity.getId() ); }
    
    @Nullable
    public static Ping.BlockData getPingData( Level level, BlockPos pos ) { return get( level ).BLOCK_PINGS.get( pos ); }
    
    @OnClient
    public static void ping( @Nullable Player player, @Nullable HitResult target, int duration ) {
        if( player != null ) get( player ).pingFor( player, target, duration );
    }
    
    @OnClient
    public static void pingEntity( Player player, Entity entity, int duration ) { get( player ).pingEntityFor( player, entity, duration ); }
    
    @OnClient
    public static void pingBlock( Player player, BlockPos pos, int duration ) { get( player ).pingBlockFor( player, pos, duration ); }
    
    
    public static int getColor( Player player ) { return 0xFF00FF; } //TODO player color logic
    
    public static void onPlayerJoinServerLevel( ServerLevel level, ServerPlayer player ) {
        PingManager manager = get( level );
        if( manager.areAnyPingsActive() ) manager.sendSync( player );
    }
    
    
    // ---- Instance Implementation ---- //
    
    private final HashMap<Integer, Ping.EntityData> ENTITY_PINGS = new HashMap<>();
    private final HashMap<BlockPos, Ping.BlockData> BLOCK_PINGS = new HashMap<>();
    
    private PingManager() {}
    
    public boolean areAnyPingsActive() { return !ENTITY_PINGS.isEmpty() || !BLOCK_PINGS.isEmpty(); }
    
    public boolean getIsPinged( Entity entity ) { return ENTITY_PINGS.containsKey( entity.getId() ); }
    
    public boolean getIsPinged( BlockPos pos ) { return BLOCK_PINGS.containsKey( pos ); }
    
    public Set<Map.Entry<Integer, Ping.EntityData>> getEntityPings() { return ENTITY_PINGS.entrySet(); }
    
    public Set<Map.Entry<BlockPos, Ping.BlockData>> getBlockPings() { return BLOCK_PINGS.entrySet(); }
    
    @OnClient
    private void pingFor( Player player, @Nullable HitResult target, int duration ) {
        if( target instanceof EntityHitResult entityTarget )
            pingEntityFor( player, entityTarget.getEntity(), duration );
        else if( target instanceof BlockHitResult blockTarget )
            pingBlockFor( player, blockTarget.getBlockPos(), duration );
    }
    
    @OnClient
    private void pingEntityFor( Player player, Entity entity, int duration ) {
        clearPingsFor( player );
        ENTITY_PINGS.put( entity.getId(), Ping.of( player, duration, entity.getId() ) );
        PacketHandler.sendPingToServer( entity.getId() );
    }
    
    @OnClient
    private void pingBlockFor( Player player, BlockPos pos, int duration ) {
        if( player.level().isLoaded( pos ) ) {
            clearPingsFor( player );
            BLOCK_PINGS.put( pos, Ping.of( player, duration, pos ) );
            PacketHandler.sendPingToServer( pos );
        }
    }
    
    public void clearPingsFor( Player player ) { clearPingsFor( player.getGameProfile().getName() ); }
    
    public void clearPingsFor( String playerName ) {
        ENTITY_PINGS.values().removeIf( pingData -> pingData.isOwner( playerName ) );
        BLOCK_PINGS.values().removeIf( pingData -> pingData.isOwner( playerName ) );
    }
    
    public void sendSync( ServerPlayer player ) {
        PacketHandler.sendSync( new ClientboundPingManagerSyncPacket( this, player ), player );
    }
    
    public void receivePing( ServerboundEntityPingPacket message, ServerPlayer sender ) {
        Entity entity = sender.level().getEntity( message.entityId() );
        if( entity == null || entity.isRemoved() || isEntityTooFar( sender, entity ) ) return;
        
        clearPingsFor( sender );
        Ping.EntityData pingData = Ping.of( sender, Config.MAIN.GENERAL.pingDuration.get(), message.entityId() );
        ENTITY_PINGS.put( message.entityId(), pingData );
        
        PacketHandler.sendPing( new ClientboundEntityPingPacket( message.entityId(), pingData ), sender );
    }
    
    public void receivePing( ServerboundBlockPingPacket message, ServerPlayer sender ) {
        if( !sender.level().isLoaded( message.blockPos() ) ) return;
        BlockState state = sender.level().getBlockState( message.blockPos() );
        if( state.isAir() || isBlockTooFar( sender, message.blockPos() ) ) return;
        
        clearPingsFor( sender );
        Ping.BlockData pingData = Ping.of( sender, Config.MAIN.GENERAL.pingDuration.get(), message.blockPos() );
        BLOCK_PINGS.put( message.blockPos(), pingData );
        
        PacketHandler.sendPing( new ClientboundBlockPingPacket( message.blockPos(), pingData ), sender );
    }
    
    @OnClient
    public void receiveSync( ClientboundPingManagerSyncPacket message ) {
        ENTITY_PINGS.putAll( message.entityPings() );
        BLOCK_PINGS.putAll( message.blockPings() );
    }
    
    @OnClient
    public void receivePing( ClientboundEntityPingPacket message ) {
        if( message.pingData() != null ) {
            clearPingsFor( message.pingData().playerName );
            ENTITY_PINGS.put( message.entityId(), message.pingData() );
        }
    }
    
    @OnClient
    public void receivePing( ClientboundBlockPingPacket message ) {
        if( message.pingData() != null ) {
            clearPingsFor( message.pingData().playerName );
            BLOCK_PINGS.put( message.blockPos(), message.pingData() );
        }
    }
    
    private static boolean isEntityTooFar( ServerPlayer player, Entity entity ) {
        double range = Config.MAIN.GENERAL.maxInspectRange.get() + 2.0;
        Vec3 eyePos = player.getEyePosition( 1.0F );
        AABB entityBB = entity.getBoundingBox();
        double dX = Math.min( Math.abs( entityBB.minX - eyePos.x ), Math.abs( entityBB.maxX - eyePos.x ) );
        double dY = Math.min( Math.abs( entityBB.minY - eyePos.y ), Math.abs( entityBB.maxY - eyePos.y ) );
        double dZ = Math.min( Math.abs( entityBB.minZ - eyePos.z ), Math.abs( entityBB.maxZ - eyePos.z ) );
        //TODO maybe log this, seems sus
        return dX * dX + dY * dY + dZ * dZ > range * range;
    }
    
    private static boolean isBlockTooFar( ServerPlayer player, BlockPos blockPos ) {
        double range = Config.MAIN.GENERAL.maxInspectRange.get() + 2.0;
        Vec3 eyePos = player.getEyePosition( 1.0F );
        double dX = blockPos.getX() + 0.5 - eyePos.x;
        double dY = blockPos.getY() + 0.5 - eyePos.y;
        double dZ = blockPos.getZ() + 0.5 - eyePos.z;
        //TODO maybe log this, seems sus
        return dX * dX + dY * dY + dZ * dZ > range * range;
    }
}