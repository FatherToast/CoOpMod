package fathertoast.coopmod.client.vfx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import fathertoast.coopmod.client.config.ClientConfig;
import fathertoast.coopmod.client.coordination.FindPlayersManager;
import fathertoast.coopmod.common.coordination.Ping;
import fathertoast.coopmod.common.coordination.PingManager;
import fathertoast.crust.api.lib.CrustMath;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.InfestedBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.RenderTypeHelper;
import net.minecraftforge.client.model.data.ModelData;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;

/**
 * Keeps track of what should be highlighted and renders highlights as needed.
 */
public final class HighlightManager {
    
    /** The entities we should highlight for the inspect feature. */
    private static final Map<Integer, Ping.EntityData> inspectEntities = new HashMap<>();
    /** The block positions we should highlight for the inspect feature. */
    private static final Map<BlockPos, Ping.BlockData> inspectBlocks = new HashMap<>();
    
    /** @return True if any blocks should be rendered with a glow effect. */
    public static boolean areAnyBlocksHighlighted() {
        return !getInspectBlocks().isEmpty() || PingManager.areAnyPingsActive( Minecraft.getInstance().level );
    }
    
    /** The entities we should highlight for the inspect feature. */
    public static Map<Integer, Ping.EntityData> getInspectEntities() { return inspectEntities; }
    
    /** The block positions we should highlight for the inspect feature. */
    public static Map<BlockPos, Ping.BlockData> getInspectBlocks() { return inspectBlocks; }
    
    /** @return True if the entity should be rendered with a glow effect. */
    public static boolean shouldHighlight( Entity entity ) {
        return getInspectEntities().containsKey( entity.getId() ) || PingManager.isPinged( entity ) ||
                FindPlayersManager.shouldHighlight( entity );
    }
    
    /** @return True if the block position should be rendered with a glow effect. */
    public static boolean shouldHighlight( BlockPos pos ) {
        ClientLevel level = Minecraft.getInstance().level;
        return level != null && (getInspectBlocks().containsKey( pos ) || PingManager.isPinged( level, pos ));
    }
    
    /** @return The RGB highlight color the entity should have. */
    public static int getHighlightColor( Entity entity ) {
        if( ClientConfig.PREFS.HIGHLIGHT_COLORS.playerColors.get() ) {
            Ping.EntityData ping = PingManager.getPingData( entity );
            if( ping != null && ping.color >= 0 ) return ping.color;
        }
        return ClientConfig.PREFS.HIGHLIGHT_COLORS.getColor( entity );
    }
    
    /** @return The RGB highlight color the block position should have. */
    public static int getHighlightColor( BlockPos pos ) {
        ClientLevel level = Minecraft.getInstance().level;
        if( level == null ) return ClientConfig.PREFS.HIGHLIGHT_COLORS.defaultColor.get();
        
        BlockState block = level.getBlockState( pos );
        if( ClientConfig.PREFS.HIGHLIGHT_COLORS.playerColors.get() ) {
            int color = getInspectBlocks().containsKey( pos ) ? getInspectBlocks().get( pos ).color :
                    PingManager.get( level ).getColor( pos );
            if( color > 0 ) return color;
        }
        return ClientConfig.PREFS.HIGHLIGHT_COLORS.getColor( block );
    }
    
    /** Called every render frame. Renders all block highlights. */
    public static void renderBlockOutlines( Minecraft client, ClientLevel level, LevelRenderer levelRenderer, PoseStack poseStack,
                                            Matrix4f projectionMatrix, int renderTick, float partialTick, Camera camera, Frustum frustum ) {
        // Render block highlights
        if( !areAnyBlocksHighlighted() ) return;
        OutlineBlockEntity.ensurePresent( levelRenderer );
        Vec3 cameraPos = camera.getPosition();
        OutlineBufferSource bufferSource = client.renderBuffers().outlineBufferSource();
        for( Map.Entry<BlockPos, Ping.BlockData> ping : PingManager.get( level ).getBlockPings() ) {
            renderBlockHighlight( client, level, bufferSource, poseStack, cameraPos, ping.getKey(),
                    ClientConfig.PREFS.HIGHLIGHT_COLORS.playerColors.get() ? ping.getValue().color : -1 );
            getInspectBlocks().remove( ping.getKey() );
        }
        if( !getInspectBlocks().isEmpty() ) {
            for( Map.Entry<BlockPos, Ping.BlockData> ping : getInspectBlocks().entrySet() ) {
                renderBlockHighlight( client, level, bufferSource, poseStack, cameraPos, ping.getKey(),
                        ClientConfig.PREFS.HIGHLIGHT_COLORS.playerColors.get() ? ping.getValue().color : -1 );
            }
        }
    }
    
    /** Renders a single block highlight. If color is negative, the highlight color will be auto-assigned by the config. */
    private static void renderBlockHighlight( Minecraft client, Level level, OutlineBufferSource bufferSource,
                                              PoseStack poseStack, Vec3 cameraPos, BlockPos pos, int color ) {
        BlockState block = level.getBlockState( pos );
        if( ClientConfig.trollHiddenBlocks() && block.getBlock() instanceof InfestedBlock infested ) {
            block = infested.hostStateByInfested( block ); // lol
        }
        
        if( block.getRenderShape() == RenderShape.MODEL ) {
            if( color < 0 ) color = ClientConfig.PREFS.HIGHLIGHT_COLORS.getColor( block );
            bufferSource.setColor( CrustMath.getRedBits( color ), CrustMath.getGreenBits( color ),
                    CrustMath.getBlueBits( color ), 0xFF ); // Alpha does not function for outlines
            
            poseStack.pushPose();
            poseStack.translate( pos.getX() - cameraPos.x, pos.getY() - cameraPos.y, pos.getZ() - cameraPos.z );
            
            BakedModel model = client.getBlockRenderer().getBlockModel( block );
            for( RenderType renderType : model.getRenderTypes( block, RandomSource.create( block.getSeed( pos ) ), ModelData.EMPTY ) ) {
                renderType = RenderTypeHelper.getMovingBlockRenderType( renderType );
                client.getBlockRenderer().getModelRenderer().tesselateBlock( level, model, block,
                        pos, poseStack, bufferSource.getBuffer( renderType ),
                        false, RandomSource.create(), block.getSeed( pos ),
                        OverlayTexture.NO_OVERLAY, ModelData.EMPTY, renderType );
            }
            poseStack.popPose();
        }
    }
    
    /** @return An appropriate outline buffer source to provide to the block entity's renderer to highlight it. */
    public static MultiBufferSource getBlockEntityBufferSource( LevelRenderer levelRenderer, BlockEntity blockEntity,
                                                                BlockPos blockPos, PoseStack poseStack ) {
        // Set up the baseline outline buffer
        Minecraft client = Minecraft.getInstance();
        OutlineBufferSource outlineBuffer = client.renderBuffers().outlineBufferSource();
        int color = HighlightManager.getHighlightColor( blockPos );
        outlineBuffer.setColor( CrustMath.getRedBits( color ), CrustMath.getGreenBits( color ),
                CrustMath.getBlueBits( color ), 0xFF ); // Alpha does not function for outlines
        
        // Global block entities have no pre-processing
        if( levelRenderer.globalBlockEntities.contains( blockEntity ) ) return outlineBuffer;
        // Standard block entities
        // We check if block break progress needs to be slapped onto the buffer source; would be lovely if we can
        // figure out a smarter way to avoid re-making the wheel here and instead wrap the originally provided buffer smartly
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
    
    
    private HighlightManager() {}
}