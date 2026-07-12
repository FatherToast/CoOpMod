package fathertoast.coopoverhaul.client.vfx;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.AABB;

import java.util.Set;

/**
 * This "block entity" exists solely to enable the processing of outline render data for block outlines.
 */
public class OutlineBlockEntity extends BlockEntity {
    
    @SuppressWarnings( "DataFlowIssue" )
    private static final BlockEntityType<OutlineBlockEntity> TYPE = new BlockEntityType<>(
            ( pos, state ) -> null, Set.of(), null );
    private static final OutlineBlockEntity INSTANCE = new OutlineBlockEntity();
    
    /** Called each render frame if any highlighted blocks can be present. */
    public static void ensurePresent( LevelRenderer levelRenderer ) { levelRenderer.globalBlockEntities.add( INSTANCE ); }
    
    public static void remove( LevelRenderer levelRenderer ) {
        levelRenderer.globalBlockEntities.removeIf( blockEntity -> blockEntity instanceof OutlineBlockEntity );
    }
    
    
    public OutlineBlockEntity() {
        super( TYPE, new BlockPos( 0, 70457, 0 ),
                Blocks.AIR.defaultBlockState() );
    }
    
    @Override // IForgeBlockEntity
    public AABB getRenderBoundingBox() { return INFINITE_EXTENT_AABB; }
    
    @Override // IForgeBlockEntity
    public boolean hasCustomOutlineRendering( Player player ) { return HighlightManager.areAnyBlocksHighlighted(); }
}