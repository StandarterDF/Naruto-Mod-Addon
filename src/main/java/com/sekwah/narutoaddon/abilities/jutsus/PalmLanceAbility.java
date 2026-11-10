package com.sekwah.narutoaddon.abilities.jutsus;

import com.sekwah.narutoaddon.abilities.Ability;
import com.sekwah.narutoaddon.abilities.jutsus.ByakuganAbility;
import com.sekwah.narutoaddon.capabilities.INinjaData;
import com.sekwah.narutoaddon.config.NarutoConfig;
import com.sekwah.narutoaddon.friends.Friends;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Palm Lance: the gentle fist thrown as a thrust instead of a strike.
 *
 * <p>Instant version: the caster dashes through bodies (passing through entities, stopping only on
 * blocks) and every living thing within one block of the travelled segment is struck.</p>
 */
public class PalmLanceAbility extends Ability implements Ability.Cooldown {

    /** A palm into a body is heard about as far as a body being hit. */
    @Override
    public int noiseRadius() {
        return 8;
    }

    /** V B V V B B - the stance's seal, narrowed to a single thrust. */
    public static final long COMBO = 3321L;

    public static final ResourceLocation ID = new ResourceLocation("narutomod", "palm_lance");

    /** What the thrust costs and how long the eye needs before it can be made again. */
    private static final float COST = 35.0f;
    private static final int COOLDOWN = 120;

    private static final int CHAKRA_REGEN_DELAY = 20;

    /** Four steps of two blocks along the gaze. */
    private static final int DASH_STEPS = 4;
    private static final double STEP = 2.0d;

    /** Radius around the travelled segment that gets struck. */
    private static final double STRIKE_RADIUS = 1.0d;

    /** The strike itself. */
    private static final float DAMAGE = 12.0f;
    private static final int SLOW_TICKS = 40;

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
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("jutsu.fail.byakugan.disabled")
                    .withStyle(ChatFormatting.RED), true);
            return false;
        }
        if (!ByakuganAbility.isActive(ninjaData)) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("jutsu.fail.byakugan.required",
                    net.minecraft.network.chat.Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW))
                    .withStyle(ChatFormatting.RED), true);
            return false;
        }

        float cost = this.masteryCost(ninjaData, COST);
        if (ninjaData.getChakra() < cost) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("jutsu.fail.notenoughchakra",
                    net.minecraft.network.chat.Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW))
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
        if (!ByakuganAbility.isActive(ninjaData)) {
            return;
        }

        Vec3 look = player.getViewVector(1.0F);
        // Flatten to horizontal travel so the dash stays a step rather than a leap.
        Vec3 flatLook = new Vec3(look.x, 0.0d, look.z);
        if (flatLook.lengthSqr() < 1.0E-6d) {
            flatLook = new Vec3(0.0d, 0.0d, 1.0d);
        } else {
            flatLook = flatLook.normalize();
        }

        Vec3 start = player.position();
        Vec3 pos = start;
        for (int i = 0; i < DASH_STEPS; i++) {
            Vec3 next = pos.add(flatLook.scale(STEP));
            AABB box = player.getBoundingBox().move(next.subtract(pos));
            if (!level.noCollision(player, box)) {
                break;
            }
            pos = next;
        }
        player.setPos(pos.x, pos.y, pos.z);
        player.hurtMarked = true;
        player.fallDistance = 0.0f;
        Vec3 end = pos;

        // Strike everything within STRIKE_RADIUS of the travelled segment.
        AABB search = new AABB(start, end).inflate(STRIKE_RADIUS + 2.0d);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, search,
                entity -> canStrike(player, entity))) {
            if (distanceToSegment(target.position().add(0.0d, target.getBbHeight() * 0.5d, 0.0d),
                    start, end) > STRIKE_RADIUS) {
                continue;
            }
            target.hurt(player.damageSources().playerAttack(player),
                    ByakuganAbility.masteredDamage(ninjaData, this, DAMAGE));
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SLOW_TICKS, 0, false, true));
            target.hurtMarked = true;
        }

        // Particles along the line.
        int samples = (int) Math.max(4, start.distanceTo(end));
        for (int i = 0; i <= samples; i++) {
            double t = (double) i / (double) samples;
            double x = start.x + (end.x - start.x) * t;
            double y = start.y + player.getBbHeight() * 0.5d + (end.y - start.y) * t;
            double z = start.z + (end.z - start.z) * t;
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, x, y, z,
                    1, 0.0d, 0.0d, 0.0d, 0.0d);
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    private static double distanceToSegment(Vec3 point, Vec3 a, Vec3 b) {
        Vec3 ab = b.subtract(a);
        double len2 = ab.lengthSqr();
        if (len2 < 1.0E-9d) {
            return point.distanceTo(a);
        }
        double t = point.subtract(a).dot(ab) / len2;
        t = Math.max(0.0d, Math.min(1.0d, t));
        Vec3 closest = a.add(ab.scale(t));
        return point.distanceTo(closest);
    }

    private static boolean canStrike(Player caster, LivingEntity target) {
        if (target == caster || target.isRemoved() || target.isSpectator() || !target.isAlive()) {
            return false;
        }
        if (target instanceof Player other && (other.isCreative() || other.isSpectator())) {
            return false;
        }
        return !target.isAlliedTo(caster) && !Friends.areFriends(caster, target);
    }
}