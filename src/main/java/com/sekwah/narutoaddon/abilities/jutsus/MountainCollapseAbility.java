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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

/**
 * Eight Trigrams: Mountain Collapse - an AoE strip of palms in front of the caster.
 */
public class MountainCollapseAbility extends Ability implements Ability.Cooldown {

    /** Eight palms landing in a row carry a long way. */
    @Override
    public int noiseRadius() {
        return 18;
    }

    /** Combo for the collapse. */
    public static final long COMBO = 3121L;

    public static final ResourceLocation ID = new ResourceLocation("narutomod", "eight_trigrams_collapse");

    /** How deep the strip runs, in blocks. */
    private static final int STEPS = 10;

    /** Half-width of the strip, in blocks. */
    private static final double HALF_WIDTH = 2.0d;

    /** What the technique costs and how long before it can be used again. */
    private static final float COST = 45.0f;
    private static final int COOLDOWN = 160;

    private static final int CHAKRA_REGEN_DELAY = 20;

    /** The strike. */
    private static final float DAMAGE = 10.0f;

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
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)),
                    true);
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

        Vec3 look = player.getViewVector(1.0F).normalize();
        Vec3 origin = player.getEyePosition();

        Set<LivingEntity> struck = new HashSet<>();
        for (int step = 1; step <= STEPS; step++) {
            Vec3 at = origin.add(look.scale(step));
            level.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y, at.z,
                    4, HALF_WIDTH * 0.5d, 0.5d, HALF_WIDTH * 0.5d, 0.02d);
            AABB cell = new AABB(
                    at.x - HALF_WIDTH, at.y - HALF_WIDTH, at.z - HALF_WIDTH,
                    at.x + HALF_WIDTH, at.y + HALF_WIDTH, at.z + HALF_WIDTH);
            struck.addAll(level.getEntitiesOfClass(LivingEntity.class, cell,
                    entity -> entity != player && entity.isAlive() && !entity.isRemoved()
                            && !entity.isSpectator()
                            && !(entity instanceof Player other && (other.isCreative() || other.isSpectator()))
                            && !com.sekwah.narutoaddon.friends.Friends.isOwnSide(player, entity)));
        }

        for (LivingEntity target : struck) {
            target.hurt(player.damageSources().playerAttack(player), DAMAGE);
            target.setDeltaMovement(target.getDeltaMovement().x, 0.8d, target.getDeltaMovement().z);
            target.hurtMarked = true;
        }
    }
}
