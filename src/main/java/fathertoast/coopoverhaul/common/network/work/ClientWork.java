package fathertoast.coopoverhaul.common.network.work;

import fathertoast.coopoverhaul.client.config.ClientConfig;
import fathertoast.coopoverhaul.client.coordination.ClientPingHelper;
import fathertoast.coopoverhaul.common.coordination.PingManager;
import fathertoast.coopoverhaul.common.network.message.ClientboundBlockPingPacket;
import fathertoast.coopoverhaul.common.network.message.ClientboundEntityPingPacket;
import fathertoast.coopoverhaul.common.network.message.ClientboundMainConfigSyncPacket;
import fathertoast.coopoverhaul.common.network.message.ClientboundPingManagerSyncPacket;
import fathertoast.crust.api.util.OnClient;

@OnClient
public final class ClientWork {
    
    @OnClient
    public static void handleMainConfigSync( ClientboundMainConfigSyncPacket message ) {
        ClientConfig.sync( message );
    }
    
    @OnClient
    public static void handlePingSync( ClientboundPingManagerSyncPacket message ) {
        PingManager.reset();
        PingManager manager = ClientPingHelper.pingManager();
        if( manager != null ) manager.receiveSync( message );
    }
    
    @OnClient
    public static void handlePing( ClientboundEntityPingPacket message ) {
        ClientPingHelper.receivePing( message );
    }
    
    @OnClient
    public static void handlePing( ClientboundBlockPingPacket message ) {
        ClientPingHelper.receivePing( message );
    }
    
    
    private ClientWork() {}
}