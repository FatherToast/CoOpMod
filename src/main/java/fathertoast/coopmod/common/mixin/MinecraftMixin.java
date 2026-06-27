package fathertoast.coopmod.common.mixin;

import fathertoast.coopmod.common.util.mixin_hooks.CommonMixinHooks;
import net.minecraft.client.Minecraft;
import net.minecraft.util.thread.ReentrantBlockableEventLoop;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin( Minecraft.class )
public abstract class MinecraftMixin extends ReentrantBlockableEventLoop<Runnable> {
    
    public MinecraftMixin( String name ) { super( name ); }
    
    /**
     * Yes, that's right, they really don't want you to be able to make entities glow from the client side.
     */
    @Inject(
            method = "shouldEntityAppearGlowing",
            at = @At( "RETURN" ),
            cancellable = true
    )
    private void onShouldEntityAppearGlowing( Entity entity, CallbackInfoReturnable<Boolean> cir ) {
        CommonMixinHooks.onShouldEntityAppearGlowing( entity, cir );
    }
}