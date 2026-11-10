package com.sekwah.narutoaddon.events;

import com.sekwah.narutoaddon.capabilities.enemychakra.EnemyChakra;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "narutoaddon", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class EnemyChakraEvents {

    private EnemyChakraEvents() {
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity e = event.getEntity();
        if (e.level().isClientSide()) {
            return;
        }
        if (e.tickCount % 20 != 0) {
            return;
        }
        if (!(e instanceof Player)) {
            if (e.level().getGameTime() - EnemyChakra.lastHitAt(e) > 100) {
                EnemyChakra.restore(e, EnemyChakra.max(e) * 0.01f);
            }
        }
        int tier = EnemyChakra.tier(EnemyChakra.ratio(e));
        if (tier >= 1) {
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0, false, true));
        }
        if (tier >= 2) {
            e.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0, false, true));
            e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 2, false, true));
        }
        if (tier >= 3) {
            e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0, false, true));
        }
        if (tier >= 4) {
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 3, false, true));
        }
        if (tier >= 5) {
            EnemyChakra.silence(e, 80);
            if (e.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.ENCHANT, e.getX(), e.getEyeY(), e.getZ(),
                        3, 0.2d, 0.2d, 0.2d, 0.0d);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingHeal(LivingHealEvent event) {
        LivingEntity e = event.getEntity();
        if (e.level().isClientSide()) {
            return;
        }
        if (EnemyChakra.tier(EnemyChakra.ratio(e)) >= 5) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof Player)) {
            return;
        }
        if (!(event.getEntity() instanceof Mob mob)) {
            return;
        }
        if (EnemyChakra.tier(EnemyChakra.ratio(mob)) < 4) {
            return;
        }
        if (mob.getRandom().nextFloat() >= 0.15f) {
            return;
        }
        if (mob.getMainHandItem().isEmpty()) {
            return;
        }
        mob.spawnAtLocation(mob.getMainHandItem().copy());
        mob.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            EnemyChakra.reset(event.getEntity());
        }
    }
}