package com.sekwah.narutoaddon.events;

import net.minecraft.world.phys.Vec3;

import com.sekwah.narutoaddon.abilities.jutsus.CircularBlockAbility;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.FORGE;

/**
 * The parry window opened by {@link CircularBlockAbility}: a blow coming from the front inside the
 * window is refused outright and the attacker is shoved back.
 */
@Mod.EventBusSubscriber(modid = "narutoaddon", bus = FORGE)
public final class ParryEvents {

    private ParryEvents() {
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player p)) {
            return;
        }
        if (p.level().isClientSide) {
            return;
        }
        long until = p.getPersistentData().getLong("narutoaddon:parry_until");
        if (until <= p.level().getGameTime()) {
            return;
        }
        Entity attacker = event.getSource().getEntity();
        if (attacker == null) {
            return;
        }
        Vec3 toAttacker = attacker.position().subtract(p.position());
        if (toAttacker.lengthSqr() < 1.0E-6d) {
            return;
        }
        toAttacker = toAttacker.normalize();
        if (p.getLookAngle().dot(toAttacker) < 0.0d) {
            return;
        }

        event.setCanceled(true);
        attacker.setDeltaMovement(toAttacker.scale(1.2d));
        attacker.hurtMarked = true;

        if (p.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.ENCHANTED_HIT,
                    p.getX(), p.getY() + p.getBbHeight() * 0.5d, p.getZ(),
                    12, 0.4d, 0.4d, 0.4d, 0.0d);
        }
        p.getPersistentData().remove("narutoaddon:parry_until");
    }
}