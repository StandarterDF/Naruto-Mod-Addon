package com.sekwah.narutoaddon.events;

import com.sekwah.narutoaddon.abilities.jutsus.SenriganAbility;
import com.sekwah.narutoaddon.capabilities.INinjaData;
import com.sekwah.narutoaddon.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutoaddon.config.NarutoConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.entity.living.LootingLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Passive effect of the Senrigan stance "Luck's Gaze": grants Looting III to
 * mob drops while the stance is open.
 */
@Mod.EventBusSubscriber(modid = "narutoaddon", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SenriganLuckEvents {

    @SubscribeEvent
    public static void onLooting(LootingLevelEvent event) {
        Entity killer = event.getDamageSource() == null ? null : event.getDamageSource().getEntity();
        if (!(killer instanceof ServerPlayer player)) return;
        if (!NarutoConfig.senriganEnabled) return;
        INinjaData ninjaData = player.getCapability(NinjaCapabilityHandler.NINJA_DATA).resolve().orElse(null);
        if (ninjaData == null || !SenriganAbility.isActive(ninjaData)) return;
        event.setLootingLevel(Math.max(3, event.getLootingLevel()));
    }
}