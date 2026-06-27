package fathertoast.coopmod.common.util.mixin_hooks;

import fathertoast.coopmod.client.InspectManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.scores.Team;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public class CommonMixinHooks {
    
    /**
     * If the entity should be highlighted and doesn't already have a team color, provide our own color.
     */
    public static void onGetTeamColor( Entity entity, CallbackInfoReturnable<Integer> cir ) {
        if( entity.level().isClientSide() && InspectManager.shouldHighlight( entity ) ) {
            Team team = entity.getTeam();
            if( team == null || team.getColor().getColor() == null ) {
                cir.setReturnValue( InspectManager.getHighlightColor( entity ) );
            }
        }
    }
    
    /**
     * If the entity is not already glowing for some reason, check if they should be glowing from inspection.
     */
    public static void onShouldEntityAppearGlowing( Entity entity, CallbackInfoReturnable<Boolean> cir ) {
        if( !cir.getReturnValue() && InspectManager.shouldHighlight( entity ) ) {
            cir.setReturnValue( true );
        }
    }
}