package fathertoast.coopmod.client.config;

import fathertoast.coopmod.api.common.util.CoOpModObjects;
import fathertoast.coopmod.client.coordination.FindPlayersManager;
import fathertoast.coopmod.client.coordination.InspectManager;
import fathertoast.coopmod.client.event.KeyBindingEvents;
import fathertoast.coopmod.common.compat.jade.CMJadePlugin;
import fathertoast.coopmod.common.config.value.HighlightEffects;
import fathertoast.crust.api.config.common.AbstractConfigCategory;
import fathertoast.crust.api.config.common.AbstractConfigFile;
import fathertoast.crust.api.config.common.ConfigManager;
import fathertoast.crust.api.config.common.field.*;
import fathertoast.crust.api.config.common.field.collection.BlockStateMapField;
import fathertoast.crust.api.config.common.field.collection.EntityMapField;
import fathertoast.crust.api.config.common.file.TomlHelper;
import fathertoast.crust.api.config.common.value.CrustAnchor;
import fathertoast.crust.api.config.common.value.collection.BlockStateMap;
import fathertoast.crust.api.config.common.value.collection.EntityMap;
import fathertoast.crust.api.util.BlockStatePropertyMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

public class ClientPreferences extends AbstractConfigFile {
    
    public final Inspection INSPECTION;
    public final PlayerFinder PLAYER_FINDER;
    public final PartyOverlay PARTY_OVERLAY;
    public final HighlightSettings HIGHLIGHT_SETTINGS;
    
    /** Builds the config spec that should be used for this config. */
    ClientPreferences( ConfigManager manager, String fileName ) {
        super( manager, fileName,
                "This config contains personal preference settings." );
        
        INSPECTION = new Inspection( this );
        PLAYER_FINDER = new PlayerFinder( this );
        PARTY_OVERLAY = new PartyOverlay( this );
        HIGHLIGHT_SETTINGS = new HighlightSettings( this );
        
        // Refresh the state of key bindings
        SPEC.callback( KeyBindingEvents::updateKeyMode );
    }
    
    public static class Inspection extends AbstractConfigCategory<ClientPreferences> {
        
        public final DoubleField range;
        
        public final DoubleField nameplateSize;
        
        public final EnumField<KeyBindingEvents.Mode> keyMode;
        
        public final EnumField<CMJadePlugin.Mode> jadeMode;
        
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
            
            nameplateSize = SPEC.define( new ScaledDoubleField( "ping_nameplate_size",
                    1.0, 0.025 * 0.15, DoubleField.Range.NON_NEGATIVE,
                    "How large nameplates over pinged blocks/entities are rendered. Setting this to 0 " +
                            "disables ping nameplates entirely." ) );
            
            SPEC.newLine();
            
            keyMode = SPEC.define( new EnumField<>( "key_mode", KeyBindingEvents.Mode.HOLD,
                    "How the inspect key bind behaves. The key itself is bound in the game's options " +
                            "(Options > Controls > Key Binds)." ) );
            
            SPEC.newLine();
            
            jadeMode = SPEC.define( new EnumField<>( "jade_integration", CMJadePlugin.Mode.OVERRIDE,
                    "Setting for the 'inspect' feature's Jade integration.",
                    " * " + TomlHelper.toLiteral( CMJadePlugin.Mode.OFF ) + " - Disables Jade integration.",
                    " * " + TomlHelper.toLiteral( CMJadePlugin.Mode.BACKUP ) + " - Jade's tooltip will be the " +
                            "inspect target only when Jade does not already have a tooltip to display.",
                    " * " + TomlHelper.toLiteral( CMJadePlugin.Mode.OVERRIDE ) + " - While inspect is active, " +
                            "Jade's tooltip will always be the inspect target.",
                    " * " + TomlHelper.toLiteral( CMJadePlugin.Mode.REPLACE ) + " - Forces Jade's tooltip to " +
                            "always be the inspect target (this disables the tooltip while not inspecting)." ) );
        }
    }
    
    public static class PlayerFinder extends AbstractConfigCategory<ClientPreferences> {
        
        public final DoubleField range;
        
        public final EnumField<KeyBindingEvents.Mode> keyMode;
        public final IntField lingerDuration;
        
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
            
            keyMode = SPEC.define( new EnumField<>( "key_mode", KeyBindingEvents.Mode.HOLD,
                    "How the find players key bind behaves. The key itself is bound in the game's options " +
                            "(Options > Controls > Key Binds)." ) );
            lingerDuration = SPEC.define( new IntField( "linger_duration",
                    100, IntField.Range.NON_NEGATIVE,
                    "If the key mode is set to " + TomlHelper.toLiteral( KeyBindingEvents.Mode.HOLD ) +
                            ", this is the time, in ticks, that player finding will stay on for after the keybind is " +
                            "released. (20 ticks = 1 second)." ) );
        }
    }
    
    public static class PartyOverlay extends AbstractConfigCategory<ClientPreferences> {
        
        public final BooleanField enabled;
        
        public final DoubleField rangeSq;//for now, since there's no party system yet
        public final BooleanField showSelf;
        
        public final EnumField<CrustAnchor> anchorY;
        public final EnumField<CrustAnchor> anchorX;
        
        public final IntField offsetY;
        public final IntField offsetX;
        
        public final IntField panelSpacing;
        public final IntField panelFaceSize;
        public final BooleanField panelsShowNames;
        public final DoubleField panelsHealthRows;
        //public final BooleanField panelsShowEffects; TODO
        public final IntField panelPadding;
        public final ColorIntField panelBorderColor;
        public final ColorIntField panelBackgroundColor;
        
        PartyOverlay( ClientPreferences parent ) {
            super( parent, "party_overlay",
                    "Options to customize the 'party status' GUI overlay (HUD element)." );
            
            enabled = SPEC.define( new BooleanField( "enabled", true,
                    "Whether the party overlay should be displayed." ) );
            
            SPEC.newLine();
            
            rangeSq = SPEC.define( new SqrDoubleField( "range",
                    3.4e38, DoubleField.Range.NON_NEGATIVE,
                    "The maximum distance at which nearby players are considered to be in your party, " +
                            "and therefore given a panel in your party overlay." ) );
            showSelf = SPEC.define( new BooleanField( "show_self", false,
                    "If true, the party overlay will display a panel with your own status." ) );
            
            SPEC.newLine();
            
            anchorY = SPEC.define( new EnumField<>( "anchor.vertical", CrustAnchor.TOP, CrustAnchor.VERTICAL,
                    "The anchor position for the party overlay. That is, where it should be positioned " +
                            "relative to the screen." ) );
            anchorX = SPEC.define( new EnumField<>( "anchor.horizontal", CrustAnchor.LEFT, CrustAnchor.HORIZONTAL,
                    (String[]) null ) );
            
            SPEC.newLine();
            
            offsetY = SPEC.define( new IntField( "offset.vertical", 64, IntField.Range.ANY,
                    "The position offset for the party overlay from the anchor position, in GUI pixels. " +
                            "Negative values move the overlay toward the top/left, positives move it toward the bottom/right." ) );
            offsetX = SPEC.define( new IntField( "offset.horizontal", 8, IntField.Range.ANY,
                    (String[]) null ) );
            
            SPEC.increaseIndent();
            SPEC.subcategory( "panel",
                    "Options to customize the information, size, and appearance of the panels used to " +
                            "display each party member's status." );
            
            panelFaceSize = SPEC.define( new IntField( "panel.face_size", 19, IntField.Range.NON_NEGATIVE ) );
            panelsShowNames = SPEC.define( new BooleanField( "panel.show_name", true ) );
            panelsHealthRows = SPEC.define( new DoubleField( "panel.health_rows", 2.0, DoubleField.Range.NON_NEGATIVE,
                    "Vertical size of the health display, in rows of hearts. Setting this below 1.0 disables the health display." ) );
            panelSpacing = SPEC.define( new IntField( "panel.spacing", 5, IntField.Range.NON_NEGATIVE,
                    "Space between each party member's panel, in GUI pixels." ) );
            panelPadding = SPEC.define( new IntField( "panel.padding", 3, IntField.Range.NON_NEGATIVE,
                    "Space between each element within a panel, in GUI pixels." ) );
            panelBorderColor = SPEC.define( new ColorIntField( "panel.border_color", 0x80_000000, true ) );
            panelBackgroundColor = SPEC.define( new ColorIntField( "panel.background_color", 0xA0_333333, true ) );
            
            SPEC.decreaseIndent();
        }
    }
    
    @SuppressWarnings( "UnstableApiUsage" )
    public static class HighlightSettings extends AbstractConfigCategory<ClientPreferences> {
        
        public final ColorIntField defaultColor;
        // TODO - Replace with registry entry field after Crust update
        public final StringField defaultSound;
        
        public final BooleanField playerColors;
        
        public final EntityMapField<HighlightEffects> entityEffects;
        
        public final BlockStateMapField<HighlightEffects> blockEffects;
        
        HighlightSettings( ClientPreferences parent ) {
            super( parent, "highlight_settings",
                    "Options to customize the colors for ping and inspect highlights (their visual outlines), as well " +
                            " as ping sound effects." );
            
            defaultColor = SPEC.define( new ColorIntField( "global_default.color", 0xFFFF00, false,
                    "The color used for all highlights not specified in the following fields. Note that " +
                            "when the settings below are all at their default values, this color will never be used." ) );
            
            defaultSound = SPEC.define( new StringField( "global_default.sound", CoOpModObjects.SoundEvents.PING_BINK.getId().toString(),
                    ( value ) -> ResourceLocation.isValidResourceLocation( value )
                            && ForgeRegistries.SOUND_EVENTS.containsKey( ResourceLocation.parse( value ) ),
                    "The sound event used for all ping types not specified in the following fields. Note that " +
                            "when the settings below are all at their default values, this sound event will never be used." ) );
            
            SPEC.newLine();//TODO add in some auto-coloring options for players, personal colors, and team colors (note team colors currently override all settings)
            
            playerColors = SPEC.define( new BooleanField( "player_colored_pings", false,
                    "When enabled, this causes other players' pings to match their personal color. " +
                            "Otherwise, their ping colors will follow the other settings." ) ); // TODO implement player personal colors
            
            SPEC.newLine();
            
            var builder = new EntityMap.Builder<>( HighlightEffects.CODEC );
            for( EntityType<?> entityType : ForgeRegistries.ENTITY_TYPES.getValues() ) {
                if( entityType == EntityType.PLAYER ) {
                    // Special color for players
                    builder.put( entityType, new HighlightEffects( 0x00FFFF, CoOpModObjects.SoundEvents.PING_BINK ) );
                    continue;
                }
                // TODO Maybe determine bosses some other way
                else if( entityType == EntityType.ENDER_DRAGON || entityType == EntityType.WITHER
                        || entityType == EntityType.WARDEN || entityType == EntityType.ELDER_GUARDIAN ) {
                    // Special sound for bosses
                    builder.put( entityType, new HighlightEffects( 0xFF0000, CoOpModObjects.SoundEvents.PING_BOSS_LOUD ) );
                    continue;
                }
                switch( entityType.getCategory() ) { // TODO update to use extends super in higher Crust ver, maybe also add auto-color options
                    case MONSTER ->
                            builder.put( entityType, new HighlightEffects( 0xFF0000, CoOpModObjects.SoundEvents.PING_HOSTILE_LOUD ) );
                    case MISC ->
                            builder.put( entityType, new HighlightEffects( 0xFFFF00, CoOpModObjects.SoundEvents.PING_BINK ) );
                    default ->
                            builder.put( entityType, new HighlightEffects( 0x00FF00, CoOpModObjects.SoundEvents.PING_BINK ) );
                }
            }
            entityEffects = SPEC.define( new EntityMapField<>( "entities",
                    builder.buildWithDefault( new HighlightEffects( 0xFF0000, CoOpModObjects.SoundEvents.PING_BINK ) ),
                    "The colors and ping sounds used for entity highlights. If no values are specified in this " +
                            "list, the global default color and value above applies." ) );
            
            SPEC.newLine();
            
            blockEffects = SPEC.define( new BlockStateMapField<>( "blocks",
                    new BlockStateMap.Builder<>( HighlightEffects.CODEC )
                            .put( Blocks.FIRE, BlockStatePropertyMap.EMPTY, new HighlightEffects( 0xFF0000, CoOpModObjects.SoundEvents.PING_BINK ) )
                            .put( Blocks.SOUL_FIRE, BlockStatePropertyMap.EMPTY, new HighlightEffects( 0xFF0000, CoOpModObjects.SoundEvents.PING_BINK ) )
                            .put( Blocks.SCULK_SHRIEKER, BlockStatePropertyMap.EMPTY, new HighlightEffects( 0xFF0000, CoOpModObjects.SoundEvents.PING_BINK ) )
                            .buildWithDefault( new HighlightEffects( 0x00FFFF, CoOpModObjects.SoundEvents.PING_BINK ) ),
                    "The colors and ping sounds used for block highlights. If no values are specified in this " +
                            "list, the global default color and sound above applies." ) );
        }
        
        public int getColor( Entity entity ) {
            if( ClientConfig.PREFS.HIGHLIGHT_SETTINGS.entityEffects.contains( entity ) )
                // noinspection ConstantConditions
                return ClientConfig.PREFS.HIGHLIGHT_SETTINGS.entityEffects.get( entity ).color.get();
            else {
                return ClientConfig.PREFS.HIGHLIGHT_SETTINGS.defaultColor.get();
            }
        }
        
        public int getColor( BlockState blockState ) {
            if( ClientConfig.PREFS.HIGHLIGHT_SETTINGS.blockEffects.contains( blockState ) )
                // noinspection ConstantConditions
                return ClientConfig.PREFS.HIGHLIGHT_SETTINGS.blockEffects.get( blockState ).color.get();
            else {
                return ClientConfig.PREFS.HIGHLIGHT_SETTINGS.defaultColor.get();
            }
        }
        
        @Nullable
        public SoundEvent getSound( Entity entity ) {
            String soundId;
            if( ClientConfig.PREFS.HIGHLIGHT_SETTINGS.entityEffects.contains( entity ) )
                // noinspection ConstantConditions
                soundId = ClientConfig.PREFS.HIGHLIGHT_SETTINGS.entityEffects.get( entity ).pingSoundId.get();
            else {
                soundId = ClientConfig.PREFS.HIGHLIGHT_SETTINGS.defaultSound.get();
            }
            return ForgeRegistries.SOUND_EVENTS.getValue( ResourceLocation.parse( soundId ) );
        }
        
        @Nullable
        public SoundEvent getSound( BlockState blockState ) {
            String soundId;
            if( ClientConfig.PREFS.HIGHLIGHT_SETTINGS.blockEffects.contains( blockState ) )
                // noinspection ConstantConditions
                soundId = ClientConfig.PREFS.HIGHLIGHT_SETTINGS.blockEffects.get( blockState ).pingSoundId.get();
            else {
                soundId = ClientConfig.PREFS.HIGHLIGHT_SETTINGS.defaultSound.get();
            }
            return ForgeRegistries.SOUND_EVENTS.getValue( ResourceLocation.parse( soundId ) );
        }
    }
}