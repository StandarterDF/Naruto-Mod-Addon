package com.sekwah.narutoaddon.abilities.jutsus;

import com.sekwah.narutoaddon.abilities.ToggleState;
import com.sekwah.narutoaddon.abilities.Ability;
import com.sekwah.narutoaddon.capabilities.INinjaData;
import com.sekwah.narutoaddon.config.NarutoConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * Senrigan Guidance: while the Senrigan is open, every attack the caster makes is redirected onto
 * the living creature closest to their crosshair, even through walls. A toggle that drains chakra
 * every tick; mastery lowers the drain.
 */
public class SenriganGuidanceAbility extends Ability implements Ability.Toggled, Ability.HandleEnded {

    /** B V B B ... guidance stance. */
    public static final long COMBO = 3321L;

    public static final ResourceLocation ID = new ResourceLocation("narutomod", "senrigan_guidance");

    /** Chakra burnt for every tick the guidance is held up. */
    private static final float PASSIVE_DRAIN = 0.4f;

    private static final int CHAKRA_REGEN_DELAY = 10;

    @Override
    public int noiseRadius() {
        return 0;
    }

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
        if (!NarutoConfig.senriganEnabled) {
            player.displayClientMessage(Component.translatable("jutsu.fail.senrigan.disabled")
                    .withStyle(ChatFormatting.RED), true);
            return false;
        }
        if (!SenriganAbility.isActive(ninjaData)) {
            player.displayClientMessage(Component.translatable("jutsu.fail.senrigan.required",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW))
                            .withStyle(ChatFormatting.RED), true);
            return false;
        }
        float cost = this.masteryCost(ninjaData, PASSIVE_DRAIN);
        if (ninjaData.getChakra() < cost) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        ninjaData.useChakra(cost, CHAKRA_REGEN_DELAY);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        if (!SenriganAbility.isActive(ninjaData)) {
            ninjaData.getToggleAbilityData().removeAbilityEnded(player, ninjaData, this);
            return;
        }
        this.recordMasteryCredit(player, ninjaData, MASTERY_CREDIT_PER_TICK);
        float drain = this.masteryCost(ninjaData, PASSIVE_DRAIN);
        if (ninjaData.getChakra() < drain) {
            ninjaData.getToggleAbilityData().removeAbilityEndedSilently(player, ninjaData, this);
            return;
        }
        ninjaData.useChakra(drain, CHAKRA_REGEN_DELAY);
    }

    @Override
    public void handleAbilityEnded(Player player, INinjaData ninjaData, int ticksActive) {
    }

    @Override
    public void performToggleClient(Player player, INinjaData ninjaData) {
    }

    public static boolean isActive(INinjaData ninjaData) {
        return ninjaData != null && ToggleState.isActive(ninjaData, ID);
    }
}