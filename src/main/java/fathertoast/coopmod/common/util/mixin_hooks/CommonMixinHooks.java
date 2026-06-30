package fathertoast.coopmod.common.util.mixin_hooks;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import fathertoast.coopmod.client.InspectManager;
import fathertoast.crust.api.lib.CrustMath;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.scores.Team;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.SortedSet;

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
     * Swap the buffer source to an outline one if the block entity should be highlighted.
     */
    public static <E extends BlockEntity> MultiBufferSource
    modifyRenderBufferSource( LevelRenderer levelRenderer, E blockEntity, float partialTick, PoseStack poseStack,
                              MultiBufferSource bufferSource ) {
        BlockPos blockPos = blockEntity.getBlockPos();
        if( !InspectManager.shouldHighlight( blockPos ) ) return bufferSource; // No modification needed
        
        // Set up the baseline outline buffer
        Minecraft client = Minecraft.getInstance();
        OutlineBufferSource outlineBuffer = client.renderBuffers().outlineBufferSource();
        int color = InspectManager.getHighlightColor( blockPos );
        outlineBuffer.setColor( CrustMath.getRedBits( color ), CrustMath.getGreenBits( color ),
                CrustMath.getBlueBits( color ), 0xFF ); // Alpha does not function for outlines
        
        // Global block entities have no pre-processing
        if( levelRenderer.globalBlockEntities.contains( blockEntity ) ) return outlineBuffer;
        // Standard block entities
        // We check if block break progress needs to be slapped onto the buffer source; would be lovely if we can
        // figure out a smarter way to avoid re-making the wheel here and instead wrap the provided buffer smartly
        SortedSet<BlockDestructionProgress> destroyProgressSet = levelRenderer.destructionProgress.get( blockPos.asLong() );
        if( destroyProgressSet != null && !destroyProgressSet.isEmpty() ) {
            int destroyProgress = destroyProgressSet.last().getProgress();
            if( destroyProgress >= 0 ) {
                PoseStack.Pose lastPoseStack = poseStack.last();
                VertexConsumer crumblingVertexConsumer = new SheetedDecalTextureGenerator( client.renderBuffers()
                        .crumblingBufferSource().getBuffer( ModelBakery.DESTROY_TYPES.get( destroyProgress ) ),
                        lastPoseStack.pose(), lastPoseStack.normal(), 1.0F );
                return ( renderType ) -> {
                    VertexConsumer vertexConsumer = outlineBuffer.getBuffer( renderType );
                    return renderType.affectsCrumbling() ?
                            VertexMultiConsumer.create( crumblingVertexConsumer, vertexConsumer ) : vertexConsumer;
                };
            }
        }
        // No block destroy progress needed
        return outlineBuffer;
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