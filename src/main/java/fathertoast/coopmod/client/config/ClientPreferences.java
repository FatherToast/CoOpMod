package fathertoast.coopmod.client.config;

import fathertoast.coopmod.client.coordination.FindPlayersManager;
import fathertoast.coopmod.client.coordination.InspectManager;
import fathertoast.coopmod.client.event.KeyBindingEvents;
import fathertoast.coopmod.common.config.ColorIntValueCodec;
import fathertoast.crust.api.config.common.AbstractConfigCategory;
import fathertoast.crust.api.config.common.AbstractConfigFile;
import fathertoast.crust.api.config.common.ConfigManager;
import fathertoast.crust.api.config.common.field.*;
import fathertoast.crust.api.config.common.field.collection.BlockStateMapField;
import fathertoast.crust.api.config.common.field.collection.EntityMapField;
import fathertoast.crust.api.config.common.file.TomlHelper;
import fathertoast.crust.api.config.common.value.collection.BlockStateMap;
import fathertoast.crust.api.config.common.value.collection.EntityMap;
import fathertoast.crust.api.util.BlockStatePropertyMap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

import static fathertoast.coopmod.client.event.KeyBindingEvents.Mode.*;
import static fathertoast.coopmod.client.event.KeyBindingEvents.Mode.TAP;

public class ClientPreferences extends AbstractConfigFile {
    
    public final Inspection INSPECTION;
    public final PlayerFinder PLAYER_FINDER;
    public final HighlightColors HIGHLIGHT_COLORS;
    
    /** Builds the config spec that should be used for this config. */
    ClientPreferences( ConfigManager manager, String fileName ) {
        super( manager, fileName,
                "This config contains personal preference settings." );
        
        INSPECTION = new Inspection( this );
        PLAYER_FINDER = new PlayerFinder( this );
        HIGHLIGHT_COLORS = new HighlightColors( this );
        
        // Refresh the state of key bindings
        SPEC.callback( KeyBindingEvents::updateKeyMode );
    }
    
    public static class Inspection extends AbstractConfigCategory<ClientPreferences> {
        
        public final DoubleField range;
        
        public final EnumField<KeyBindingEvents.Mode> keyMode;
        
        Inspection( ClientPreferences parent ) {
            super( parent, "inspect",
                    "Options to customize the 'inspect' and 'ping' functions." );
            
            range = SPEC.define( new DoubleField( "range",
                    3.4e38, DoubleField.Range.NON_NEGATIVE,
                    "How far you can inspect and ping blocks/entities from, in blocks.",
                    "Leaving this at a very high value effectively just sets your range to the max allowed by the " +
                            "server or to your render distance, whichever is lower." ) );
            SPEC.callback( InspectManager::updateRange );
            
            SPEC.newLine();
            
            keyMode = SPEC.define( new EnumField<>( "key_mode", HOLD, KeyBindingEvents.MODES_NO_TAP,
                    "How the inspect key bind behaves. The key itself is bound in the game's options " +
                            "(Options > Controls > Key Binds)." ) );
        }
    }
    
    public static class PlayerFinder extends AbstractConfigCategory<ClientPreferences> {
        
        public final DoubleField range;
        
        public final EnumField<KeyBindingEvents.Mode> keyMode;
        public final IntField tapDuration;
        
        PlayerFinder( ClientPreferences parent ) {
            super( parent, "player_finder",
                    "Options to customize the 'find players' function." );
            
            range = SPEC.define( new DoubleField( "range",
                    3.4e38, DoubleField.Range.NON_NEGATIVE,
                    "How far the 'find players' function can highlight friendly players, in blocks.",
                    "Leaving this at a very high value effectively just sets your range to the max allowed by the " +
                            "server or to your render distance, whichever is lower." ) );
            SPEC.callback( FindPlayersManager::updateRange );
            
            SPEC.newLine();
            
            keyMode = SPEC.define( new EnumField<>( "key_mode", TAP,
                    "How the find players key bind behaves. The key itself is bound in the game's options " +
                            "(Options > Controls > Key Binds)." ) );
            tapDuration = SPEC.define( new IntField( "tap_duration",
                    100, IntField.Range.NON_NEGATIVE,
                    "If the key mode is set to \"" + TomlHelper.toLiteral( TAP ) +
                            "\", this is the time, in ticks, that player finding will be on for when the keybind is " +
                            "pressed. (20 ticks = 1 second)." ) );
        }
    }
    
    @SuppressWarnings( "UnstableApiUsage" )
    public static class HighlightColors extends AbstractConfigCategory<ClientPreferences> {
        
        public final ColorIntField defaultColor;
        
        public final BooleanField playerColors;
        
        public final EntityMapField<Integer> entityColors;
        
        public final BlockStateMapField<Integer> blockColors;
        
        HighlightColors( ClientPreferences parent ) {
            super( parent, "highlight_colors",
                    "Options to customize the colors for ping and inspect highlights (their visual outlines)." );
            
            defaultColor = SPEC.define( new ColorIntField( "global_default", 0xFFFF00, false,
                    "The color used for all highlights not specified in the following fields. Note that " +
                            "when the settings below are all at their default values, this color will never be used." ) );
            
            SPEC.newLine();//TODO add in some auto-coloring options for players, personal colors, and team colors (note team colors currently override all settings)
            
            playerColors = SPEC.define( new BooleanField( "player_colored_pings", false,
                    "When enabled, this causes other players' pings to match their personal color. " +
                            "Otherwise, their ping colors will follow the other settings." ) );
            
            SPEC.newLine();
            
            var builder = new EntityMap.Builder<>( ColorIntValueCodec.NO_ALPHA );
            for( EntityType<?> entityType : ForgeRegistries.ENTITY_TYPES.getValues() ) {
                switch( entityType.getCategory() ) { // TODO update to use extends super in higher Crust ver, maybe also add auto-color options
                    case MONSTER -> {} // Skip so they get the default color
                    case MISC -> builder.put( entityType, 0xFFFF00 );
                    default -> builder.put( entityType, 0x00FF00 );
                }
            }
            entityColors = SPEC.define( new EntityMapField<>( "entities",
                    builder.buildWithDefault( 0xFF0000 ),
                    "The colors used for entity highlights. If no default color is specified in this " +
                            "list, the global default color above applies." ) );
            
            SPEC.newLine();
            
            blockColors = SPEC.define( new BlockStateMapField<>( "blocks",
                    new BlockStateMap.Builder<>( ColorIntValueCodec.NO_ALPHA )
                            .put( Blocks.FIRE, BlockStatePropertyMap.EMPTY, 0xFF0000 )
                            .put( Blocks.SCULK_SHRIEKER, BlockStatePropertyMap.EMPTY, 0xFF0000 )
                            .buildWithDefault( 0x00FFFF ),
                    "The colors used for block highlights. If no default color is specified in this " +
                            "list, the global default color above applies." ) );
        }
        
        public int getColor( Entity entity ) {
            return ClientConfig.PREFS.HIGHLIGHT_COLORS.entityColors.getOrElse( entity,
                    ClientConfig.PREFS.HIGHLIGHT_COLORS.defaultColor.get() );
        }
        
        public int getColor( BlockState blockState ) {
            return ClientConfig.PREFS.HIGHLIGHT_COLORS.blockColors.getOrElse( blockState,
                    ClientConfig.PREFS.HIGHLIGHT_COLORS.defaultColor.get() );
        }
    }
}