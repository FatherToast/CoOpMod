package fathertoast.coopmod.api.common.util;

import com.mojang.brigadier.arguments.ArgumentType;
import fathertoast.coopmod.common.core.CoOpMod;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryObject;

/**
 * This helper class contains references/getters for all registry objects provided by the Co-Op Mod.
 */
public final class CoOpModObjects {
    
    /** Sound event types. */
    public interface SoundEvents {
        RegistryObject<SoundEvent> PING_BINK = sound( "ping.bink" );
        RegistryObject<SoundEvent> PING_BINK_LOUD = sound( "ping.bink_loud" );
        RegistryObject<SoundEvent> PING_HOSTILE = sound( "ping.hostile" );
        RegistryObject<SoundEvent> PING_BOSS = sound( "ping.boss" );
    }
    
    // No command args yet
    //    /** Command argument types. */
    //    public interface CommandArguments {
    //        RegistryObject<ArgumentTypeInfo<ArgumentType<PortalBuilder>, ?>> PORTAL_TYPE = cmdArg( "portal_type" );
    //    }
    
    
    // ---- Internal Methods ---- //
    
    /** @return An object holder for a sound event type. */
    private static RegistryObject<SoundEvent> sound( String name ) { return ro( name, ForgeRegistries.SOUND_EVENTS ); }
    
    //    /** @return An object holder for a command argument type. */
    //    private static <T extends ArgumentType<?>> RegistryObject<ArgumentTypeInfo<T, ?>> cmdArg( String name ) { return ro( name, ForgeRegistries.COMMAND_ARGUMENT_TYPES ); }
    
    /** @return An object holder for a Forge registry object. */
    private static <R, T extends R> RegistryObject<T> ro( String name, IForgeRegistry<R> reg ) {
        return RegistryObject.create( rl( name ), reg );
    }
    
    /** @return An object holder for a custom registry object. */
    private static <T> RegistryObject<T> ro( String name, ResourceKey<? extends Registry<T>> registryKey ) {
        return RegistryObject.createOptional( rl( name ), registryKey, CoOpMod.MOD_ID );
    }
    
    /** @return A resource location. */
    private static ResourceLocation rl( String path ) {
        return ResourceLocation.fromNamespaceAndPath( CoOpMod.MOD_ID, path );
    }
    
    
    private CoOpModObjects() {}
}