package fathertoast.coopoverhaul.common.config;

import fathertoast.coopoverhaul.common.network.PacketHandler;
import fathertoast.coopoverhaul.common.network.message.ClientboundMainConfigSyncPacket;
import fathertoast.coopoverhaul.common.protection.FriendlyFireHelper;
import fathertoast.coopoverhaul.common.util.AttributeModUtil;
import fathertoast.crust.api.config.common.AbstractConfigCategory;
import fathertoast.crust.api.config.common.AbstractConfigFile;
import fathertoast.crust.api.config.common.ConfigManager;
import fathertoast.crust.api.config.common.field.BooleanField;
import fathertoast.crust.api.config.common.field.DoubleField;
import fathertoast.crust.api.config.common.field.EnumField;
import fathertoast.crust.api.config.common.field.IntField;
import fathertoast.crust.api.config.common.file.TomlHelper;
import net.minecraft.server.level.ServerPlayer;

public class MainConfig extends AbstractConfigFile {
    
    public final General GENERAL;
    
    /** Builds the config spec that should be used for this config. */
    MainConfig( ConfigManager manager, String fileName ) {
        super( manager, fileName,
                "This config contains options several features in the mod." );
        
        //TODO maybe it's getting around time we should split this into categories like the client prefs
        GENERAL = new General( this );
        
        // Sync relevant fields to all clients any time the config is loaded
        SPEC.callback( this::sendSyncPacket );
    }
    
    /** Generated a sync packet for this config. Only send fields the client actually needs. */
    private ClientboundMainConfigSyncPacket makeSyncPacket() {
        return new ClientboundMainConfigSyncPacket(
                GENERAL.spyglassInspectRange.get(),
                GENERAL.maxInspectRange.get(),
                GENERAL.allowRecoloringHidden.get(),
                GENERAL.maxFindPlayersRange.get(),
                GENERAL.pingDuration.get(),
                GENERAL.pingCooldown.get() );
    }
    
    private void sendSyncPacket() { PacketHandler.sendSync( makeSyncPacket() ); }
    
    public void sendSyncPacket( ServerPlayer player ) { PacketHandler.sendSync( makeSyncPacket(), player ); }
    
    public static class General extends AbstractConfigCategory<MainConfig> {
        
        public final DoubleField friendlyFireMulti;
        public final EnumField<FriendlyFireHelper.Mode> friendlyFireMode;
        
        //        public final BooleanField reviveEnabled;
        
        public final DoubleField baseInspectRange;
        public final DoubleField spyglassInspectRange;
        public final DoubleField maxInspectRange;
        
        public final BooleanField allowRecoloringHidden;
        
        public final DoubleField maxFindPlayersRange;
        public final IntField findPlayersUpdateCooldown;
        
        public final IntField pingDuration;
        public final IntField pingCooldown;
        
        General( MainConfig parent ) {
            super( parent, "general",
                    "Options to customize misc settings that apply to the mod as a whole." );
            
            friendlyFireMulti = SPEC.define( new DoubleField( "friendly_fire.multiplier",
                    0.2, DoubleField.Range.NON_NEGATIVE,
                    "Multiplier applied to damage dealt between friendly players. When set to 0, " +
                            "friendly players cannot damage each other." ) );
            friendlyFireMode = SPEC.define( new EnumField<>( "friendly_fire.mode", FriendlyFireHelper.Mode.DEFAULT,
                    "Specifies when two players are considered 'friendly' and therefore have the " +
                            "friendly fire multiplier applied.",
                    " * " + TomlHelper.toLiteral( FriendlyFireHelper.Mode.OFF ) +
                            " - Nothing is considered friendly fire (this feature is disabled).",
                    " * " + TomlHelper.toLiteral( FriendlyFireHelper.Mode.DEFAULT ) +
                            " - Players are only considered non-friendly if they are in an opposing team (teamless " +
                            "players are friendly to all).",
                    " * " + TomlHelper.toLiteral( FriendlyFireHelper.Mode.STRICT ) +
                            " - Players are only considered friendly if you are both in allied teams (teamless " +
                            "players are non-friendly to all)." ) );
            
            //            SPEC.newLine();
            //
            //            reviveEnabled = SPEC.define( new BooleanField( "allow_revive", true,
            //                    "When enabled, players who would die will be put into a 'downed' state instead. " +
            //                            "Downed players can be revived by other players or choose to give up to respawn normally." ) );
            
            SPEC.newLine();
            
            baseInspectRange = SPEC.define( new DoubleField( "inspect_range.base",
                    32.0, DoubleField.Range.NON_NEGATIVE,
                    "How far players are allowed to inspect and ping blocks/entities from, in blocks.",
                    "This is the default/base range that players have without any modifiers from equipment, such as a spyglass." ) );
            spyglassInspectRange = SPEC.define( new DoubleField( "inspect_range.spyglass_modifier",
                    96.0, DoubleField.Range.NON_NEGATIVE,
                    "The additional inspect range gained by holding/using a spyglass." ) );
            maxInspectRange = SPEC.define( new DoubleField( "inspect_range.max",
                    128.0, DoubleField.Range.NON_NEGATIVE,
                    "The absolute maximum inspect and ping distance from all sources (base value and modifiers), in blocks.",
                    "Setting this to 0 completely disables both the 'inspect' and 'ping' features." ) );
            SPEC.callback( AttributeModUtil::updateServerModifiers );
            
            SPEC.newLine();
            
            allowRecoloringHidden = SPEC.define( new BooleanField( "allow_recoloring_hidden", false,
                    "When enabled, allows players to recolor highlights for normally hidden blocks, " +
                            "like silverfish-infested blocks." ) );
            
            SPEC.newLine();
            
            maxFindPlayersRange = SPEC.define( new DoubleField( "max_find_players_range",
                    3.4e38, DoubleField.Range.NON_NEGATIVE,
                    "How far players are allowed to find friendly players from, in blocks.",
                    "Setting this to 0 completely disables the 'find players' feature." ) );
            findPlayersUpdateCooldown = SPEC.define( new IntField( "find_players_update_cooldown",
                    33, IntField.Range.NON_NEGATIVE,
                    "The time, in ticks, between sending position update packets to players so they can " +
                            "find distant players. (20 ticks = 1 second)" ) );
            
            SPEC.newLine();
            
            pingDuration = SPEC.define( new IntField( "ping.duration",
                    600, IntField.Range.NON_NEGATIVE,
                    "The time, in ticks, that pings are displayed for. (20 ticks = 1 second)" ) );
            pingCooldown = SPEC.define( new IntField( "ping.cooldown",
                    10, IntField.Range.NON_NEGATIVE,
                    "The time, in ticks, players need to wait between each ping. (20 ticks = 1 second)" ) );
        }
    }
}