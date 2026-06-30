package fathertoast.coopmod.common.core;

import fathertoast.coopmod.common.config.Config;
import fathertoast.coopmod.common.network.PacketHandler;
import fathertoast.coopmod.common.network.message.*;
import fathertoast.crust.api.util.OnClient;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
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
        if( manager == null ) MANAGERS.put( level.dimension(), manager = new PingManager( level ) );
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
    
    private final Map<Integer, Ping.EntityData> ENTITY_PINGS = new HashMap<>();
    private final Map<BlockPos, Ping.BlockData> BLOCK_PINGS = new HashMap<>();
    
    public final Level level;
    
    private PingManager( Level lvl ) { level = lvl; }
    
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
        gatherAttachedEntities( ENTITY_PINGS, level, entity.getId(), Ping.of( player, duration, entity.getId() ) );
        PacketHandler.sendPingToServer( entity.getId() );
    }
    
    @OnClient
    private void pingBlockFor( Player player, BlockPos pos, int duration ) {
        if( player.level().isLoaded( pos ) ) {
            clearPingsFor( player );
            gatherAttachedBlocks( BLOCK_PINGS, level, pos, Ping.of( player, duration, pos ) );
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
        gatherAttachedEntities( ENTITY_PINGS, level, message.entityId(), pingData );
        
        PacketHandler.sendPing( new ClientboundEntityPingPacket( message.entityId(), pingData ), sender );
    }
    
    public void receivePing( ServerboundBlockPingPacket message, ServerPlayer sender ) {
        if( !sender.level().isLoaded( message.blockPos() ) ) return;
        BlockState state = sender.level().getBlockState( message.blockPos() );
        if( state.isAir() || isBlockTooFar( sender, message.blockPos() ) ) return;
        
        clearPingsFor( sender );
        Ping.BlockData pingData = Ping.of( sender, Config.MAIN.GENERAL.pingDuration.get(), message.blockPos() );
        gatherAttachedBlocks( BLOCK_PINGS, level, message.blockPos(), pingData );
        
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
            gatherAttachedEntities( ENTITY_PINGS, level, message.entityId(), message.pingData() );
        }
    }
    
    @OnClient
    public void receivePing( ClientboundBlockPingPacket message ) {
        if( message.pingData() != null ) {
            clearPingsFor( message.pingData().playerName );
            gatherAttachedBlocks( BLOCK_PINGS, level, message.blockPos(), message.pingData() );
        }
    }
    
    /** Populates the provided map with all ping data that should be generated by pinging the initial target. */
    public static void gatherAttachedEntities( Map<Integer, Ping.EntityData> pingMap, Level level, int entityId, Ping.EntityData pingData ) {
        // The initial target entity
        pingMap.put( entityId, pingData );
        // Also ping all other entities in the stack of passengers
        Entity entity = level.getEntity( entityId );
        if( entity != null ) {
            entity.getRootVehicle().getPassengersAndSelf().forEach( ( passenger ) -> {
                if( passenger.getId() != entityId ) pingMap.put( passenger.getId(), pingData );
            } );
        }
    }
    
    /** Populates the provided map with all ping data that should be generated by pinging the initial target. */
    public static void gatherAttachedBlocks( Map<BlockPos, Ping.BlockData> pingMap, Level level, BlockPos pos, Ping.BlockData pingData ) {
        // The initial target block
        pingMap.put( pos, pingData );
        // Also ping other parts of multipart blocks (for now, only very specific ones; no infinite connected blocks like redstone/rails/seaweed)
        // In general, we first use the initial target block to identify where any connected blocks should be,
        // then validate that those blocks are correct and pinging them if so - sadly no generic way to handle these that I know of
        BlockState state = level.getBlockState( pos );
        if( state.hasProperty( BlockStateProperties.DOUBLE_BLOCK_HALF ) ) {
            // Double blocks (e.g., doors, tall grass, etc.)
            DoubleBlockHalf half = state.getValue( BlockStateProperties.DOUBLE_BLOCK_HALF );
            BlockPos otherPos = half == DoubleBlockHalf.UPPER ?
                    pos.below() : pos.above();
            BlockState otherState = level.getBlockState( otherPos );
            if( otherState.hasProperty( BlockStateProperties.DOUBLE_BLOCK_HALF ) &&
                    otherState.getValue( BlockStateProperties.DOUBLE_BLOCK_HALF ) != half ) {
                pingMap.put( otherPos, Ping.of( pingData, otherState.getBlock() ) );
            }
        }
        else if( state.hasProperty( BlockStateProperties.BED_PART ) && state.hasProperty( BlockStateProperties.HORIZONTAL_FACING ) ) {
            // Beds
            BedPart bedPart = state.getValue( BlockStateProperties.BED_PART );
            Direction direction = state.getValue( BlockStateProperties.HORIZONTAL_FACING );
            BlockPos otherPos = pos.relative( bedPart == BedPart.FOOT ? direction : direction.getOpposite() );
            BlockState otherState = level.getBlockState( otherPos );
            if( otherState.hasProperty( BlockStateProperties.BED_PART ) && otherState.hasProperty( BlockStateProperties.HORIZONTAL_FACING ) &&
                    otherState.getValue( BlockStateProperties.BED_PART ) != bedPart &&
                    otherState.getValue( BlockStateProperties.HORIZONTAL_FACING ) == direction ) {
                pingMap.put( otherPos, Ping.of( pingData, otherState.getBlock() ) );
            }
        }
        else if( state.hasProperty( BlockStateProperties.CHEST_TYPE ) && state.hasProperty( BlockStateProperties.HORIZONTAL_FACING ) ) {
            ChestType chestType = state.getValue( BlockStateProperties.CHEST_TYPE );
            if( chestType != ChestType.SINGLE ) {
                // Double chests
                Direction direction = state.getValue( BlockStateProperties.HORIZONTAL_FACING );
                BlockPos otherPos = pos.relative( chestType == ChestType.LEFT ? direction.getClockWise() : direction.getCounterClockWise() );
                BlockState otherState = level.getBlockState( otherPos );
                if( otherState.hasProperty( BlockStateProperties.CHEST_TYPE ) && otherState.hasProperty( BlockStateProperties.HORIZONTAL_FACING ) &&
                        otherState.getValue( BlockStateProperties.CHEST_TYPE ) == (chestType == ChestType.LEFT ? ChestType.RIGHT : ChestType.LEFT) &&
                        otherState.getValue( BlockStateProperties.HORIZONTAL_FACING ) == direction ) {
                    pingMap.put( otherPos, Ping.of( pingData, otherState.getBlock() ) );
                }
            }
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