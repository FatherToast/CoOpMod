package fathertoast.coopmod.common.core;

import fathertoast.coopmod.api.common.util.CoOpModObjects;
import fathertoast.coopmod.common.config.Config;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.Objects;
import java.util.function.Supplier;

public final class CMAttributes {
    
    private static final DeferredRegister<Attribute> REGISTRY = DeferredRegister.create( ForgeRegistries.ATTRIBUTES, CoOpMod.MOD_ID );
    
    static {
        register( CoOpModObjects.Attributes.INSPECTION_RANGE, true,
                () -> 32.0,
                () -> 0.0,
                Config.MAIN.GENERAL.maxInspectRange::get );
    }
    
    /** Called to register this class. */
    public static void register( IEventBus bus ) { REGISTRY.register( bus ); }
    
    /**
     * Registers an attribute with the specified value range to the deferred register.
     *
     * @param sync If true, attribute values will be synced from server to client.
     */
    private static void register( RegistryObject<Attribute> regObj, boolean sync,
                                  double defaultValue, double min, double max ) {
        ResourceLocation id = Objects.requireNonNull( regObj.getId() );
        REGISTRY.register( id.getPath(), () -> new RangedAttribute( descId( id ), defaultValue, min, max ).setSyncable( sync ) );
    }
    
    /**
     * Registers an attribute with the specified value range to the deferred register.
     *
     * @param sync If true, attribute values will be synced from server to client.
     */
    private static void register( RegistryObject<Attribute> regObj, boolean sync,
                                  Supplier<Double> defaultValue, Supplier<Double> min, Supplier<Double> max ) {
        register( regObj, sync, defaultValue.get(), min.get(), max.get() );
    }
    
    /** @return The given resource location as a description ID string. */
    private static String descId( ResourceLocation rl ) {
        return "attribute.name." + rl.getNamespace() + "." + rl.getPath();
    }
    
    
    private CMAttributes() { }
}
