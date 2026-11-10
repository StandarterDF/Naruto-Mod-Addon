package com.sekwah.narutoaddon.mixin.common;

import com.sekwah.narutoaddon.abilities.jutsus.SilenceAbility;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventDispatcher;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A silent caster is invisible to sculk sensors and the warden.
 *
 * <p>Vanilla dispatches every game event from {@link GameEventDispatcher#post}, so cancelling there
 * swallows the whole family - footsteps, item pickups, jumps - in one place instead of hooking each
 * emitter. The silence technique itself ({@link SilenceAbility#isSilenced(Player)}) is the only
 * source of silence now.</p>
 */
@Mixin(GameEventDispatcher.class)
public class GameEventDispatcherMixin {

    @Inject(method = "post", at = @At("HEAD"), cancellable = true)
    private void narutoaddon$silencePlayerEvents(GameEvent gameEvent, Vec3 source, GameEvent.Context context,
                                                 CallbackInfo callback) {
        Entity entity = context.sourceEntity();
        if (entity instanceof Player player) {
            if (SilenceAbility.isSilenced(player)) {
                callback.cancel();
            }
        }
    }

}
