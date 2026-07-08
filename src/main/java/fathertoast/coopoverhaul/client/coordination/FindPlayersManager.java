package fathertoast.coopoverhaul.client.coordination;

import fathertoast.coopoverhaul.client.config.ClientConfig;
import fathertoast.coopoverhaul.client.vfx.HighlightManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.Team;

/**
 * Controls the 'find players' function. This causes all friendly players within the configured distance
 * to be highlighted, such that they are easier to find.
 * <p>
 * Cannot help finding players beyond your render distance, for now.
 *
 * @see HighlightManager
 */
public final class FindPlayersManager {
    
    /** Number of ticks until we disable ourselves. */
    private static int enabledDuration;
    
    /** True while the player is using find players mode. */
    private static boolean enabled;
    /** The max squared distance that we can highlight friendly players. */
    private static double rangeSq;
    
    /** Turns on "find players" mode, which highlights nearby friendly players. */
    public static void enable() { setEnabled( true ); }
    
    /** Turns off "find players" mode, which highlights nearby friendly players. */
    public static void disable() { setEnabled( false ); }
    
    /** Sets "find players" mode on or off, which highlights nearby friendly players. */
    public static void setEnabled( boolean on ) {
        enabled = on;
        enabledDuration = 0;
    }
    
    /** Flags "find players" mode to disable after the configured delay. */
    public static void delayDisable() {
        enabledDuration = ClientConfig.PREFS.PLAYER_FINDER.lingerDuration.get();
        if( enabledDuration == 0 ) enabled = false;
    }
    
    /** @return Whether "find players" mode is on or off. */
    public static boolean isEnabled() { return enabled; }
    
    /** Updates the find players range based on current settings. */
    public static void updateRange() {
        rangeSq = Math.min( ClientConfig.PREFS.PLAYER_FINDER.range.get(), ClientConfig.getMaxFindPlayersRange() );
        rangeSq *= rangeSq;
    }
    
    /** @return True if the entity should be highlighted. */
    public static boolean shouldHighlight( Entity entity ) {
        LocalPlayer player = Minecraft.getInstance().player;
        return isEnabled() && !entity.isSpectator() && !entity.isRemoved() && player != null && !player.isSpectator() &&
                !entity.equals( player ) && /* Comment this out for easier testing -> */ entity instanceof Player &&
                !entity.isInvisibleTo( player ) && notOnOpposingTeams( player, entity ) &&
                player.distanceToSqr( entity ) < rangeSq;
    }
    
    /** @return False only if both the player and entity are in teams and those teams are not allied. */
    private static boolean notOnOpposingTeams( Player player, Entity entity ) {
        Team playerTeam = player.getTeam();
        Team otherTeam = entity.getTeam();
        return playerTeam == null || otherTeam == null || playerTeam.isAlliedTo( otherTeam );
    }
    
    /** Called at the end of each tick to update logic. */
    public static void onTick() {
        if( enabledDuration > 0 ) {
            enabledDuration--;
            if( enabledDuration == 0 ) disable();
        }
    }
    
    
    private FindPlayersManager() {}
}