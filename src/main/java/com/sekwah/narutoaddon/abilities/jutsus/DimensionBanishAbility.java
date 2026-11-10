package com.sekwah.narutoaddon.abilities.jutsus;

import com.sekwah.narutoaddon.abilities.Ability;
import com.sekwah.narutoaddon.abilities.jutsus.DimensionStepAbility;
import com.sekwah.narutoaddon.capabilities.INinjaData;
import com.sekwah.narutoaddon.util.Targeting;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

import java.util.function.Function;

/**
 * Dharma: banishes a looked-at mob into the dharma cube.
 */
public class DimensionBanishAbility extends Ability implements Ability.Cooldown {

    /** B V V C B V - the dharma eye, then the banish key. */
    public static final long COMBO = 3113L;

    public static final ResourceLocation ID = new ResourceLocation("narutoaddon", "dimension_banish");

    private static final float COST = 50.0f;
    private static final int COOLDOWN = 100;
    private static final int CHAKRA_REGEN_DELAY = 20;

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
        Entity looked = Targeting.lookedAtEntity(player, 20.0d);
        if (!(looked instanceof Mob)) {
            player.displayClientMessage(Component.translatable("jutsu.fail.dimension_banish.notarget"), true);
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
        Entity looked = Targeting.lookedAtEntity(player, 20.0d);
        if (!(looked instanceof Mob mob)) {
            return;
        }
        ServerLevel target = level.getServer().getLevel(DimensionStepAbility.DHARMA_DIMENSION);
        if (target == null) {
            player.displayClientMessage(Component.translatable("jutsu.fail.dimension_banish.notarget"), true);
            return;
        }
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1.0d, player.getZ(),
                32, 0.3d, 0.5d, 0.3d, 0.4d);
        Entity moved = mob.changeDimension(target, new net.minecraftforge.common.util.ITeleporter() {
            @Override
            public Entity placeEntity(Entity entity, ServerLevel currentWorld, ServerLevel destWorld, float yaw,
                                      Function<Boolean, Entity> repositionEntity) {
                return repositionEntity.apply(false);
            }
        });
        // changeDimension returns the entity that actually landed in the target level: for anything
        // but a player it is a fresh copy, and moving the old one (as this used to) left the mob
        // nowhere at all - the technique looked like it did nothing.
        if (moved == null) {
            player.displayClientMessage(Component.translatable("jutsu.fail.dimension_banish.notarget"), true);
            return;
        }
        BlockPos arrival = new BlockPos(24, 65, 24);
        target.getChunkAt(arrival);
        moved.moveTo(24.5d, 65.0d, 24.5d, moved.getYRot(), moved.getXRot());
        moved.setDeltaMovement(0.0d, 0.0d, 0.0d);
        moved.getPersistentData().putBoolean("narutoaddon:banished", true);
        target.sendParticles(ParticleTypes.REVERSE_PORTAL, 24.5d, 66.0d, 24.5d,
                32, 0.3d, 0.5d, 0.3d, 0.4d);
    }
}