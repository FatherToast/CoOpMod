package fathertoast.coopmod.common.event;

import fathertoast.coopmod.api.common.util.CoOpModObjects;
import fathertoast.coopmod.common.core.CoOpMod;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * Contains and automatically registers all common-side mod events.
 */
@Mod.EventBusSubscriber( modid = CoOpMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD )
public final class ModEventHandler {
    
    /**
     * Called after registry events, but before the client- and server-specific setup events.
     *
     * @param event The event data.
     */
    @SubscribeEvent
    public static void onCommonSetup( FMLCommonSetupEvent event ) { }
    
    /**
     * Called when other mods can safely add attributes to existing entity types.
     *
     * @param event The event data.
     */
    @SubscribeEvent
    public static void onAttributeModification( EntityAttributeModificationEvent event ) {
        event.add( EntityType.PLAYER, CoOpModObjects.Attributes.INSPECTION_RANGE.get() );
    }
    
    /**
     * This event is called to allow each entity type to register its own spawn predicate.
     *
     * @param event The event data.
     */
    @SubscribeEvent
    public static void onRegisterSpawnPlacement( SpawnPlacementRegisterEvent event ) { }
}