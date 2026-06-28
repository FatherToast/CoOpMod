package fathertoast.coopmod.common.network.work;

import fathertoast.coopmod.client.InspectManager;
import fathertoast.coopmod.common.core.PingManager;
import fathertoast.coopmod.common.network.message.ClientboundBlockPingPacket;
import fathertoast.coopmod.common.network.message.ClientboundEntityPingPacket;
import fathertoast.coopmod.common.network.message.ClientboundMainConfigSyncPacket;
import fathertoast.coopmod.common.network.message.ClientboundPingManagerSyncPacket;
import fathertoast.crust.api.util.OnClient;

@OnClient
public final class ClientWork {
    
    @OnClient
    public static void handleMainConfigSync( ClientboundMainConfigSyncPacket message ) {
        InspectManager.handleMainConfigSync( message );
    }
    
    @OnClient
    public static void handlePingSync( ClientboundPingManagerSyncPacket message ) {
        PingManager.reset();
        PingManager manager = InspectManager.pingManager();
        if( manager != null ) manager.receiveSync( message );
    }
    
    @OnClient
    public static void handlePing( ClientboundEntityPingPacket message ) {
        PingManager manager = InspectManager.pingManager();
        if( manager != null ) manager.receivePing( message );
    }
    
    @OnClient
    public static void handlePing( ClientboundBlockPingPacket message ) {
        PingManager manager = InspectManager.pingManager();
        if( manager != null ) manager.receivePing( message );
    }
    
    
    private ClientWork() {}
}