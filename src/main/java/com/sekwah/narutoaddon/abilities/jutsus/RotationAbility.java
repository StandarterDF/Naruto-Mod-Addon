package com.sekwah.narutoaddon.abilities.jutsus;

import com.sekwah.narutoaddon.abilities.Ability;
import com.sekwah.narutoaddon.abilities.jutsus.ByakuganAbility;
import com.sekwah.narutoaddon.capabilities.INinjaData;
import com.sekwah.narutoaddon.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutoaddon.config.NarutoConfig;
import com.sekwah.narutoaddon.friends.Friends;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Eight Trigrams Palms Revolving Heaven: the caster spins and everything that was coming at them
 * stops.
 *
 * <p>Channeled version: while held, mobs in the dome are struck every few ticks and projectiles
 * entering the dome are cancelled by {@code RotationEvents}.</p>
 */
public class RotationAbility extends Ability implements Ability.Channeled {

    /** The spin is heard a good way off. */
    @Override
    public int noiseRadius() {
        return 14;
    }

    /** V B V V B - the Byakugan, then one more key. */
    public static final long COMBO = 3113L;
    public static final ResourceLocation ID = new ResourceLocation("narutomod", "rotation");

    /** How far the spin reaches for bodies and projectiles. */
    private static final double RADIUS = 3.0d;

    /** What it costs per tick. */
    private static final float COST_PER_TICK = 0.5f;

    /** Flat chakra cost of the short tap that purges debuffs. */
    private static final float TAP_COST = 25.0f;

    /** Persistent-data key holding the debuffs waiting to be transferred on the next hit. */
    private static final String PENDING_KEY = "narutoaddon:rotation_pending";

    /** Cap on how long a stored debuff keeps its remaining duration. */
    private static final int MAX_STORED_DURATION = 600;

    private static final int CHAKRA_REGEN_DELAY = 20;

    /** How hard the spin throws a body outwards, and how high it lifts it. */
    private static final double PUSH_SPEED = 1.0d;
    private static final double PUSH_LIFT = 0.3d;

    /** Damage dealt to mobs inside the dome. */
    private static final float DAMAGE = 2.0f;

    @Override
    public ActivationType activationType() {
        return ActivationType.CHANNELED;
    }

    @Override
    public long defaultCombo() {
        return COMBO;
    }

    @Override
    public boolean canActivateBelowMinCharge() {
        return true;
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

        float cost;
        if (chargeAmount < 0) {
            cost = this.masteryCost(ninjaData, TAP_COST);
        } else {
            cost = this.masteryCost(ninjaData, COST_PER_TICK);
        }
        if (ninjaData.getChakra() < cost) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        ninjaData.useChakra(cost, CHAKRA_REGEN_DELAY);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        if (ticksActive >= 0) {
            // Channeled: per-tick work lives in handleChannelling. Nothing to do on release.
            return;
        }

        // Short tap: purge every harmful effect from the caster, store them and hand them
        // to the next melee hit via RotationEvents.
        List<MobEffectInstance> active = new ArrayList<>(player.getActiveEffects());
        ListTag pending = new ListTag();
        for (MobEffectInstance instance : active) {
            if (instance.getEffect().isBeneficial()) {
                continue;
            }
            CompoundTag tag = new CompoundTag();
            tag.putString("id", BuiltInRegistries.MOB_EFFECT.getKey(instance.getEffect()).toString());
            tag.putInt("amplifier", instance.getAmplifier());
            tag.putInt("duration", Math.min(instance.getDuration(), MAX_STORED_DURATION));
            pending.add(tag);
            player.removeEffect(instance.getEffect());
        }
        player.getPersistentData().put(PENDING_KEY, pending);

        if (player.level() instanceof ServerLevel level) {
            double y = player.getY() + player.getBbHeight() * 0.5d;
            level.sendParticles(ParticleTypes.END_ROD, player.getX(), y, player.getZ(),
                    20, 0.5d, 0.5d, 0.5d, 0.02d);
        }
        player.level().playSound(null, player, SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.6f, 1.6f);
    }

    @Override
    public void handleChannelling(Player player, INinjaData ninjaData, int ticksChanneled) {
        this.recordMasteryCredit(player, ninjaData, MASTERY_CREDIT_PER_TICK);
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        if (ticksChanneled % 5 == 0) {
            List<Mob> nearby = level.getEntitiesOfClass(Mob.class,
                    player.getBoundingBox().inflate(RADIUS), mob -> canPush(player, mob));
            for (Mob target : nearby) {
                if (target.distanceToSqr(player) > RADIUS * RADIUS) {
                    continue;
                }
                target.hurt(player.damageSources().playerAttack(player),
                        ByakuganAbility.masteredDamage(ninjaData, this, DAMAGE));
                Vec3 away = target.position().subtract(player.position());
                if (away.lengthSqr() < 0.01d) {
                    away = player.getLookAngle().reverse();
                }
                away = away.normalize().scale(PUSH_SPEED);
                target.push(away.x, PUSH_LIFT, away.z);
                target.hurtMarked = true;
                target.fallDistance = 0.0f;
            }
        }

        if (ticksChanneled % 2 == 0) {
            double y = player.getY() + player.getBbHeight() * 0.5d;
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, player.getX(), y, player.getZ(),
                    10, RADIUS * 0.4d, 0.4d, RADIUS * 0.4d, 0.0d);
            level.sendParticles(ParticleTypes.END_ROD, player.getX(), y, player.getZ(),
                    24, RADIUS * 0.35d, 0.5d, RADIUS * 0.35d, 0.06d);
        }
    }

    @Override
    public void recordMastery(Player player, INinjaData ninjaData) {
    }

    /** @return whether this player has debuffs waiting to be transferred on the next hit. */
    public static boolean hasPending(Player player) {
        ListTag list = player.getPersistentData().getList(PENDING_KEY, 10);
        return !list.isEmpty();
    }

    /** Transfers every stored debuff onto {@code target} and clears the pending list. */
    public static void transferPending(Player player, LivingEntity target) {
        ListTag list = player.getPersistentData().getList(PENDING_KEY, 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(new ResourceLocation(tag.getString("id")));
            if (effect == null) {
                continue;
            }
            target.addEffect(new MobEffectInstance(effect, tag.getInt("duration"), tag.getInt("amplifier"), false, true));
        }
        player.getPersistentData().remove(PENDING_KEY);

        if (target.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.ENCHANTED_HIT, target.getX(),
                    target.getY() + target.getBbHeight() * 0.5d, target.getZ(),
                    12, 0.3d, 0.4d, 0.3d, 0.02d);
        }
        target.level().playSound(null, target, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.7f, 0.8f);
    }

    /** @return whether this player is currently channeling Rotation. */
    public static boolean isActive(Player player) {
        if (player.level().isClientSide) {
            return false;
        }
        INinjaData data = player.getCapability(NinjaCapabilityHandler.NINJA_DATA).resolve().orElse(null);
        if (data == null) {
            return false;
        }
        // The registry stores the addon namespace while the constant above is written in the base
        // one, so the id is resolved through the addon registry rather than compared as a string.
        return com.sekwah.narutoaddon.abilities.NarutoAbilities.byId(data.getCurrentlyChanneledAbility())
                instanceof RotationAbility;
    }

    private boolean canPush(Player caster, Mob target) {
        if (!target.isAlive()) {
            return false;
        }
        return !target.isAlliedTo(caster) && !Friends.areFriends(caster, target);
    }
}