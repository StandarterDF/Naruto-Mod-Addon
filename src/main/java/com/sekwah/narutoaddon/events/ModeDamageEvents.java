package com.sekwah.narutoaddon.events;

import com.sekwah.narutoaddon.abilities.ToggleState;
import com.sekwah.narutoaddon.abilities.jutsus.ChakraCloakAbility;
import com.sekwah.narutoaddon.abilities.jutsus.CurseSealOneAbility;
import com.sekwah.narutoaddon.abilities.jutsus.CurseSealTwoAbility;
import com.sekwah.narutoaddon.abilities.jutsus.DividedCloakAbility;
import com.sekwah.narutoaddon.abilities.jutsus.PerfectCloakAbility;
import com.sekwah.narutoaddon.abilities.jutsus.SixPathsModeAbility;
import com.sekwah.narutoaddon.abilities.jutsus.TailedCloakAbility;
import com.sekwah.narutoaddon.capabilities.INinjaData;
import com.sekwah.narutoaddon.capabilities.NinjaCapabilityHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "narutoaddon", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ModeDamageEvents {
    private ModeDamageEvents() {}

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide() || event.isCanceled() || event.getAmount() <= 0.0f) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof Player caster)) {
            return;
        }
        INinjaData data = caster.getCapability(NinjaCapabilityHandler.NINJA_DATA).resolve().orElse(null);
        if (data == null) return;

        float multiplier = 1.0f;
        if (SixPathsModeAbility.isActive(caster)) {
            multiplier = Math.max(multiplier, 4.0f);
        } else if (data.isSageMode()) {
            multiplier = Math.max(multiplier, 3.0f);
        }
        if (ToggleState.isActive(data, CurseSealOneAbility.ID)
                || ToggleState.isActive(data, CurseSealTwoAbility.ID)) {
            multiplier = Math.max(multiplier, 1.75f);
        }
        if (ToggleState.isActive(data, PerfectCloakAbility.ID)) {
            multiplier = Math.max(multiplier, 3.0f);
        } else if (ToggleState.isActive(data, DividedCloakAbility.ID)) {
            multiplier = Math.max(multiplier, 2.5f);
        } else if (ToggleState.isActive(data, TailedCloakAbility.ID)) {
            multiplier = Math.max(multiplier, 2.0f);
        } else if (ToggleState.isActive(data, ChakraCloakAbility.ID)) {
            multiplier = Math.max(multiplier, 2.0f);
        }
        // Eight Gates: no technique damage boost (taijutsu only).

        if (multiplier > 1.0f) {
            event.setAmount(event.getAmount() * multiplier);
        }
    }
}