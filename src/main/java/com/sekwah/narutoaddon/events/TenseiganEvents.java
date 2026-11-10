package com.sekwah.narutoaddon.events;

import com.sekwah.narutoaddon.abilities.jutsus.KamuiPhaseAbility;
import com.sekwah.narutoaddon.abilities.jutsus.SixPathsModeAbility;
import com.sekwah.narutoaddon.abilities.jutsus.TenseiganAbility;
import com.sekwah.narutoaddon.capabilities.INinjaData;
import com.sekwah.narutoaddon.capabilities.NinjaCapabilityHandler;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Keeps the Tenseigan's chakra mode honest: flight, strength, damage reduction and walking on
 * water and lava are reasserted while the eye is open.
 */
@Mod.EventBusSubscriber(modid = "narutoaddon", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TenseiganEvents {

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (player.level().isClientSide) {
            return;
        }
        INinjaData ninjaData = player.getCapability(NinjaCapabilityHandler.NINJA_DATA).resolve().orElse(null);
        if (ninjaData == null) {
            return;
        }
        if (!TenseiganAbility.isActive(ninjaData)) {
            return;
        }
        // The halo regrows one orb every five seconds: fast enough to keep shooting, slow enough
        // that a six-orb charged shot is a decision rather than a free action.
        if (player.tickCount % TenseiganAbility.ORB_REGEN_TICKS == 0
                && TenseiganAbility.orbs(ninjaData) < TenseiganAbility.MAX_ORBS) {
            TenseiganAbility.setOrbs(ninjaData, TenseiganAbility.orbs(ninjaData) + 1);
        }
        // Water and lava walking: keep the caster on the surface every tick.
        BlockState below = player.level().getBlockState(player.blockPosition().below());
        if (below.is(Blocks.WATER) || below.is(Blocks.LAVA)) {
            Vec3 d = player.getDeltaMovement();
            player.setDeltaMovement(d.x, 0.0d, d.z);
            player.fallDistance = 0.0f;
        }
        // Flight: keep the wings lent while the mode is up.
        if (!player.getAbilities().mayfly) {
            player.getAbilities().mayfly = true;
            player.onUpdateAbilities();
        }
        // Strength and the rest of the mode, refreshed on a slower beat.
        if (player.tickCount % 10 == 0) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 0, false, true));
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (player.level().isClientSide) {
            return;
        }
        INinjaData ninjaData = player.getCapability(NinjaCapabilityHandler.NINJA_DATA).resolve().orElse(null);
        if (ninjaData == null) {
            return;
        }
        if (!TenseiganAbility.isActive(ninjaData)) {
            return;
        }
        event.setAmount(event.getAmount() * 0.7f);
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) {
            return;
        }
        Player original = event.getOriginal();
        Player player = event.getEntity();
        INinjaData originalData = original.getCapability(NinjaCapabilityHandler.NINJA_DATA).resolve().orElse(null);
        boolean wasActive = originalData != null && TenseiganAbility.isActive(originalData);
        if (wasActive) {
            return;
        }
        if (player.isCreative()) {
            return;
        }
        player.getAbilities().mayfly = false;
        player.getAbilities().flying = false;
        player.onUpdateAbilities();
    }
}