package fathertoast.coopmod.common.protection;

import fathertoast.coopmod.common.config.Config;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.Team;

/**
 * Responsible for logic relating to friendly fire protection.
 */
public final class FriendlyFireHelper {
    
    public enum Mode { OFF, DEFAULT, STRICT }
    
    public static boolean shouldCancelDamage( Player player, DamageSource source ) {
        return Config.MAIN.GENERAL.friendlyFireMulti.get() == 0.0 && isFriendlyFire( player, source );
    }
    
    public static boolean isFriendlyFire( Player player, DamageSource source ) {
        return source.getEntity() instanceof Player attacker && isFriendlyFire( player, attacker );
    }
    
    public static boolean isFriendlyFire( Player player, Player attacker ) {
        Team team = player.getTeam();
        Team attackerTeam = attacker.getTeam();
        return switch( Config.MAIN.GENERAL.friendlyFireMode.get() ) {
            case DEFAULT -> team == null || attackerTeam == null || team.isAlliedTo( attackerTeam );
            case STRICT -> team != null && attackerTeam != null && team.isAlliedTo( attackerTeam );
            default -> false;
        };
    }
    
    
    private FriendlyFireHelper() {}
}