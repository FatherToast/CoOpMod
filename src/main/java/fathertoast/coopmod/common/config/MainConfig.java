package fathertoast.coopmod.common.config;

import fathertoast.coopmod.common.network.PacketHandler;
import fathertoast.coopmod.common.network.message.ClientboundMainConfigSyncPacket;
import fathertoast.crust.api.config.common.AbstractConfigCategory;
import fathertoast.crust.api.config.common.AbstractConfigFile;
import fathertoast.crust.api.config.common.ConfigManager;
import fathertoast.crust.api.config.common.field.BooleanField;
import fathertoast.crust.api.config.common.field.DoubleField;
import fathertoast.crust.api.config.common.field.IntField;
import net.minecraft.server.level.ServerPlayer;

public class MainConfig extends AbstractConfigFile {
    
    public final General GENERAL;
    
    /** Builds the config spec that should be used for this config. */
    MainConfig( ConfigManager manager, String fileName ) {
        super( manager, fileName,
                "This config contains options several features in the mod." );
        
        GENERAL = new General( this );
        
        // Sync relevant fields to all clients any time the config is loaded
        SPEC.callback( this::sendSyncPacket );
    }
    
    /** Generated a sync packet for this config. Only send fields the client actually needs. */
    private ClientboundMainConfigSyncPacket makeSyncPacket() {
        return new ClientboundMainConfigSyncPacket(
                GENERAL.maxInspectRange.get(),
                GENERAL.allowInspectingHidden.get(),
                GENERAL.maxFindPlayersRange.get(),
                GENERAL.pingDuration.get(),
                GENERAL.pingCooldown.get() );
    }
    
    private void sendSyncPacket() { PacketHandler.sendSync( makeSyncPacket() ); }
    
    public void sendSyncPacket( ServerPlayer player ) { PacketHandler.sendSync( makeSyncPacket(), player ); }
    
    public static class General extends AbstractConfigCategory<MainConfig> {
        
        public final DoubleField maxInspectRange;
        public final BooleanField allowInspectingHidden;
        public final DoubleField maxFindPlayersRange;
        
        public final IntField pingDuration;
        public final IntField pingCooldown;
        
        General( MainConfig parent ) {
            super( parent, "general",
                    "Options to customize misc settings that apply to the mod as a whole." );
            
            maxInspectRange = SPEC.define( new DoubleField( "max_inspect_range",
                    32.0, DoubleField.Range.NON_NEGATIVE,
                    "How far players are allowed to inspect and ping blocks/entities from, in blocks.",
                    "Setting this to 0 completely disables both the 'inspect' and 'ping' features." ) );
            allowInspectingHidden = SPEC.define( new BooleanField( "allow_recoloring_hidden", false,
                    "When enabled, allows players to recolor highlights for normally hidden blocks, " +
                            "like silverfish-infested blocks." ) );
            maxFindPlayersRange = SPEC.define( new DoubleField( "max_find_players_range",
                    3.4e38, DoubleField.Range.NON_NEGATIVE,
                    "How far players are allowed to find friendly players from, in blocks.",
                    "Setting this to 0 completely disables the 'find players' feature." ) );
            
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