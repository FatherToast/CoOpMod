package fathertoast.coopoverhaul.client.event;


import fathertoast.coopoverhaul.client.config.ClientConfig;
import fathertoast.coopoverhaul.client.coordination.ClientPingHelper;
import fathertoast.coopoverhaul.client.coordination.FindPlayersManager;
import fathertoast.coopoverhaul.client.coordination.InspectManager;
import fathertoast.coopoverhaul.client.vfx.HighlightManager;
import fathertoast.coopoverhaul.common.core.CoOpOverhaulMod;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Contains and automatically registers all client-side forge events.
 */
@Mod.EventBusSubscriber( value = Dist.CLIENT, modid = CoOpOverhaulMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE )
public final class ClientGameEventHandler {
    
    /**
     * Called at the start and end of each client tick.
     *
     * @param event The event data.
     */
    @SubscribeEvent( priority = EventPriority.NORMAL )
    static void onClientTick( TickEvent.ClientTickEvent event ) {
        //        if( event.phase == TickEvent.Phase.START ) {
        //            ChatWidget.onTick();
        //        }
        //        else
        if( event.phase == TickEvent.Phase.END ) {
            ClientPingHelper.onTick();
            FindPlayersManager.onTick();
        }
    }
    
    /**
     * Called at several stages of level rendering.
     * You need to check the event's stage to know which point of the rendering process we are in.
     *
     * @param event The event data.
     */
    @SubscribeEvent( priority = EventPriority.NORMAL )
    static void onRenderLevel( RenderLevelStageEvent event ) {
        if( event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY ) {
            Minecraft client = Minecraft.getInstance();
            if( client.level == null || client.player == null ) return;
            
            // We do this here so that it is always in sync with visuals
            InspectManager.updateTarget( client, client.player, client.level, event.getPartialTick() );
            
            HighlightManager.renderBlockOutlines( client, client.level, event.getLevelRenderer(), event.getPoseStack(),
                    event.getProjectionMatrix(), event.getRenderTick(), event.getPartialTick(), event.getCamera(), event.getFrustum() );
        }
        else if( event.getStage() == RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES ) {
            Minecraft client = Minecraft.getInstance();
            if( client.level == null || client.player == null ) return;
            
            if( ClientConfig.PREFS.INSPECT.nameplateSize.get() > 0.0 ) {
                HighlightManager.renderNameplates( client, client.level, client.player, event.getLevelRenderer(), event.getPoseStack(),
                        event.getProjectionMatrix(), event.getRenderTick(), event.getPartialTick(), event.getCamera(), event.getFrustum() );
            }
        }
    }
    
    private static boolean skipNameTag;
    
    public static void skipNextNameTag() { skipNameTag = true; }
    
    /**
     * Fired before an entity renderer renders an entity name.
     * The event cannot be canceled, but has a result.
     *
     * @param event The event data.
     */
    @SubscribeEvent( priority = EventPriority.HIGHEST )
    static void onRenderNameTag( RenderNameTagEvent event ) {
        if( skipNameTag && event.getEntity() instanceof Player ) {
            skipNameTag = false;
            event.setResult( Event.Result.DENY );
        }
        else if( HighlightManager.hasNameplate( event.getEntity() ) ) {
            // Disable vanilla name tag if we are rendering one ourselves so we don't see duplicate names
            event.setResult( Event.Result.DENY );
        }
    }
    
    //    /**
    //     * Fired after the screen's overridable initialization method is called.
    //     *
    //     * @param event The event data.
    //     */
    //    @SubscribeEvent( priority = EventPriority.LOWEST )
    //    static void onPostInitScreen( ScreenEvent.Init.Post event ) {
    //        // The idea here is to only allow opening the chat over non-chat screens while in game
    //        if( Minecraft.getInstance().player != null && !(event.getScreen() instanceof ChatScreen) ) {
    //            event.addListener( new ChatWidget( event.getScreen() ) );
    //        }
    //    }
    
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