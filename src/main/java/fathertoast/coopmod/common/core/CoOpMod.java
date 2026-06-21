package fathertoast.coopmod.common.core;

import fathertoast.coopmod.common.config.Config;
import fathertoast.coopmod.common.network.PacketHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DeferredWorkQueue;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingStage;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * The core of the mod. Contains basic info about the mod, initializes configs, and hooks into FML.
 */
@Mod( CoOpMod.MOD_ID )
public final class CoOpMod {
    
    /* Features list:
     * (KEY: - = complete in current version, o = incomplete feature from previous version,
     *       + = incomplete new feature, ? = feature to consider adding)
     *  - general
     *      - nothing, yet
     *
     * Possible future additions:
     *  - tbd
     */
    
    /** The mod ID used by this mod. */
    public static final String MOD_ID = "coopmod";
    
    /** The logger used by this mod. */
    public static final Logger LOG = LogManager.getLogger( MOD_ID );
    
    /** Packet handler instance */
    public PacketHandler packetHandler = new PacketHandler();
    
    
    public CoOpMod( FMLJavaModLoadingContext context ) {
        IEventBus eventBus = context.getModEventBus();
        
        packetHandler.registerMessages();
        
        eventBus.addListener( this::onCommonSetup );
        
        //        DWBlocks.REGISTRY.register( eventBus );
        //        DWItems.REGISTRY.register( eventBus );
        //        DWCreativeModeTabs.REGISTRY.register( eventBus );
        //        DWEntities.REGISTRY.register( eventBus );
        //        DWSoundEvents.REGISTRY.register( eventBus );
        //        DWBlockEntities.REGISTRY.register( eventBus );
        //        DWLootModifiers.REGISTRY.register( eventBus );
        //        DWBiomeModifiers.REGISTRY.register( eventBus );
        //        DWFishingPranks.REGISTRY.register( eventBus );
        //        DWDecoyTypes.REGISTRY.register( eventBus );
        //        DWFluids.REGISTRY.register( eventBus );
        //        DWFluids.TYPE_REGISTRY.register( eventBus );
        
        Config.initializeEarly();
        DeferredWorkQueue.lookup( Optional.of( ModLoadingStage.COMMON_SETUP ) ).ifPresent(
                ( workQueue ) -> workQueue.enqueueWork( ModList.get().getModContainerById( MOD_ID ).orElseThrow(),
                        Config::initialize )
        );
        
        //        DWFieldProviders.register( eventBus );
        //        DWFeatures.REGISTRY.register( eventBus );
        //        DWPlacementTypes.REGISTRY.register( eventBus );
    }
    
    public void onCommonSetup( FMLCommonSetupEvent event ) {
        event.enqueueWork( () -> {
            //            DWFluids.registerFluidInteractions();
            //            DWDispenserBehavior.register();
        } );
    }
    
    /** @return A ResourceLocation with the mod's modid. */
    public static ResourceLocation rl( String path ) { return ResourceLocation.fromNamespaceAndPath( MOD_ID, path ); }
    
    public static String logPrefix( Class<?> clazz ) {
        return "[" + MOD_ID + "/" + clazz.getSimpleName() + "] ";
    }
    
    /** @return Returns the resource location as a string, or "null" if it is null. */
    public static String toString( @Nullable ResourceLocation res ) { return res == null ? "null" : res.toString(); }
}