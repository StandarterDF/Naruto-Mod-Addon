package com.sekwah.narutoaddon.abilities.jutsus;

import com.sekwah.narutoaddon.abilities.Ability;
import com.sekwah.narutoaddon.abilities.jutsus.ByakuganAbility;
import com.sekwah.narutoaddon.capabilities.INinjaData;
import com.sekwah.narutoaddon.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutoaddon.config.NarutoConfig;
import com.sekwah.narutoaddon.friends.Friends;
import com.sekwah.narutoaddon.util.Targeting;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ChakraGripAbility extends Ability implements Ability.Channeled, Ability.HandleEnded {

    @Override
    public int noiseRadius() {
        return 6;
    }

    public static final long COMBO = 3121L;

    public static final ResourceLocation ID = new ResourceLocation("narutomod", "chakra_grip");

    private static final double RANGE = 12.0d;

    private static final float COST_PER_TICK = 0.4f;

    private static final int CHAKRA_REGEN_DELAY = 20;

    private final Map<UUID, UUID> grips = new HashMap<>();

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
        return false;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (chargeAmount < 0) {
            return false;
        }
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

        if (chargeAmount == 0) {
            LivingEntity target = resolve(player);
            if (target == null) {
                player.displayClientMessage(Component.translatable("jutsu.fail.chakra_grip.notarget",
                        Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW))
                        .withStyle(ChatFormatting.RED), true);
                return false;
            }
            if (!pay(player, ninjaData)) {
                return false;
            }
            this.grips.put(player.getUUID(), target.getUUID());
            return true;
        }

        if (!this.grips.containsKey(player.getUUID())) {
            return false;
        }
        return pay(player, ninjaData);
    }

    @Override
    public void handleChannelling(Player player, INinjaData ninjaData, int ticksChanneled) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        LivingEntity target = this.heldTarget(player, level);
        if (target == null) {
            UUID targetId = this.grips.get(player.getUUID());
            if (targetId != null) {
                Entity entity = level.getEntity(targetId);
                if (entity instanceof LivingEntity living) {
                    living.setNoGravity(false);
                }
            }
            this.release(player, ninjaData);
            return;
        }

        this.recordMasteryCredit(player, ninjaData, MASTERY_CREDIT_PER_TICK);

        target.setNoGravity(true);
        target.setDeltaMovement(Vec3.ZERO);
        Vec3 point = player.getEyePosition().add(player.getViewVector(1.0F).scale(3.0d));
        target.setPos(point.x, point.y, point.z);
        target.hurtMarked = true;
        target.fallDistance = 0.0f;

        if (ticksChanneled > 0 && ticksChanneled % 10 == 0) {
            target.hurt(player.damageSources().playerAttack(player), 1.0f);
        }

        Vec3 start = player.getEyePosition();
        Vec3 end = target.position().add(0.0d, target.getBbHeight() * 0.5d, 0.0d);
        for (int i = 0; i <= 6; i++) {
            Vec3 at = start.lerp(end, i / 6.0d);
            level.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z,
                    1, 0.02d, 0.02d, 0.02d, 0.0d);
        }

        if (ticksChanneled == 0) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 0.35f, 1.5f);
        }
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        if (player.level() instanceof ServerLevel level) {
            UUID targetId = this.grips.get(player.getUUID());
            if (targetId != null) {
                Entity entity = level.getEntity(targetId);
                if (entity instanceof LivingEntity living) {
                    living.setNoGravity(false);
                }
            }
        }
        this.grips.remove(player.getUUID());
    }

    @Override
    public void handleAbilityEnded(Player player, INinjaData ninjaData, int ticksActive) {
        if (player.level() instanceof ServerLevel level) {
            UUID targetId = this.grips.get(player.getUUID());
            if (targetId != null) {
                Entity entity = level.getEntity(targetId);
                if (entity instanceof LivingEntity living) {
                    living.setNoGravity(false);
                }
            }
        }
        this.grips.remove(player.getUUID());
    }

    @Override
    public void recordMastery(Player player, INinjaData ninjaData) {
    }

    @Nullable
    private LivingEntity resolve(Player player) {
        if (!(Targeting.lookedAtEntity(player, RANGE) instanceof LivingEntity target)) {
            return null;
        }
        return canHold(player, target) ? target : null;
    }

    @Nullable
    private LivingEntity heldTarget(Player caster, ServerLevel level) {
        UUID targetId = this.grips.get(caster.getUUID());
        if (targetId == null) {
            return null;
        }
        Entity entity = level.getEntity(targetId);
        if (!(entity instanceof LivingEntity target)
                || target.isRemoved()
                || target.isSpectator()
                || !target.isAlive()
                || target.level() != caster.level()
                || target.distanceToSqr(caster) > RANGE * RANGE) {
            return null;
        }
        return target;
    }

    private void release(Player player, INinjaData ninjaData) {
        this.grips.remove(player.getUUID());
        ninjaData.setCurrentlyChanneledAbility(player, null);
    }

    private boolean pay(Player player, INinjaData ninjaData) {
        float cost = this.masteryCost(ninjaData, COST_PER_TICK);
        if (ninjaData.getChakra() < cost) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW))
                    .withStyle(ChatFormatting.RED), true);
            return false;
        }
        ninjaData.useChakra(cost, CHAKRA_REGEN_DELAY);
        return true;
    }

    private static boolean canHold(Player caster, LivingEntity target) {
        if (target == caster || target.isRemoved() || target.isSpectator() || !target.isAlive()) {
            return false;
        }
        if (target instanceof Player other && other.isCreative()) {
            return false;
        }
        return !target.isAlliedTo(caster) && !Friends.areFriends(caster, target);
    }
}
