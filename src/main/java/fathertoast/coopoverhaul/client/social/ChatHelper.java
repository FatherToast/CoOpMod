package fathertoast.coopoverhaul.client.social;

/**
 * Notes so far:
 * {@link net.minecraft.client.Minecraft#openChatScreen(String)} typically is what opens the chat.
 * The chat is {@link net.minecraft.client.gui.screens.ChatScreen}.
 * GUI component for the chat window is {@link net.minecraft.client.gui.components.ChatComponent}.
 * The actual text is logically broken up into {@link net.minecraft.network.chat.Component}s filled with
 * {@link net.minecraft.network.chat.ComponentContents}.
 */
public final class ChatHelper {
    
    
    private ChatHelper() {}
}