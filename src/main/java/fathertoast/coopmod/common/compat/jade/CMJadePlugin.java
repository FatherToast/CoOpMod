package fathertoast.coopmod.common.compat.jade;

import fathertoast.coopmod.client.config.ClientConfig;
import fathertoast.coopmod.client.coordination.InspectManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;
import snownee.jade.api.Accessor;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.callback.JadeRayTraceCallback;

@WailaPlugin
public class CMJadePlugin implements IWailaPlugin {
    
    public enum Mode { OFF, BACKUP, OVERRIDE, REPLACE }
    
    @Override
    public void registerClient( IWailaClientRegistration registration ) {
        registration.addRayTraceCallback( -5000, new RayTraceCallback( registration ) );
    }
    
    private record RayTraceCallback( IWailaClientRegistration registration ) implements JadeRayTraceCallback {
        /**
         * Called by Jade after ray tracing to allow modifying the tooltip accessor.
         *
         * @return The accessor to generate the tooltip with, or null if no tooltip should be displayed.
         */
        @Override
        @Nullable
        public Accessor<?> onRayTrace( HitResult hitResult, @Nullable Accessor<?> accessor,
                                       @Nullable Accessor<?> originalAccessor ) {
            return switch( ClientConfig.PREFS.INSPECTION.jadeMode.get() ) {
                // Off; don't change the tooltip at all
                case OFF -> accessor;
                // Backup; only add a tooltip if there isn't already a tooltip to display
                case BACKUP -> accessor == null ? inspectTargetAccessor() : accessor;
                // Override; ensure the tooltip is always the inspect target while inspecting
                case OVERRIDE -> InspectManager.isEnabled() ? inspectTargetAccessor() : accessor;
                // Replace; disable Jade's ray trace entirely and replace it with the inspect function
                case REPLACE -> inspectTargetAccessor();
            };
        }
        
        /** @return A Jade accessor representing the current inspect target. */
        @Nullable
        private Accessor<?> inspectTargetAccessor() {
            HitResult target = InspectManager.target();
            if( target != null ) {
                if( target instanceof EntityHitResult entityHit ) {
                    return registration.entityAccessor().hit( entityHit ).entity( entityHit.getEntity() )
                            .requireVerification().build();
                }
                ClientLevel level = Minecraft.getInstance().level;
                if( target instanceof BlockHitResult blockHit && level != null ) {
                    return registration.blockAccessor().blockState( level.getBlockState( blockHit.getBlockPos() ) )
                            .blockEntity( level.getBlockEntity( blockHit.getBlockPos() ) ).hit( blockHit )
                            .requireVerification().build();
                }
            }
            return null;
        }
    }
}