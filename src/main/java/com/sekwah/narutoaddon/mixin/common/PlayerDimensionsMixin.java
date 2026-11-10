package com.sekwah.narutoaddon.mixin.common;

import com.sekwah.narutoaddon.capabilities.size.EntitySize;

import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Scales a player's body: the hitbox and the eye height together.
 *
 * <p>In 1.20.1 {@code Player} overrides {@code getDimensions(Pose)} and answers straight from its
 * pose table without calling {@code super}, so a hook on {@code Entity#getDimensions} never runs for
 * a player and the size changed nothing but the saved number. {@code Player} also overrides
 * {@code getStandingEyeHeight} with fixed values (1.62 standing, 1.27 crouching), so scaling the box
 * alone would leave a five-times-tall caster looking out of a normal-sized head - and aiming from
 * the wrong place. Both hooks sit on the overrides that actually decide the body.</p>
 */
@Mixin(Player.class)
public class PlayerDimensionsMixin {

    @Inject(method = "getDimensions", at = @At("RETURN"), cancellable = true)
    private void narutoaddon$scalePlayerDimensions(Pose pose, CallbackInfoReturnable<EntityDimensions> ci) {
        float scale = EntitySize.get((Player) (Object) this);
        if (scale != EntitySize.DEFAULT) {
            ci.setReturnValue(ci.getReturnValue().scale(scale));
        }
    }

    @Inject(method = "getStandingEyeHeight", at = @At("RETURN"), cancellable = true)
    private void narutoaddon$scalePlayerEyeHeight(Pose pose, EntityDimensions dimensions,
                                                  CallbackInfoReturnable<Float> ci) {
        float scale = EntitySize.get((Player) (Object) this);
        if (scale != EntitySize.DEFAULT) {
            ci.setReturnValue(ci.getReturnValue() * scale);
        }
    }
}
