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
import net.minecraft.world.phys.Vec3;

public class ZeroGravityAbility extends Ability implements Ability.Channeled {

    public static final long COMBO = 3112L;

    public static final ResourceLocation ID = new ResourceLocation("narutoaddon", "zero_gravity");

    private static final float COST_PER_TICK = 0.5f;
    private static final int CHAKRA_REGEN_DELAY = 20;
    private static final double RANGE = 8.0d;

    @Override
    public int noiseRadius() {
        return 0;
    }

    @Override
    public ActivationType activationType() {
        return ActivationType.CHANNELED;
    }

    @Override
    public long defaultCombo() {
        return COMBO;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (!TenseiganAbility.requireOpen(player, ninjaData, this)) {
            return false;
        }
        float cost = this.masteryCost(ninjaData, COST_PER_TICK);
        if (ninjaData.getChakra() < cost) {
            if (chargeAmount == 0) {
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable("jutsu.fail.notenoughchakra",
                        net.minecraft.network.chat.Component.translatable(this.getTranslationKey(ninjaData)).withStyle(net.minecraft.ChatFormatting.YELLOW)), true);
            }
            return false;
        }
        ninjaData.useChakra(cost, CHAKRA_REGEN_DELAY);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        // Channeled: per-tick work lives in handleChannelling.
    }

    @Override
    public void handleChannelling(Player player, INinjaData ninjaData, int ticksChanneled) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        if (ticksChanneled % 5 != 0) {
            return;
        }

        AABB box = new AABB(
                player.getX() - RANGE, player.getY() - RANGE, player.getZ() - RANGE,
                player.getX() + RANGE, player.getY() + RANGE, player.getZ() + RANGE);

        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != player && e.isAlive() && !e.isRemoved() && !e.isSpectator()
                        && !(e instanceof Player other && (other.isCreative() || other.isSpectator()))
                        && !com.sekwah.narutoaddon.friends.Friends.isOwnSide(player, e))) {
            entity.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 20, 0));
            Vec3 motion = entity.getDeltaMovement();
            entity.setDeltaMovement(motion.x, 0.35d, motion.z);
            entity.hurtMarked = true;
            level.sendParticles(ParticleTypes.END_ROD, entity.getX(), entity.getY(), entity.getZ(),
                    4, 0.2d, 0.1d, 0.2d, 0.01d);
        }
        this.recordMasteryCredit(player, ninjaData, MASTERY_CREDIT_PER_TICK);
    }

    @Override
    public void recordMastery(Player player, INinjaData ninjaData) {
    }
}
