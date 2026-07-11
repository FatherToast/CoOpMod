package fathertoast.coopoverhaul.common.coordination;

import fathertoast.coopoverhaul.common.core.CoOpOverhaulMod;
import fathertoast.crust.api.config.common.value.collection.key.IRegWrapper;
import fathertoast.crust.api.lib.EntityEventHelper;
import fathertoast.crust.api.lib.NBTHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.EntityGetter;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Team;
import net.minecraftforge.common.extensions.IForgeBlockEntity;
import net.minecraftforge.event.ForgeEventFactory;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * TODO
 */
public final class ReviveManager {
    
    public static final String TAG_DOWNED = "Downed";
    
    /**
     * Called on the server side when a player would have been killed, but instead should be
     * put into the "downed" state.
     */
    public static void downPlayer( ServerPlayer player, DamageSource source ) {
        emulateDeath( player, source );
        
        player.setHealth( player.getMaxHealth() );
        player.setGameMode( GameType.SPECTATOR );
        
        CompoundTag tag = NBTHelper.getPlayerData( player, CoOpOverhaulMod.MOD_ID );
        tag.putBoolean( TAG_DOWNED, true );
        
        //TODO send packet to the downed player and any others as needed
    }
    
    /** @return True if the player is in the 'downed' state. */
    public static boolean isDowned( Player player ) {
        CompoundTag tag = NBTHelper.getPlayerData( player, CoOpOverhaulMod.MOD_ID );
        return NBTHelper.containsNumber( tag, TAG_DOWNED ) && tag.getBoolean( TAG_DOWNED );
    }
    
    
    /**
     * We cancel the actual death, but we want it to still do some of the usual on-death things.
     * Copied from {@link ServerPlayer#die(DamageSource)} and modified; see comments for modifications.
     */
    private static void emulateDeath( ServerPlayer player, DamageSource source ) {
        // Main thing we want is the death message
        if( player.level().getGameRules().getBoolean( GameRules.RULE_SHOWDEATHMESSAGES ) ) {
            Component deathMessage = player.getCombatTracker().getDeathMessage();
            player.connection.send( new ClientboundPlayerCombatKillPacket( player.getId(), deathMessage ), PacketSendListener.exceptionallySend( () -> {
                String message = deathMessage.getString( 256 );
                Component component1 = Component.translatable( "death.attack.message_too_long", Component.literal( message ).withStyle( ChatFormatting.YELLOW ) );
                Component component2 = Component.translatable( "death.attack.even_more_magic", player.getDisplayName() ).withStyle( style -> style.withHoverEvent( new HoverEvent( HoverEvent.Action.SHOW_TEXT, component1 ) ) );
                return new ClientboundPlayerCombatKillPacket( player.getId(), component2 );
            } ) );
            Team team = player.getTeam();
            if( team != null && team.getDeathMessageVisibility() != Team.Visibility.ALWAYS ) {
                if( team.getDeathMessageVisibility() == Team.Visibility.HIDE_FOR_OTHER_TEAMS ) {
                    player.server.getPlayerList().broadcastSystemToTeam( player, deathMessage );
                }
                else if( team.getDeathMessageVisibility() == Team.Visibility.HIDE_FOR_OWN_TEAM ) {
                    player.server.getPlayerList().broadcastSystemToAllExceptTeam( player, deathMessage );
                }
            }
            else {
                player.server.getPlayerList().broadcastSystemMessage( deathMessage, false );
            }
        }
        else {
            player.connection.send( new ClientboundPlayerCombatKillPacket( player.getId(), CommonComponents.EMPTY ) );
        }
        
        /* We want to defer these until the actual death
        player.removeEntitiesOnShoulder();
        if( player.level().getGameRules().getBoolean( GameRules.RULE_FORGIVE_DEAD_PLAYERS ) ) {
            player.tellNeutralMobsThatIDied();
        }
        if( !player.isSpectator() ) {
            player.dropAllDeathLoot( source );
        }
         */
        
        LivingEntity killer = player.getKillCredit();
        if( killer != null ) {
            player.awardStat( Stats.ENTITY_KILLED_BY.get( killer.getType() ) );
            // The death score doesn't appear to ever get a value assigned and is an unused parameter in #awardKillScore
            killer.awardKillScore( player, 0/*player.deathScore*/, source );
            //player.createWitherRose( killer ); // Inaccessible; replaced by below
            if( killer instanceof WitherBoss ) {
                boolean placedBlock = false;
                if( ForgeEventFactory.getMobGriefingEvent( player.level(), killer ) ) {
                    BlockPos pos = player.blockPosition();
                    BlockState block = Blocks.WITHER_ROSE.defaultBlockState();
                    if( player.level().isEmptyBlock( pos ) && block.canSurvive( player.level(), pos ) ) {
                        player.level().setBlock( pos, block, Block.UPDATE_ALL );
                        placedBlock = true;
                    }
                }
                if( !placedBlock ) {
                    player.level().addFreshEntity( new ItemEntity( player.level(),
                            player.getX(), player.getY(), player.getZ(),
                            new ItemStack( Items.WITHER_ROSE ) ) );
                }
            }
        }
        
        //player.level().broadcastEntityEvent( player, (byte) 3 ); // Replaced by Crust method below
        //EntityEventHelper.DEATH_ANIM.broadcast( player );
        /* We don't want to double-count
        player.awardStat( Stats.DEATHS );
        player.resetStat( Stats.CUSTOM.get( Stats.TIME_SINCE_DEATH ) );
        player.resetStat( Stats.CUSTOM.get( Stats.TIME_SINCE_REST ) );
         */
        player.clearFire();
        player.setTicksFrozen( 0 );
        player.setSharedFlagOnFire( false );
        player.getCombatTracker().recheckStatus();
        //player.setLastDeathLocation( Optional.of( GlobalPos.of( player.level().dimension(), player.blockPosition() ) ) );
    }
    
    //    public static final String TAG_SRC_DAMAGE_TYPE = "DmgType";
    //    public static final String TAG_SRC_ENTITY = "Entity";
    //    public static final String TAG_SRC_DIRECT_ENTITY = "DirectEntity";
    //    public static final String TAG_SRC_POSITION = "Position";
    //
    //    /** @return Serializes the damage source to a new NBT compound and returns it. */
    //    @SuppressWarnings( "UnstableApiUsage" )
    //    public static CompoundTag serializeDamageSource( DamageSource source ) {
    //        CompoundTag tag = new CompoundTag();
    //        ResourceLocation key = IRegWrapper.forKey( Registries.DAMAGE_TYPE ).getKey( source.type() );
    //        tag.putString( TAG_SRC_DAMAGE_TYPE, key == null ? "minecraft:generic" : key.toString() );
    //        Vec3 pos = source.sourcePositionRaw();
    //        if( pos != null ) {
    //            NBTHelper.putDoubleList( tag, TAG_SRC_POSITION, List.of( pos.x(), pos.y(), pos.z() ) );
    //        }
    //        else if( source.getEntity() != null ) {
    //            tag.putUUID( TAG_SRC_ENTITY, source.getEntity().getUUID() );
    //            if( source.getDirectEntity() != null && source.isIndirect() )
    //                tag.putUUID( TAG_SRC_DIRECT_ENTITY, source.getDirectEntity().getUUID() );
    //        }
    //        return tag;
    //    }
    //
    //    /**
    //     * @return Deserializes the damage source from an NBT compound and returns it.
    //     * Null if unable to properly deserialize due to a registry issue.
    //     */
    //    @SuppressWarnings( "UnstableApiUsage" )
    //    @Nullable
    //    public static DamageSource deserializeDamageSource( EntityGetter level, CompoundTag tag ) {
    //        Holder<DamageType> typeHolder = null;
    //        if( NBTHelper.containsString( tag, TAG_SRC_DAMAGE_TYPE ) ) {
    //            ResourceLocation key = ResourceLocation.tryParse( tag.getString( TAG_SRC_DAMAGE_TYPE ) );
    //            if( key != null ) {
    //                IRegWrapper<DamageType> registry = IRegWrapper.forKey( Registries.DAMAGE_TYPE );
    //                DamageType type = registry.get( key );
    //                if( type != null ) {
    //                    Registry<DamageType> dynamicReg = registry.asVanillaRegistry();
    //                    if( dynamicReg != null ) typeHolder = dynamicReg.wrapAsHolder( type );
    //                }
    //            }
    //        }
    //        if( typeHolder == null ) return null;
    //
    //        if( NBTHelper.containsNumberList( tag, TAG_SRC_POSITION ) ) {
    //            List<Double> list = NBTHelper.getDoubleList( tag, TAG_SRC_POSITION );
    //            return new DamageSource( typeHolder, new Vec3(
    //                    !list.isEmpty() ? list.get( 0 ) : 0.0,
    //                    list.size() > 1 ? list.get( 1 ) : 0.0,
    //                    list.size() > 2 ? list.get( 2 ) : 0.0 ) );
    //        }
    //        else if( tag.hasUUID( TAG_SRC_ENTITY ) ) {
    //            Entity entity = getEntityByUUID( level, tag.getUUID( TAG_SRC_ENTITY ) );
    //            if( entity != null ) {
    //                if( tag.hasUUID( TAG_SRC_DIRECT_ENTITY ) ) {
    //                    Entity directEntity = getEntityByUUID( level, tag.getUUID( TAG_SRC_ENTITY ) );
    //                    if( directEntity != null )
    //                        return new DamageSource( typeHolder, directEntity, entity );
    //                }
    //                return new DamageSource( typeHolder, entity );
    //            }
    //        }
    //        return new DamageSource( typeHolder );
    //    }
    //
    //    /** @return The entity with a given UUID, or null if it does not exist in the level. */
    //    @Nullable
    //    private static Entity getEntityByUUID( EntityGetter level, UUID uuid ) {
    //        for( Entity entity : level.getEntities( (Entity) null, IForgeBlockEntity.INFINITE_EXTENT_AABB,
    //                ( entity ) -> true ) ) {
    //            if( uuid.equals( entity.getUUID() ) ) return entity;
    //        }
    //        return null;
    //    }
    
    
    private ReviveManager() {}
}