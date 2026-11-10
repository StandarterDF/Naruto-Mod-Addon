package com.sekwah.narutoaddon.events;

import com.sekwah.narutoaddon.abilities.jutsus.ChakraCloakAbility;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "narutoaddon", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModeSealCleanupEvents {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Player player = event.player;
        if (ChakraCloakAbility.isActive(player)) {
            return;
        }
        // Remove cloak-granted short-duration effects when the cloak is no longer active.
        player.removeEffect(MobEffects.DAMAGE_BOOST);
        player.removeEffect(MobEffects.MOVEMENT_SPEED);
        player.removeEffect(MobEffects.DAMAGE_RESISTANCE);
        player.removeEffect(MobEffects.REGENERATION);
    }
}
