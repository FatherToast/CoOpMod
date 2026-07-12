package fathertoast.coopoverhaul.common.network.message;

import fathertoast.coopoverhaul.common.core.CoOpOverhaulMod;
import fathertoast.coopoverhaul.common.network.work.ServerWork;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ServerboundDataRequestPacket( Type type, boolean enable ) {
    
    public enum Type {
        NULL,
        FIND_PLAYERS;
        
        public byte serialize() { return (byte) ordinal(); }
        
        public static Type deserialize( byte b ) { return b >= 0 && b < values().length ? values()[b] : NULL; }
    }
    
    public static void handle( ServerboundDataRequestPacket message, Supplier<NetworkEvent.Context> contextSupplier ) {
        NetworkEvent.Context context = contextSupplier.get();
        
        if( context.getDirection().getReceptionSide().isServer() ) {
            context.enqueueWork( () -> ServerWork.handleDataRequest( message, context.getSender() ) );
        }
        context.setPacketHandled( true );
    }
    
    public static ServerboundDataRequestPacket decode( FriendlyByteBuf buffer ) {
        try {
            return new ServerboundDataRequestPacket(
                    Type.deserialize( buffer.readByte() ),
                    buffer.readBoolean() );
        }
        catch( IndexOutOfBoundsException | DecoderException ex ) {
            CoOpOverhaulMod.LOG.error( ex );
            // noinspection CallToPrintStackTrace
            ex.printStackTrace();
        }
        return new ServerboundDataRequestPacket( Type.NULL, false );
    }
    
    public static void encode( ServerboundDataRequestPacket message, FriendlyByteBuf buffer ) {
        buffer.writeByte( message.type().serialize() );
        buffer.writeBoolean( message.enable() );
    }
}