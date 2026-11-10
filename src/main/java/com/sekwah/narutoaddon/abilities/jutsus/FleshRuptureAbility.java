package com.sekwah.narutoaddon.abilities.jutsus;

import com.sekwah.narutoaddon.abilities.Ability;
import com.sekwah.narutoaddon.capabilities.INinjaData;
import com.sekwah.narutoaddon.util.Targeting;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class FleshRuptureAbility extends Ability implements Ability.Cooldown {

    public static final long COMBO = 3112L;

    public static final ResourceLocation ID = new ResourceLocation("narutoaddon", "flesh_rupture");

    private static final float COST = 40.0f;
    private static final int COOLDOWN = 200;
    private static final double RANGE = 15.0d;

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
        if (!TenseiganAbility.requireOpen(player, ninjaData, this)) {
            return false;
        }
        if (this.checkCooldown(player, ninjaData, this.getTranslationKey(ninjaData))) {
            return false;
        }
        float cost = this.masteryCost(ninjaData, COST);
        if (ninjaData.getChakra() < cost) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        Entity looked = Targeting.lookedAtEntity(player, RANGE);
        if (!(looked instanceof LivingEntity)) {
            player.displayClientMessage(Component.translatable("jutsu.fail.flesh_rupture.notarget"), true);
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
        if (!(looked instanceof LivingEntity target)) {
            return;
        }
        if (com.sekwah.narutoaddon.friends.Friends.isOwnSide(player, target)) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("jutsu.fail.flesh_rupture.notarget"), true);
            return;
        }

        target.hurt(level.damageSources().playerAttack(player), 25.0f);

        level.sendParticles(ParticleTypes.CRIT, target.getX(),
                target.getY() + target.getBbHeight() * 0.5d, target.getZ(),
                12, 0.3d, 0.3d, 0.3d, 0.1d);
        level.sendParticles(ParticleTypes.END_ROD, target.getX(),
                target.getY() + target.getBbHeight() * 0.5d, target.getZ(),
                8, 0.2d, 0.2d, 0.2d, 0.1d);

        // Two ticks pulling the target in, then two ticks pushing it away.
        Runnable pull = () -> {
            if (!target.isAlive()) {
                return;
            }
            Vec3 dir = player.position().subtract(target.position());
            if (dir.lengthSqr() > 1.0E-6d) {
                target.setDeltaMovement(dir.normalize().scale(0.8d));
                target.hurtMarked = true;
            }
        };
        pull.run();
        ninjaData.scheduleDelayedTickEvent(delayedPlayer -> {
            if (!target.isAlive()) {
                return;
            }
            Vec3 dir = delayedPlayer.position().subtract(target.position());
            if (dir.lengthSqr() > 1.0E-6d) {
                target.setDeltaMovement(dir.normalize().scale(0.8d));
                target.hurtMarked = true;
            }
        }, 1);
        ninjaData.scheduleDelayedTickEvent(delayedPlayer -> {
            if (!target.isAlive()) {
                return;
            }
            Vec3 dir = target.position().subtract(delayedPlayer.position());
            if (dir.lengthSqr() > 1.0E-6d) {
                target.setDeltaMovement(dir.normalize().scale(1.2d));
                target.hurtMarked = true;
            }
        }, 2);
        ninjaData.scheduleDelayedTickEvent(delayedPlayer -> {
            if (!target.isAlive()) {
                return;
            }
            Vec3 dir = target.position().subtract(delayedPlayer.position());
            if (dir.lengthSqr() > 1.0E-6d) {
                target.setDeltaMovement(dir.normalize().scale(1.2d));
                target.hurtMarked = true;
            }
        }, 3);

        this.registerCooldown(player, ninjaData, this.getTranslationKey(ninjaData));
        this.recordMastery(player, ninjaData);
    }
}
