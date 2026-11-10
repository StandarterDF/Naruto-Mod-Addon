package com.sekwah.narutoaddon.abilities.jutsus;

import com.sekwah.narutoaddon.abilities.Ability;
import com.sekwah.narutoaddon.capabilities.INinjaData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

public class GravityPressAbility extends Ability implements Ability.Cooldown {

    public static final long COMBO = 3121L;

    public static final ResourceLocation ID = new ResourceLocation("narutoaddon", "gravity_press");

    private static final float COST = 35.0f;
    private static final int COOLDOWN = 160;
    private static final int CHAKRA_REGEN_DELAY = 20;
    private static final float DAMAGE = 6.0f;
    private static final double RANGE = 8.0d;

    @Override
    public int noiseRadius() {
        return 16;
    }

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
        float cost = this.masteryCost(ninjaData, COST);
        if (ninjaData.getChakra() < cost) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("jutsu.fail.notenoughchakra",
                    net.minecraft.network.chat.Component.translatable(this.getTranslationKey(ninjaData)).withStyle(net.minecraft.ChatFormatting.YELLOW)), true);
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

        AABB box = new AABB(
                player.getX() - RANGE, player.getY() - RANGE, player.getZ() - RANGE,
                player.getX() + RANGE, player.getY() + RANGE, player.getZ() + RANGE);

        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != player && e.isAlive() && !e.isRemoved() && !e.isSpectator()
                        && !(e instanceof Player other && (other.isCreative() || other.isSpectator()))
                        && !com.sekwah.narutoaddon.friends.Friends.isOwnSide(player, e))) {
            entity.hurt(player.damageSources().playerAttack(player), DAMAGE);
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 300, 4));
            entity.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 300, 0));
            entity.setDeltaMovement(0.0d, -0.5d, 0.0d);
            entity.hurtMarked = true;
            level.sendParticles(ParticleTypes.POOF, entity.getX(), entity.getY() + entity.getBbHeight() * 0.5d, entity.getZ(),
                    8, 0.3d, 0.3d, 0.3d, 0.02d);
        }
        level.sendParticles(ParticleTypes.POOF, player.getX(), player.getY() + player.getBbHeight() * 0.5d, player.getZ(),
                20, RANGE * 0.25d, 0.5d, RANGE * 0.25d, 0.02d);
    }
}
