package com.sekwah.narutoaddon.client.sky;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.Camera;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.io.IOException;

/**
 * The sky picture that the sky blocks show.
 *
 * <p>Once per frame - and only while a sky block is in view - the real sky of the current level is
 * drawn into an off-screen texture: the vanilla sky pass, the sun and moon, the stars and the
 * clouds. Every sky-block face then samples that texture through projected coordinates, so the
 * block shows exactly the piece of sky that lies behind it, and the picture slides with the camera
 * like a window. Nothing here is a texture standing in for the sky: it is the same sky the level
 * itself draws.</p>
 */
public final class SkyBlockSky {

    /** The off-screen sky. Sized to the window; recreated when the window changes. */
    private static TextureTarget target;
    private static int width = -1;
    private static int height = -1;

    private static ShaderInstance shader;
    private static RenderType renderType;

    /** Set by the block renderer whenever a face was actually drawn this frame. */
    private static boolean requested;

    private SkyBlockSky() {
    }

    /** Marks that at least one sky face was drawn, so the sky texture has to be refreshed. */
    public static void request() {
        requested = true;
    }

    /** @return the draw type of the sky faces, or null until the shader has loaded. */
    public static RenderType renderType() {
        return renderType;
    }

    public static void onRegisterShaders(RegisterShadersEvent event) {
        try {
            ShaderInstance instance = new ShaderInstance(event.getResourceProvider(),
                    new ResourceLocation("narutoaddon_sky"), DefaultVertexFormat.POSITION);
            event.registerShader(instance, loaded -> {
                shader = loaded;
                renderType = createRenderType();
            });
        } catch (IOException exception) {
            org.slf4j.LoggerFactory.getLogger("narutoaddon").error("Could not load the sky block shader", exception);
        }
    }

    private static RenderType createRenderType() {
        return RenderType.create("narutoaddon_sky", DefaultVertexFormat.POSITION, VertexFormat.Mode.QUADS, 256,
                false, false,
                RenderType.CompositeState.builder()
                        .setShaderState(new RenderStateShard.ShaderStateShard(() -> shader))
                        .setTextureState(new RenderStateShard.EmptyTextureStateShard(
                                SkyBlockSky::bindSkyTexture, () -> {
                        }))
                        .createCompositeState(false));
    }

    private static void bindSkyTexture() {
        if (target != null) {
            RenderSystem.setShaderTexture(0, target.getColorTextureId());
        } else {
            RenderSystem.setShaderTexture(0, 0);
        }
    }

    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRIPWIRE_BLOCKS) {
            return;
        }
        if (!requested || renderType == null) {
            return;
        }
        requested = false;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        Window window = minecraft.getWindow();
        int windowWidth = window.getWidth();
        int windowHeight = window.getHeight();
        if (windowWidth <= 0 || windowHeight <= 0) {
            return;
        }
        if (target == null || width != windowWidth || height != windowHeight) {
            if (target != null) {
                target.destroyBuffers();
            }
            target = new TextureTarget(windowWidth, windowHeight, true, Minecraft.ON_OSX);
            width = windowWidth;
            height = windowHeight;
        }
        RenderTarget main = minecraft.getMainRenderTarget();
        target.clear(Minecraft.ON_OSX);
        target.bindWrite(true);
        drawSky(minecraft, event.getPoseStack(), event.getPartialTick(), event.getProjectionMatrix());
        target.unbindWrite();
        main.bindWrite(true);
    }

    /**
     * The same steps the level runs for its own sky, aimed at the off-screen target instead of the
     * screen. Weather is left out on purpose: rain in a sealed cube would be a lie.
     */
    private static void drawSky(Minecraft minecraft, PoseStack poseStack, float partialTick,
                                Matrix4f projectionMatrix) {
        GameRenderer gameRenderer = minecraft.gameRenderer;
        Camera camera = gameRenderer.getMainCamera();
        float renderDistance = gameRenderer.getRenderDistance();

        FogRenderer.setupColor(camera, partialTick, minecraft.level,
                minecraft.options.getEffectiveRenderDistance(),
                gameRenderer.getDarkenWorldAmount(partialTick));
        FogRenderer.setupNoFog();
        RenderSystem.clear(16640, Minecraft.ON_OSX);
        FogRenderer.setupFog(camera, FogRenderer.FogMode.FOG_SKY, renderDistance, false, partialTick);
        RenderSystem.setShader(GameRenderer::getPositionShader);
        minecraft.levelRenderer.renderSky(poseStack, projectionMatrix, partialTick, camera, false,
                () -> FogRenderer.setupFog(camera, FogRenderer.FogMode.FOG_SKY, renderDistance, false, partialTick));

        PoseStack modelView = RenderSystem.getModelViewStack();
        modelView.pushPose();
        modelView.mulPoseMatrix(poseStack.last().pose());
        RenderSystem.applyModelViewMatrix();
        if (minecraft.options.getCloudsType() != CloudStatus.OFF) {
            RenderSystem.setShader(GameRenderer::getPositionTexColorNormalShader);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            minecraft.levelRenderer.renderClouds(poseStack, projectionMatrix, partialTick,
                    camera.getPosition().x, camera.getPosition().y, camera.getPosition().z);
        }
        modelView.popPose();
        RenderSystem.applyModelViewMatrix();
        FogRenderer.setupNoFog();
    }
}
