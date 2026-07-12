package fathertoast.coopoverhaul.common.network.message;

import fathertoast.coopoverhaul.common.core.CoOpOverhaulMod;
import fathertoast.coopoverhaul.common.network.work.ClientWork;
import io.netty.handler.codec.DecoderException;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public record ClientboundFindPlayersDataPacket( Map<UUID, BlockPos> playerPositions ) {
    
    public static void handle( ClientboundFindPlayersDataPacket message, Supplier<NetworkEvent.Context> contextSupplier ) {
        NetworkEvent.Context context = contextSupplier.get();
        
        if( context.getDirection().getReceptionSide().isClient() ) {
            context.enqueueWork( () -> ClientWork.handleFindPlayersData( message ) );
        }
        context.setPacketHandled( true );
    }
    
    public static ClientboundFindPlayersDataPacket decode( FriendlyByteBuf buffer ) {
        Map<UUID, BlockPos> playerPositions = new HashMap<>();
        try {
            int playerCount = buffer.readInt();
            for( int p = 0; p < playerCount; p++ ) {
                playerPositions.put(
                        buffer.readUUID(),
                        buffer.readBlockPos() );
            }
        }
        catch( IndexOutOfBoundsException | DecoderException ex ) {
            CoOpOverhaulMod.LOG.error( ex );
            // noinspection CallToPrintStackTrace
            ex.printStackTrace();
        }
        return new ClientboundFindPlayersDataPacket( playerPositions );
    }
    
    public static void encode( ClientboundFindPlayersDataPacket message, FriendlyByteBuf buffer ) {
        buffer.writeInt( message.playerPositions().size() );
        message.playerPositions().forEach( ( key, value ) -> {
            buffer.writeUUID( key );
            buffer.writeBlockPos( value );
        } );
    }
}