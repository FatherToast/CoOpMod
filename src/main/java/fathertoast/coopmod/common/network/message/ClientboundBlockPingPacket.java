package fathertoast.coopmod.common.network.message;

import fathertoast.coopmod.common.core.CoOpMod;
import fathertoast.coopmod.common.coordination.Ping;
import fathertoast.coopmod.common.network.work.ClientWork;
import io.netty.handler.codec.DecoderException;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public record ClientboundBlockPingPacket( BlockPos blockPos, @Nullable Ping.BlockData pingData ) {
    
    public static void handle( ClientboundBlockPingPacket message, Supplier<NetworkEvent.Context> contextSupplier ) {
        NetworkEvent.Context context = contextSupplier.get();
        
        if( context.getDirection().getReceptionSide().isClient() ) {
            context.enqueueWork( () -> ClientWork.handlePing( message ) );
        }
        context.setPacketHandled( true );
    }
    
    public static ClientboundBlockPingPacket decode( FriendlyByteBuf buffer ) {
        try {
            BlockPos blockPos = buffer.readBlockPos();
            Ping.BlockData pingData = new Ping.BlockData( buffer );
            return new ClientboundBlockPingPacket( blockPos, pingData );
        }
        catch( IndexOutOfBoundsException | DecoderException ex ) {
            CoOpMod.LOG.error( ex );
            // noinspection CallToPrintStackTrace
            ex.printStackTrace();
        }
        return new ClientboundBlockPingPacket( BlockPos.ZERO, null );
    }
    
    public static void encode( ClientboundBlockPingPacket message, FriendlyByteBuf buffer ) {
        buffer.writeBlockPos( message.blockPos() );
        if( message.pingData() != null ) message.pingData().encode( buffer );
    }
}