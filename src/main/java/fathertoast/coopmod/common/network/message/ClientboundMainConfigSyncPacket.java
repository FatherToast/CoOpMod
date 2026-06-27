package fathertoast.coopmod.common.network.message;

import fathertoast.coopmod.common.core.CoOpMod;
import fathertoast.coopmod.common.network.work.ClientWork;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ClientboundMainConfigSyncPacket( double maxInspectRange ) {
    
    public static void handle( ClientboundMainConfigSyncPacket message, Supplier<NetworkEvent.Context> contextSupplier ) {
        NetworkEvent.Context context = contextSupplier.get();
        
        if( context.getDirection().getReceptionSide().isClient() ) {
            context.enqueueWork( () -> ClientWork.handleSyncMaxInspectRange( message ) );
        }
        context.setPacketHandled( true );
    }
    
    public static ClientboundMainConfigSyncPacket decode( FriendlyByteBuf buffer ) {
        try {
            return new ClientboundMainConfigSyncPacket(
                    buffer.readDouble() );
        }
        catch( IndexOutOfBoundsException | DecoderException ex ) {
            CoOpMod.LOG.error( ex );
            // noinspection CallToPrintStackTrace
            ex.printStackTrace();
            return new ClientboundMainConfigSyncPacket( 0.0 );
        }
    }
    
    public static void encode( ClientboundMainConfigSyncPacket message, FriendlyByteBuf buffer ) {
        buffer.writeDouble( message.maxInspectRange() );
    }
}