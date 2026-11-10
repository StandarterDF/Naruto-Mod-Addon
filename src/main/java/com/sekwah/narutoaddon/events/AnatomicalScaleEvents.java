package com.sekwah.narutoaddon.events;

import com.sekwah.narutoaddon.capabilities.INinjaData;
import com.sekwah.narutoaddon.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutoaddon.capabilities.scale.AnatomicalScale;
import com.sekwah.narutoaddon.skills.SkillTree;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Tracks per-mob-type kill counts for Byakugan owners and exposes the resulting damage multiplier
 * for future Juken techniques.
 */
@Mod.EventBusSubscriber(modid = "narutoaddon", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AnatomicalScaleEvents {

    private static final String ROOT_KEY = "narutoaddon:anatomical_kills";

    private AnatomicalScaleEvents() {
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof Player killer)) {
            return;
        }
        LivingEntity victim = event.getEntity();
        if (victim == killer) {
            return;
        }
        if (victim instanceof Player) {
            return;
        }
        killer.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(data -> {
            if (SkillTree.hasUnlock(data.getSkillData().getUnlocked(), "mechanic:anatomical_scale")) {
                AnatomicalScale.addKill(killer, victim.getType());
            }
        });
    }

    /**
     * Damage multiplier granted by the anatomical-scale mechanic for a Juken attacker against the
     * given target. Returns {@code 1.0f} when the attacker lacks the unlock or has no ninja data.
     */
    public static float damageMultiplier(Player attacker, LivingEntity target) {
        if (attacker == null || target == null) {
            return 1.0f;
        }
        INinjaData data = attacker.getCapability(NinjaCapabilityHandler.NINJA_DATA).resolve().orElse(null);
        if (data == null) {
            return 1.0f;
        }
        if (!SkillTree.hasUnlock(data.getSkillData().getUnlocked(), "mechanic:anatomical_scale")) {
            return 1.0f;
        }
        return AnatomicalScale.multiplier(attacker, target.getType());
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) {
            return;
        }
        if (!event.getOriginal().getPersistentData().contains(ROOT_KEY)) {
            return;
        }
        CompoundTag copy = event.getOriginal().getPersistentData().getCompound(ROOT_KEY).copy();
        event.getEntity().getPersistentData().put(ROOT_KEY, copy);
    }
}
