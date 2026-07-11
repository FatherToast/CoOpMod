package fathertoast.coopoverhaul.common.mixin;

import fathertoast.coopoverhaul.common.util.mixin_hooks.CommonMixinHooks;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraftforge.common.capabilities.CapabilityProvider;
import net.minecraftforge.common.extensions.IForgeEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SuppressWarnings( "UnstableApiUsage" )
@Mixin( Entity.class )
public abstract class EntityMixin extends CapabilityProvider<Entity> implements EntityAccess, IForgeEntity {
    
    protected EntityMixin( Class<Entity> baseClass ) { super( baseClass ); }
    
    /**
     * We need to be able to set glow color somehow.
     */
    @Inject(
            method = "getTeamColor",
            at = @At( "HEAD" ),
            cancellable = true
    )
    private void onGetTeamColor( CallbackInfoReturnable<Integer> cir ) {
        CommonMixinHooks.onGetTeamColor( (Entity) ((Object) this), cir );
    }
}