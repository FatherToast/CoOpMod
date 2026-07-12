package fathertoast.coopoverhaul.client.vfx;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * TODO
 */
public final class RenderHelper {
    
    /** Pushes the pose stack and sets up for rendering in GUI-like coordinates. */
    public static void pushPose( PoseStack poseStack, Camera camera, float scale, double x, double y, double z ) {
        poseStack.pushPose();
        Vec3 cameraPos = camera.getPosition();
        poseStack.translate( x - cameraPos.x, y - cameraPos.y, z - cameraPos.z );
        poseStack.mulPose( camera.rotation() );
        poseStack.scale( -scale, -scale, scale );
    }
    
    /** Uses player context to determine if the player face should be rendered with a hat and/or upside down. */
    public static void drawFace( PoseStack poseStack, AbstractClientPlayer player, float x, float y, float size ) {
        boolean upsideDown = LivingEntityRenderer.isEntityUpsideDown( player );
        boolean hasHat = player.isModelPartShown( PlayerModelPart.HAT );
        drawFace( poseStack, player.getSkinTextureLocation(), x, y, size, hasHat, upsideDown );
    }
    
    /** Based on {@link PlayerFaceRenderer#draw(GuiGraphics, ResourceLocation, int, int, int, boolean, boolean)}. */
    public static void drawFace( PoseStack poseStack, ResourceLocation skinLocation, float x, float y, float size,
                                 boolean hasHat, boolean upsideDown ) {
        float v = 8 + (upsideDown ? 8 : 0);
        int pixelHeight = 8 * (upsideDown ? -1 : 1);
        blit( poseStack, skinLocation, x, y, size, size,
                8.0F, v, 8, pixelHeight, 64, 64 );
        if( hasHat ) drawHat( poseStack, skinLocation, x, y, size, upsideDown );
    }
    
    /** Based on {@link PlayerFaceRenderer#drawHat(GuiGraphics, ResourceLocation, int, int, int, boolean)}. */
    private static void drawHat( PoseStack poseStack, ResourceLocation skinLocation, float x, float y, float size,
                                 boolean upsideDown ) {
        float v = 8 + (upsideDown ? 8 : 0);
        int pixelHeight = 8 * (upsideDown ? -1 : 1);
        RenderSystem.enableBlend();
        blit( poseStack, skinLocation, x, y, size, size,
                40.0F, v, 8, pixelHeight, 64, 64 );
        RenderSystem.disableBlend();
    }
    
    /** Based on {@link GuiGraphics#blit(ResourceLocation, int, int, int, int, float, float, int, int, int, int)}. */
    public static void blit( PoseStack poseStack, ResourceLocation textureLocation, float x, float y, float width, float height,
                             float u, float v, int pixelWidth, int pixelHeight, int textureWidth, int textureHeight ) {
        blit( poseStack, textureLocation, x, x + width, y, y + height, 0,
                pixelWidth, pixelHeight, u, v, textureWidth, textureHeight );
    }
    
    /** Based on {@link GuiGraphics#blit(ResourceLocation, int, int, int, int, int, int, int, float, float, int, int)}. */
    public static void blit( PoseStack poseStack, ResourceLocation textureLocation,
                             float x1, float x2, float y1, float y2, float z,
                             int pixelWidth, int pixelHeight, float u, float v, int textureWidth, int textureHeight ) {
        innerBlit( poseStack, textureLocation, x1, x2, y1, y2, z,
                u / textureWidth, (u + pixelWidth) / textureWidth,
                v / textureHeight, (v + pixelHeight) / textureHeight );
    }
    
    /** Based on {@link GuiGraphics#innerBlit(ResourceLocation, int, int, int, int, int, float, float, float, float)}. */
    private static void innerBlit( PoseStack poseStack, ResourceLocation textureLocation,
                                   float x1, float x2, float y1, float y2, float z,
                                   float u1, float u2, float v1, float v2 ) {
        RenderSystem.setShaderTexture( 0, textureLocation );
        RenderSystem.setShader( GameRenderer::getPositionTexShader );
        Matrix4f pose = poseStack.last().pose();
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin( VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX );
        buffer.vertex( pose, x1, y1, z )
                .uv( u1, v1 ).endVertex();
        buffer.vertex( pose, x1, y2, z )
                .uv( u1, v2 ).endVertex();
        buffer.vertex( pose, x2, y2, z )
                .uv( u2, v2 ).endVertex();
        buffer.vertex( pose, x2, y1, z )
                .uv( u2, v1 ).endVertex();
        BufferUploader.drawWithShader( buffer.end() );
    }
    
    
    private RenderHelper() {}
}