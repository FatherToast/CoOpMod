package fathertoast.coopmod.common.network;

import fathertoast.coopmod.common.core.CoOpMod;
import fathertoast.coopmod.common.network.message.ClientboundMainConfigSyncPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
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
    
    // ---- Message Sending ---- //
    
    /** Called when the config is edited to sync relevant data to all connected clients. */
    public static void sendSync( ClientboundMainConfigSyncPacket packet ) {
        sendToAll( packet );
    }
    
    /** Called when a client connects to sync relevant data. */
    public static void sendSync( ClientboundMainConfigSyncPacket packet, ServerPlayer player ) {
        sendToClient( packet, player );
    }
    
    
    // ---- Internal Message Sending ---- //
    
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
        int messageIndex = 0;
        
        // Server -> Client
        registerClientboundMessage( messageIndex++, ClientboundMainConfigSyncPacket.class,
                ClientboundMainConfigSyncPacket::encode, ClientboundMainConfigSyncPacket::decode, ClientboundMainConfigSyncPacket::handle );
        
        // Client -> Server
    }
    
    private <MSG> void registerClientboundMessage( int messageIndex, Class<MSG> messageType,
                                                   BiConsumer<MSG, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, MSG> decoder,
                                                   BiConsumer<MSG, Supplier<NetworkEvent.Context>> messageConsumer ) {
        registerMessage( messageIndex, messageType, encoder, decoder, messageConsumer, NetworkDirection.PLAY_TO_CLIENT );
    }
    
    private <MSG> void registerServerboundMessage( int messageIndex, Class<MSG> messageType,
                                                   BiConsumer<MSG, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, MSG> decoder,
                                                   BiConsumer<MSG, Supplier<NetworkEvent.Context>> messageConsumer ) {
        registerMessage( messageIndex, messageType, encoder, decoder, messageConsumer, NetworkDirection.PLAY_TO_SERVER );
    }
    
    private <MSG> void registerMessage( int messageIndex, Class<MSG> messageType,
                                        BiConsumer<MSG, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, MSG> decoder,
                                        BiConsumer<MSG, Supplier<NetworkEvent.Context>> messageConsumer, NetworkDirection direction ) {
        CHANNEL.registerMessage( messageIndex, messageType, encoder, decoder, messageConsumer,
                Optional.of( direction ) );
    }
    
    static {
        CHANNEL = NetworkRegistry.ChannelBuilder
                .named( CoOpMod.rl( "channel" ) )
                .serverAcceptedVersions( PROTOCOL_VERSION::equals )
                .clientAcceptedVersions( PROTOCOL_VERSION::equals )
                .networkProtocolVersion( () -> PROTOCOL_VERSION )
                .simpleChannel();
    }
}