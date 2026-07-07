package fathertoast.coopmod.client.vfx;

import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

/**
 * Helper enum for rendering health. Based on the inaccessible (to us)
 * vanilla enum, {@link net.minecraft.client.gui.Gui.HeartType}.
 */
public enum HeartType {
    CONTAINER( 0, false ),
    NORMAL( 2, true ),
    POISONED( 4, true ),
    WITHERED( 6, true ),
    ABSORPTION( 8, false ),
    FROZEN( 9, false );
    
    private final int pos;
    private final boolean hasBlinkTexture;
    
    HeartType( int index, boolean canBlink ) {
        pos = 16 + index * 18;
        hasBlinkTexture = canBlink;
    }
    
    public int getU( boolean half, boolean blinking ) {
        return pos + 9 * (this == CONTAINER ? (blinking ? 1 : 0) :
                (half ? 1 : 0) + (hasBlinkTexture && blinking ? 2 : 0));
    }
    
    public int getV( boolean hardcore ) { return hardcore ? 45 : 0; }
    
    
    /**
     * Same order of precedence as vanilla heart rendering in
     * {@link net.minecraft.client.gui.Gui.HeartType#forPlayer(Player)}.
     */
    public static HeartType forPlayer( Player player ) {
        if( player.hasEffect( MobEffects.POISON ) ) return POISONED;
        else if( player.hasEffect( MobEffects.WITHER ) ) return WITHERED;
        else if( player.isFullyFrozen() ) return FROZEN;
        return NORMAL;
    }
}