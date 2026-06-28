package fathertoast.coopmod.common.core;

import fathertoast.coopmod.common.util.TrackingHelper;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * Stores the metadata associated with a ping (not the ping target).
 * <p>
 * This is to be used as a value in ping target-data maps to track information necessary to manage pings.
 *
 * @see PingManager
 */
public abstract class Ping {
    
    public static Ping.EntityData of( Player player, int duration, int entityId ) {
        return new EntityData( player, duration, entityId );
    }
    
    public static Ping.BlockData of( Player player, int duration, BlockPos pos ) {
        return new BlockData( player, duration, pos );
    }
    
    
    public final String playerName;
    public final long expiryTime;
    public final int color;
    
    protected Ping( Player player, int duration ) {
        playerName = player.getGameProfile().getName();
        expiryTime = player.level().getGameTime() + duration;
        color = TrackingHelper.isLocalPlayer( player ) ? -1 : PingManager.getColor( player );
    }
    
    protected Ping( FriendlyByteBuf buffer ) {
        playerName = buffer.readUtf( 16 );
        expiryTime = buffer.readLong();
        color = buffer.readInt();
    }
    
    public final void encode( FriendlyByteBuf buffer ) {
        buffer.writeUtf( playerName );
        buffer.writeLong( expiryTime );
        buffer.writeInt( color );
        encodeType( buffer );
    }
    
    protected abstract void encodeType( FriendlyByteBuf buffer );
    
    /** @return True if this ping data is owned by the player. */
    public boolean isOwner( Player player ) { return isOwner( player.getGameProfile().getName() ); }
    
    /** @return True if this ping data is owned by the named player. */
    public boolean isOwner( String name ) { return playerName.equalsIgnoreCase( name ); }
    
    /** @return True if this ping data is expired. */
    public boolean isExpired( long gameTime ) { return expiryTime < gameTime; }
    
    /** @return How long (in ticks) until this ping expires. */
    public int timeRemaining( long gameTime ) { return clampToInt( expiryTime - gameTime ); }
    
    private static int clampToInt( long l ) {
        return l > (long) Integer.MAX_VALUE ? Integer.MAX_VALUE : l < (long) Integer.MIN_VALUE ? Integer.MIN_VALUE : (int) l;
    }
    
    
    public static class EntityData extends Ping {
        
        private EntityData( Player player, int duration, int ignoredEntityId ) { super( player, duration ); }
        
        public EntityData( FriendlyByteBuf buffer ) { super( buffer ); }
        
        @Override
        protected void encodeType( FriendlyByteBuf buffer ) {}
        
        /**
         * @param level    The level this ping is in.
         * @param entityId This ping's target.
         * @return True if the pinged entity is no longer present.
         */
        public boolean isRemoved( Level level, int entityId ) {
            Entity entity = level.getEntity( entityId );
            return entity == null || entity.isRemoved();
        }
    }
    
    public static class BlockData extends Ping {
        /** Block of the ping target; used to detect when the block is destroyed to cancel the ping. */
        public final Block block;
        
        private BlockData( Player player, int duration, BlockPos pos ) {
            super( player, duration );
            block = player.level().getBlockState( pos ).getBlock();
        }
        
        public BlockData( FriendlyByteBuf buffer ) {
            super( buffer );
            //noinspection deprecation
            block = buffer.readById( BuiltInRegistries.BLOCK );
        }
        
        @Override
        protected void encodeType( FriendlyByteBuf buffer ) {
            //noinspection deprecation
            buffer.writeId( BuiltInRegistries.BLOCK, block );
        }
        
        /**
         * @param level The level this ping is in.
         * @param pos   This ping's target.
         * @return True if the pinged block is no longer present.
         */
        public boolean isRemoved( Level level, BlockPos pos ) {
            return !level.isLoaded( pos ) || !block.equals( level.getBlockState( pos ).getBlock() );
        }
    }
}