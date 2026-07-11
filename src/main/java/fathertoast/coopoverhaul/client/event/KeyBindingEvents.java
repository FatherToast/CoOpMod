package fathertoast.coopoverhaul.client.event;

import com.mojang.blaze3d.platform.InputConstants;
import fathertoast.coopoverhaul.client.config.ClientConfig;
import fathertoast.coopoverhaul.client.coordination.ClientPingHelper;
import fathertoast.coopoverhaul.client.coordination.FindPlayersManager;
import fathertoast.coopoverhaul.client.coordination.InspectManager;
import fathertoast.coopoverhaul.common.core.CoOpOverhaulMod;
import fathertoast.crust.api.client.SortedKeyMapping;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber( value = Dist.CLIENT, modid = CoOpOverhaulMod.MOD_ID )
public final class KeyBindingEvents {
    
    public enum Mode { HOLD, TOGGLE, ALWAYS_ON }
    
    
    private static final String KEY_CAT = "key.categories." + CoOpOverhaulMod.MOD_ID;
    
    private static final String KEY = "key." + CoOpOverhaulMod.MOD_ID + ".";
    
    private static final KeyMapping INSPECT = new SortedKeyMapping( 0, KEY + "inspect", KEY_CAT,
            InputConstants.KEY_V ).inGameOnly();
    private static final KeyMapping PING = new SortedKeyMapping( 1, KEY + "ping", KEY_CAT,
            InputConstants.Type.MOUSE, InputConstants.MOUSE_BUTTON_LEFT ).inGameOnly();
    private static final KeyMapping QUICK_PING = new SortedKeyMapping( 2, KEY + "quick_ping", KEY_CAT,
            InputConstants.KEY_G ).inGameOnly();
    private static final KeyMapping FIND_PLAYERS = new SortedKeyMapping( 3, KEY + "find_players", KEY_CAT,
            InputConstants.KEY_TAB ).inGameOnly();
    
    
    /** Updates the key bind state based on current settings. */
    public static void updateKeyMode() {
        switch( ClientConfig.PREFS.INSPECTION.keyMode.get() ) {
            case HOLD -> InspectManager.disable();
            case ALWAYS_ON -> InspectManager.enable();
        }
        switch( ClientConfig.PREFS.PLAYER_FINDER.keyMode.get() ) {
            case HOLD -> FindPlayersManager.disable();
            case ALWAYS_ON -> FindPlayersManager.enable();
        }
    }
    
    /** Registers this mod's additional key bindings. */
    static void register( RegisterKeyMappingsEvent event ) {
        event.register( INSPECT );
        event.register( PING );
        event.register( QUICK_PING );
        event.register( FIND_PLAYERS );
    }
    
    /** Called when a mouse button action occurs. */
    @SubscribeEvent
    static void onMouseInput( InputEvent.MouseButton.Pre event ) {
        if( onInput( event.getButton(), event.getAction() ) ) event.setCanceled( true );
    }
    
    /** Called when a keyboard key action occurs. */
    @SubscribeEvent
    static void onKeyInput( InputEvent.Key event ) {
        onInput( event.getKey(), event.getAction() );
    }
    
    /**
     * Called when a mouse button or keyboard key action occurs.
     *
     * @return True if the input event should be canceled (only applicable to mouse button inputs).
     */
    @SuppressWarnings( "RedundantIfStatement" )
    private static boolean onInput( int key, int action ) {
        Minecraft client = Minecraft.getInstance();
        Screen screen = client.screen;
        if( key != InputConstants.UNKNOWN.getValue() && screen == null /*&& screen.isPauseScreen()*/ ) {
            
            if( action == GLFW.GLFW_PRESS ) {
                // Key pressed
                if( isActive( key, INSPECT ) ) {
                    switch( ClientConfig.PREFS.INSPECTION.keyMode.get() ) {
                        case HOLD -> InspectManager.enable();
                        case TOGGLE -> InspectManager.setEnabled( !InspectManager.isEnabled() );
                    }
                }
                else if( isActive( key, PING ) ) {
                    if( InspectManager.isEnabled() ) {
                        if( InspectManager.target() != null ) ClientPingHelper.ping();
                        if( ClientConfig.PREFS.INSPECTION.keyMode.get() == Mode.HOLD ) return true;
                    }
                }
                else if( isActive( key, QUICK_PING ) ) {
                    ClientPingHelper.quickPing();
                }
                else if( isActive( key, FIND_PLAYERS ) ) {
                    switch( ClientConfig.PREFS.PLAYER_FINDER.keyMode.get() ) {
                        case HOLD -> FindPlayersManager.enable();
                        case TOGGLE -> FindPlayersManager.setEnabled( !FindPlayersManager.isEnabled() );
                    }
                }
            }
            else if( action == GLFW.GLFW_RELEASE ) {
                // Key released
                if( isActive( key, INSPECT ) ) {
                    if( ClientConfig.PREFS.INSPECTION.keyMode.get() == Mode.HOLD ) InspectManager.disable();
                }
                if( isActive( key, FIND_PLAYERS ) ) {
                    if( ClientConfig.PREFS.PLAYER_FINDER.keyMode.get() == Mode.HOLD ) FindPlayersManager.delayDisable();
                }
            }
        }
        return false;
    }
    
    /** @return True if the key code should be considered an action on a specific key bind. */
    private static boolean isActive( int key, KeyMapping keyBind ) {
        return key == keyBind.getKey().getValue() && keyBind.isConflictContextAndModifierActive();
    }
    
    
    private KeyBindingEvents() {}
}