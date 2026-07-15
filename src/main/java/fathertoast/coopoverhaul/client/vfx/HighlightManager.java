package fathertoast.coopoverhaul.client.vfx;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import fathertoast.coopoverhaul.client.config.ClientConfig;
import fathertoast.coopoverhaul.client.coordination.FindPlayersManager;
import fathertoast.coopoverhaul.common.coordination.Ping;
import fathertoast.coopoverhaul.common.coordination.PingManager;
import fathertoast.crust.api.lib.CrustMath;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.SpyglassItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.InfestedBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraftforge.client.RenderTypeHelper;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.*;

/**
 * Keeps track of what should be highlighted and renders highlights as needed.
 * <p>
 * For highlights other than inspect highlights, also renders a nameplate above.
 *
 * @see fathertoast.coopoverhaul.client.coordination.InspectManager
 * @see PingManager
 * @see FindPlayersManager
 */
public final class HighlightManager {
    
    /** The entities we should highlight for the inspect feature. */
    private static final Map<Integer, Ping.EntityData> inspectEntities = new HashMap<>();
    /** The block positions we should highlight for the inspect feature. */
    private static final Map<BlockPos, Ping.BlockData> inspectBlocks = new HashMap<>();
    
    /** The entities we should highlight for the inspect feature. */
    public static Map<Integer, Ping.EntityData> getInspectEntities() { return inspectEntities; }
    
    /** The block positions we should highlight for the inspect feature. */
    public static Map<BlockPos, Ping.BlockData> getInspectBlocks() { return inspectBlocks; }
    
    /** @return True if the entity has an active ping nameplate. */
    public static boolean hasNameplate( Entity entity ) {
        return ClientConfig.PREFS.INSPECT.nameplateSize.get() > 0.0 &&
                (PingManager.isPinged( entity ) || FindPlayersManager.shouldHighlight( entity ));
    }
    
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
    
    /** @return True if any blocks should be rendered with a glow effect. */
    public static boolean areAnyBlocksHighlighted() {
        return !getInspectBlocks().isEmpty() || PingManager.areAnyPingsActive( Minecraft.getInstance().level );
    }
    
    /** @return The RGB highlight color the entity should have. */
    public static int getHighlightColor( Entity entity ) {
        if( ClientConfig.PREFS.HIGHLIGHT_SETTINGS.playerColors.get() ) {
            Ping.EntityData ping = PingManager.getPingData( entity );
            if( ping != null && ping.color >= 0 ) return ping.color;
        }
        return ClientConfig.PREFS.HIGHLIGHT_SETTINGS.getColor( entity );
    }
    
    /** @return The RGB highlight color the block position should have. */
    public static int getHighlightColor( BlockPos pos ) {
        ClientLevel level = Minecraft.getInstance().level;
        if( level == null ) return ClientConfig.PREFS.HIGHLIGHT_SETTINGS.defaultColor.get();
        
        BlockState block = level.getBlockState( pos );
        if( ClientConfig.PREFS.HIGHLIGHT_SETTINGS.playerColors.get() ) {
            int color = getInspectBlocks().containsKey( pos ) ? getInspectBlocks().get( pos ).color :
                    PingManager.get( level ).getColor( pos );
            if( color > 0 ) return color;
        }
        return ClientConfig.PREFS.HIGHLIGHT_SETTINGS.getColor( block );
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
    
    
    // ---- Render Block Highlights ---- //
    
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
                    ClientConfig.PREFS.HIGHLIGHT_SETTINGS.playerColors.get() ? ping.getValue().color : -1 );
            getInspectBlocks().remove( ping.getKey() );
        }
        if( !getInspectBlocks().isEmpty() ) {
            for( Map.Entry<BlockPos, Ping.BlockData> ping : getInspectBlocks().entrySet() ) {
                renderBlockHighlight( client, level, bufferSource, poseStack, cameraPos, ping.getKey(),
                        ClientConfig.PREFS.HIGHLIGHT_SETTINGS.playerColors.get() ? ping.getValue().color : -1 );
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
            if( color < 0 ) color = ClientConfig.PREFS.HIGHLIGHT_SETTINGS.getColor( block );
            bufferSource.setColor( CrustMath.getRedBits( color ), CrustMath.getGreenBits( color ),
                    CrustMath.getBlueBits( color ), 0xFF ); // Alpha does not function for outlines
            
            poseStack.pushPose();
            poseStack.translate(
                    offset( pos.getX() - cameraPos.x ),
                    offset( pos.getY() - cameraPos.y ),
                    offset( pos.getZ() - cameraPos.z )
            );
            //TODO Do this the right way, eventually - it works decently now for solid and cutout blocks, but NOT translucent
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
    
    /**
     * @return An offset to resolve z-fighting. Should cause the model we render for the outline to always be hidden.
     * Because I really just don't understand how block rendering works well enough to solve it the right way.
     */
    private static double offset( double off ) { return off + (off > -0.5 ? 0.001 : -0.001); }
    
    
    // ---- Render Ping Nameplates ---- //
    
    /** Called every render frame. Renders all pinged entity and block nameplates. */
    public static void renderNameplates( Minecraft client, ClientLevel level, LocalPlayer player, LevelRenderer levelRenderer,
                                         PoseStack poseStack, Matrix4f projectionMatrix, int renderTick, float partialTick,
                                         Camera camera, Frustum frustum ) {
        PingManager manager = PingManager.get( level );
        if( !FindPlayersManager.isEnabled() && !manager.areAnyPingsActive() ) return;
        // In case we swap back to rendering AFTER_LEVEL
        //        poseStack.setIdentity();
        //        poseStack.mulPose( Axis.XP.rotationDegrees( camera.getXRot() ) );
        //        poseStack.mulPose( Axis.YP.rotationDegrees( camera.getYRot() + 180.0F ) );
        MultiBufferSource.BufferSource bufferSource = client.renderBuffers().bufferSource();
        
        float baseScale = ClientConfig.PREFS.INSPECT.nameplateSize.getFloat() *
                (isScoping( camera ) ? SpyglassItem.ZOOM_FOV_MODIFIER : 1.0F);
        
        //TODO Perhaps combine entity stacks into one combined nameplate, similar to how multi-block pings work
        
        // Render find players nameplates
        Set<UUID> renderedPlayers;
        if( FindPlayersManager.isEnabled() ) {
            renderedPlayers = new HashSet<>();
            for( AbstractClientPlayer otherPlayer : level.players() ) {
                if( FindPlayersManager.shouldHighlight( otherPlayer ) ) {
                    Vec3 pos = otherPlayer.getPosition( partialTick );
                    renderNameplate( client, otherPlayer.getDisplayName(), poseStack, bufferSource,
                            camera, baseScale, pos.x, pos.y + otherPlayer.getNameTagOffsetY(), pos.z, otherPlayer );
                    renderedPlayers.add( otherPlayer.getUUID() );
                }
            }
        }
        else renderedPlayers = null;
        
        // Render pinged entity nameplates
        for( Map.Entry<Integer, Ping.EntityData> ping : manager.getEntityPings() ) {
            Entity entity = level.getEntity( ping.getKey() );
            if( entity != null && (renderedPlayers == null || !renderedPlayers.contains( entity.getUUID() )) ) {
                AbstractClientPlayer otherPlayer = entity instanceof AbstractClientPlayer p ? p : null;
                Vec3 pos = entity.getPosition( partialTick );
                
                // Item projectiles' item stack usually provides
                // better info about the projectile itself.
                Component name = entity instanceof ThrowableItemProjectile projectile
                        ? projectile.getItem().getHoverName()
                        : entity.getDisplayName();
                
                renderNameplate( client, name, poseStack, bufferSource,
                        camera, baseScale, pos.x, pos.y + entity.getNameTagOffsetY(), pos.z, otherPlayer );
                if( renderedPlayers != null && otherPlayer != null ) renderedPlayers.add( otherPlayer.getUUID() );
            }
        }
        
        // Render find players nameplates for any players not already rendered
        if( renderedPlayers != null ) {
            for( Map.Entry<UUID, BlockPos> playerPos : FindPlayersManager.getPlayerPositions() ) {
                if( !renderedPlayers.contains( playerPos.getKey() ) ) {
                    PlayerInfo info = player.connection.getPlayerInfo( playerPos.getKey() );
                    if( info != null && FindPlayersManager.isInRange( player, playerPos.getValue() ) ) {
                        Vec3 pos = FindPlayersManager.getPos( playerPos.getKey(), playerPos.getValue(), partialTick );
                        renderNameplate( client, getDisplayName( info ), poseStack, bufferSource,
                                camera, baseScale, pos.x, pos.y, pos.z,
                                info.getSkinLocation(), false, false );
                    }
                }
            }
        }
        
        // Render pinged block nameplates
        for( Map.Entry<BlockPos, Ping.BlockData> ping : manager.getBlockPings() ) {
            BlockPos pos = ping.getKey();
            BlockState block = level.getBlockState( pos );
            Vec3 offset = getNameplateOffset( level, pos, block );
            if( offset != null ) {
                Component name = Component.translatable( I18n.get( block.getBlock().getDescriptionId() ) );
                renderNameplate( client, name, poseStack, bufferSource,
                        camera, baseScale, pos.getX() + offset.x, pos.getY() + offset.y, pos.getZ() + offset.z, null );
            }
        }
        
        // In case we swap back to rendering AFTER_LEVEL
        //        RenderSystem.applyModelViewMatrix();
    }
    
    /** @return True if the camera is zoomed in by a scope. */
    private static boolean isScoping( Camera camera ) {
        return Minecraft.getInstance().options.getCameraType().isFirstPerson() &&
                camera.getEntity() instanceof LocalPlayer player && player.isScoping();
    }
    
    /** @return The display name to use for players outside render distance. */
    public static Component getDisplayName( PlayerInfo playerInfo ) {
        return playerInfo.getTabListDisplayName() != null ? playerInfo.getTabListDisplayName().copy() :
                PlayerTeam.formatNameForTeam( playerInfo.getTeam(),
                        Component.literal( playerInfo.getProfile().getName() ) );
    }
    
    /** @return The nameplate position offset to apply for the block, or null if no nameplate should be rendered. */
    @Nullable
    private static Vec3 getNameplateOffset( Level level, BlockPos pos, BlockState state ) {
        double height = state.getShape( level, pos ).max( Direction.Axis.Y );
        Vec3 offset = new Vec3( 0.5, height + 0.25, 0.5 );
        
        // Only render a nameplate for a single block from multipart blocks
        if( state.hasProperty( BlockStateProperties.DOUBLE_BLOCK_HALF ) ) {
            // Double blocks (e.g., doors, tall grass, etc.)
            return state.getValue( BlockStateProperties.DOUBLE_BLOCK_HALF ) == DoubleBlockHalf.UPPER ? offset : null;
        }
        else if( state.hasProperty( BlockStateProperties.BED_PART ) && state.hasProperty( BlockStateProperties.HORIZONTAL_FACING ) ) {
            // Beds
            if( state.getValue( BlockStateProperties.BED_PART ) != BedPart.FOOT ) return null;
            return offset.relative( state.getValue( BlockStateProperties.HORIZONTAL_FACING ), 0.5 );
        }
        else if( state.hasProperty( BlockStateProperties.CHEST_TYPE ) && state.hasProperty( BlockStateProperties.HORIZONTAL_FACING ) ) {
            ChestType chestType = state.getValue( BlockStateProperties.CHEST_TYPE );
            if( chestType != ChestType.SINGLE ) {
                // Double chests
                if( chestType != ChestType.RIGHT ) return null;
                return offset.relative( state.getValue( BlockStateProperties.HORIZONTAL_FACING )
                        .getCounterClockWise(), 0.5 );
            }
        }
        return offset;
    }
    
    // Packed light calculation as by #pack(int blockLightLevel, int skyLightLevel).
    //private static final int NAMEPLATE_PACKED_LIGHT = LightTexture.pack( 15, 15 );// = 0xF000F0;
    
    /** Renders a nameplate at the given position. */
    private static void renderNameplate( Minecraft client, Component text, PoseStack poseStack, MultiBufferSource.BufferSource bufferSource,
                                         Camera camera, float baseScale, double x, double y, double z,
                                         @Nullable AbstractClientPlayer player ) {
        ResourceLocation skinLocation;
        boolean hasHat;
        boolean upsideDown;
        if( player == null ) {
            skinLocation = null;
            hasHat = false;
            upsideDown = false;
        }
        else {
            skinLocation = player.getSkinTextureLocation();
            hasHat = player.isModelPartShown( PlayerModelPart.HAT );
            upsideDown = LivingEntityRenderer.isEntityUpsideDown( player );
        }
        renderNameplate( client, text, poseStack, bufferSource, camera, baseScale, x, y, z,
                skinLocation, hasHat, upsideDown );
    }
    
    /** Renders a nameplate at the given position. */
    private static void renderNameplate( Minecraft client, Component text, PoseStack poseStack, MultiBufferSource.BufferSource bufferSource,
                                         Camera camera, float baseScale, double x, double y, double z,
                                         @Nullable ResourceLocation skinLocation, boolean hasHat, boolean upsideDown ) {
        poseStack.pushPose();
        Vec3 cameraPos = camera.getPosition();
        Vector3f cameraUpVec = camera.getUpVector();
        float scale = baseScale * (float) Math.sqrt( cameraPos.distanceToSqr( x, y, z ) );
        poseStack.translate( x - cameraPos.x + 0.5F * cameraUpVec.x,
                y - cameraPos.y + 0.5F * cameraUpVec.y,
                z - cameraPos.z + 0.5F * cameraUpVec.z );
        poseStack.mulPose( camera.rotation() );
        poseStack.scale( -scale, -scale, scale );
        
        Matrix4f pose = poseStack.last().pose();
        float offset = -client.font.width( text ) >> 1;
        
        if( skinLocation != null ) {
            float size = ClientConfig.PREFS.INSPECT.nameplateFaceSize.getFloat();
            if( size > 0.0F )
                RenderHelper.drawFace( poseStack, skinLocation, -size / 2.0F, -size - 8.0F, size, hasHat, upsideDown );
        }
        RenderSystem.disableDepthTest();
        client.font.drawInBatch( text, offset, -5.0F, 0xFF_FFFFFF,
                false, pose, bufferSource, Font.DisplayMode.SEE_THROUGH,
                0, 0xF000F0 );
        bufferSource.endBatch();
        RenderSystem.enableDepthTest();
        
        poseStack.popPose();
    }
    
    
    private HighlightManager() { }
}