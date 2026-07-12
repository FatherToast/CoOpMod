package fathertoast.coopoverhaul.common.network.message;

import fathertoast.coopoverhaul.common.coordination.Ping;
import fathertoast.coopoverhaul.common.coordination.PingManager;
import fathertoast.coopoverhaul.common.core.CoOpOverhaulMod;
import fathertoast.coopoverhaul.common.network.work.ClientWork;
import fathertoast.coopoverhaul.common.util.TrackingHelper;
import io.netty.handler.codec.DecoderException;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public record ClientboundPingManagerSyncPacket( Map<Integer, Ping.EntityData> entityPings,
                                                Map<BlockPos, Ping.BlockData> blockPings ) {
    
    public ClientboundPingManagerSyncPacket( PingManager manager, ServerPlayer player ) {
        this( new HashMap<>(), new HashMap<>() );
        
        // Filter out all entities and blocks we expect the client to not be tracking
        for( Map.Entry<Integer, Ping.EntityData> ping : manager.getEntityPings() ) {
            if( TrackingHelper.isEntityInTrackingRange( player, player.level().getEntity( ping.getKey() ) ) )
                entityPings.put( ping.getKey(), ping.getValue() );
        }
        for( Map.Entry<BlockPos, Ping.BlockData> ping : manager.getBlockPings() ) {
            if( TrackingHelper.isBlockInTrackingRange( player, ping.getKey() ) )
                blockPings.put( ping.getKey(), ping.getValue() );
        }
    }
    
    public static void handle( ClientboundPingManagerSyncPacket message, Supplier<NetworkEvent.Context> contextSupplier ) {
        NetworkEvent.Context context = contextSupplier.get();
        
        if( context.getDirection().getReceptionSide().isClient() ) {
            context.enqueueWork( () -> ClientWork.handlePingSync( message ) );
        }
        context.setPacketHandled( true );
    }
    
    public static ClientboundPingManagerSyncPacket decode( FriendlyByteBuf buffer ) {
        Map<Integer, Ping.EntityData> entityPings = new HashMap<>();
        Map<BlockPos, Ping.BlockData> blockPings = new HashMap<>();
        try {
            int entityPingCount = buffer.readInt();
            for( int p = 0; p < entityPingCount; p++ ) {
                int entityId = buffer.readInt();
                Ping.EntityData pingData = new Ping.EntityData( buffer );
                entityPings.put( entityId, pingData );
            }
            int blockPingCount = buffer.readInt();
            for( int p = 0; p < blockPingCount; p++ ) {
                BlockPos blockPos = buffer.readBlockPos();
                Ping.BlockData pingData = new Ping.BlockData( buffer );
                blockPings.put( blockPos, pingData );
            }
        }
        catch( IndexOutOfBoundsException | DecoderException ex ) {
            CoOpOverhaulMod.LOG.error( ex );
            // noinspection CallToPrintStackTrace
            ex.printStackTrace();
        }
        return new ClientboundPingManagerSyncPacket( entityPings, blockPings );
    }
    
    public static void encode( ClientboundPingManagerSyncPacket message, FriendlyByteBuf buffer ) {
        buffer.writeInt( message.entityPings().size() );
        message.entityPings().forEach( ( key, value ) -> {
            buffer.writeInt( key );
            value.encode( buffer );
        } );
        
        buffer.writeInt( message.blockPings().size() );
        message.blockPings().forEach( ( key, value ) -> {
            buffer.writeBlockPos( key );
            value.encode( buffer );
        } );
    }
}