package fathertoast.coopmod.client;

import fathertoast.coopmod.common.core.CoOpMod;
import fathertoast.crust.api.config.common.AbstractConfigCategory;
import fathertoast.crust.api.config.common.AbstractConfigFile;
import fathertoast.crust.api.config.common.ConfigManager;
import fathertoast.crust.api.config.common.field.BooleanField;
import fathertoast.crust.api.config.common.field.ColorIntField;
import fathertoast.crust.api.config.common.field.DoubleField;
import fathertoast.crust.api.config.common.field.EnumField;
import fathertoast.crust.api.config.common.field.collection.BlockStateMapField;
import fathertoast.crust.api.config.common.field.collection.EntityMapField;
import fathertoast.crust.api.config.common.value.collection.BlockStateMap;
import fathertoast.crust.api.config.common.value.collection.EntityMap;
import fathertoast.crust.api.config.common.value.collection.value.IntValueCodec;
import fathertoast.crust.api.util.BlockStatePropertyMap;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;

/**
 * Used as the sole hub for all client-side config access from outside the config package.
 * <p>
 * Contains references to all client-side config files used in this mod, which in turn provide direct
 * 'getter' access to each configurable value.
 * <p>
 * Also, this contains the main client-side config spec itself.
 */
public class ClientConfig extends AbstractConfigFile {
    
    public static ClientConfig PREFS;
    
    /** Performs loading of configs in this mod. Added to deferred work queue at common setup. */
    public static void initialize() {
        ConfigManager manager = ConfigManager.getRequired( CoOpMod.MOD_ID );
        
        PREFS = new ClientConfig( manager, "_client_prefs" );
        PREFS.SPEC.initialize();
    }
    
    
    // ---- Main Client Config Impl ---- //
    
    public final Inspection INSPECTION;
    public final HighlightColors HIGHLIGHT_COLORS;
    
    /** Builds the config spec that should be used for this config. */
    ClientConfig( ConfigManager manager, String fileName ) {
        super( manager, fileName,
                "This config contains personal preference settings." );
        
        INSPECTION = new Inspection( this );
        HIGHLIGHT_COLORS = new HighlightColors( this );
    }
    
    public static class Inspection extends AbstractConfigCategory<ClientConfig> {
        
        public final DoubleField range;
        
        public final EnumField<KeyBindingEvents.Mode> keyMode;
        
        Inspection( ClientConfig parent ) {
            super( parent, "inspect",
                    "Options to customize the 'inspect' function, including pings and ping highlights." );
            
            range = SPEC.define( new DoubleField( "range",
                    3.4e38, DoubleField.Range.NON_NEGATIVE,
                    "How far you can inspect blocks/entities from, in blocks.",
                    "Leaving this at a very high value effectively just sets your range to the max allowed by the " +
                            "server or to your render distance, whichever is lower.",
                    "You will at least be able to inspect blocks/entities you can physically reach, unless your " +
                            "inspect range is 0, which completely disables the inspect feature." ) );
            // We have to update the calculated 'effective inspection range'
            SPEC.callback( InspectManager::updateInspectRange );
            
            SPEC.newLine();
            
            keyMode = SPEC.define( new EnumField<>( "key_mode", KeyBindingEvents.Mode.HOLD,
                    "How the inspect key bind behaves. The key itself is bound in the game's options " +
                            "(Options > Controls > Key Binds)." ) );
            // Refresh the state of the key binding
            SPEC.callback( KeyBindingEvents::updateKeyMode );
        }
    }
    
    @SuppressWarnings( "UnstableApiUsage" )
    public static class HighlightColors extends AbstractConfigCategory<ClientConfig> {
        
        public final ColorIntField defaultColor;
        
        public final BooleanField inspectUsesDefault;
        public final BooleanField playerColors;//TODO implement
        
        public final EntityMapField<Integer> entityColors;
        
        public final BlockStateMapField<Integer> blockColors;
        
        HighlightColors( ClientConfig parent ) {
            super( parent, "highlight_colors",
                    "Options to customize the colors for ping and inspect highlights (their visual outlines)." );
            IntValueCodec colorCodec = IntValueCodec.of( 0x000000, 0x000000, 0xFFFFFF );// Until we implement a color value codec in Crust; too lazy to do here
            
            defaultColor = SPEC.define( new ColorIntField( "default", 0xFFFF00, false,
                    "The color used for all highlights not specified in the following fields." ) );
            
            SPEC.newLine();
            
            inspectUsesDefault = SPEC.define( new BooleanField( "inspect_always_uses_default", false,
                    "When enabled, your current inspect target highlight will always use the default color " +
                            "specified above. Otherwise, it will follow these settings." ) );
            playerColors = SPEC.define( new BooleanField( "other_player_pings", true,
                    "When enabled, this causes other players' pings to match their personal color. " +
                            "Otherwise, their pings will follow these settings." ) );
            
            SPEC.newLine();
            
            entityColors = SPEC.define( new EntityMapField<>( "entities", new EntityMap.Builder<>( colorCodec )
                    .put( EntityType.CREEPER, 0x00FF00 )
                    .buildWithDefault( 0xFF0000 ),
                    "The colors used for entity highlights." ) );
            
            SPEC.newLine();
            
            blockColors = SPEC.define( new BlockStateMapField<>( "blocks", new BlockStateMap.Builder<>( colorCodec )
                    .putWildcard( "minecraft", "nether", BlockStatePropertyMap.EMPTY, 0xFF00FF )
                    .put( Blocks.INFESTED_STONE, BlockStatePropertyMap.EMPTY, 0xFF0000 )
                    .buildWithDefault( 0x00FF00 ),
                    "The colors used for entity highlights." ) );
        }
    }
}