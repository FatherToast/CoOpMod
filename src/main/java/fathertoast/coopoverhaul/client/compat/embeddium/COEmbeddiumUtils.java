package fathertoast.coopoverhaul.client.compat.embeddium;

import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fml.ModList;

/** Helper class containing utilities and convenience methods for the Embeddium mod. */
public final class COEmbeddiumUtils {
    
    
    /** @return True if the Embeddium mod is installed. */
    public static boolean installed() {
        return ModList.get().isLoaded( "embeddium" );
    }
    
    /**
     * Adds the given block entity to the current level renderer instance's
     * collection of global block entities.
     */
    public static void addGlobalBlockEntity( BlockEntity blockEntity ) {
        LevelRenderer vanillaRenderer = Minecraft.getInstance().levelRenderer;
        vanillaRenderer.globalBlockEntities.add( blockEntity );
        
        if( installed() ) {
            SodiumWorldRenderer renderer = SodiumWorldRenderer.instanceNullable();
            
            if( renderer != null ) {
                renderer.forEachVisibleBlockEntity( ( be ) -> System.out.println( be.getClass().getSimpleName() ) );
            }
        }
    }
    
    
    private COEmbeddiumUtils() { }
}
