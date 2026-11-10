package com.sekwah.narutoaddon.abilities.jutsus;

import com.sekwah.narutoaddon.abilities.Ability;
import com.sekwah.narutoaddon.abilities.ToggleState;
import com.sekwah.narutoaddon.abilities.jutsus.SenriganAbility;
import com.sekwah.narutoaddon.capabilities.INinjaData;
import com.sekwah.narutoaddon.config.NarutoConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * Senrigan Deathmark: while this mode is on and the Senrigan is open, the caster's death leaves a
 * marker behind at the spot they fell, so fate can be walked back once with {@link SenriganDeathstepAbility}.
 */
public class SenriganDeathmarkAbility extends Ability implements Ability.Toggled, Ability.HandleEnded {

    /** T B V B ... the mark left behind at the place of death. */
    public static final long COMBO = 3112L;

    public static final ResourceLocation ID = new ResourceLocation("narutomod", "senrigan_deathmark");

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
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        player.getPersistentData().putBoolean("narutomod:senriganDeathmark", true);
    }

    @Override
    public void handleAbilityEnded(Player player, INinjaData ninjaData, int ticksActive) {
        ToggleState.clear(ninjaData, ID);
        player.getPersistentData().putBoolean("narutomod:senriganDeathmark", false);
    }

    @Override
    public void performToggleClient(Player player, INinjaData ninjaData) {
    }

    public static boolean isActive(INinjaData ninjaData) {
        return ninjaData != null && ToggleState.isActive(ninjaData, ID);
    }
}