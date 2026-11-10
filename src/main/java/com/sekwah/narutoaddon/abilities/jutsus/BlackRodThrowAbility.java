package com.sekwah.narutoaddon.abilities.jutsus;

import com.sekwah.narutoaddon.abilities.Ability;
import com.sekwah.narutoaddon.capabilities.INinjaData;
import com.sekwah.narutoaddon.capabilities.enemychakra.EnemyChakra;
import com.sekwah.narutoaddon.util.Targeting;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Dharma: hurls a black rod at the body under the caster's gaze, pinning it in place.
 */
public class BlackRodThrowAbility extends Ability implements Ability.Cooldown {

    /** A short, sharp strike is heard at most across a courtyard. */
    @Override
    public int noiseRadius() {
        return 12;
    }

    public static final long COMBO = 3113L;

    public static final ResourceLocation ID = new ResourceLocation("narutoaddon", "black_rod_throw");

    /** How long the rod holds a body to the ground, in ticks. */
    public static final int PIN_TICKS = 1200;

    /** Persistent keys shared with the tick/death handler. */
    public static final String PIN_UNTIL_KEY = "narutoaddon:rod_pinned_until";
    public static final String PIN_OWNER_KEY = "narutoaddon:rod_pin_owner";

    private static final float COST = 25.0f;
    private static final int COOLDOWN = 120;
    private static final double RANGE = 20.0d;

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
        if (this.checkCooldown(player, ninjaData, this.getTranslationKey(ninjaData))) {
            return false;
        }
        Entity looked = Targeting.lookedAtEntity(player, RANGE);
        if (!(looked instanceof LivingEntity)) {
            player.displayClientMessage(Component.translatable("jutsu.fail.black_rod_throw.notarget",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW))
                            .withStyle(ChatFormatting.RED), true);
            return false;
        }
        float cost = this.masteryCost(ninjaData, COST);
        if (ninjaData.getChakra() < cost) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        ninjaData.useChakra(cost, 20);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        Entity looked = Targeting.lookedAtEntity(player, RANGE);
        if (!(looked instanceof LivingEntity target) || target == player) {
            return;
        }
        if (com.sekwah.narutoaddon.friends.Friends.isOwnSide(player, target)) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("jutsu.fail.black_rod_throw.notarget"), true);
            return;
        }
        target.hurt(level.damageSources().playerAttack(player), 8.0f);
        EnemyChakra.drain(target, 20.0f);
        EnemyChakra.silence(target, 100);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, PIN_TICKS, 5));
        // The rod itself is the pin: the tick handler keeps the body in place and the death
        // handler waits for it to be killed while nailed down.
        target.getPersistentData().putLong(PIN_UNTIL_KEY, level.getGameTime() + PIN_TICKS);
        target.getPersistentData().putUUID(PIN_OWNER_KEY, player.getUUID());
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable("jutsu.black_rod.pinned",
                target.getDisplayName().copy().withStyle(ChatFormatting.YELLOW)), true);
        target.setNoGravity(false);
        target.setDeltaMovement(0.0d, -0.4d, 0.0d);
        target.hurtMarked = true;
        level.sendParticles(ParticleTypes.CRIT, target.getX(),
                target.getY() + target.getBbHeight() * 0.5d, target.getZ(),
                8, 0.2d, 0.2d, 0.2d, 0.1d);
        level.sendParticles(ParticleTypes.SMOKE, target.getX(),
                target.getY() + target.getBbHeight() * 0.5d, target.getZ(),
                8, 0.2d, 0.2d, 0.2d, 0.05d);
        this.registerCooldown(player, ninjaData, this.getTranslationKey(ninjaData));
        this.recordMastery(player, ninjaData);
    }
}