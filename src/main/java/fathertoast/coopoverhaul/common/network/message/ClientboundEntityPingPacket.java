package fathertoast.coopoverhaul.common.network.message;

import fathertoast.coopoverhaul.common.core.CoOpOverhaulMod;
import fathertoast.coopoverhaul.common.coordination.Ping;
import fathertoast.coopoverhaul.common.network.work.ClientWork;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public record ClientboundEntityPingPacket( int entityId, @Nullable Ping.EntityData pingData ) {
    
    public static void handle( ClientboundEntityPingPacket message, Supplier<NetworkEvent.Context> contextSupplier ) {
        NetworkEvent.Context context = contextSupplier.get();
        
        if( context.getDirection().getReceptionSide().isClient() ) {
            context.enqueueWork( () -> ClientWork.handlePing( message ) );
        }
        context.setPacketHandled( true );
    }
    
    public static ClientboundEntityPingPacket decode( FriendlyByteBuf buffer ) {
        try {
            int entityId = buffer.readInt();
            Ping.EntityData pingData = new Ping.EntityData( buffer );
            return new ClientboundEntityPingPacket( entityId, pingData );
        }
        catch( IndexOutOfBoundsException | DecoderException ex ) {
            CoOpOverhaulMod.LOG.error( ex );
            // noinspection CallToPrintStackTrace
            ex.printStackTrace();
        }
        return new ClientboundEntityPingPacket( -1, null );
    }
    
    public static void encode( ClientboundEntityPingPacket message, FriendlyByteBuf buffer ) {
        buffer.writeInt( message.entityId() );
        if( message.pingData() != null ) message.pingData().encode( buffer );
    }
}