package fathertoast.coopmod.common.network.message;

import fathertoast.coopmod.common.core.CoOpMod;
import fathertoast.coopmod.common.network.work.ServerWork;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ServerboundEntityPingPacket( int entityId ) {
    
    public static void handle( ServerboundEntityPingPacket message, Supplier<NetworkEvent.Context> contextSupplier ) {
        NetworkEvent.Context context = contextSupplier.get();
        
        if( context.getDirection().getReceptionSide().isServer() ) {
            context.enqueueWork( () -> ServerWork.handlePing( message, context.getSender() ) );
        }
        context.setPacketHandled( true );
    }
    
    public static ServerboundEntityPingPacket decode( FriendlyByteBuf buffer ) {
        try {
            int entityId = buffer.readInt();
            return new ServerboundEntityPingPacket( entityId );
        }
        catch( IndexOutOfBoundsException | DecoderException ex ) {
            CoOpMod.LOG.error( ex );
            // noinspection CallToPrintStackTrace
            ex.printStackTrace();
        }
        return new ServerboundEntityPingPacket( -1 );
    }
    
    public static void encode( ServerboundEntityPingPacket message, FriendlyByteBuf buffer ) {
        buffer.writeInt( message.entityId() );
    }
}