package com.sekwah.narutoaddon.abilities.jutsus;

import com.sekwah.narutoaddon.abilities.Ability;
import com.sekwah.narutoaddon.abilities.jutsus.ByakuganAbility;
import com.sekwah.narutoaddon.capabilities.INinjaData;
import com.sekwah.narutoaddon.config.NarutoConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Eight Trigrams: Empty Palm - a wedge of chakra pushed out of the hand that shoves a body back.
 */
public class AirPalmAbility extends Ability implements Ability.Cooldown {

    /** The shove is heard as a hard breath. */
    @Override
    public int noiseRadius() {
        return 12;
    }

    /** Combo for the empty palm. */
    public static final long COMBO = 3121L;

    public static final ResourceLocation ID = new ResourceLocation("narutomod", "air_palm");

    /** How far the palm reaches, in blocks. */
    private static final double RANGE = 20.0d;

    /** What it costs and how long before it can be cast again. */
    private static final float COST = 25.0f;
    private static final int COOLDOWN = 60;

    private static final int CHAKRA_REGEN_DELAY = 20;

    /** The strike itself. */
    private static final float DAMAGE = 4.0f;

    /** Hit radius around a point on the line. */
    private static final double HIT_RADIUS = 1.5d;

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public long defaultCombo() {
        return COMBO;
    }

    @Override
    public int getCooldown() {
        return COOLDOWN;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (!NarutoConfig.byakuganEnabled) {
            player.displayClientMessage(Component.translatable("jutsu.fail.byakugan.disabled")
                    .withStyle(ChatFormatting.RED), true);
            return false;
        }
        if (!ByakuganAbility.isActive(ninjaData)) {
            player.displayClientMessage(Component.translatable("jutsu.fail.byakugan.required",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW))
                    .withStyle(ChatFormatting.RED), true);
            return false;
        }

        float cost = this.masteryCost(ninjaData, COST);
        if (ninjaData.getChakra() < cost) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW))
                    .withStyle(ChatFormatting.RED), true);
            return false;
        }
        ninjaData.useChakra(cost, CHAKRA_REGEN_DELAY);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();

        // Find the nearest living body standing on the line of the palm.
        LivingEntity target = null;
        outer:
        for (double d = 0.0d; d <= RANGE; d += 0.5d) {
            Vec3 pos = eye.add(look.scale(d));
            AABB box = new AABB(pos, pos).inflate(HIT_RADIUS);
            for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box)) {
                if (entity == player || !entity.isAlive() || entity.isRemoved() || entity.isSpectator()) {
                    continue;
                }
                if (entity instanceof Player other && (other.isCreative() || other.isSpectator())) {
                    continue;
                }
                if (com.sekwah.narutoaddon.friends.Friends.isOwnSide(player, entity)) {
                    continue;
                }
                if (entity.distanceToSqr(pos) <= HIT_RADIUS * HIT_RADIUS) {
                    target = entity;
                    break outer;
                }
            }
        }

        if (target != null) {
            target.hurt(player.damageSources().playerAttack(player),
                    ByakuganAbility.masteredDamage(ninjaData, this, DAMAGE));
            target.knockback(1.5d, player.getX() - target.getX(), player.getZ() - target.getZ());
            target.hurtMarked = true;
            if (target instanceof Mob mob) {
                mob.setTarget(null);
            }
        }

        // Particles along the line.
        for (double d = 0.0d; d <= RANGE; d += 1.0d) {
            Vec3 at = eye.add(look.scale(d));
            level.sendParticles(ParticleTypes.CLOUD, at.x, at.y, at.z,
                    2, 0.2d, 0.2d, 0.2d, 0.01d);
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, at.x, at.y, at.z,
                    1, 0.2d, 0.2d, 0.2d, 0.0d);
        }
    }
}