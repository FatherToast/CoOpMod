package fathertoast.coopmod.common.event;


import fathertoast.coopmod.common.config.Config;
import fathertoast.coopmod.common.core.CoOpMod;
import fathertoast.coopmod.common.coordination.PingManager;
import fathertoast.crust.api.util.OnClient;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ChunkDataEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Contains and automatically registers all common-side forge events.
 */
@Mod.EventBusSubscriber( modid = CoOpMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE )
public final class GameEventHandler {
    
    @OnClient
    public static int localPingCooldown;
    
    /**
     * Called when a player logs in.
     *
     * @param event The event data.
     */
    @SubscribeEvent( priority = EventPriority.NORMAL )
    static void onPlayerLoggedIn( PlayerEvent.PlayerLoggedInEvent event ) {
        if( event.getEntity() instanceof ServerPlayer player ) {
            Config.MAIN.sendSyncPacket( player );
        }
    }
    
    /**
     * Called at the start and end of each level tick, on both the client and server side.
     *
     * @param event The event data.
     */
    @SubscribeEvent( priority = EventPriority.NORMAL )
    static void onLevelTick( TickEvent.LevelTickEvent event ) {
        if( event.phase == TickEvent.Phase.END ) {
            PingManager manager = PingManager.get( event.level );
            if( event.level.isClientSide() ) {
                localPingCooldown = Math.max( 0, localPingCooldown - 1 );
            }
            
            // Remove expired and OBE pings
            long gameTime = event.level.getGameTime();
            manager.getEntityPings().removeIf( ( ping ) ->
                    ping.getValue().isExpired( gameTime ) || ping.getValue().isRemoved( event.level, ping.getKey() ) );
            manager.getBlockPings().removeIf( ( ping ) ->
                    ping.getValue().isExpired( gameTime ) || ping.getValue().isRemoved( event.level, ping.getKey() ) );
        }
    }
    
    /**
     * Called at the start of {@link LivingEntity#hurt(DamageSource, float)} before all damage calculations.
     * If the event is canceled, no damage is dealt.
     *
     * @param event The event data.
     */
    @SubscribeEvent( priority = EventPriority.NORMAL )
    static void onLivingAttack( LivingAttackEvent event ) {
        //TODO Friendly fire protection goes here?
    }
    
    /**
     * Called when a block is placed. If canceled, the block will not be placed.
     *
     * @param event The event data.
     */
    @SubscribeEvent( priority = EventPriority.NORMAL )
    static void onEntityPlaceBlock( BlockEvent.EntityPlaceEvent event ) {
        //if( event.isCanceled() || !(event.getLevel() instanceof ServerLevel level) ) return;
        //TODO Some chunk protection goes here?
    }
    
    /**
     * Called whenever an entity is spawned during {@link Level#addFreshEntity(Entity)} through
     * {@link net.minecraft.world.level.entity.PersistentEntitySectionManager#addEntity(EntityAccess, boolean)}.
     * If the event is canceled, the entity will not be spawned.
     * <p>
     * Note: This event may be called before the underlying chunk is fully loaded; you will cause chunk
     * loading deadlocks if you do not delay world interactions!
     *
     * @param event The event data.
     */
    @SuppressWarnings( "JavadocReference" )
    @SubscribeEvent( priority = EventPriority.NORMAL )
    static void onEntityJoinLevel( EntityJoinLevelEvent event ) {
        if( event.isCanceled() ) return;
        
        if( event.getLevel() instanceof ServerLevel level && event.getEntity() instanceof ServerPlayer player ) {
            PingManager.onPlayerJoinServerLevel( level, player );
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
     * <p>
     * Used to trigger falling dripstone.
     *
     * @param event The event data.
     */
    @SubscribeEvent( priority = EventPriority.NORMAL )
    static void onBlockAboutToBreak( BlockEvent.BreakEvent event ) {
        //TODO Some chunk protection goes here?
    }
    
    /**
     * Called during chunk loading (async).
     *
     * @param event The event data.
     */
    @SubscribeEvent( priority = EventPriority.NORMAL )
    static void onChunkDataLoad( ChunkDataEvent.Load event ) {
        //TODO load chunk data
    }
    
    /**
     * Called during chunk saving.
     *
     * @param event The event data.
     */
    @SubscribeEvent( priority = EventPriority.NORMAL )
    static void onChunkDataSave( ChunkDataEvent.Save event ) {
        //TODO save chunk data
    }
    
    /**
     * Called during chunk unloading.
     *
     * @param event The event data.
     */
    @SubscribeEvent( priority = EventPriority.NORMAL )
    static void onChunkUnload( ChunkEvent.Unload event ) {
        if( !event.getLevel().isClientSide() ) {
            //TODO forget chunk data
        }
    }
    
    /**
     * Called during chunk unloading.
     *
     * @param event The event data.
     */
    @SubscribeEvent( priority = EventPriority.NORMAL )
    static void onLevelUnload( LevelEvent.Unload event ) {
        if( !event.getLevel().isClientSide() ) {
            //TODO forget all chunk data
        }
    }
}