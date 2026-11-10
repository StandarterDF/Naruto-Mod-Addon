package com.sekwah.narutoaddon.events;

import com.sekwah.narutoaddon.abilities.jutsus.SenriganAbility;
import com.sekwah.narutoaddon.capabilities.INinjaData;
import com.sekwah.narutoaddon.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutoaddon.config.NarutoConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Passive Senrigan technique: Fate Cancellation.
 *
 * <p>While the Senrigan is open, a killing blow is refused once every thirty minutes: the caster
 * is left at a single point of health, made briefly invulnerable, and stepped five blocks back from
 * the incoming danger. The cooldown is stored on the player's persistent data so it survives
 * dimension changes and relogging, and damage that bypasses invulnerability is left untouched so the
 * void and {@code /kill} still behave as the world ending.</p>
 *
 * <p>The retreat point is searched in a small cone behind the caster so a wall at the exact back
 * does not cancel the rescue; if no safe spot is found the teleport is simply skipped.</p>
 */
@Mod.EventBusSubscriber(modid = "narutoaddon", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SenriganFateEvents {

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.isCanceled()) return;
        if (!NarutoConfig.senriganEnabled) return;
        if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        if (event.getAmount() < player.getHealth()) return;
        INinjaData ninjaData = player.getCapability(NinjaCapabilityHandler.NINJA_DATA).resolve().orElse(null);
        if (ninjaData == null || !SenriganAbility.isActive(ninjaData)) return;
        long now = player.level().getGameTime();
        long last = player.getPersistentData().getLong("narutomod:senriganFateTick");
        if (last > 0L && now - last < 30L * 60L * 20L) return;
        event.setCanceled(true);
        player.setHealth(1.0f);
        player.invulnerableTime = 20;
        player.getPersistentData().putLong("narutomod:senriganFateTick", now);
        ServerLevel level = player.serverLevel();
        Vec3 back = player.getLookAngle().scale(-1.0d);
        Vec3 base = new Vec3(back.x, 0.0d, back.z);
        if (base.lengthSqr() < 1.0e-4d) return;
        base = base.normalize();
        Vec3 spot = null;
        for (int i = 0; i < 16; i++) {
            double angle = Math.toRadians((i / 2) * 5.0d) * (i % 2 == 0 ? 1.0d : -1.0d);
            double cos = Math.cos(angle), sin = Math.sin(angle);
            Vec3 dir = new Vec3(base.x * cos - base.z * sin, 0.0d, base.x * sin + base.z * cos);
            Vec3 candidate = player.position().add(dir.scale(5.0d));
            if (!level.getBlockState(BlockPos.containing(candidate).below()).isSolid()) continue;
            if (!level.noCollision(player, player.getBoundingBox().move(candidate.subtract(player.position())))) continue;
            spot = candidate;
            break;
        }
        if (spot != null) {
            player.teleportTo(spot.x, spot.y, spot.z);
            player.setDeltaMovement(Vec3.ZERO);
            player.resetFallDistance();
        }
        level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0d, player.getZ(), 24, 0.4d, 0.6d, 0.4d, 0.05d);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.7f, 1.4f);
        player.displayClientMessage(Component.translatable("jutsu.senrigan.fate"), true);
    }
}