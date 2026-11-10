package com.sekwah.narutoaddon.events;

import com.sekwah.narutoaddon.capabilities.size.EntitySize;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "narutoaddon", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SizeCombatEvents {

    private static boolean splashProcessing = false;

    private SizeCombatEvents() {
    }

    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (EntitySize.get(player) < 0.7f) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (splashProcessing) {
            return;
        }
        LivingEntity target = event.getEntity();
        if (target == null) {
            return;
        }
        net.minecraft.world.damagesource.DamageSource source = event.getSource();
        if (!(source.getEntity() instanceof Player player)) {
            return;
        }
        if (EntitySize.get(player) <= 2.0f) {
            return;
        }
        splashProcessing = true;
        try {
            float splash = event.getAmount() * 0.5f;
            AABB box = target.getBoundingBox().inflate(3.0d);
            for (LivingEntity other : target.level().getEntitiesOfClass(LivingEntity.class, box)) {
                if (other == target || other == player || !other.isAlive()) {
                    continue;
                }
                if (other instanceof Player otherPlayer && (otherPlayer.isCreative() || otherPlayer.isSpectator())) {
                    continue;
                }
                if (com.sekwah.narutoaddon.friends.Friends.isOwnSide(player, other)) {
                    continue;
                }
                other.hurt(player.damageSources().playerAttack(player), splash);
            }
        } finally {
            splashProcessing = false;
        }
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (player.level().isClientSide()) {
            return;
        }
        if (player.tickCount % 20 != 0) {
            return;
        }
        if (EntitySize.get(player) >= 0.7f) {
            return;
        }
        AABB box = new AABB(player.position().subtract(128.0d, 128.0d, 128.0d),
                player.position().add(128.0d, 128.0d, 128.0d));
        for (Mob mob : player.level().getEntitiesOfClass(Mob.class, box)) {
            if (mob.getTarget() != player) {
                continue;
            }
            if (mob.distanceTo(player) <= 3.0d) {
                continue;
            }
            mob.setTarget(null);
        }
    }
}