package com.sekwah.narutoaddon.mixin.common;

import com.sekwah.narutoaddon.capabilities.size.EntitySize;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes a resized body jump in proportion to itself.
 *
 * <p>Jump height follows the square of the launch speed, so multiplying the jump power by the square
 * root of the size gives a jump whose height is the size: a five-times-tall caster clears five times
 * the blocks instead of tripping over a single one. The factor never drops below 1, so a small body
 * still clears a full block instead of being trapped in a one-block hole. The hook sits on
 * {@code LivingEntity#getJumpPower} because {@code Player} does not override it - the player's own
 * {@code jumpFromGround} calls {@code super} straight into this method.</p>
 */
@Mixin(LivingEntity.class)
public class PlayerJumpPowerMixin {

    @Inject(method = "getJumpPower", at = @At("RETURN"), cancellable = true)
    private void narutoaddon$scaleJumpPower(CallbackInfoReturnable<Float> ci) {
        if (!((Object) this instanceof Player player)) {
            return;
        }
        float scale = EntitySize.get(player);
        if (scale == EntitySize.DEFAULT) {
            return;
        }
        // Never below normal: a half-sized body that cannot clear a single block is a body that
        // cannot leave a one-block hole. Proportional size for growth, a floor of one vanilla jump
        // for shrinking.
        float factor = (float) Math.max(1.0d, Math.sqrt(scale));
        ci.setReturnValue(ci.getReturnValue() * factor);
    }
}
