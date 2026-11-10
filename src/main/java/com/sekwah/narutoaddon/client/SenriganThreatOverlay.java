package com.sekwah.narutoaddon.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sekwah.narutoaddon.abilities.jutsus.SenriganAbility;
import com.sekwah.narutoaddon.capabilities.INinjaData;
import com.sekwah.narutoaddon.capabilities.NinjaCapabilityHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * Client-side threat readout for the Senrigan: while the eye is open, every living body within 32
 * blocks carries a small stack of data above its head - health, armour, active effects and the loot
 * its table is most likely to drop. Drawn in the world as a billboard, like the Byakugan readout,
 * but without any post-processing screen effect.
 */
@Mod.EventBusSubscriber(modid = "narutoaddon", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SenriganThreatOverlay {

    private SenriganThreatOverlay() {
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
        INinjaData ninjaData = mc.player.getCapability(NinjaCapabilityHandler.NINJA_DATA).resolve().orElse(null);
        if (ninjaData == null || !SenriganAbility.isActive(ninjaData)) {
            return;
        }

        List<LivingEntity> entities = mc.level.getEntitiesOfClass(
                LivingEntity.class, mc.player.getBoundingBox().inflate(32.0d),
                entity -> entity != mc.player && entity.isAlive());

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
        Font font = mc.font;
        Vec3 cam = event.getCamera().getPosition();

        for (LivingEntity entity : entities) {
            if (entity.distanceToSqr(mc.player) > 32.0d * 32.0d) {
                continue;
            }

            // Do not draw the card when a block sits between the camera and the mob.
            Vec3 cameraPos = event.getCamera().getPosition();
            Vec3 target = entity.getEyePosition();
            ClipContext clipContext = new ClipContext(cameraPos, target, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, mc.player);
            HitResult hit = mc.level.clip(clipContext);
            if (hit.getType() != HitResult.Type.MISS) {
                double hitDist = hit.getLocation().distanceTo(cameraPos);
                double entityDist = target.distanceTo(cameraPos);
                if (hitDist < entityDist - 0.5d) {
                    continue;
                }
            }

            String hp = entity.getDisplayName().getString()
                    + String.format(Locale.ROOT, " %.0f/%.0f", entity.getHealth(), entity.getMaxHealth());
            float healthFraction = entity.getHealth() / Math.max(1.0f, entity.getMaxHealth());
            int hpColor = net.minecraft.util.Mth.hsvToRgb(healthFraction / 3.0f, 1.0f, 1.0f);

            int armor = entity.getArmorValue();

            Collection<MobEffectInstance> effects = entity.getActiveEffects();
            StringBuilder effectBuilder = new StringBuilder();
            int effectCount = 0;
            for (MobEffectInstance instance : effects) {
                if (effectCount >= 3) {
                    break;
                }
                if (effectCount > 0) {
                    effectBuilder.append(", ");
                }
                effectBuilder.append(instance.getEffect().getDisplayName().getString())
                        .append(" ").append(instance.getAmplifier() + 1);
                effectCount++;
            }
            if (effects.size() > 3) {
                if (effectCount > 0) {
                    effectBuilder.append(", ");
                }
                effectBuilder.append("+").append(effects.size() - 3);
            }
            String effectLine = effectBuilder.toString();

            List<ItemStack> loot = ClientThreatDrops.get(entity.getId());

            double dx = entity.getX() - cam.x;
            double dy = entity.getY() + entity.getBbHeight() + 0.6d - cam.y;
            double dz = entity.getZ() - cam.z;

            poseStack.pushPose();
            poseStack.translate((float) dx, (float) dy, (float) dz);
            poseStack.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
            poseStack.scale(-0.025f, -0.025f, 0.025f);

            int lineHeight = font.lineHeight;
            float y = 0f;

            // Top line: name + health.
            int widthHp = font.width(hp);
            font.drawInBatch(hp, -widthHp / 2f, y, hpColor, false,
                    poseStack.last().pose(), buffer, Font.DisplayMode.SEE_THROUGH, 0, 0xF000F0);
            y += lineHeight;

            // Effects line.
            if (!effectLine.isEmpty()) {
                int widthFx = font.width(effectLine);
                font.drawInBatch(effectLine, -widthFx / 2f, y, 0xFFCCCCCC, false,
                        poseStack.last().pose(), buffer, Font.DisplayMode.SEE_THROUGH, 0, 0xF000F0);
                y += lineHeight;
            }

            // Armour line.
            if (armor > 0) {
                String armorText = String.valueOf(armor);
                int widthAr = font.width(armorText);
                font.drawInBatch(armorText, -widthAr / 2f, y, 0xFFAAAAAA, false,
                        poseStack.last().pose(), buffer, Font.DisplayMode.SEE_THROUGH, 0, 0xF000F0);
                y += lineHeight;
            }

            // Loot icons line (bottom).
            if (!loot.isEmpty()) {
                float iconY = y;
                poseStack.pushPose();
                poseStack.translate(0f, iconY + 7f, 0f);
                poseStack.scale(-13f, -13f, 13f);
                float spacing = 1.2f;
                float startX = -(loot.size() - 1) * spacing / 2f;
                int light = 0xF000F0;
                for (int i = 0; i < loot.size(); i++) {
                    poseStack.pushPose();
                    poseStack.translate(startX + i * spacing, 0f, 0f);
                    mc.getItemRenderer().renderStatic(loot.get(i), ItemDisplayContext.GUI, light,
                            OverlayTexture.NO_OVERLAY, poseStack, buffer, mc.level, 0);
                    poseStack.popPose();
                }
                poseStack.popPose();
            }

            poseStack.popPose();
        }

        buffer.endBatch();
    }
}
