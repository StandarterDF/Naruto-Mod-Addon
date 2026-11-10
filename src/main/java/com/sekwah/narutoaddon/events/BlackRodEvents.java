package com.sekwah.narutoaddon.events;

import com.sekwah.narutoaddon.abilities.jutsus.BlackRodThrowAbility;
import com.sekwah.narutoaddon.friends.Friends;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

/**
 * The black rod's aftermath: a pinned body cannot move, and killing it while the rod still holds
 * spreads the withering through everything standing around it.
 *
 * <p>Both halves live here rather than in the ability because the ability is over the moment the rod
 * leaves the hand; the pin has to be enforced tick by tick and the burst has to happen on a death,
 * which is an event the ability never sees.</p>
 */
@Mod.EventBusSubscriber(modid = "narutoaddon", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BlackRodEvents {

    /** Radius of the withering burst, in blocks. */
    private static final double BURST_RADIUS = 10.0d;

    /** How long the burst's wither lasts, in ticks. */
    private static final int BURST_WITHER_TICKS = 200;

    /** Amplifier of the burst's wither (1 = wither II). */
    private static final int BURST_WITHER_AMPLIFIER = 1;

    private BlackRodEvents() {
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) {
            return;
        }
        CompoundTag data = entity.getPersistentData();
        if (!data.contains(BlackRodThrowAbility.PIN_UNTIL_KEY)) {
            return;
        }
        long until = data.getLong(BlackRodThrowAbility.PIN_UNTIL_KEY);
        if (entity.level().getGameTime() >= until) {
            data.remove(BlackRodThrowAbility.PIN_UNTIL_KEY);
            return;
        }
        // Nailed down: no stride, no jump, no knockback drift. Gravity is left alone so the body
        // still settles onto the ground it was pinned to.
        Vec3 motion = entity.getDeltaMovement();
        entity.setDeltaMovement(0.0d, Math.min(0.0d, motion.y), 0.0d);
        entity.hurtMarked = true;
        entity.setNoGravity(false);
        if (entity.tickCount % 10 == 0 && entity.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.SMOKE, entity.getX(),
                    entity.getY() + entity.getBbHeight() * 0.5d, entity.getZ(),
                    3, 0.25d, 0.25d, 0.25d, 0.01d);
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        CompoundTag data = victim.getPersistentData();
        if (!data.contains(BlackRodThrowAbility.PIN_UNTIL_KEY)) {
            return;
        }
        long until = data.getLong(BlackRodThrowAbility.PIN_UNTIL_KEY);
        boolean stillPinned = victim.level().getGameTime() < until;
        UUID ownerId = data.contains(BlackRodThrowAbility.PIN_OWNER_KEY)
                ? data.getUUID(BlackRodThrowAbility.PIN_OWNER_KEY) : null;
        data.remove(BlackRodThrowAbility.PIN_UNTIL_KEY);
        data.remove(BlackRodThrowAbility.PIN_OWNER_KEY);
        if (!stillPinned || !(victim.level() instanceof ServerLevel level)) {
            return;
        }
        Player owner = ownerId == null || level.getServer() == null
                ? null : level.getServer().getPlayerList().getPlayer(ownerId);
        AABB area = victim.getBoundingBox().inflate(BURST_RADIUS);
        for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, area)) {
            if (other == victim || other == owner || !other.isAlive()) {
                continue;
            }
            if (other instanceof Player player && (player.isCreative() || player.isSpectator())) {
                continue;
            }
            if (owner != null && Friends.isOwnSide(owner, other)) {
                continue;
            }
            other.addEffect(new MobEffectInstance(MobEffects.WITHER, BURST_WITHER_TICKS,
                    BURST_WITHER_AMPLIFIER));
        }
        level.sendParticles(ParticleTypes.SQUID_INK, victim.getX(),
                victim.getY() + victim.getBbHeight() * 0.5d, victim.getZ(),
                60, BURST_RADIUS * 0.5d, 1.5d, BURST_RADIUS * 0.5d, 0.05d);
    }
}
