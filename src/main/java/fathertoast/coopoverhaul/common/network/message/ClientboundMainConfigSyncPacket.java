package fathertoast.coopoverhaul.common.network.message;

import fathertoast.coopoverhaul.common.core.CoOpOverhaulMod;
import fathertoast.coopoverhaul.common.network.work.ClientWork;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ClientboundMainConfigSyncPacket( double spyglassInspectRange,
                                               double maxInspectRange,
                                               boolean allowRecoloringHidden,
                                               double maxFindPlayersRange,
                                               int pingDuration,
                                               int pingCooldown ) {
    
    public static void handle( ClientboundMainConfigSyncPacket message, Supplier<NetworkEvent.Context> contextSupplier ) {
        NetworkEvent.Context context = contextSupplier.get();
        
        if( context.getDirection().getReceptionSide().isClient() ) {
            context.enqueueWork( () -> ClientWork.handleMainConfigSync( message ) );
        }
        context.setPacketHandled( true );
    }
    
    public static ClientboundMainConfigSyncPacket decode( FriendlyByteBuf buffer ) {
        try {
            return new ClientboundMainConfigSyncPacket(
                    buffer.readDouble(),
                    buffer.readDouble(),
                    buffer.readBoolean(),
                    buffer.readDouble(),
                    buffer.readInt(),
                    buffer.readInt() );
        }
        catch( IndexOutOfBoundsException | DecoderException ex ) {
            CoOpOverhaulMod.LOG.error( ex );
            // noinspection CallToPrintStackTrace
            ex.printStackTrace();
            return new ClientboundMainConfigSyncPacket( 0.0, 0.0,
                    false, 0.0, 0, Integer.MAX_VALUE );
        }
    }
    
    public static void encode( ClientboundMainConfigSyncPacket message, FriendlyByteBuf buffer ) {
        buffer.writeDouble( message.spyglassInspectRange() );
        buffer.writeDouble( message.maxInspectRange() );
        buffer.writeBoolean( message.allowRecoloringHidden() );
        buffer.writeDouble( message.maxFindPlayersRange() );
        buffer.writeInt( message.pingDuration() );
        buffer.writeInt( message.pingCooldown() );
    }
}