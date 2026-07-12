package fathertoast.coopoverhaul.client.coordination;

import com.mojang.blaze3d.systems.RenderSystem;
import fathertoast.coopoverhaul.client.config.ClientConfig;
import fathertoast.coopoverhaul.client.config.ClientPreferences;
import fathertoast.coopoverhaul.client.event.ClientGameEventHandler;
import fathertoast.coopoverhaul.client.vfx.HeartType;
import fathertoast.coopoverhaul.common.compat.naturalabsorption.CONaturalAbsorptionPlugin;
import fathertoast.coopoverhaul.common.core.CoOpOverhaulMod;
import fathertoast.crust.api.lib.CrustMath;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.Optionull;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import org.lwjgl.opengl.GL11;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @see net.minecraftforge.client.gui.overlay.VanillaGuiOverlay
 * @see ForgeGui
 */
public final class PartyStatusGuiOverlay {
    
    /** The player order from {@link net.minecraft.client.gui.components.PlayerTabOverlay}, but with the local player first. */
    private static final Comparator<Entry> PLAYER_COMPARATOR = Comparator
            .<Entry>comparingInt( entry -> entry.player().isLocalPlayer() ? 0 : 1 )
            .thenComparing( entry -> entry.info().getGameMode() == GameType.SPECTATOR ? 1 : 0 )
            .thenComparing( entry ->
                    Optionull.mapOrDefault( entry.info().getTeam(), PlayerTeam::getName, "" ) )
            .thenComparing( entry ->
                    entry.info().getProfile().getName(), String::compareToIgnoreCase );
    
    private static final int ROW_HEIGHT = 8;
    private static final ResourceLocation GUI_ICONS_LOCATION = ResourceLocation.withDefaultNamespace( "textures/gui/icons.png" );
    private static final int ICON_SIZE = 9;
    private static final int HEARTS_PER_ROW = 10;
    private static final int HEALTH_WIDTH = (ICON_SIZE - 1) * HEARTS_PER_ROW + 1; // Size-1 because they overlap 1 px
    
    // Calculations for player model rendering; based on values/texture of InventoryScreen
    //    private static final int M_WIDTH = 50;
    //    private static final int M_HEIGHT = 65;
    //    private static final int M_Y_POS = M_HEIGHT - 3;
    //    private static final int M_SCALE = 30;
    //    private static final double H2W = (double) M_WIDTH / M_HEIGHT;
    //    private static final double H2Y = (double) M_Y_POS / M_HEIGHT;
    //    private static final double H2S = (double) M_SCALE / M_HEIGHT;
    private static final double H2W = 0.7692307692307693;
    private static final double H2Y = 0.9538461538461539;
    private static final double H2S = 0.46153846153846156;
    
    
    /** Health trackers used to animate health bars. */
    private static final Map<Integer, HealthState> HEALTH_STATES = new Int2ObjectOpenHashMap<>();
    
    /** Called each GUI overlay render cycle to render this overlay. */
    public static void render( ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight ) {
        Minecraft client = gui.getMinecraft();
        ClientPreferences.PartyOverlay config = ClientConfig.PREFS.PARTY_OVERLAY;
        if( client.options.hideGui || !config.enabled.get() || client.level == null || client.player == null ||
                config.hideWhenDebugOn.get() && client.options.renderDebug ) return;
        
        List<Entry> players = client.player.connection.getListedOnlinePlayers().stream()
                .mapMulti( PartyStatusGuiOverlay::mapPlayer ).sorted( PLAYER_COMPARATOR ).limit( 16 ).toList();
        if( players.isEmpty() ) return;
        
        // Configured panel contents
        int playerDisplayHeight = config.panelPortraitHeight.get();
        boolean showModel = config.panelPortraitUsesModel.get();
        //        boolean showEffects = false; //TODO sync player effects to render them here
        //        boolean showEffectLevels = false;
        int padding = config.panelPadding.get();
        int borderColor = config.panelBorderColor.get();
        
        // Calculate handy constants
        int panels = players.size();
        int panelWidth = 0;
        int panelHeight = 0;
        
        boolean smallFaces = playerDisplayHeight <= ROW_HEIGHT;
        int playerDisplayWidth;
        if( playerDisplayHeight > 0 ) {
            if( showModel ) {
                playerDisplayWidth = (int) (playerDisplayHeight * H2W);
            }
            else {
                //noinspection SuspiciousNameCombination
                playerDisplayWidth = playerDisplayHeight;
            }
            panelWidth += playerDisplayWidth + padding;
        }
        else playerDisplayWidth = 0;
        if( config.panelsShowNames.get() ) {
            // Note: Max chars in a profile's name is 16, and glyphs are typically no wider than 6px
            int nameBoxWidth = HEALTH_WIDTH - (playerDisplayHeight + padding);
            for( Entry entry : players ) {
                int nameWidth = client.font.width( getNameForDisplay( entry ) );
                if( nameBoxWidth < nameWidth ) nameBoxWidth = nameWidth;
            }
            panelWidth += nameBoxWidth + padding;
            panelHeight += ROW_HEIGHT + padding;
        }
        int healthHeight = healthHeightLimit( config.panelsHealthRows.get() );
        if( healthHeight > 0 ) {
            if( config.panelsShowNames.get() ) {
                panelWidth = Math.max( panelWidth, HEALTH_WIDTH + (smallFaces ? padding : playerDisplayWidth + (padding << 1)) );
            }
            else panelWidth += HEALTH_WIDTH + padding;
            panelHeight += healthHeight + padding;
        }
        
        panelWidth += padding;
        panelHeight = Math.max( panelHeight, playerDisplayHeight + padding ) + padding;
        
        //TODO Make this smarter; limit and maybe compress panels to fit on screen
        int panelStep = panelHeight + config.panelSpacing.get();
        int width = panelWidth;
        int height = panelStep * panels - config.panelSpacing.get();
        
        // Configured screen location
        int posX = config.anchorX.get().pos( screenWidth, width ) +
                config.offsetX.get();
        int posY = config.anchorY.get().pos( screenHeight, height ) +
                config.offsetY.get();
        
        // Setup rendering
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate( GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                GL11.GL_ONE, GL11.GL_ZERO );
        
        int yp = posY;
        for( Entry entry : players ) {
            int x = posX;
            int y = yp;
            
            guiGraphics.fill( x, y, x + panelWidth, y + 1,
                    borderColor );
            guiGraphics.fill( x, y + panelHeight - 1, x + panelWidth, y + panelHeight,
                    borderColor );
            guiGraphics.fill( x, y + 1, x + 1, y + panelHeight - 1,
                    borderColor );
            guiGraphics.fill( x + panelWidth - 1, y + 1, x + panelWidth, y + panelHeight - 1,
                    borderColor );
            guiGraphics.fill( x + 1, y + 1, x + panelWidth - 1, y + panelHeight - 1,
                    config.panelBackgroundColor.get() );
            x += padding;
            y += padding;
            
            RenderSystem.enableBlend();
            if( playerDisplayHeight > 0 ) {
                if( showModel ) {
                    // Render the player's model similar to the inventory
                    // Derived from rendering in an approximate 50px wide x 70px high box at the point (25, 57) at scale 30
                    int offsetX = playerDisplayWidth / 2;
                    int offsetY = (int) (playerDisplayHeight * H2Y);
                    int modelScale = (int) (playerDisplayHeight * H2S);
                    
                    guiGraphics.fill( x, y, x + playerDisplayWidth, y + playerDisplayHeight,
                            config.panelPortraitColor.get() );
                    ClientGameEventHandler.skipNextNameTag();
                    // Note the Forge-added parameters are misleading:
                    // "angleXComponent" rotates on the +Y axis (looking from the top of the screen, clockwise)
                    // "angleYComponent" rotates on the -X axis (looking from the left side of the screen, anti-clockwise)
                    // Units are in degrees and the method multiplies these angles by 20 (head angle multiplied by 40)
                    //TODO fix walk/run/swim animations freezing when players are culled by the frustum
                    InventoryScreen.renderEntityInInventoryFollowsAngle( guiGraphics, x + offsetX, y + offsetY,
                            modelScale, -1.0F, -0.2F, entry.player() );
                }
                else {
                    // Render the player's face texture
                    boolean upsideDown = LivingEntityRenderer.isEntityUpsideDown( entry.player() );
                    boolean hasHat = entry.player().isModelPartShown( PlayerModelPart.HAT );
                    PlayerFaceRenderer.draw( guiGraphics, entry.info().getSkinLocation(),
                            x, y, playerDisplayHeight, hasHat, upsideDown );
                }
                x += playerDisplayWidth + padding;
            }
            if( config.panelsShowNames.get() ) {
                guiGraphics.drawString( client.font, getNameForDisplay( entry ),
                        x, y, entry.player().isSpectator() ? 0x90_FFFFFF : 0xFF_FFFFFF );
                y += ROW_HEIGHT + padding;
            }
            if( healthHeight > 0 ) {
                if( smallFaces && config.panelsShowNames.get() ) x = posX + padding;
                renderHearts( x, y, healthHeight, gui, guiGraphics, entry );
            }
            
            yp += panelStep;
        }
    }
    
    /** Maps and player info into player entries and filters them out based on config settings. */
    @SuppressWarnings( "DataFlowIssue" ) // Ignore the NPE warnings, we checked before calling this
    private static void mapPlayer( PlayerInfo playerInfo, Consumer<Entry> add ) {
        AbstractClientPlayer player = (AbstractClientPlayer) Minecraft.getInstance().level
                .getPlayerByUUID( playerInfo.getProfile().getId() );
        if( player != null ) {
            if( player.isLocalPlayer() ) {
                if( ClientConfig.PREFS.PARTY_OVERLAY.showSelf.get() ) add.accept( new Entry( player, playerInfo ) );
            }
            else if( Minecraft.getInstance().player.distanceToSqr( player ) < ClientConfig.PREFS.PARTY_OVERLAY.rangeSq.get() ) {
                add.accept( new Entry( player, playerInfo ) );
            }
        }
    }
    
    /** @return The name to display for a player entry. */
    private static Component getNameForDisplay( Entry entry ) {
        return entry.info().getTabListDisplayName() != null ?
                decorateName( entry.player(), entry.info().getTabListDisplayName().copy() ) :
                decorateName( entry.player(), PlayerTeam.formatNameForTeam( entry.info().getTeam(),
                        Component.literal( entry.info().getProfile().getName() ) ) );
    }
    
    /** @return The name, formatted based on the player's state. */
    private static Component decorateName( AbstractClientPlayer player, MutableComponent name ) {
        return player.isSpectator() ? name.withStyle( ChatFormatting.ITALIC ) : name;
    }
    
    /** Renders the player's hearts display panel element. */
    private static void renderHearts( int x, int y, int height, ForgeGui gui, GuiGraphics guiGraphics, Entry entry ) {
        
        Minecraft client = gui.getMinecraft();
        
        int health = Mth.ceil( entry.player().getHealth() );
        HealthState healthState = HEALTH_STATES.computeIfAbsent( entry.player().getId(),
                id -> new HealthState( entry, health ) );
        healthState.update( health, gui.getGuiTicks() );
        int maxHealth = Math.max( Mth.ceil( entry.player().getMaxHealth() ), healthState.displayedValue() );
        int heartContainers = Mth.positiveCeilDiv( maxHealth, 2 );
        
        int absorbStart = heartContainers << 1;
        int absorb = Mth.ceil( entry.player().getAbsorptionAmount() );
        int absorbContainers;
        if( CoOpOverhaulMod.NA_INSTALLED )
            absorbContainers = Mth.ceil( CONaturalAbsorptionPlugin.getMaxAbsorption( entry.player() ) / 2.0 );
        else absorbContainers = Mth.positiveCeilDiv( absorb, 2 );
        
        int containers = heartContainers + absorbContainers;
        int rows = Mth.positiveCeilDiv( containers, HEARTS_PER_ROW );
        int rowSpacing = healthRowSpacing( rows, height );
        
        HeartType heartType = HeartType.forPlayer( entry.player() );
        boolean blinking = healthState.isBlinking( gui.getGuiTicks() );
        boolean hardcore = entry.player().level().getLevelData().isHardcore();
        
        boolean jittering = health + absorb < 5;
        
        if( rowSpacing < 3 ) {
            // TODO Placeholder; copied from vanilla "health scoreboard" display
            // Too many hearts, write it out
            // Shift color by health %: red @ 0%, yellow @ 50%, green at 100%
            float g = Mth.clamp( (float) health / (float) maxHealth, 0.0F, 1.0F );
            int color = CrustMath.toRGB( 1.0F - g, g, 0 );
            String healthDisplay = "" + (float) health / 2.0F;
            if( client.font.width( healthDisplay + "hp" ) <= HEALTH_WIDTH ) {
                healthDisplay += "hp";
            }
            guiGraphics.drawString( client.font, healthDisplay, x, y, color );
        }
        else {
            // Actually render hearts
            for( int heart = 0; heart < containers; heart++ ) {
                int row = heart / HEARTS_PER_ROW;
                int col = heart % HEARTS_PER_ROW;
                int xh = x + col * 8;
                int yh = y + row * rowSpacing;
                
                if( jittering ) yh += healthState.random().nextInt( 2 );
                
                blitHeart( guiGraphics, xh, yh, HeartType.CONTAINER, false, blinking, hardcore );
                int heartHealth = heart << 1;
                if( heart >= heartContainers ) {
                    int heartAbsorb = heartHealth - absorbStart;
                    if( heartAbsorb < absorb ) {
                        blitHeart( guiGraphics, xh, yh, heartType == HeartType.WITHERED ? heartType : HeartType.ABSORPTION,
                                heartAbsorb + 1 == absorb, false, hardcore );
                    }
                }
                if( blinking && heartHealth < healthState.displayedValue() ) {
                    blitHeart( guiGraphics, xh, yh, heartType,
                            heartHealth + 1 == healthState.displayedValue(), true, hardcore );
                }
                if( heartHealth < health ) {
                    blitHeart( guiGraphics, xh, yh, heartType,
                            heartHealth + 1 == health, false, hardcore );
                }
            }
        }
    }
    
    /** @return The space to allocate for health rendering given a number of rows. */
    private static int healthHeightLimit( double rows ) {
        if( rows > 1.0 ) {
            int rowH = rows > 8 ? 3 : 12 - Mth.floor( rows );
            return Mth.ceil( (double) rowH * (rows - 1) ) + ICON_SIZE;
        }
        return rows == 1.0 ? ICON_SIZE : 0;
    }
    
    /**
     * @return The space between each row of hearts given the space constraint.
     * If the returned height is less than 3, we don't have enough space to render hearts normally.
     */
    private static int healthRowSpacing( int rows, int maxHeight ) {
        if( rows < 2 ) return 3; // Doesn't matter what we return, as long as it's not less than 3
        int rowH = rows > 8 ? 3 : 12 - rows;
        int openSpace = maxHeight - (rowH * (rows - 1) + ICON_SIZE);
        if( openSpace < 0 ) return rowH - Mth.positiveCeilDiv( -openSpace, rows );
        return rowH;
    }
    
    /** Draws a heart icon. */
    private static void blitHeart( GuiGraphics guiGraphics, int x, int y, HeartType type,
                                   boolean half, boolean blinking, boolean hardcore ) {
        blitIcon( guiGraphics, x, y, type.getU( half, blinking ), type.getV( hardcore ) );
    }
    
    /** Draws a standard 9x9 GUI icon. */
    private static void blitIcon( GuiGraphics guiGraphics, int x, int y, int u, int v ) {
        guiGraphics.blit( GUI_ICONS_LOCATION, x, y,
                u, v, ICON_SIZE, ICON_SIZE );
    }
    
    /** Simple class for tracking a player's health state so that it can be animated. */
    private static class HealthState {
        private static final long DISPLAY_UPDATE_DELAY = 20L;
        private static final long DECREASE_BLINK_DURATION = 20L;
        private static final long INCREASE_BLINK_DURATION = 10L;
        
        private final RandomSource random = RandomSource.create();
        private final Entry playerEntry;
        
        private int lastValue;
        private int displayedValue;
        private long lastUpdateTick;
        private long blinkUntilTick;
        
        public HealthState( Entry entry, int health ) {
            playerEntry = entry;
            displayedValue = health;
            lastValue = health;
        }
        
        public void update( int newValue, long guiTicks ) {
            random.setSeed( guiTicks ^ playerEntry.info().getProfile().getId().getMostSignificantBits() );
            
            if( newValue != lastValue ) {
                long blinkDuration = newValue < lastValue ? DECREASE_BLINK_DURATION : INCREASE_BLINK_DURATION;
                blinkUntilTick = guiTicks + blinkDuration;
                lastValue = newValue;
                lastUpdateTick = guiTicks;
            }
            
            if( guiTicks - lastUpdateTick > DISPLAY_UPDATE_DELAY ) {
                displayedValue = newValue;
            }
        }
        
        public RandomSource random() { return random; }
        
        public int displayedValue() { return displayedValue; }
        
        public boolean isBlinking( long guiTicks ) {
            return blinkUntilTick > guiTicks && (blinkUntilTick - guiTicks) % 6L >= 3L;
        }
    }
    
    /** Used to prevent us needing to look up the player entity more than once per player. */
    private record Entry( AbstractClientPlayer player, PlayerInfo info ) {}
    
    
    private PartyStatusGuiOverlay() {}
}