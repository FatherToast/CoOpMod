package fathertoast.coopoverhaul.common.protection;

import fathertoast.coopoverhaul.common.config.Config;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.Team;
import org.jetbrains.annotations.Nullable;

/**
 * Responsible for logic relating to friendly fire protection.
 */
public final class FriendlyFireHelper {
    
    public enum Mode {
        /** Friendly fire is disabled. */
        OFF,
        /** Friendly fire applies to all players and owned entities unless in an opposing team. */
        DEFAULT,
        /** Friendly fire only applies to yourself, your owned entities, and allied teams. */
        STRICT
    }
    
    /** @return True if the damage source is not capable of dealing damage to a particular entity due to FF settings. */
    public static boolean shouldCancelDamage( Entity entity, DamageSource source ) {
        return Config.MAIN.GENERAL.friendlyFireMulti.get() == 0.0 && isFriendlyFire( entity, source );
    }
    
    /** @return True if the damage source should be considered friendly fire against a particular entity. */
    public static boolean isFriendlyFire( Entity entity, DamageSource source ) {
        return isFriendlyFire( getResponsiblePlayer( entity ), getResponsiblePlayer( source ) );
    }
    
    /** @return True if the two players are considered friendly. */
    public static boolean isFriendlyFire( @Nullable Player player, @Nullable Player attacker ) {
        if( player == null || attacker == null ) return false;
        Team team = player.getTeam();
        Team attackerTeam = attacker.getTeam();
        return switch( Config.MAIN.GENERAL.friendlyFireMode.get() ) {
            case DEFAULT -> player == attacker ||
                    team == null || attackerTeam == null || team.isAlliedTo( attackerTeam );
            case STRICT -> player == attacker ||
                    team != null && attackerTeam != null && team.isAlliedTo( attackerTeam );
            default -> false;
        };
    }
    
    /** @return The player associated with the damage source, if any. */
    @Nullable
    public static Player getResponsiblePlayer( @Nullable DamageSource source ) {
        return source == null ? null : getResponsiblePlayer( source.getEntity() );
    }
    
    /** @return The player associated with the entity, if any. */
    @Nullable
    public static Player getResponsiblePlayer( @Nullable Entity entity ) {
        return entity instanceof Player player ? player : entity instanceof OwnableEntity ownable ?
                ownable.getOwner() instanceof Player player ? player : null : null;
    }
    
    
    private FriendlyFireHelper() {}
}