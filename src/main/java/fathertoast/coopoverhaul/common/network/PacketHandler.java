package fathertoast.coopoverhaul.common.network;

import fathertoast.coopoverhaul.common.core.CoOpOverhaulMod;
import fathertoast.coopoverhaul.common.network.message.*;
import fathertoast.coopoverhaul.common.util.TrackingHelper;
import fathertoast.crust.api.util.OnClient;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public final class PacketHandler {
    
    // ---- Message & Channel Setup ---- //
    
    /**
     * Current protocol version. This should be incremented any time our packets are changed,
     * and does break backwards compatibility.
     */
    private static final String PROTOCOL_VERSION = "0";
    
    /** The network channel our mod will be using when sending messages. */
    private static final SimpleChannel CHANNEL;
    
    /** Registers this mod's messages. */
    public void registerMessages() {
        int messageIndex = -1;
        
        // Server -> Client
        registerClientboundMessage( ++messageIndex, ClientboundMainConfigSyncPacket.class,
                ClientboundMainConfigSyncPacket::encode, ClientboundMainConfigSyncPacket::decode, ClientboundMainConfigSyncPacket::handle );
        registerClientboundMessage( ++messageIndex, ClientboundPingManagerSyncPacket.class,
                ClientboundPingManagerSyncPacket::encode, ClientboundPingManagerSyncPacket::decode, ClientboundPingManagerSyncPacket::handle );
        registerClientboundMessage( ++messageIndex, ClientboundEntityPingPacket.class,
                ClientboundEntityPingPacket::encode, ClientboundEntityPingPacket::decode, ClientboundEntityPingPacket::handle );
        registerClientboundMessage( ++messageIndex, ClientboundBlockPingPacket.class,
                ClientboundBlockPingPacket::encode, ClientboundBlockPingPacket::decode, ClientboundBlockPingPacket::handle );
        registerClientboundMessage( ++messageIndex, ClientboundFindPlayersDataPacket.class,
                ClientboundFindPlayersDataPacket::encode, ClientboundFindPlayersDataPacket::decode, ClientboundFindPlayersDataPacket::handle );
        
        // Client -> Server
        registerServerboundMessage( ++messageIndex, ServerboundDataRequestPacket.class,
                ServerboundDataRequestPacket::encode, ServerboundDataRequestPacket::decode, ServerboundDataRequestPacket::handle );
        registerServerboundMessage( ++messageIndex, ServerboundEntityPingPacket.class,
                ServerboundEntityPingPacket::encode, ServerboundEntityPingPacket::decode, ServerboundEntityPingPacket::handle );
        registerServerboundMessage( ++messageIndex, ServerboundBlockPingPacket.class,
                ServerboundBlockPingPacket::encode, ServerboundBlockPingPacket::decode, ServerboundBlockPingPacket::handle );
    }
    
    
    // ---- Server -> Client Message Sending ---- //
    
    /** Called when the config is edited to sync relevant data to all connected clients. */
    public static void sendSync( ClientboundMainConfigSyncPacket message ) {
        sendToAll( message );
    }
    
    /** Called when a client connects to sync relevant config data. */
    public static void sendSync( ClientboundMainConfigSyncPacket message, ServerPlayer player ) {
        sendToClient( message, player );
    }
    
    /** Called when a player enters a dimension to sync relevant active pings. */
    public static void sendSync( ClientboundPingManagerSyncPacket message, ServerPlayer player ) {
        sendToClient( message, player );
    }
    
    /** Sends a ping to all tracking players. */
    public static void sendPing( ClientboundEntityPingPacket message, ServerPlayer sender ) {
        Entity entity = sender.serverLevel().getEntity( message.entityId() );
        if( entity != null ) {
            for( ServerPlayer player : sender.serverLevel().players() ) {
                if( player != sender && TrackingHelper.isEntityInTrackingRange( player, entity ) ) {
                    sendToClient( message, player );
                }
            }
        }
    }
    
    /** Sends a ping to all tracking players. */
    public static void sendPing( ClientboundBlockPingPacket message, ServerPlayer sender ) {
        if( sender.serverLevel().isLoaded( message.blockPos() ) ) {
            for( ServerPlayer player : sender.serverLevel().players() ) {
                if( player != sender && TrackingHelper.isBlockInTrackingRange( player, message.blockPos() ) ) {
                    sendToClient( message, player );
                }
            }
        }
    }
    
    /** Called periodically to tell clients where far-away friendly players are located. */
    public static void sendFindPlayersData( ClientboundFindPlayersDataPacket message, ServerPlayer player ) {
        sendToClient( message, player );
    }
    
    
    // ---- Client -> Server Message Sending ---- //
    
    /** Sends a data request to the server. */
    @OnClient
    public static void requestFindPlayersData( boolean enable ) {
        CHANNEL.sendToServer( new ServerboundDataRequestPacket( ServerboundDataRequestPacket.Type.FIND_PLAYERS, enable ) );
    }
    
    /** Sends a data request to the server. */
    @OnClient
    public static void requestData( ServerboundDataRequestPacket.Type type, boolean enable ) {
        CHANNEL.sendToServer( new ServerboundDataRequestPacket( type, enable ) );
    }
    
    /** Sends a ping to the server. */
    @OnClient
    public static void sendPingToServer( int entityId ) {
        CHANNEL.sendToServer( new ServerboundEntityPingPacket( entityId ) );
    }
    
    /** Sends a ping to the server. */
    @OnClient
    public static void sendPingToServer( BlockPos blockPos ) {
        CHANNEL.sendToServer( new ServerboundBlockPingPacket( blockPos ) );
    }
    
    
    // ---- Internal Methods ---- //
    
    /**
     * Sends the specified message to all connected clients.
     *
     * @param message The message to send to the clients.
     * @param <MSG>   Packet type.
     */
    private static <MSG> void sendToAll( MSG message ) {
        if( ServerLifecycleHooks.getCurrentServer() != null ) {
            CHANNEL.send( PacketDistributor.ALL.noArg(), message );
        }
    }
    
    /**
     * Sends the specified message to all connected clients in the level/dimension.
     *
     * @param message The message to send to the clients.
     * @param level   The level that should receive this message.
     * @param <MSG>   Packet type.
     */
    private static <MSG> void sendToDimension( MSG message, Level level ) {
        CHANNEL.send( PacketDistributor.DIMENSION.with( level::dimension ), message );
    }
    
    /**
     * Sends the specified message to all connected clients in the level/dimension.
     *
     * @param message   The message to send to the clients.
     * @param dimension The dimension that should receive this message.
     * @param <MSG>     Packet type.
     */
    private static <MSG> void sendToDimension( MSG message, ResourceKey<Level> dimension ) {
        CHANNEL.send( PacketDistributor.DIMENSION.with( () -> dimension ), message );
    }
    
    /**
     * Sends the specified message to the client.
     *
     * @param message The message to send to the client.
     * @param player  The player client that should receive this message.
     * @param <MSG>   Packet type.
     */
    private static <MSG> void sendToClient( MSG message, ServerPlayer player ) {
        CHANNEL.sendTo( message, player.connection.connection, NetworkDirection.PLAY_TO_CLIENT );
    }
    
    /** Registers a typical clientbound message. */
    private <MSG> void registerClientboundMessage( int messageIndex, Class<MSG> messageType,
                                                   BiConsumer<MSG, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, MSG> decoder,
                                                   BiConsumer<MSG, Supplier<NetworkEvent.Context>> messageConsumer ) {
        registerMessage( messageIndex, messageType, encoder, decoder, messageConsumer, NetworkDirection.PLAY_TO_CLIENT );
    }
    
    /** Registers a typical serverbound message. */
    private <MSG> void registerServerboundMessage( int messageIndex, Class<MSG> messageType,
                                                   BiConsumer<MSG, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, MSG> decoder,
                                                   BiConsumer<MSG, Supplier<NetworkEvent.Context>> messageConsumer ) {
        registerMessage( messageIndex, messageType, encoder, decoder, messageConsumer, NetworkDirection.PLAY_TO_SERVER );
    }
    
    /** Registers a message with specified direction. */
    private <MSG> void registerMessage( int messageIndex, Class<MSG> messageType,
                                        BiConsumer<MSG, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, MSG> decoder,
                                        BiConsumer<MSG, Supplier<NetworkEvent.Context>> messageConsumer, NetworkDirection direction ) {
        CHANNEL.registerMessage( messageIndex, messageType, encoder, decoder, messageConsumer,
                Optional.of( direction ) );
    }
    
    static {
        CHANNEL = NetworkRegistry.ChannelBuilder
                .named( CoOpOverhaulMod.rl( "channel" ) )
                .serverAcceptedVersions( PROTOCOL_VERSION::equals )
                .clientAcceptedVersions( PROTOCOL_VERSION::equals )
                .networkProtocolVersion( () -> PROTOCOL_VERSION )
                .simpleChannel();
    }
}