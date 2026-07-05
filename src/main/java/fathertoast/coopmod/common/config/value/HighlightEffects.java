package fathertoast.coopmod.common.config.value;

import fathertoast.coopmod.common.config.ColorIntValueCodec;
import fathertoast.crust.api.config.common.value.collection.value.MultiValueCodec;
import fathertoast.crust.api.config.common.value.collection.value.StringValueCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@SuppressWarnings( "UnstableApiUsage" )
public class HighlightEffects extends MultiValueCodec<HighlightEffects> {
    
    /** The highlight effects codec "singleton". */
    public static final HighlightEffects CODEC = new HighlightEffects();
    
    /** The highlight color. */
    public final SubValue<Integer> color = subValue( ColorIntValueCodec.NO_ALPHA,
            ColorIntValueCodec.NO_ALPHA.getFormat( "Duration" ) );
    
    /** The effect amplifier (0 = I, 1 = II, etc.). */
    public final SubValue<String> pingSoundId = subValue( StringValueCodec.of( ( val ) -> ResourceLocation.isValidResourceLocation( val )
                    && ForgeRegistries.SOUND_EVENTS.containsKey( ResourceLocation.parse( val ) ) ),
            StringValueCodec.RES_LOC.getFormat() );
    
    
    /** A constructor used to define default values. */
    public HighlightEffects( int col, SoundEvent sound ) {
        color.set( col );
        // noinspection ConstantConditions
        pingSoundId.set( ForgeRegistries.SOUND_EVENTS.getKey( sound ).toString() );
    }
    
    /** A constructor used to define default values. */
    public HighlightEffects( int col, RegistryObject<? extends SoundEvent> regObj ) {
        color.set( col );
        // noinspection ConstantConditions
        pingSoundId.set( regObj.getId().toString() );
    }
    
    /** The no-args constructor used to create the codec "singleton" and for value loading. */
    public HighlightEffects() { }
}
