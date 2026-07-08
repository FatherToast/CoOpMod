package fathertoast.coopoverhaul.common.event;

import fathertoast.coopoverhaul.common.core.CoOpOverhaulMod;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * Contains and automatically registers all common-side mod events.
 */
@Mod.EventBusSubscriber( modid = CoOpOverhaulMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD )
public final class ModEventHandler {
    
    /**
     * Called after registry events, but before the client- and server-specific setup events.
     *
     * @param event The event data.
     */
    @SubscribeEvent( priority = EventPriority.NORMAL )
    public static void onCommonSetup( FMLCommonSetupEvent event ) {}
    
    /**
     * This event is called to allow each entity type to register its own spawn predicate.
     *
     * @param event The event data.
     */
    @SubscribeEvent( priority = EventPriority.NORMAL )
    public static void onRegisterSpawnPlacement( SpawnPlacementRegisterEvent event ) {}
}