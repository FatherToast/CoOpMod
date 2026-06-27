package fathertoast.coopmod.client;

import com.mojang.blaze3d.platform.InputConstants;
import fathertoast.coopmod.common.core.CoOpMod;
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

@Mod.EventBusSubscriber( value = Dist.CLIENT, modid = CoOpMod.MOD_ID )
public final class KeyBindingEvents {
    
    public enum Mode { HOLD, TOGGLE, ALWAYS_ON }
    
    
    private static final String KEY_CAT = "key.categories." + CoOpMod.MOD_ID;
    
    private static final String KEY = "key." + CoOpMod.MOD_ID + ".";
    
    private static final KeyMapping INSPECT = new SortedKeyMapping( 0, KEY + "inspect", KEY_CAT,
            InputConstants.KEY_V ).inGameOnly();
    private static final KeyMapping PING = new SortedKeyMapping( 1, KEY + "ping", KEY_CAT,
            InputConstants.MOUSE_BUTTON_LEFT ).inGameOnly();
    private static final KeyMapping QUICK_PING = new SortedKeyMapping( 2, KEY + "quick_ping", KEY_CAT,
            InputConstants.KEY_G ).inGameOnly();
    
    
    /** Updates the key bind state based on current settings. */
    public static void updateKeyMode() {
        switch( ClientConfig.PREFS.INSPECTION.keyMode.get() ) {
            case HOLD -> InspectManager.disableInspect();
            case ALWAYS_ON -> InspectManager.enableInspect();
        }
    }
    
    /** Registers this mod's additional key bindings. */
    static void register( RegisterKeyMappingsEvent event ) {
        event.register( INSPECT );
        event.register( PING );
        event.register( QUICK_PING );
    }
    
    /** Called when a key is pressed. */
    @SubscribeEvent
    static void onKeyInput( InputEvent.Key event ) {
        Minecraft minecraft = Minecraft.getInstance();
        Screen screen = minecraft.screen;
        if( event.getKey() == InputConstants.UNKNOWN.getValue() || screen != null && screen.isPauseScreen() ) return;
        
        if( event.getAction() == GLFW.GLFW_PRESS ) {
            if( event.getKey() == INSPECT.getKey().getValue() && INSPECT.isConflictContextAndModifierActive() ) {
                switch( ClientConfig.PREFS.INSPECTION.keyMode.get() ) {
                    case HOLD -> InspectManager.enableInspect();
                    case TOGGLE -> InspectManager.setInspectOn( !InspectManager.getInspectOn() );
                }
            }
            else if( event.getKey() == PING.getKey().getValue() && PING.isConflictContextAndModifierActive() ) {
                if( InspectManager.getInspectOn() && InspectManager.target() != null ) InspectManager.ping();
            }
            else if( event.getKey() == QUICK_PING.getKey().getValue() && QUICK_PING.isConflictContextAndModifierActive() ) {
                InspectManager.quickPing();
            }
        }
        else if( event.getAction() == GLFW.GLFW_RELEASE ) {
            if( event.getKey() == INSPECT.getKey().getValue() && INSPECT.isConflictContextAndModifierActive() ) {
                if( ClientConfig.PREFS.INSPECTION.keyMode.get() == Mode.HOLD ) InspectManager.disableInspect();
            }
        }
    }
    
    
    private KeyBindingEvents() {}
}