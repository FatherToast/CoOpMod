package fathertoast.coopmod.common.config;

import fathertoast.coopmod.common.network.PacketHandler;
import fathertoast.coopmod.common.network.message.ClientboundMainConfigSyncPacket;
import fathertoast.crust.api.config.common.AbstractConfigCategory;
import fathertoast.crust.api.config.common.AbstractConfigFile;
import fathertoast.crust.api.config.common.ConfigManager;
import fathertoast.crust.api.config.common.field.DoubleField;
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
                GENERAL.maxInspectRange.get() );
    }
    
    private void sendSyncPacket() { PacketHandler.sendSync( makeSyncPacket() ); }
    
    public void sendSyncPacket( ServerPlayer player ) { PacketHandler.sendSync( makeSyncPacket(), player ); }
    
    public static class General extends AbstractConfigCategory<MainConfig> {
        
        public final DoubleField maxInspectRange;
        
        General( MainConfig parent ) {
            super( parent, "general",
                    "Options to customize misc settings that apply to the mod as a whole." );
            
            maxInspectRange = SPEC.define( new DoubleField( "max_inspect_range",
                    32.0, DoubleField.Range.NON_NEGATIVE,
                    "How far players are allowed to inspect blocks/entities from, in blocks.",
                    "Unless this is set to 0 (which completely disables the inspect feature), players are, at a " +
                            "minimum, allowed to inspect anything they can physically reach." ) );
        }
    }
}