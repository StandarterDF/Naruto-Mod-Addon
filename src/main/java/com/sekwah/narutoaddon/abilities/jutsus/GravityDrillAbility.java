package com.sekwah.narutoaddon.abilities.jutsus;

import com.sekwah.narutoaddon.abilities.Ability;
import com.sekwah.narutoaddon.capabilities.INinjaData;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class GravityDrillAbility extends Ability implements Ability.Channeled {

    public static final long COMBO = 33122L;

    public static final ResourceLocation ID = new ResourceLocation("narutoaddon", "gravity_drill");

    private static final float COST_PER_TICK = 0.4f;
    private static final int CHAKRA_REGEN_DELAY = 20;

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
                        Component.translatable(this.getTranslationKey(ninjaData)).withStyle(net.minecraft.ChatFormatting.YELLOW)), true);
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
        this.recordMasteryCredit(player, ninjaData, MASTERY_CREDIT_PER_TICK);
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        if (ticksChanneled % 10 != 0) {
            return;
        }

        Direction forward = player.getDirection();
        Direction right = (forward.getAxis() == Direction.Axis.Y)
                ? Direction.EAST
                : forward.getClockWise();

        for (int step = 1; step <= 2; step++) {
            for (int w = 0; w < 2; w++) {
                for (int h = 0; h < 3; h++) {
                    BlockPos pos = player.blockPosition()
                            .relative(forward, step)
                            .relative(right, w)
                            .above(h);
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
                    if (speed < 0.0f || speed > 5.0f) {
                        continue;
                    }
                    level.removeBlock(pos, false);
                    level.sendParticles(ParticleTypes.POOF, pos.getX() + 0.5d, pos.getY() + 0.5d, pos.getZ() + 0.5d,
                            4, 0.1d, 0.1d, 0.1d, 0.01d);
                }
            }
        }
    }

    @Override
    public void recordMastery(Player player, INinjaData ninjaData) {
    }
}
