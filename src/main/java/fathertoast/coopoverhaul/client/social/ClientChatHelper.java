package fathertoast.coopoverhaul.client.social;

import fathertoast.coopoverhaul.common.network.PacketHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.LastSeenMessagesTracker;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.SignedMessageBody;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.util.Crypt;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.time.Instant;

/**
 * Notes so far:
 * {@link net.minecraft.client.Minecraft#openChatScreen(String)} typically is what opens the chat.
 * The chat is {@link net.minecraft.client.gui.screens.ChatScreen}.
 * GUI component for the chat window is {@link net.minecraft.client.gui.components.ChatComponent}.
 * The actual text is logically broken up into {@link net.minecraft.network.chat.Component}s filled with
 * {@link net.minecraft.network.chat.ComponentContents}.
 * Clickable and hoverable components can be made by applying a special {@link net.minecraft.network.chat.Style}.
 */
public final class ClientChatHelper {
    
    @Nullable
    private static String insertedText;
    private static ItemStack linkedItem = ItemStack.EMPTY;
    
    /**
     * Attempts to link the item currently hovered to chat.
     * Will only work when an item is not on the pointer.
     *
     * @return True if further processing of mouse input should be canceled.
     */
    public static boolean linkItem() {
        ItemStack item = getHoveredItem();
        if( !item.isEmpty() ) {
            Minecraft client = Minecraft.getInstance();
            if( client.getChatStatus().isChatAllowed( client.isLocalServer() ) ) {
                String link = item.getDisplayName().getString();
                client.setScreen( new ChatScreen( link ) );
                if( client.screen instanceof ChatScreen ) {
                    insertedText = link;
                    linkedItem = item.copy();
                    return true;
                }
            }
        }
        return false;
    }
    
    /** @return Scans the message for a linked item and returns true if there is one. */
    public static boolean scanForLink( String message ) {
        LocalPlayer player = Minecraft.getInstance().player;
        // If an item was linked, verify that it still exists in the message being sent
        if( player != null && insertedText != null ) {
            int index = message.indexOf( insertedText );
            if( index >= 0 ) {
                // Send a fatter chat packet
                String strippedMsg = message.substring( 0, index ) + message.substring( index + insertedText.length() );
                Instant timeStamp = Instant.now();
                long salt = Crypt.SaltSupplier.getLong();
                LastSeenMessagesTracker.Update lastSeenMessages = player.connection.lastSeenMessages.generateAndApplyUpdate();
                MessageSignature signature = player.connection.signedMessageEncoder.pack( new SignedMessageBody(
                        strippedMsg, timeStamp, salt, lastSeenMessages.lastSeen() ) );
                PacketHandler.sendLinkedItemChat( new ServerboundChatPacket( strippedMsg, timeStamp,
                        salt, signature, lastSeenMessages.update() ), index, linkedItem );
                //onChatClosed(); // Really this should happen immediately anyway, not sure if worthwhile to call here
                return true;
            }
        }
        return false;
    }
    
    /** Called when a chat screen is closed. */
    public static void onChatClosed() {
        // Forget any item we tried to link.
        insertedText = null;
        linkedItem = ItemStack.EMPTY;
    }
    
    /** @return The item currently being hovered over, if no item is being held on the pointer. */
    public static ItemStack getHoveredItem() {
        Minecraft client = Minecraft.getInstance();
        if( client.player != null ) {
            ItemStack carried = client.player.containerMenu.getCarried();
            if( !carried.isEmpty() ) {
                return ItemStack.EMPTY; // We don't want to open the chat if an item is on pointer
            }
            else if( client.screen instanceof AbstractContainerScreen<?> containerScreen ) {
                Slot hoveredSlot = containerScreen.getSlotUnderMouse();
                if( hoveredSlot != null && hoveredSlot.hasItem() )
                    return hoveredSlot.getItem();
            }
        }
        return ItemStack.EMPTY;
    }
    
    /** @return The item currently on the mouse pointer or being hovered over, if any. */
    public static ItemStack getItemOnPointer() {
        Minecraft client = Minecraft.getInstance();
        if( client.player != null ) {
            ItemStack carried = client.player.containerMenu.getCarried();
            if( !carried.isEmpty() ) {
                return carried;
            }
            else if( client.screen instanceof AbstractContainerScreen<?> containerScreen ) {
                Slot hoveredSlot = containerScreen.getSlotUnderMouse();
                if( hoveredSlot != null && hoveredSlot.hasItem() )
                    return hoveredSlot.getItem();
            }
        }
        return ItemStack.EMPTY;
    }
    
    
    private ClientChatHelper() {}
}