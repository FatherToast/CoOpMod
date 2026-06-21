package fathertoast.coopmod.common.util.mixin_hooks;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.fluids.FluidType;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public class CommonMixinHooks {
    
    public static void pointedDripstoneProjectileHit( Level level, BlockState state, BlockHitResult hitResult,
                                                      Projectile projectile, CallbackInfo ci ) {
    }
    
    public static void changeMonsterRoomSpawner( CallbackInfoReturnable<Boolean> cir, BlockPos blockpos,
                                                 RandomSource randomsource, WorldGenLevel worldgenlevel ) {
    }
    
    public static double modifyExplosionKnockback( Entity source, double original ) {
        return original;
    }
    
    public static void onIsInLava( boolean firstTick, Object2DoubleMap<FluidType> forgeFluidTypeHeight,
                                   CallbackInfoReturnable<Boolean> cir ) {
    }
}