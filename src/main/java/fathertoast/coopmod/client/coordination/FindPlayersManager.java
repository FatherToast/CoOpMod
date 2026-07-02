package fathertoast.coopmod.client.coordination;

import fathertoast.coopmod.client.config.ClientConfig;

public final class FindPlayersManager {
    
    /** The max distance that we can highlight friendly players. */
    private static double range;
    
    /** Updates the find players range based on current settings. */
    public static void updateRange() {
        range = Math.min( ClientConfig.PREFS.PLAYER_FINDER.range.get(), ClientConfig.getMaxFindPlayersRange() );
    }
    
    
    private FindPlayersManager() {}
}