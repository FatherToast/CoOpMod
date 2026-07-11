package fathertoast.coopoverhaul.common.core;

import fathertoast.coopoverhaul.api.common.util.CoOpOverhaulObjects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.Objects;

public final class COSoundEvents {
    
    private static final DeferredRegister<SoundEvent> REGISTRY = DeferredRegister.create( ForgeRegistries.SOUND_EVENTS, CoOpOverhaulMod.MOD_ID );
    
    static {
        register( CoOpOverhaulObjects.SoundEvents.PING_BINK );
        register( CoOpOverhaulObjects.SoundEvents.PING_BINK_LOUD );
        register( CoOpOverhaulObjects.SoundEvents.PING_HOSTILE );
        register( CoOpOverhaulObjects.SoundEvents.PING_HOSTILE_LOUD );
        register( CoOpOverhaulObjects.SoundEvents.PING_BOSS );
        register( CoOpOverhaulObjects.SoundEvents.PING_BOSS_LOUD );
    }
    
    /** Called to register this class. */
    public static void register( IEventBus bus ) { REGISTRY.register( bus ); }
    
    /** Registers a standard sound event to the deferred register. */
    private static void register( RegistryObject<SoundEvent> regObj ) {
        ResourceLocation id = Objects.requireNonNull( regObj.getId() );
        REGISTRY.register( id.getPath(), () -> SoundEvent.createVariableRangeEvent( id ) );
    }
    
    
    private COSoundEvents() {}
}