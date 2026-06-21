package fathertoast.coopmod.common.event;


import fathertoast.coopmod.common.core.CoOpMod;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Contains and automatically registers all needed forge events.
 */
@Mod.EventBusSubscriber( modid = CoOpMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE )
public final class GameEventHandler {
    
    /**
     * Called at the start of {@link LivingEntity#hurt(DamageSource, float)} before all damage calculations.
     * If the event is canceled, no damage is dealt.
     *
     * @param event The event data.
     */
    @SubscribeEvent( priority = EventPriority.NORMAL )
    public static void onLivingAttack( LivingAttackEvent event ) {
        //TODO Friendly fire protection goes here?
    }
    
    /**
     * Called when a block is placed. If canceled, the block will not be placed.
     *
     * @param event The event data.
     */
    @SubscribeEvent( priority = EventPriority.NORMAL )
    public static void onEntityPlaceBlock( BlockEvent.EntityPlaceEvent event ) {
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
    public static void onEntityJoinLevel( EntityJoinLevelEvent event ) {
        //if( event.isCanceled() || !(event.getLevel() instanceof ServerLevel level) ) return;
        // Probably will need this
    }
    
    /**
     * Called when a block is about to be broken by a player.
     * Canceling this event will prevent the block from being broken.
     *
     * @param event The event data.
     */
    @SubscribeEvent( priority = EventPriority.NORMAL )
    public static void onBlockBreak( BlockEvent.BreakEvent event ) {
        //if( event.isCanceled() || !(event.getLevel() instanceof ServerLevel level) ) return;
        //TODO Some chunk protection goes here?
    }
    
    /**
     * This event is fired on both sides whenever the player right clicks while targeting a block.
     *
     * @param event The event data.
     */
    @SubscribeEvent( priority = EventPriority.NORMAL )
    public static void onRightClickContainer( PlayerInteractEvent.RightClickBlock event ) {
        //TODO Some chunk protection goes here?
    }
    
    /**
     * This event is fired on both sides whenever the player right clicks while targeting an entity.
     *
     * @param event The event data.
     */
    @SubscribeEvent( priority = EventPriority.NORMAL )
    public static void onEntityInteract( PlayerInteractEvent.EntityInteract event ) {
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
    public static void onBlockAboutToBreak( BlockEvent.BreakEvent event ) {
        //TODO Some chunk protection goes here?
    }
}