package fathertoast.coopoverhaul.common.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import fathertoast.coopoverhaul.common.util.mixin_hooks.CommonMixinHooks;
import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin( SodiumWorldRenderer.class )
public abstract class SodiumWorldRendererMixin {
    /**
     * Modify the level renderer's call to the block entity render dispatcher to allow
     * highlighting blocks (or block parts) rendered by a block entity.
     */
    @ModifyArg(
            method = "renderBlockEntity",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderDispatcher;" +
                            "render(Lnet/minecraft/world/level/block/entity/BlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;" +
                            "Lnet/minecraft/client/renderer/MultiBufferSource;)V"
            ),
            index = 3
    )
    private static <E extends BlockEntity> MultiBufferSource
    modifyRenderBufferSource( E blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource ) {
        return CommonMixinHooks.modifyRenderBufferSource( blockEntity, partialTick, poseStack, bufferSource );
    }
}