package com.sekwah.narutoaddon.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.sekwah.narutoaddon.config.NarutoConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * The screen-space half of the Byakugan: swollen chakra veins creeping in from the corners with a
 * cold vignette behind them, both pulsing slowly.
 *
 * <p>The veins are one generated texture rather than geometry - they are decoration and nothing
 * more, so a single stretched quad keeps them almost free. The centre of the texture is fully
 * transparent, which is what keeps the middle of the screen readable; the vignette only bites at
 * the rim. Drawn under the whole HUD, so the hotbar, chat and the mod's own readouts stay legible.</p>
 *
 * <p>The overlay is never on screen without the eye: it follows the same fade the world picture
 * does, so closing the eye dissolves the veins together with the silhouettes.</p>
 */
@Mod.EventBusSubscriber(modid = "narutoaddon", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ByakuganVisionOverlay {

    private static final ResourceLocation VEINS =
            new ResourceLocation("narutoaddon", "textures/gui/byakugan_veins.png");

    /** How opaque the texture is at its strongest; the texture itself already carries the shape. */
    private static final float BASE_ALPHA = 0.45f;

    /** How much the veins breathe. Kept small: a strong pulse reads as a warning, not as an eye. */
    private static final float PULSE = 0.14f;

    private ByakuganVisionOverlay() {
    }

    @SubscribeEvent
    public static void render(RenderGuiEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || minecraft.options.hideGui) {
            return;
        }
        if (minecraft.player.isSpectator()) {
            return;
        }
        float fade = ClientSensingData.getFade();
        if (fade <= 0f || !ClientSensingData.isByakugan() || !NarutoConfig.byakuganScreenFx) {
            return;
        }

        long time = minecraft.level.getGameTime();
        float pulse = 0.5f + 0.5f * Mth.sin(time * 0.08f);
        float alpha = Mth.clamp(fade * (BASE_ALPHA - PULSE * 0.5f + PULSE * pulse), 0f, 1f);

        GuiGraphics graphics = event.getGuiGraphics();
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, alpha);
        graphics.blit(VEINS, 0, 0, width, height, 0f, 0f, 1024, 1024, 1024, 1024);
        // Blend stays on: the rest of the GUI expects it and the next overlay re-enables it anyway.
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }
}
