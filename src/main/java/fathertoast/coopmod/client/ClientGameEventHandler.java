package fathertoast.coopmod.client;


import fathertoast.coopmod.common.core.CoOpMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Contains and automatically registers all client-side forge events.
 */
@Mod.EventBusSubscriber( value = Dist.CLIENT, modid = CoOpMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE )
public final class ClientGameEventHandler {
    
    //    /**
    //     * Called at the start and end of each client tick.
    //     *
    //     * @param event The event data.
    //     */
    //    @SubscribeEvent( priority = EventPriority.NORMAL )
    //    static void onClientTick( TickEvent.ClientTickEvent event ) {
    //        if( event.phase == TickEvent.Phase.END ) {
    //            InspectManager.onTickEnd();
    //        }
    //    }
    
    /**
     * Called at several stages of level rendering.
     * You need to check the event's stage to know which point of the rendering process we are in.
     *
     * @param event The event data.
     */
    @SubscribeEvent( priority = EventPriority.NORMAL )
    static void onRenderLevel( RenderLevelStageEvent event ) {
        if( event.getStage() == RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS ) {
            InspectManager.render( event );
        }
    }
    
    /**
     * Called when a block is about to be broken by a player.
     * Canceling this event will prevent the block from being broken.
     *
     * @param event The event data.
     */
    @SubscribeEvent( priority = EventPriority.NORMAL )
    static void onBlockBreak( BlockEvent.BreakEvent event ) {
        //if( event.isCanceled() || !(event.getLevel() instanceof ServerLevel level) ) return;
        //TODO Some chunk protection goes here?
    }
    
    /**
     * This event is fired on both sides whenever the player right clicks while targeting a block.
     *
     * @param event The event data.
     */
    @SubscribeEvent( priority = EventPriority.NORMAL )
    static void onRightClickContainer( PlayerInteractEvent.RightClickBlock event ) {
        //TODO Some chunk protection goes here?
    }
    
    /**
     * This event is fired on both sides whenever the player right clicks while targeting an entity.
     *
     * @param event The event data.
     */
    @SubscribeEvent( priority = EventPriority.NORMAL )
    static void onEntityInteract( PlayerInteractEvent.EntityInteract event ) {
        //if( event.isCanceled() || !(event.getLevel() instanceof ServerLevel level) ) return;
        //TODO Some chunk protection goes here?
    }
    
    /**
     * Called when a block is about to be broken by a player.
     * Canceling this event will prevent the block from being broken.
     *
     * @param event The event data.
     */
    @SubscribeEvent( priority = EventPriority.NORMAL )
    static void onBlockAboutToBreak( BlockEvent.BreakEvent event ) {
        //TODO Some chunk protection goes here?
    }
}