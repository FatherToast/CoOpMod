package fathertoast.coopmod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import fathertoast.coopmod.client.vfx.OutlineBlockEntity;
import fathertoast.crust.api.lib.CrustMath;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.InfestedBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraftforge.client.RenderTypeHelper;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings( "UnstableApiUsage" )
public final class InspectManager {
    
    // ---- Inspect Mode Controls ---- //
    
    /** Turns on "inspect" mode, which highlights what you are looking at and allows you to ping it. */
    public static void enableInspect() { setInspectOn( true ); }
    
    /** Turns off "inspect" mode, which highlights what you are looking at and allows you to ping it. */
    public static void disableInspect() { setInspectOn( false ); }
    
    /** Sets "inspect" mode on or off, which highlights what you are looking at and allows you to ping it. */
    public static void setInspectOn( boolean on ) { inspectOn = on; }
    
    /** @return Whether "inspect" mode is on or off. */
    public static boolean getInspectOn() { return inspectOn; }
    
    /** Sets the maximum distance (in blocks) you can inspect things from. (The server's limit.) */
    public static void setMaxInspectRange( double d ) {
        maxInspectRange = d;
        updateInspectRange();
    }
    
    /** Updates the inspection range based on current settings. */
    public static void updateInspectRange() {
        inspectRange = Math.min( ClientConfig.PREFS.INSPECTION.range.get(), maxInspectRange );
    }
    
    /**
     * The hit result for the object we are currently inspecting.
     * Null if not inspecting anything at the moment (not active or ray cast miss).
     */
    @Nullable
    public static HitResult target() { return inspectOn ? target : null; }
    
    /**
     * The hit result for the block we are currently inspecting.
     * Null if not inspecting a block at the moment (not active, ray cast miss, or inspecting entity).
     */
    @Nullable
    public static BlockHitResult targetBlock() { return inspectOn ? targetBlock : null; }
    
    /**
     * The hit result for the entity we are currently inspecting.
     * Null if not inspecting an entity at the moment (not active, ray cast miss, or inspecting block).
     */
    @Nullable
    public static EntityHitResult targetEntity() { return inspectOn ? targetEntity : null; }
    
    /** @return True if the entity is our current inspect target. */
    public static boolean isInspectTarget( @Nullable Entity entity ) {
        return inspectOn && targetEntity != null && targetEntity.getEntity().equals( entity );
    }
    
    
    // ---- Ping Highlight Controls ---- //
    
    public static void ping() { ping( target() ); }
    
    public static void ping( @Nullable HitResult target ) {
        if( target instanceof BlockHitResult blockTarget ) pingBlock( blockTarget );
        if( target instanceof EntityHitResult entityTarget ) pingEntity( entityTarget );
    }
    
    /** Pings whatever the player is currently looking at. Performs a ray trace if not already inspecting something. */
    public static void quickPing() {
        if( inspectOn ) { ping(); }
        else {
            Minecraft client = Minecraft.getInstance();
            if( client.player != null ) ping( rayCast( client, client.player, 1.0F ) );
        }
    }
    
    public static boolean isPinged( @Nullable Entity entity ) {
        return entity != null && ENTITY_PINGS.containsKey( entity );
    }
    
    /** @return True if the entity should be rendered with a glow effect. */
    public static boolean shouldHighlight( @Nullable Entity entity ) { return isInspectTarget( entity ) || isPinged( entity ); }
    
    /** @return The RGB highlight color the entity should have. */
    public static int getHighlightColor( Entity entity ) {
        if( isInspectTarget( entity ) ) {
            return ClientConfig.PREFS.HIGHLIGHT_COLORS.inspectUsesDefault.get() ?
                    ClientConfig.PREFS.HIGHLIGHT_COLORS.defaultColor.get() :
                    ClientConfig.PREFS.HIGHLIGHT_COLORS.entityColors.getOrElse( entity,
                            ClientConfig.PREFS.HIGHLIGHT_COLORS.defaultColor.get() );
        }
        PingData ping = ENTITY_PINGS.get( entity );
        return ping.color < 0 ? ClientConfig.PREFS.HIGHLIGHT_COLORS.entityColors.getOrElse( entity,
                ClientConfig.PREFS.HIGHLIGHT_COLORS.defaultColor.get() ) : ping.color;
    }
    
    /** @return True if any blocks should be rendered with a glow effect. */
    public static boolean areAnyBlocksHighlighted() { return targetBlock() != null || !BLOCK_PINGS.isEmpty(); }
    
    
    // ---- Logic ---- //
    
    private static boolean inspectOn = true;//TODO temp until key bound
    
    private static double maxInspectRange;
    private static double inspectRange;
    
    @Nullable
    private static HitResult target;
    @Nullable
    private static BlockHitResult targetBlock;
    @Nullable
    private static EntityHitResult targetEntity;
    
    /** Simple way of detecting a dimension change to clear all active pings. */
    @Nullable
    private static Level currentLevel;
    
    private static final HashMap<BlockPos, BlockPingData> BLOCK_PINGS = new HashMap<>();
    private static final HashMap<Entity, PingData> ENTITY_PINGS = new HashMap<>();
    
    
    public static void render( RenderLevelStageEvent event ) {
        render( Minecraft.getInstance(), event.getLevelRenderer(), event.getPoseStack(), event.getProjectionMatrix(),
                event.getRenderTick(), event.getPartialTick(), event.getCamera(), event.getFrustum() );
    }
    
    private static void render( Minecraft client, LevelRenderer levelRenderer, PoseStack poseStack, Matrix4f projectionMatrix,
                                int renderTick, float partialTick, Camera camera, Frustum frustum ) {
        // Update inspection target
        LocalPlayer player = client.player;
        if( !inspectOn || inspectRange <= 0.0 || player == null || player.isSpectator() ) {
            target = null;
            targetBlock = null;
            targetEntity = null;
        }
        else {
            target = rayCast( client, player, partialTick );
            targetBlock = target instanceof BlockHitResult ? (BlockHitResult) target : null;
            targetEntity = target instanceof EntityHitResult ? (EntityHitResult) target : null;
        }
        
        // Render block highlights
        if( client.level == null || !areAnyBlocksHighlighted() ) return;
        BlockPos targetPos = targetBlock() == null ? null : targetBlock().getBlockPos();
        OutlineBlockEntity.ensurePresent( levelRenderer );
        Vec3 cameraPos = camera.getPosition();
        OutlineBufferSource bufferSource = client.renderBuffers().outlineBufferSource();
        if( targetPos != null ) {
            renderBlockHighlight( client, client.level, bufferSource, poseStack, cameraPos, targetPos,
                    ClientConfig.PREFS.HIGHLIGHT_COLORS.inspectUsesDefault.get() ?
                            ClientConfig.PREFS.HIGHLIGHT_COLORS.defaultColor.get() : -1 );
        }
        for( Map.Entry<BlockPos, BlockPingData> ping : BLOCK_PINGS.entrySet() ) {
            if( !ping.getKey().equals( targetPos ) ) {
                renderBlockHighlight( client, client.level, bufferSource, poseStack, cameraPos, ping.getKey(), ping.getValue().color );
            }
        }
    }
    
    private static void renderBlockHighlight( Minecraft client, Level level, OutlineBufferSource bufferSource,
                                              PoseStack poseStack, Vec3 cameraPos, BlockPos pos, int color ) {
        BlockState block = level.getBlockState( pos );
        if( block.getBlock() instanceof InfestedBlock infested ) {
            block = infested.hostStateByInfested( block ); // TrollFaceNoSpace TODO maybe add server-side config?
        }
        
        if( block.getRenderShape() == RenderShape.MODEL ) {
            if( color < 0 ) {
                color = ClientConfig.PREFS.HIGHLIGHT_COLORS.blockColors.getOrElse( block,
                        ClientConfig.PREFS.HIGHLIGHT_COLORS.defaultColor.get() );
            }
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
    
    /**
     * Performs a ray cast from the player's eyes to identify the world object on the HUD crosshairs,
     * if any valid thing is in the inspect range.
     *
     * @return The block or entity hit result, or null if the ray cast missed.
     */
    @Nullable
    private static HitResult rayCast( Minecraft client, LocalPlayer player, float partialTick ) {
        double range = Math.min( inspectRange, client.gameRenderer.getRenderDistance() );
        
        Vec3 eyePos = player.getEyePosition( partialTick );
        Vec3 viewVec = player.getViewVector( 1.0F );
        Vec3 endPos = eyePos.add( viewVec.x * range, viewVec.y * range, viewVec.z * range );
        
        // First, we ray cast for a block, ignoring fluids
        BlockHitResult blockHit = player.level().clip( new ClipContext( eyePos, endPos,
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player ) );
        if( blockHit.getType() == HitResult.Type.MISS ) blockHit = null;
        
        double distSq = blockHit != null ? blockHit.getLocation().distanceToSqr( eyePos ) : range * range;
        AABB searchBounds = player.getBoundingBox().expandTowards( viewVec.scale( range ) ).inflate( 1.0 );
        
        // Then, ray cast for an entity
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult( player, eyePos, endPos,
                searchBounds, InspectManager::canTarget, distSq );
        
        // Return the closest of the two hits
        return entityHit == null || entityHit.getLocation().distanceToSqr( eyePos ) >= distSq ? blockHit : entityHit;
    }
    
    private static boolean canTarget( Entity entity ) {
        return !entity.isSpectator() && !entity.isRemoved();
        // Note: isPickable is used instead of !isRemoved in the client code, but doesn't let us inspect things like item entities
        // && entity.isPickable()
    }
    
    
    public static void onTickEnd() {
        Minecraft client = Minecraft.getInstance();
        if( client.level == null || client.level != currentLevel ) {
            currentLevel = client.level;
            BLOCK_PINGS.clear();
            ENTITY_PINGS.clear();
        }
        else {
            long gameTime = client.level.getGameTime();
            BLOCK_PINGS.entrySet().removeIf( ( entry ) ->
                    entry.getValue().expiryTime < gameTime || entry.getValue().isDestroyed( client.level, entry.getKey() ) );
            ENTITY_PINGS.entrySet().removeIf( ( entry ) ->
                    entry.getValue().expiryTime < gameTime || entry.getKey().isRemoved() );
        }
    }
    
    private static final int PING_DURATION = 20 * 5;//TODO Config somehow
    
    private static void pingBlock( BlockHitResult blockTarget ) {
        Minecraft client = Minecraft.getInstance();
        if( client.level != null ) {
            BlockPos pos = blockTarget.getBlockPos();
            if( client.level.isLoaded( pos ) ) {
                // TODO implement real ping ability
                BLOCK_PINGS.put( pos, new BlockPingData( client.level, PING_DURATION, -1, pos ) );
            }
        }
    }
    
    private static void pingEntity( EntityHitResult entityTarget ) {
        Entity entity = entityTarget.getEntity();
        // TODO implement real ping ability
        ENTITY_PINGS.put( entity, new PingData( entity.level(), PING_DURATION, -1 ) );
    }
    
    private static class PingData {
        public final long expiryTime;
        public final int color;
        
        private PingData( Level level, long duration, int col ) {
            expiryTime = level.getGameTime() + duration;
            color = col;
        }
    }
    
    private static class BlockPingData extends PingData {
        public final BlockState blockState;
        
        private BlockPingData( Level level, long duration, int clr, BlockPos pos ) {
            super( level, duration, clr );
            blockState = level.getBlockState( pos );
        }
        
        public boolean isDestroyed( Level level, BlockPos pos ) {
            return !level.isLoaded( pos ) || !blockState.equals( level.getBlockState( pos ) );
        }
    }
    
    private InspectManager() {}
}