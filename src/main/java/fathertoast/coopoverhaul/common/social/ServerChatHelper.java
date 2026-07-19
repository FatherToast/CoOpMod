package fathertoast.coopoverhaul.common.social;

import fathertoast.coopoverhaul.common.network.message.ServerboundLinkedItemChatPacket;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.chat.SignedMessageChain;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.FilteredText;
import net.minecraftforge.common.ForgeHooks;

import java.util.concurrent.CompletableFuture;

/**
 * TODO
 */
public final class ServerChatHelper {
    
    /**
     * Based on {@link net.minecraft.server.network.ServerGamePacketListenerImpl#isChatMessageIllegal(String)}.
     *
     * @return True if the chat message contains any illegal characters.
     */
    public static boolean isChatMessageIllegal( String message ) {
        for( int i = 0; i < message.length(); i++ ) {
            if( !SharedConstants.isAllowedChatCharacter( message.charAt( i ) ) ) return true;
        }
        return false;
    }
    
    /**
     * Processes the incoming linked item chat message.
     * <p>
     * Based on {@link net.minecraft.server.network.ServerGamePacketListenerImpl#handleChat(ServerboundChatPacket)}.
     */
    public static void handleLinkedItemChat( ServerboundLinkedItemChatPacket message, ServerPlayer sender ) {
        ServerboundChatPacket basePacket = message.wrappedPacket();
        if( isChatMessageIllegal( basePacket.message() ) ) {
            sender.connection.disconnect( Component.translatable( "multiplayer.disconnect.illegal_characters" ) );
            return;
        }
        sender.connection.tryHandleChat( getPseudoMessage( message ), basePacket.timeStamp(),
                basePacket.lastSeenMessages() ).ifPresent( lastSeenMessages ->
                sender.server.submit( () -> {
                    PlayerChatMessage chatMessage;
                    try {
                        chatMessage = sender.connection.getSignedMessage( basePacket, lastSeenMessages )
                                .withUnsignedContent( getMessage( message ) );
                    }
                    catch( SignedMessageChain.DecodeException ex ) {
                        sender.connection.handleMessageDecodeFailure( ex );
                        return;
                    }
                    
                    CompletableFuture<FilteredText> asyncFilter = sender.connection
                            .filterTextPacket( chatMessage.signedContent() );
                    CompletableFuture<Component> asyncDecorator = ForgeHooks.getServerChatSubmittedDecorator()
                            .decorate( sender, chatMessage.decoratedContent() );
                    sender.connection.chatMessageChain.append( executor ->
                            CompletableFuture.allOf( asyncFilter, asyncDecorator ).thenAcceptAsync( nil -> {
                                Component decoratedContent = asyncDecorator.join();
                                if( decoratedContent == null ) return; // ServerChatEvent was canceled
                                sender.connection.broadcastChatMessage( chatMessage
                                        .withUnsignedContent( decoratedContent )
                                        .filter( asyncFilter.join().mask() ) );
                            }, executor ) );
                } ) );
    }
    
    /** @return A message resembling the actual translatable message, to use for server-side error reporting. */
    public static String getPseudoMessage( ServerboundLinkedItemChatPacket message ) {
        return message.wrappedPacket().message().substring( 0, message.index() ) +
                "[" + message.linkedItem().getDescriptionId() + "]" +
                message.wrappedPacket().message().substring( message.index() );
    }
    
    /** @return A translatable message including the linked item. */
    public static Component getMessage( ServerboundLinkedItemChatPacket message ) {
        return Component.literal( message.wrappedPacket().message().substring( 0, message.index() ) )
                .append( message.linkedItem().getDisplayName() )
                .append( message.wrappedPacket().message().substring( message.index() ) );
    }
    
    
    private ServerChatHelper() {}
}