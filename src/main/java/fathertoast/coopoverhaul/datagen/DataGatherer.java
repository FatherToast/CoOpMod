package fathertoast.coopoverhaul.datagen;

import fathertoast.coopoverhaul.common.config.Config;
import fathertoast.coopoverhaul.common.core.CoOpOverhaulMod;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.concurrent.CompletableFuture;

@Mod.EventBusSubscriber( modid = CoOpOverhaulMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD )
public class DataGatherer {
    
    /** Data provider that generates registry entries from supported registry types. */
    private static final RegistrySetBuilder BUILDER = new RegistrySetBuilder();
    //            .add( Registries.DAMAGE_TYPE, DWDamageTypes::bootstrap )
    //            .add( Registries.CONFIGURED_FEATURE, DWConfiguredFeatureProvider::bootstrap )
    //            .add( Registries.PLACED_FEATURE, DWPlacedFeatureProvider::bootstrap );
    
    @SubscribeEvent
    public static void onGatherData( GatherDataEvent event ) {
        // Ensure config is loaded before doing anything.
        // Many common mod lifecycle events are not fired when running data gen.
        Config.initialize();
        
        final DataGenerator generator = event.getGenerator();
        final PackOutput packOutput = generator.getPackOutput();
        final CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();
        final ExistingFileHelper fileHelper = event.getExistingFileHelper();
        
        if( event.includeClient() ) {
            //            generator.addProvider( true, new DWModelProvider( packOutput, fileHelper ) );
            //            generator.addProvider( true, new DWLangProvider( packOutput ) );
        }
        if( event.includeServer() ) {
            //            DatapackBuiltinEntriesProvider builtInProvider =
            //                    generator.addProvider( true, new DatapackBuiltinEntriesProvider( packOutput, lookupProvider, BUILDER, Set.of( CoOpMod.MOD_ID ) ) );
            //
            //            generator.addProvider( true, new DWLootTableProvider( packOutput ) );
            //            generator.addProvider( true, new DWLootModProvider( packOutput ) );
            //
            //            // Tags
            //            DWBlockTagsProvider blockTags =
            //                    generator.addProvider( true, new DWBlockTagsProvider( packOutput, lookupProvider, fileHelper ) );
            //            generator.addProvider( true, new DWItemTagsProvider( packOutput, lookupProvider, blockTags.contentsGetter(), fileHelper ) );
            //            generator.addProvider( true, new DWEntityTypeTagsProvider( packOutput, lookupProvider, fileHelper ) );
            //            generator.addProvider( true, new DWConfiguredFeatureTagsProvider( packOutput, builtInProvider.getRegistryProvider(), fileHelper ) );
            //            generator.addProvider( true, new DWPlacedFeatureTagsProvider( packOutput, builtInProvider.getRegistryProvider(), fileHelper ) );
            //            generator.addProvider( true, new DWDecoyTagsProvider( packOutput, builtInProvider.getRegistryProvider(), fileHelper ) );
            //            generator.addProvider( true, new DWFluidTagsProvider( packOutput, lookupProvider, fileHelper ) );
            //            generator.addProvider( true, new DWDamageTypeTagsProvider( packOutput, lookupProvider, fileHelper ) );
        }
    }
}