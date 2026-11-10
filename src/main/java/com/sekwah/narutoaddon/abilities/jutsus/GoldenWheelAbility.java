package com.sekwah.narutoaddon.abilities.jutsus;

import com.sekwah.narutoaddon.abilities.Ability;
import com.sekwah.narutoaddon.capabilities.INinjaData;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

/**
 * Tenseigan branch: a channeled golden wheel of chakra that cuts through bodies and blocks.
 */
public class GoldenWheelAbility extends Ability implements Ability.Channeled {

    public static final long COMBO = 3321L;

    public static final ResourceLocation ID = new ResourceLocation("narutoaddon", "golden_wheel");

    private static final float COST_PER_TICK = 0.8f;
    private static final int CHAKRA_REGEN_DELAY = 20;
    private static final double RANGE = 30.0d;
    private static final double LINE_RADIUS = 1.0d;
    private static final float DAMAGE = 20.0f;
    private static final int MAX_BLOCKS_PER_TICK = 8;

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
                player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                        Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
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

        HitResult hit = player.pick(RANGE, 0.0f, true);
        Vec3 start = player.getEyePosition();
        Vec3 end;
        if (hit.getType() == HitResult.Type.BLOCK && hit instanceof BlockHitResult bhr) {
            end = bhr.getLocation();
        } else {
            end = start.add(player.getLookAngle().scale(RANGE));
        }
        double dist = start.distanceTo(end);
        if (dist < 0.01d) {
            return;
        }
        Vec3 dir = end.subtract(start).normalize();

        // Damage every living entity within the line radius.
        AABB box = new AABB(start, end).inflate(LINE_RADIUS);
        Set<LivingEntity> toHurt = new HashSet<>();
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != player && e.isAlive() && !e.isRemoved() && !com.sekwah.narutoaddon.friends.Friends.isOwnSide(player, e))) {
            Vec3 center = entity.position().add(0.0d, entity.getBbHeight() * 0.5d, 0.0d);
            Vec3 toEntity = center.subtract(start);
            double proj = toEntity.dot(dir);
            proj = Math.max(0.0d, Math.min(dist, proj));
            Vec3 closest = start.add(dir.scale(proj));
            if (center.distanceTo(closest) <= LINE_RADIUS) {
                toHurt.add(entity);
            }
        }
        for (LivingEntity entity : toHurt) {
            entity.hurt(player.damageSources().playerAttack(player), DAMAGE);
        }

        // Remove soft blocks along the line, capped per tick.
        int removed = 0;
        Set<BlockPos> seen = new HashSet<>();
        double step = 0.5d;
        for (double t = 0.0d; t <= dist && removed < MAX_BLOCKS_PER_TICK; t += step) {
            Vec3 p = start.add(dir.scale(t));
            BlockPos pos = new BlockPos((int) Math.floor(p.x), (int) Math.floor(p.y), (int) Math.floor(p.z));
            if (!seen.add(pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) {
                continue;
            }
            if (state.hasBlockEntity()) {
                continue;
            }
            if (state.is(Blocks.BEDROCK)) {
                continue;
            }
            float speed = state.getDestroySpeed(level, pos);
            if (speed < 0.0f || speed > 3.0f) {
                continue;
            }
            level.removeBlock(pos, false);
            removed++;
        }

        // Particles along the line.
        for (double t = 0.0d; t <= dist; t += 0.5d) {
            Vec3 p = start.add(dir.scale(t));
            level.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z,
                    1, 0.02d, 0.02d, 0.02d, 0.0d);
            if (t % 1.0d == 0.0d) {
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, p.x, p.y, p.z,
                        1, 0.02d, 0.02d, 0.02d, 0.0d);
            }
        }
    }

    @Override
    public void recordMastery(Player player, INinjaData ninjaData) {
    }
}
