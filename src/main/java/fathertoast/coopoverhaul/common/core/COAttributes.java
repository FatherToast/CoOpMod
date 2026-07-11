package fathertoast.coopoverhaul.common.core;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.Objects;

import static fathertoast.coopoverhaul.api.common.util.CoOpOverhaulObjects.Attributes.INSPECTION_RANGE;

public final class COAttributes {
    
    private static final DeferredRegister<Attribute> REGISTRY = DeferredRegister.create( ForgeRegistries.ATTRIBUTES, CoOpOverhaulMod.MOD_ID );
    
    static {
        register( INSPECTION_RANGE, true, 0.0, 0.0, 2048.0 );
    }
    
    /** Called to register this class. */
    public static void register( IEventBus bus ) { REGISTRY.register( bus ); }
    
    /**
     * Registers an attribute with the specified value range to the deferred register.
     *
     * @param sync True if the attribute should be synced to clients.
     */
    @SuppressWarnings( "SameParameterValue" )
    private static void register( RegistryObject<Attribute> regObj, boolean sync,
                                  double defaultValue, double min, double max ) {
        ResourceLocation id = Objects.requireNonNull( regObj.getId() );
        REGISTRY.register( id.getPath(), () -> new RangedAttribute( descId( id ), defaultValue, min, max ).setSyncable( sync ) );
    }
    
    /** @return The given resource location as a description ID string. */
    private static String descId( ResourceLocation rl ) {
        return "attribute.name." + rl.getNamespace() + "." + rl.getPath();
    }
    
    
    private COAttributes() {}
}