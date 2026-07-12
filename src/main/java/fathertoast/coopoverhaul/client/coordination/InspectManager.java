package fathertoast.coopoverhaul.client.coordination;

import fathertoast.coopoverhaul.api.common.util.CoOpOverhaulObjects;
import fathertoast.coopoverhaul.client.config.ClientConfig;
import fathertoast.coopoverhaul.client.vfx.HighlightManager;
import fathertoast.coopoverhaul.common.coordination.Ping;
import fathertoast.coopoverhaul.common.coordination.PingManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Controls the 'inspect' function. This performs ray casts while the player is inspecting to identify an
 * inspect target (i.e., what the player is looking at). The inspect target is highlighted and can be pinged.
 *
 * @see HighlightManager
 * @see ClientPingHelper
 */
public final class InspectManager {
    
    /** True while the player is using inspect mode. */
    private static boolean enabled;
    /** True while the player is scoped. */
    private static boolean scoped;
    
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
    public static boolean isEnabled() { return enabled || scoped; }
    
    /** @return True when the "inspect while scoping" option is on and the player is scoping. */
    public static boolean isScoped() { return scoped; }
    
    /** Retrieves the inspection range attribute value of the given player, capped by the configured maximum. */
    public static double getInspectRange( Player player ) {
        double value = player.getAttributeValue( CoOpOverhaulObjects.Attributes.INSPECT_RANGE.get() );
        return Math.min( value, ClientConfig.getMaxInspectRange() );
    }
    
    /**
     * The hit result for the object we are currently inspecting.
     * Null if not inspecting anything at the moment (not active or ray cast miss).
     */
    @Nullable
    public static HitResult target() { return isEnabled() ? target : null; }
    
    /** Called every render frame. Updates the current inspection target and inspection highlights. */
    public static void updateTarget( Minecraft client, LocalPlayer player, ClientLevel level, float partialTick ) {
        HighlightManager.getInspectEntities().clear();
        HighlightManager.getInspectBlocks().clear();
        final double range = getInspectRange( player );
        
        scoped = ClientConfig.PREFS.INSPECT.whileScoped.get() && player.isScoping();
        
        if( !isEnabled() || range <= 0.0 || player.isSpectator() ) {
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
     * @return The block or entity hit result, or null if the ray cast did not hit anything within range.
     */
    @Nullable
    public static HitResult rayCast( Minecraft client, LocalPlayer player, float partialTick ) {
        double range = Math.min( getInspectRange( player ), client.gameRenderer.getRenderDistance() );
        
        Vec3 eyePos = player.getEyePosition( partialTick );
        Vec3 viewVec = player.getViewVector( 1.0F );
        Vec3 endPos = eyePos.add( viewVec.x * range, viewVec.y * range, viewVec.z * range );
        
        // First, we ray cast for a block, ignoring fluids
        BlockHitResult blockHit = player.level().clip( new ClipContext( eyePos, endPos,
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player ) );
        if( blockHit.getType() == HitResult.Type.MISS ) blockHit = null;
        
        // Then, ray cast for an entity
        AABB searchBounds = player.getBoundingBox().expandTowards( viewVec.scale( range ) ).inflate( 1.0 );
        double rangeLimitSq = blockHit != null ? blockHit.getLocation().distanceToSqr( eyePos ) : range * range;
        EntityHitResult entityHit = entityRayCast( client, player, eyePos, endPos, searchBounds, rangeLimitSq );
        
        // Return the closest of the two hits
        return entityHit == null ? blockHit : entityHit;
    }
    
    /** @return The entity hit result, or null if the ray cast did not hit anything within range. */
    @Nullable
    private static EntityHitResult entityRayCast( Minecraft client, LocalPlayer player, Vec3 eyePos, Vec3 endPos,
                                                  AABB bounds, double rangeSq ) {
        double nearestHitDistSq = rangeSq;
        Entity nearestHit = null;
        Vec3 nearestHitPos = null;
        
        EntityRenderDispatcher renderDispatcher = client.getEntityRenderDispatcher();
        for( Entity entity : player.level().getEntities( player, bounds,
                entity -> InspectManager.canTarget( entity, player, renderDispatcher ) ) ) {
            AABB entityBounds = getBounds( entity );
            if( entityBounds == null ) continue;
            
            Optional<Vec3> clipResult = entityBounds.clip( eyePos, endPos );
            if( entityBounds.contains( eyePos ) ) {
                nearestHit = entity;
                nearestHitPos = clipResult.orElse( eyePos );
                break;
            }
            else if( clipResult.isPresent() ) {
                Vec3 hitPos = clipResult.get();
                double hitDistSq = eyePos.distanceToSqr( hitPos );
                if( hitDistSq < nearestHitDistSq ) {
                    nearestHit = entity;
                    nearestHitPos = hitPos;
                    nearestHitDistSq = hitDistSq;
                }
            }
        }
        return nearestHit == null ? null : new EntityHitResult( nearestHit, nearestHitPos );
    }
    
    /** @return True if the entity is a valid inspect target. */
    private static boolean canTarget( Entity entity, LocalPlayer player, EntityRenderDispatcher renderDispatcher ) {
        return !entity.isSpectator() && !entity.isRemoved() && !entity.isInvisibleTo( player ) &&
                !(renderDispatcher.getRenderer( entity ) instanceof NoopRenderer) &&
                !entity.isPassengerOfSameVehicle( player );
    }
    
    /** Minimum bounding box size applied in each axis to make inspecting very small entities easier. */
    private static final double MIN_BOUNDS = 0.9;
    
    /** @return The bounding box to use for a specific entity for ray tracing, or null if the entity cannot be hit. */
    @Nullable
    private static AABB getBounds( Entity entity ) {
        AABB bb = entity.getBoundingBox().inflate( entity.getPickRadius() );
        double xSize = bb.getXsize();
        double ySize = bb.getYsize();
        double zSize = bb.getZsize();
        if( xSize == 0.0 || ySize == 0.0 || zSize == 0.0 ) return null;
        boolean xTooSmall = MIN_BOUNDS > xSize;
        boolean yTooSmall = MIN_BOUNDS > ySize;
        boolean zTooSmall = MIN_BOUNDS > zSize;
        if( xTooSmall || yTooSmall || zTooSmall ) {
            return bb.inflate(
                    xTooSmall ? (MIN_BOUNDS - xSize) / 2.0 : 0.0,
                    yTooSmall ? (MIN_BOUNDS - ySize) / 2.0 : 0.0,
                    zTooSmall ? (MIN_BOUNDS - zSize) / 2.0 : 0.0 );
        }
        return bb;
    }
    
    
    private InspectManager() {}
}