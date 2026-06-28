package fathertoast.coopmod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import fathertoast.coopmod.client.vfx.OutlineBlockEntity;
import fathertoast.coopmod.common.core.Ping;
import fathertoast.coopmod.common.core.PingManager;
import fathertoast.coopmod.common.event.GameEventHandler;
import fathertoast.coopmod.common.network.message.ClientboundMainConfigSyncPacket;
import fathertoast.crust.api.lib.CrustMath;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.NoopRenderer;
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

import java.util.Map;

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
    
    
    // ---- Ping Controls ---- //
    
    /** @return The client's ping manager. Only returns null when not in game. */
    @Nullable
    public static PingManager pingManager() {
        Level level = client().level;
        return level == null ? null : PingManager.get( level );
    }
    
    /** Pings the target, if possible. */
    public static void ping( @Nullable HitResult hitResult ) {
        if( GameEventHandler.localPingCooldown <= 0 ) {
            GameEventHandler.localPingCooldown = pingCooldown;
            PingManager.ping( client().player, hitResult, pingDuration );
        }
    }
    
    /** Pings the current inspect target, if any. */
    public static void ping() { ping( target() ); }
    
    /** Pings whatever the player is currently looking at. Performs a ray trace if not already inspecting something. */
    public static void quickPing() {
        if( inspectOn ) { ping(); }
        else {
            Minecraft client = client();
            if( client.player != null && !client.player.isSpectator() ) {
                ping( rayCast( client, client.player, 1.0F ) );
            }
        }
    }
    
    /** @return True if the entity should be rendered with a glow effect. */
    public static boolean shouldHighlight( Entity entity ) {
        return isInspectTarget( entity ) || PingManager.isPinged( entity );
    }
    
    /** @return The RGB highlight color the entity should have. */
    public static int getHighlightColor( Entity entity ) {
        if( isInspectTarget( entity ) ) {
            if( ClientConfig.PREFS.HIGHLIGHT_COLORS.inspectUsesDefault.get() )
                return ClientConfig.PREFS.HIGHLIGHT_COLORS.defaultColor.get();
        }
        else if( ClientConfig.PREFS.HIGHLIGHT_COLORS.playerColors.get() ) {
            Ping.EntityData ping = PingManager.getPingData( entity );
            if( ping != null && ping.color >= 0 ) return ping.color;
        }
        return ClientConfig.PREFS.HIGHLIGHT_COLORS.getColor( entity );
    }
    
    /** @return True if any blocks should be rendered with a glow effect. */
    public static boolean areAnyBlocksHighlighted() {
        return targetBlock() != null || PingManager.areAnyPingsActive( client().level );
    }
    
    
    // ---- Logic ---- //
    
    /** The max value allowed for inspect range. Set by logical server. */
    private static double maxInspectRange;
    /** True if we should not allow identifying hidden blocks (e.g, infested). Set by logical server. */
    private static boolean trollHiddenBlocks = true;
    /** Ticks before pings fade. Set by logical server. */
    private static int pingDuration;
    /** Minimum ticks required between pings. Set by logical server. */
    private static int pingCooldown = Integer.MAX_VALUE;
    
    /** True while the player is using inspect mode. */
    private static boolean inspectOn;
    /** The max distance that inspect can ray cast. Will not ray cast farther than render distance regardless of this value. */
    private static double inspectRange;
    
    
    /** The current inspect target. Null if we have no target. Will never have a type of HitResult.Type.MISS. */
    @Nullable
    private static HitResult target;
    /** The current block inspect target. Null if we have no block target. Type can only be HitResult.Type.BLOCK. */
    @Nullable
    private static BlockHitResult targetBlock;
    /** The current entity inspect target. Null if we have no entity target. Type can only be HitResult.Type.ENTITY. */
    @Nullable
    private static EntityHitResult targetEntity;
    
    
    private static Minecraft client() { return Minecraft.getInstance(); }
    
    /** Updates all fields set by the logical server. */
    public static void handleMainConfigSync( ClientboundMainConfigSyncPacket message ) {
        setMaxInspectRange( message.maxInspectRange() );
        trollHiddenBlocks = !message.allowInspectingHidden();
        pingDuration = message.pingDuration();
        pingCooldown = message.pingCooldown();
    }
    
    /** Called by the event listener to do rendering. */
    public static void render( RenderLevelStageEvent event ) {
        render( client(), event.getLevelRenderer(), event.getPoseStack(), event.getProjectionMatrix(),
                event.getRenderTick(), event.getPartialTick(), event.getCamera(), event.getFrustum() );
    }
    
    /** Called every render frame. Updates the current render target and then renders all block highlights. */
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
        for( Map.Entry<BlockPos, Ping.BlockData> ping : PingManager.get( client.level ).getBlockPings() ) {
            if( !ping.getKey().equals( targetPos ) ) {
                renderBlockHighlight( client, client.level, bufferSource, poseStack, cameraPos,
                        ping.getKey(), ClientConfig.PREFS.HIGHLIGHT_COLORS.playerColors.get() ?
                                ping.getValue().color : -1 );
            }
        }
    }
    
    /** Renders a single block highlight. If color is negative, the highlight color will be auto-assigned by the config. */
    private static void renderBlockHighlight( Minecraft client, Level level, OutlineBufferSource bufferSource,
                                              PoseStack poseStack, Vec3 cameraPos, BlockPos pos, int color ) {
        BlockState block = level.getBlockState( pos );
        if( trollHiddenBlocks && block.getBlock() instanceof InfestedBlock infested ) {
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
        EntityRenderDispatcher renderDispatcher = client.getEntityRenderDispatcher();
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult( player, eyePos, endPos,
                searchBounds, entity -> InspectManager.canTarget( entity, player, renderDispatcher ),
                distSq );
        
        // Return the closest of the two hits
        return entityHit == null || entityHit.getLocation().distanceToSqr( eyePos ) >= distSq ? blockHit : entityHit;
    }
    
    /** @return True if the entity is a valid inspect target. */
    private static boolean canTarget( Entity entity, LocalPlayer player, EntityRenderDispatcher renderDispatcher ) {
        return !entity.isSpectator() && !entity.isRemoved() && !entity.isInvisibleTo( player ) &&
                !(renderDispatcher.getRenderer( entity ) instanceof NoopRenderer);
    }
    
    
    private InspectManager() {}
}