package fathertoast.coopmod.client.coordination;

import fathertoast.coopmod.client.config.ClientConfig;
import fathertoast.coopmod.client.vfx.HighlightManager;
import fathertoast.coopmod.common.coordination.Ping;
import fathertoast.coopmod.common.coordination.PingManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import org.jetbrains.annotations.Nullable;

public final class InspectManager {
    
    /** True while the player is using inspect mode. */
    private static boolean enabled;
    /** The max distance that inspect can ray cast. Will not ray cast farther than render distance regardless of this value. */
    private static double range;
    
    /** The current inspect target. Null if we have no target. Will never have a type of HitResult.Type.MISS. */
    @Nullable
    private static HitResult target;
    
    /** Turns on "inspect" mode, which highlights what you are looking at and allows you to ping it. */
    public static void enable() { setEnabled( true ); }
    
    /** Turns off "inspect" mode, which highlights what you are looking at and allows you to ping it. */
    public static void disable() { setEnabled( false ); }
    
    /** Sets "inspect" mode on or off, which highlights what you are looking at and allows you to ping it. */
    public static void setEnabled( boolean on ) { enabled = on; }
    
    /** @return Whether "inspect" mode is on or off. */
    public static boolean isEnabled() { return enabled; }
    
    /** Updates the inspection range based on current settings. */
    public static void updateRange() {
        range = Math.min( ClientConfig.PREFS.INSPECTION.range.get(), ClientConfig.getMaxInspectRange() );
    }
    
    /**
     * The hit result for the object we are currently inspecting.
     * Null if not inspecting anything at the moment (not active or ray cast miss).
     */
    @Nullable
    public static HitResult target() { return enabled ? target : null; }
    
    /** Called every render frame. Updates the current inspection target and inspection highlights. */
    public static void updateTarget( Minecraft client, LocalPlayer player, ClientLevel level, float partialTick ) {
        HighlightManager.getInspectEntities().clear();
        HighlightManager.getInspectBlocks().clear();
        if( !enabled || range <= 0.0 || player.isSpectator() ) {
            target = null;
        }
        else {
            target = rayCast( client, player, partialTick );
            if( target instanceof EntityHitResult entityHit ) {
                int entityId = entityHit.getEntity().getId();
                PingManager.gatherAttachedEntities( HighlightManager.getInspectEntities(), level, entityId,
                        Ping.of( player, 0, entityId ) );
            }
            if( target instanceof BlockHitResult blockHit ) {
                BlockPos blockPos = blockHit.getBlockPos();
                PingManager.gatherAttachedBlocks( HighlightManager.getInspectBlocks(), level, blockPos,
                        Ping.of( player, 0, blockPos ) );
            }
        }
    }
    
    /**
     * Performs a ray cast from the player's eyes to identify the world object on the HUD crosshairs,
     * if any valid thing is in the inspect range.
     *
     * @return The block or entity hit result, or null if the ray cast missed.
     */
    @Nullable
    public static HitResult rayCast( Minecraft client, LocalPlayer player, float partialTick ) {
        double range = Math.min( InspectManager.range, client.gameRenderer.getRenderDistance() );
        
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