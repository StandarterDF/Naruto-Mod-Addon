package com.sekwah.narutoaddon.abilities.jutsus;

import com.sekwah.narutoaddon.abilities.Ability;
import com.sekwah.narutoaddon.capabilities.INinjaData;
import com.sekwah.narutoaddon.util.Targeting;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Dharma: collapses the armour off a looked-at body.
 */
public class ArmorCollapseAbility extends Ability implements Ability.Cooldown {

    public static final long COMBO = 3112L;

    public static final ResourceLocation ID = new ResourceLocation("narutoaddon", "armor_collapse");

    private static final float COST = 45.0f;
    private static final int COOLDOWN = 200;
    private static final int CHAKRA_REGEN_DELAY = 20;
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
            player.displayClientMessage(Component.translatable("jutsu.fail.armor_collapse.notarget"), true);
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
        Entity looked = Targeting.lookedAtEntity(player, 4.0d);
        if (!(looked instanceof LivingEntity target)) {
            return;
        }
        if (com.sekwah.narutoaddon.friends.Friends.isOwnSide(player, target)) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("jutsu.fail.armor_collapse.notarget"), true);
            return;
        }
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = target.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                target.spawnAtLocation(stack.copy());
                target.setItemSlot(slot, ItemStack.EMPTY);
            }
        }
        target.hurt(level.damageSources().playerAttack(player), DAMAGE);
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 300, 1));
        level.sendParticles(ParticleTypes.CRIT, target.getX(),
                target.getY() + target.getBbHeight() * 0.5d, target.getZ(),
                16, 0.3d, 0.3d, 0.3d, 0.1d);
        level.sendParticles(ParticleTypes.ENCHANTED_HIT, target.getX(),
                target.getY() + target.getBbHeight() * 0.5d, target.getZ(),
                16, 0.3d, 0.3d, 0.3d, 0.1d);
    }
}