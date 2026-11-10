package com.sekwah.narutoaddon.mixin.client;

import com.sekwah.narutoaddon.client.ClientSizeData;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Draws the local player at their real size.
 *
 * <p>{@code PlayerRenderer} overrides {@code scale(LivingEntity, ...)} with its own
 * {@code scale(AbstractClientPlayer, ...)}, so an injection into {@code LivingEntityRenderer#scale}
 * never runs for a player and the model stayed full size while the hitbox and the reach changed.
 * This one sits on the override, after the vanilla 0.9375 pass, so the model, the held items and the
 * name tag scale together.</p>
 */
@Mixin(PlayerRenderer.class)
public class PlayerRendererScaleMixin {

    @Inject(method = "scale(Lnet/minecraft/client/player/AbstractClientPlayer;Lcom/mojang/blaze3d/vertex/PoseStack;F)V",
            at = @At("TAIL"))
    private void narutoaddon$scalePlayerModel(AbstractClientPlayer player, PoseStack poseStack,
                                              float partialTicks, CallbackInfo ci) {
        if (player != Minecraft.getInstance().player) {
            return;
        }
        float scale = ClientSizeData.get();
        if (scale != 1.0f) {
            poseStack.scale(scale, scale, scale);
        }
    }
}
