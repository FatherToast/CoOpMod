package fathertoast.coopoverhaul.common.network.message;

import fathertoast.coopoverhaul.common.core.CoOpOverhaulMod;
import fathertoast.coopoverhaul.common.network.work.ServerWork;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.LastSeenMessages;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.time.Instant;
import java.util.BitSet;
import java.util.function.Supplier;

public record ServerboundLinkedItemChatPacket( ServerboundChatPacket wrappedPacket, int index, ItemStack linkedItem ) {
    
    public static void handle( ServerboundLinkedItemChatPacket message, Supplier<NetworkEvent.Context> contextSupplier ) {
        NetworkEvent.Context context = contextSupplier.get();
        
        if( context.getDirection().getReceptionSide().isServer() ) {
            context.enqueueWork( () -> ServerWork.handleLinkedItemChat( message, context.getSender() ) );
        }
        context.setPacketHandled( true );
    }
    
    public static ServerboundLinkedItemChatPacket decode( FriendlyByteBuf buffer ) {
        try {
            return new ServerboundLinkedItemChatPacket(
                    new ServerboundChatPacket( buffer ),
                    buffer.readInt(),
                    buffer.readItem() );
        }
        catch( IndexOutOfBoundsException | DecoderException ex ) {
            CoOpOverhaulMod.LOG.error( ex );
            // noinspection CallToPrintStackTrace
            ex.printStackTrace();
        }
        return new ServerboundLinkedItemChatPacket(
                new ServerboundChatPacket( "", Instant.EPOCH, 0L, null,
                        new LastSeenMessages.Update( 0, new BitSet() ) ),
                -1, ItemStack.EMPTY );
    }
    
    public static void encode( ServerboundLinkedItemChatPacket message, FriendlyByteBuf buffer ) {
        message.wrappedPacket().write( buffer );
        buffer.writeInt( message.index() );
        buffer.writeItemStack( message.linkedItem(), false );
    }
}