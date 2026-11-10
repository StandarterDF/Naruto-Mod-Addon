package com.sekwah.narutoaddon.abilities.jutsus;

import com.sekwah.narutoaddon.abilities.ToggleState;
import com.sekwah.narutoaddon.abilities.Ability;
import com.sekwah.narutoaddon.capabilities.INinjaData;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public class OtsutsukiWillAbility extends Ability implements Ability.Toggled, Ability.HandleEnded {

    public static final long COMBO = 3321L;

    public static final ResourceLocation ID = new ResourceLocation("narutoaddon", "otsutsuki_will");

    private static final float COST_PER_TICK = 1.0f;
    private static final int CHAKRA_REGEN_DELAY = 20;

    private static final UUID ATTACK_DAMAGE_MODIFIER_ID =
            UUID.fromString("33123000-0000-0000-0000-000000000001");
    private static final UUID MOVEMENT_SPEED_MODIFIER_ID =
            UUID.fromString("33123000-0000-0000-0000-000000000002");
    private static final UUID MAX_HEALTH_MODIFIER_ID =
            UUID.fromString("33123000-0000-0000-0000-000000000003");

    @Override
    public ActivationType activationType() {
        return ActivationType.TOGGLE;
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
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(net.minecraft.ChatFormatting.YELLOW)), true);
            return false;
        }
        ninjaData.useChakra(cost, CHAKRA_REGEN_DELAY);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        applyModifiers(player);

        if (player.level() instanceof ServerLevel level) {
            if (player.tickCount % 10 == 0) {
                for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class,
                        player.getBoundingBox().inflate(50.0d))) {
                    if (entity == player || !entity.isAlive() || com.sekwah.narutoaddon.friends.Friends.isOwnSide(player, entity)) {
                        continue;
                    }
                    entity.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 20, 0));
                }
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, player.getX(),
                        player.getY() + player.getBbHeight() * 0.5d, player.getZ(),
                        10, 1.0d, 1.0d, 1.0d, 0.02d);
            }
        }

        this.recordMasteryCredit(player, ninjaData, Ability.MASTERY_CREDIT_PER_TICK);
    }

    private static void applyModifiers(Player player) {
        AttributeInstance attack = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack != null && attack.getModifier(ATTACK_DAMAGE_MODIFIER_ID) == null) {
            attack.addTransientModifier(new AttributeModifier(ATTACK_DAMAGE_MODIFIER_ID,
                    "narutoaddon:otsutsuki_will_damage", 10.0d, AttributeModifier.Operation.ADDITION));
        }
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null && speed.getModifier(MOVEMENT_SPEED_MODIFIER_ID) == null) {
            speed.addTransientModifier(new AttributeModifier(MOVEMENT_SPEED_MODIFIER_ID,
                    "narutoaddon:otsutsuki_will_speed", 0.5d, AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
        AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
        if (health != null && health.getModifier(MAX_HEALTH_MODIFIER_ID) == null) {
            health.addTransientModifier(new AttributeModifier(MAX_HEALTH_MODIFIER_ID,
                    "narutoaddon:otsutsuki_will_health", 20.0d, AttributeModifier.Operation.ADDITION));
        }
    }

    public static void removeModifiers(Player player) {
        AttributeInstance attack = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack != null) {
            attack.removeModifier(ATTACK_DAMAGE_MODIFIER_ID);
        }
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(MOVEMENT_SPEED_MODIFIER_ID);
        }
        AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            health.removeModifier(MAX_HEALTH_MODIFIER_ID);
        }
    }

    @Override
    public void performToggleClient(Player player, INinjaData ninjaData) {
    }

    @Override
    public void handleAbilityEnded(Player player, INinjaData ninjaData, int ticksActive) {
        removeModifiers(player);
        player.setHealth(Math.min(player.getHealth(), player.getMaxHealth()));
    }

    @Override
    public void recordMastery(Player player, INinjaData ninjaData) {
    }

    public static boolean isActive(INinjaData ninjaData) {
        return ninjaData != null && ToggleState.isActive(ninjaData, ID);
    }
}
