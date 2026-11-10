package com.sekwah.narutoaddon.abilities.jutsus;

import com.sekwah.narutoaddon.abilities.Ability;
import com.sekwah.narutoaddon.abilities.mastery.Mastery;
import com.sekwah.narutoaddon.capabilities.INinjaData;
import com.sekwah.narutoaddon.capabilities.enemychakra.EnemyChakra;
import com.sekwah.narutoaddon.util.Targeting;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

public class JukenPurgeAbility extends Ability implements Ability.Cooldown {

    @Override
    public int noiseRadius() {
        return 10;
    }

    public static final long COMBO = 3112L;

    public static final ResourceLocation ID = new ResourceLocation("narutoaddon", "juken_purge");

    private static final float COST = 40.0f;
    private static final int COOLDOWN = 200;

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
        float cost = this.masteryCost(ninjaData, COST);
        if (ninjaData.getChakra() < cost) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        Entity looked = Targeting.lookedAtEntity(player, 4.0d);
        if (!(looked instanceof LivingEntity)) {
            player.displayClientMessage(Component.translatable("jutsu.fail.juken_purge.notarget"), true);
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
        Entity looked = Targeting.lookedAtEntity(player, 4.0d);
        if (!(looked instanceof LivingEntity target)) {
            return;
        }
        if (com.sekwah.narutoaddon.friends.Friends.isOwnSide(player, target)) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("jutsu.fail.juken_purge.notarget"), true);
            return;
        }
        List<MobEffectInstance> toRemove = new ArrayList<>();
        for (MobEffectInstance effect : target.getActiveEffects()) {
            if (effect.getEffect().isBeneficial()) {
                toRemove.add(effect);
            }
        }
        for (MobEffectInstance effect : toRemove) {
            target.removeEffect(effect.getEffect());
        }
        target.hurt(level.damageSources().playerAttack(player),
                ByakuganAbility.masteredDamage(ninjaData, this, 6.0f));
        level.sendParticles(ParticleTypes.CRIT, target.getX(),
                target.getY() + target.getBbHeight() * 0.5d, target.getZ(),
                8, 0.2d, 0.2d, 0.2d, 0.1d);
        this.registerCooldown(player, ninjaData, this.getTranslationKey(ninjaData));
        this.recordMastery(player, ninjaData);
    }
}
