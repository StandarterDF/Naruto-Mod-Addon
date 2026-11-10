package com.sekwah.narutoaddon.mixin.client;

import com.sekwah.narutoaddon.client.ClientSizeData;

import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Pulls the third-person camera back for a grown caster.
 *
 * <p>The vanilla third-person distance is a flat four blocks. That is fine for a normal body, but a
 * five-times-tall one fills the whole screen from four blocks away, and the camera ends up inside the
 * caster's own silhouette. Scaling the distance with the body keeps the same framing at every size;
 * the returned distance still goes through vanilla's block clipping, so the camera does not pass
 * through walls either.</p>
 */
@Mixin(Camera.class)
public class CameraSizeMixin {

    @ModifyVariable(method = "getMaxZoom", at = @At("HEAD"), argsOnly = true)
    private double narutoaddon$scaleThirdPersonDistance(double distance) {
        Entity entity = ((Camera) (Object) this).getEntity();
        if (!(entity instanceof Player player) || player != net.minecraft.client.Minecraft.getInstance().player) {
            return distance;
        }
        float scale = ClientSizeData.get();
        if (scale <= 1.0f) {
            // Only ever pull back: a shrunken body is already small, and moving the camera closer
            // would just push it into the model.
            return distance;
        }
        // Square root rather than the size itself: proportional pull-back put a five-times-tall
        // caster twenty blocks away, which framed the body but lost the world around it. The whole
        // body still fits at sqrt(5) ~ 2.24 body-lengths, and the view stays readable.
        return distance * Math.sqrt(scale);
    }
}
