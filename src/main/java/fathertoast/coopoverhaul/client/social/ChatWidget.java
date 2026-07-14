package fathertoast.coopoverhaul.client.social;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.Util;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;

import javax.annotation.Nullable;

/**
 * TODO Doesn't seem to work at all, try again later
 * A widget added to every in game non-chat screen, which allows opening the chat screen
 * on top of that other screen, hosted within the widget.
 *
 * @see ChatScreen
 * @see fathertoast.coopoverhaul.client.event.KeyBindingEvents
 * @see fathertoast.coopoverhaul.client.event.ClientGameEventHandler
 */
public class ChatWidget implements GuiEventListener, Renderable, NarratableEntry {
    
    /**
     * The most recently instantiated chat widget. We operate under the assumption that the player
     * will only have one screen open at a time with a chat widget on it.
     */
    @Nullable
    private static ChatWidget activeChatWidget;
    
    /** Called any time the vanilla chat or command key binds are pressed. */
    public static void onChatKeyPressed( boolean cmd ) {
        if( activeChatWidget != null && activeChatWidget.isOnScreen() && !activeChatWidget.isChatOpen() ) {
            activeChatWidget.openChat( cmd ? "/" : "" );
        }
    }
    
    /** Called at the start of each client tick to update logic. */
    public static void onTick() {
        if( activeChatWidget != null && activeChatWidget.wrappedScreen != null ) {
            if( activeChatWidget.isOnScreen() ) activeChatWidget.wrappedScreen.tick();
            else activeChatWidget.closeChat(); // Yikes
        }
    }
    
    
    // ---- Instance ---- //
    
    private final Screen parentScreen;
    @Nullable
    private Screen wrappedScreen;
    private boolean focused;
    
    public ChatWidget( Screen parent ) {
        parentScreen = parent;
        activeChatWidget = this;
    }
    
    
    // ---- Chat Screen Management ---- //
    
    /** @return True if this widget is part of the currently open screen. */
    public boolean isOnScreen() { return Minecraft.getInstance().screen == parentScreen; }
    
    /** @return True if the chat is currently open. */
    public boolean isChatOpen() { return wrappedScreen != null; }
    
    public void openChat( String initialContents ) {
        Minecraft client = Minecraft.getInstance();
        Minecraft.ChatStatus chatStatus = client.getChatStatus();
        if( !chatStatus.isChatAllowed( client.isLocalServer() ) ) {
            if( client.gui.isShowingChatDisabledByPlayer() ) {
                client.gui.setChatDisabledByPlayerShown( false );
                setScreen( new ConfirmLinkScreen( confirmed -> {
                    if( confirmed ) Util.getPlatform().openUri( "https://aka.ms/JavaAccountSettings" );
                    closeChat();
                }, Component.translatable( "chat.disabled.profile.moreInfo" ),
                        "https://aka.ms/JavaAccountSettings", true ) );
            }
            else {
                Component component = chatStatus.getMessage();
                client.gui.setOverlayMessage( component, false );
                client.getNarrator().sayNow( component );
                client.gui.setChatDisabledByPlayerShown( chatStatus == Minecraft.ChatStatus.DISABLED_BY_PROFILE );
            }
        }
        else {
            setScreen( new ChatScreen( initialContents ) );
        }
    }
    
    public void closeChat() { setScreen( null ); }
    
    /** Based on {@link Minecraft#setScreen(Screen)}. */
    public void setScreen( @Nullable Screen screen ) {
        Minecraft client = Minecraft.getInstance();
        
        // Open the new screen
        if( screen != null ) {
            @SuppressWarnings( "UnstableApiUsage" )
            ScreenEvent.Opening event = new ScreenEvent.Opening( wrappedScreen, screen );
            if( MinecraftForge.EVENT_BUS.post( event ) ) return;
            screen = event.getNewScreen();
        }
        // Close and de-initialize the old screen
        if( wrappedScreen != null && screen != wrappedScreen ) {
            //noinspection UnstableApiUsage
            MinecraftForge.EVENT_BUS.post( new ScreenEvent.Closing( wrappedScreen ) );
            wrappedScreen.removed();
        }
        
        //TODO figure out if we should really call any of the below commented-out methods
        
        // Assign the new screen and initialize it
        wrappedScreen = screen;
        if( wrappedScreen != null ) {
            setFocused( true );
            wrappedScreen.added();
            
            //BufferUploader.reset();
            //client.mouseHandler.releaseMouse();
            KeyMapping.releaseAll();
            wrappedScreen.init( client, client.getWindow().getGuiScaledWidth(),
                    client.getWindow().getGuiScaledHeight() );
        }
        else {
            setFocused( false );
            
            //BufferUploader.reset();
            //client.getSoundManager().resume();
            //client.mouseHandler.grabMouse();
        }
        //client.updateTitle();
    }
    
    
    // ---- GuiEventListener ---- //
    
    @Override
    public void mouseMoved( double mouseX, double mouseY ) {
        if( wrappedScreen != null ) wrappedScreen.mouseMoved( mouseX, mouseY );
    }
    
    @Override
    public boolean mouseClicked( double mouseX, double mouseY, int key ) {
        return wrappedScreen != null && wrappedScreen.mouseClicked( mouseX, mouseY, key );
    }
    
    @Override
    public boolean mouseReleased( double mouseX, double mouseY, int key ) {
        return wrappedScreen != null && wrappedScreen.mouseReleased( mouseX, mouseY, key );
    }
    
    @Override
    public boolean mouseDragged( double mouseX, double mouseY, int key, double deltaX, double deltaY ) {
        return wrappedScreen != null && wrappedScreen.mouseDragged( mouseX, mouseY, key, deltaX, deltaY );
    }
    
    @Override
    public boolean mouseScrolled( double mouseX, double mouseY, double deltaScroll ) {
        return wrappedScreen != null && wrappedScreen.mouseScrolled( mouseX, mouseY, deltaScroll );
    }
    
    @Override
    public boolean keyPressed( int key, int scancode, int mods ) {
        if( wrappedScreen != null ) {
            // Catch esc key; close chat widget instead of closing screen
            if( key == InputConstants.KEY_ESCAPE ) {
                closeChat();
                return true;
            }
            return wrappedScreen.keyPressed( key, scancode, mods );
        }
        else {
            //            //TODO - this seems to not work at all?
            //            Minecraft client = Minecraft.getInstance();
            //            if( key == client.options.keyChat.getKey().getValue() && client.options.keyChat.isConflictContextAndModifierActive() ) {
            //                openChat( "" );
            //                return true;
            //            }
            //            if( key == client.options.keyCommand.getKey().getValue() && client.options.keyCommand.isConflictContextAndModifierActive() ) {
            //                openChat( "/" );
            //                return true;
            //            }
            return false;
        }
    }
    
    @Override
    public boolean keyReleased( int key, int scancode, int mods ) {
        return wrappedScreen != null && wrappedScreen.keyReleased( key, scancode, mods );
    }
    
    @Override
    public boolean charTyped( char codePoint, int mods ) {
        return wrappedScreen != null && wrappedScreen.charTyped( codePoint, mods );
    }
    
    @Override
    @Nullable
    public ComponentPath nextFocusPath( FocusNavigationEvent event ) {
        return wrappedScreen != null ? wrappedScreen.nextFocusPath( event ) : null;
    }
    
    @Override
    public boolean isMouseOver( double mouseX, double mouseY ) {
        return wrappedScreen != null && wrappedScreen.isMouseOver( mouseX, mouseY );
    }
    
    @Override
    public void setFocused( boolean on ) { focused = on; }
    
    @Override
    public boolean isFocused() { return focused; }
    
    @Override
    @Nullable
    public ComponentPath getCurrentFocusPath() {
        return wrappedScreen != null ? wrappedScreen.getCurrentFocusPath() : null;
    }
    
    @Override
    public ScreenRectangle getRectangle() {
        return wrappedScreen != null ? wrappedScreen.getRectangle() : ScreenRectangle.empty();
    }
    
    
    // ---- Renderable ---- //
    
    @Override
    public void render( GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick ) {
        if( wrappedScreen != null ) wrappedScreen.render( guiGraphics, mouseX, mouseY, partialTick );
    }
    
    
    // ---- NarratableEntry ---- //
    
    @Override
    public NarratableEntry.NarrationPriority narrationPriority() {
        return wrappedScreen != null ? NarrationPriority.FOCUSED : NarrationPriority.NONE;
    }
    
    @Override
    public boolean isActive() { return wrappedScreen != null; }
    
    @Override
    public void updateNarration( NarrationElementOutput narrationOutput ) {} // TODO figure out how to use this
}