package fathertoast.coopmod.test.common;

import fathertoast.coopmod.common.core.CoOpMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber( modid = CoOpMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD )
public class TestCommonRegister {
    
    @SubscribeEvent
    static void onConstructMod( FMLConstructModEvent event ) {
        IEventBus eventBus = CoOpMod.INSTANCE.CONTAINER.getEventBus();
        TAB_REGISTRY.register( eventBus );
    }
    
    // ---- Creative Mode Tabs ---- //
    
    public static final DeferredRegister<CreativeModeTab> TAB_REGISTRY = DeferredRegister.create( Registries.CREATIVE_MODE_TAB, CoOpMod.MOD_ID );
    
    public static final RegistryObject<CreativeModeTab> TEST_TAB = TAB_REGISTRY.register( "coopmod_test",
            () -> CreativeModeTab.builder().icon( () -> new ItemStack( Blocks.PLAYER_HEAD ) )
                    .title( Component.literal( "Co-Op Mod Test" ) ).build() );
}