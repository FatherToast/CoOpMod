package fathertoast.coopoverhaul.common.network.message;

import fathertoast.coopoverhaul.common.core.CoOpOverhaulMod;
import fathertoast.coopoverhaul.common.network.work.ServerWork;
import io.netty.handler.codec.DecoderException;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ServerboundBlockPingPacket( BlockPos blockPos ) {
    
    public static void handle( ServerboundBlockPingPacket message, Supplier<NetworkEvent.Context> contextSupplier ) {
        NetworkEvent.Context context = contextSupplier.get();
        
        if( context.getDirection().getReceptionSide().isServer() ) {
            context.enqueueWork( () -> ServerWork.handlePing( message, context.getSender() ) );
        }
        context.setPacketHandled( true );
    }
    
    public static ServerboundBlockPingPacket decode( FriendlyByteBuf buffer ) {
        try {
            BlockPos blockPos = buffer.readBlockPos();
            return new ServerboundBlockPingPacket( blockPos );
        }
        catch( IndexOutOfBoundsException | DecoderException ex ) {
            CoOpOverhaulMod.LOG.error( ex );
            // noinspection CallToPrintStackTrace
            ex.printStackTrace();
        }
        return new ServerboundBlockPingPacket( BlockPos.ZERO );
    }
    
    public static void encode( ServerboundBlockPingPacket message, FriendlyByteBuf buffer ) {
        buffer.writeBlockPos( message.blockPos() );
    }
}