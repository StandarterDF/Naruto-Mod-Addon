package com.sekwah.narutoaddon.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sekwah.narutoaddon.abilities.jutsus.ByakuganAbility;
import com.sekwah.narutoaddon.capabilities.INinjaData;
import com.sekwah.narutoaddon.capabilities.NinjaCapabilityHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * Client-side overlay for the Byakugan: while the eye is open every living body inside its vision
 * bubble carries three small lines above its head - name and health, armour, and the effects running
 * through it. Drawn in the world as a billboard, so it reads through walls the same way the eye sees
 * through them. Nothing here touches the server or the world model.
 */
@Mod.EventBusSubscriber(modid = "narutoaddon", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ByakuganReadOverlay {

    /** A light grey for the effect line. */
    private static final int EFFECTS_COLOUR = 0xFFCCCCCC;

    private ByakuganReadOverlay() {
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }
        Player player = mc.player;
        INinjaData data = player.getCapability(NinjaCapabilityHandler.NINJA_DATA).resolve().orElse(null);
        if (data == null || !ByakuganAbility.isActive(data)) {
            return;
        }

        float radius = ByakuganAbility.visionRadius(data);
        Vec3 cam = event.getCamera().getPosition();

        List<LivingEntity> entities = mc.level.getEntitiesOfClass(
                LivingEntity.class, player.getBoundingBox().inflate(radius));

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
        Font font = mc.font;

        for (LivingEntity entity : entities) {
            if (entity == player) {
                continue;
            }
            if (entity.distanceToSqr(player) > (double) radius * radius) {
                continue;
            }

            String line1 = entity.getDisplayName().getString()
                    + String.format(" %.1f / %.1f", entity.getHealth(), entity.getMaxHealth());
            String line2 = "ARM " + entity.getArmorValue();

            StringBuilder effects = new StringBuilder();
            int effectCount = 0;
            for (MobEffectInstance effect : entity.getActiveEffects()) {
                if (effectCount >= 4) {
                    break;
                }
                if (effectCount > 0) {
                    effects.append(", ");
                }
                effects.append(effect.getEffect().getDisplayName().getString())
                        .append(" ").append(effect.getAmplifier() + 1);
                effectCount++;
            }
            String line3 = effects.toString();

            double dx = entity.getX() - cam.x;
            double dy = entity.getY() + entity.getBbHeight() + 0.5D - cam.y;
            double dz = entity.getZ() - cam.z;

            poseStack.pushPose();
            poseStack.translate((float) dx, (float) dy, (float) dz);
            poseStack.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
            poseStack.scale(-0.025f, -0.025f, 0.025f);

            int width1 = font.width(line1);
            font.drawInBatch(line1, -width1 / 2f, 0f, 0xFFFFFFFF, false,
                    poseStack.last().pose(), buffer, Font.DisplayMode.SEE_THROUGH, 0, 0xF000F0);

            int width2 = font.width(line2);
            font.drawInBatch(line2, -width2 / 2f, (float) font.lineHeight, 0xFF66D9FF, false,
                    poseStack.last().pose(), buffer, Font.DisplayMode.SEE_THROUGH, 0, 0xF000F0);

            if (!line3.isEmpty()) {
                int width3 = font.width(line3);
                font.drawInBatch(line3, -width3 / 2f, (float) (font.lineHeight * 2), EFFECTS_COLOUR, false,
                        poseStack.last().pose(), buffer, Font.DisplayMode.SEE_THROUGH, 0, 0xF000F0);
            }

            poseStack.popPose();
        }

        buffer.endBatch();
    }
}