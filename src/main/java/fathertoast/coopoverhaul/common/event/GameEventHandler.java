package fathertoast.coopoverhaul.common.event;


import fathertoast.coopoverhaul.common.config.Config;
import fathertoast.coopoverhaul.common.coordination.PingManager;
import fathertoast.coopoverhaul.common.coordination.ReviveManager;
import fathertoast.coopoverhaul.common.core.CoOpOverhaulMod;
import fathertoast.coopoverhaul.common.protection.FriendlyFireHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ChunkDataEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Contains and automatically registers all common-side forge events.
 */
@Mod.EventBusSubscriber( modid = CoOpOverhaulMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE )
public final class GameEventHandler {
    /**
     * Called when a player logs in.
     *
     * @param event The event data.
     */
    @SubscribeEvent( priority = EventPriority.NORMAL )
    public static void onPlayerLoggedIn( PlayerEvent.PlayerLoggedInEvent event ) {
        if( event.getEntity() instanceof ServerPlayer player ) {
            // Send config sync packet to client
            Config.MAIN.sendSyncPacket( player );
            
            // Add the base inspection range modifier to the player
            AttributeInstance attributeInst = player.getAttribute( CoOpModObjects.Attributes.INSPECTION_RANGE.get() );
            if( player.getAttributes().hasModifier( CoOpModObjects.Attributes.INSPECTION_RANGE.get(), AttributeModUtil.getBaseInspectionRangeMod().getId() ) ) {
                // noinspection ConstantConditions
                attributeInst.removePermanentModifier( AttributeModUtil.getBaseInspectionRangeMod().getId() );
            }
            attributeInst.addPermanentModifier( AttributeModUtil.getBaseInspectionRangeMod() );
        }
    }
    
    /**
     * Called when the attributes for an item stack are being calculated.
     *
     * @param event The event data.
     */
    @SubscribeEvent( priority = EventPriority.NORMAL )
    public static void onItemAttributeModifier( ItemAttributeModifierEvent event ) {
        final Item item = event.getItemStack().getItem();
        final EquipmentSlot slot = event.getSlotType();
        
        // Spyglass
        if( item == Items.SPYGLASS && (slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND) ) {
            if( !event.getModifiers().containsValue( AttributeModUtil.getSpyglassInspectionRangeMod() ) ) {
                event.addModifier( CoOpModObjects.Attributes.INSPECTION_RANGE.get(), AttributeModUtil.getSpyglassInspectionRangeMod() );
            }
        }
    }
    
    /**
     * Called at the start and end of each level tick, on both the client and server side.
     *
     * @param event The event data.
     */
    @SubscribeEvent( priority = EventPriority.NORMAL )
    public static void onLevelTick( TickEvent.LevelTickEvent event ) {
        if( event.phase == TickEvent.Phase.END ) {
            // Remove expired and OBE pings
            PingManager manager = PingManager.get( event.level );
            long gameTime = event.level.getGameTime();
            manager.getEntityPings().removeIf( ( ping ) ->
                    ping.getValue().isExpired( gameTime ) || ping.getValue().isRemoved( event.level, ping.getKey() ) );
            manager.getBlockPings().removeIf( ( ping ) ->
                    ping.getValue().isExpired( gameTime ) || ping.getValue().isRemoved( event.level, ping.getKey() ) );
        }
    }
    
    /**
     * Called immediately before shutting down, on the dedicated server, and before returning
     * to the main menu on the client.
     *
     * @param event The event data.
     */
    @SubscribeEvent( priority = EventPriority.NORMAL )
    public static void onServerStopped( ServerStoppedEvent event ) {
        PingManager.reset();
    }
    
    /**
     * Called at the start of {@link LivingEntity#hurt(DamageSource, float)} before all damage calculations.
     * If the event is canceled, no damage is dealt.
     *
     * @param event The event data.
     */
    @SubscribeEvent( priority = EventPriority.NORMAL )
    public static void onLivingAttack( LivingAttackEvent event ) {
        LivingEntity entity = event.getEntity();
        if( !entity.level().isClientSide() ) {
            if( entity instanceof Player player && FriendlyFireHelper.shouldCancelDamage( player, event.getSource() ) ) {
                event.setCanceled( true );
            }
        }
    }
    
    /**
     * Called when damage is being dealt after block damage reduction, right before armor and enchantment
     * damage reduction is calculated.
     * If the event is canceled or damage is set to 0, no damage is dealt, but some effects like blocking
     * have already happened and helmet damage will happen anyway.
     *
     * @param event The event data.
     */
    @SubscribeEvent( priority = EventPriority.NORMAL )
    public static void onLivingHurt( LivingHurtEvent event ) {
        LivingEntity entity = event.getEntity();
        if( !entity.level().isClientSide() ) {
            if( entity instanceof Player player && FriendlyFireHelper.isFriendlyFire( player, event.getSource() ) ) {
                event.setAmount( event.getAmount() * Config.MAIN.GENERAL.friendlyFireMulti.getFloat() );
            }
        }
    }
    
    //    /**
    //     * Called when an entity dies. If canceled, the entity does not die.
    //     *
    //     * @param event The event data.
    //     */
    //    @SubscribeEvent( priority = EventPriority.LOWEST )
    //    public static void onLivingDeath( LivingDeathEvent event ) {
    //        if( event.isCanceled() || event.getEntity().level().isClientSide() ) return;
    //
    //        if( Config.MAIN.GENERAL.reviveEnabled.get() && event.getEntity() instanceof ServerPlayer player ) {
    //            event.setCanceled( true );
    //            ReviveManager.downPlayer( player, event.getSource() );
    //        }
    //    }
    
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
    
    /**
     * Called during chunk loading (async).
     *
     * @param event The event data.
     */
    @SubscribeEvent( priority = EventPriority.NORMAL )
    public static void onChunkDataLoad( ChunkDataEvent.Load event ) {
        //TODO load chunk data
    }
    
    /**
     * Called during chunk saving.
     *
     * @param event The event data.
     */
    @SubscribeEvent( priority = EventPriority.NORMAL )
    public static void onChunkDataSave( ChunkDataEvent.Save event ) {
        //TODO save chunk data
    }
    
    /**
     * Called during chunk unloading.
     *
     * @param event The event data.
     */
    @SubscribeEvent( priority = EventPriority.NORMAL )
    public static void onChunkUnload( ChunkEvent.Unload event ) {
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
    public static void onLevelUnload( LevelEvent.Unload event ) {
        if( !event.getLevel().isClientSide() ) {
            //TODO forget all chunk data
        }
    }
}