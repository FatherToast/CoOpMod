package fathertoast.coopoverhaul.common.util.mixin_hooks;

import com.mojang.blaze3d.vertex.PoseStack;
import fathertoast.coopoverhaul.client.vfx.HighlightManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.scores.Team;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public class CommonMixinHooks {
    
    /**
     * If the entity should be highlighted and doesn't already have a team color, provide our own color.
     */
    public static void onGetTeamColor( Entity entity, CallbackInfoReturnable<Integer> cir ) {
        if( entity.level().isClientSide() && HighlightManager.shouldHighlight( entity ) ) {
            Team team = entity.getTeam();
            if( team == null || team.getColor().getColor() == null ) {
                cir.setReturnValue( HighlightManager.getHighlightColor( entity ) );
            }
        }
    }
    
    /**
     * Swap the buffer source to an outline one if the block entity should be highlighted.
     * This is the version called by the Embeddium compatibility mixin.
     */
    public static <E extends BlockEntity> MultiBufferSource
    modifyRenderBufferSource( E blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource ) {
        return modifyRenderBufferSource( Minecraft.getInstance().levelRenderer,
                blockEntity, partialTick, poseStack, bufferSource );
    }
    
    /**
     * Swap the buffer source to an outline one if the block entity should be highlighted.
     */
    public static <E extends BlockEntity> MultiBufferSource
    modifyRenderBufferSource( LevelRenderer levelRenderer, E blockEntity, float partialTick, PoseStack poseStack,
                              MultiBufferSource bufferSource ) {
        BlockPos blockPos = blockEntity.getBlockPos();
        return HighlightManager.shouldHighlight( blockPos ) ?
                HighlightManager.getBlockEntityBufferSource( levelRenderer, blockEntity, blockPos, poseStack ) : bufferSource;
    }
    
    /**
     * If the entity is not already glowing for some reason, check if they should be glowing (highlighted).
     */
    public static void onShouldEntityAppearGlowing( Entity entity, CallbackInfoReturnable<Boolean> cir ) {
        if( !cir.getReturnValue() && HighlightManager.shouldHighlight( entity ) ) {
            cir.setReturnValue( true );
        }
    }
}