package fathertoast.coopmod.common.network.work;

import fathertoast.coopmod.client.InspectManager;
import fathertoast.coopmod.common.network.message.ClientboundMainConfigSyncPacket;

public class ClientWork {
    
    public static void handleSyncMaxInspectRange( ClientboundMainConfigSyncPacket message ) {
        InspectManager.setMaxInspectRange( message.maxInspectRange() );
    }
}