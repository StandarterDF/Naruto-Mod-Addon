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
 * Senrigan Freecam: sends the caster's viewpoint out of the body. While the stance is on, the camera
 * may fly freely through blocks up to fifty blocks away from the body, which stays put. Damage taken
 * or recasting the stance brings the camera back.
 */
public class SenriganFreecamAbility extends Ability implements Ability.Toggled, Ability.HandleEnded {

    /** B V B V B ... the far eye sent out of the body. */
    public static final long COMBO = 3113L;

    public static final ResourceLocation ID = new ResourceLocation("narutomod", "senrigan_freecam");

    /** Chakra burnt for every tick the eye is out of the body. */
    private static final float PASSIVE_DRAIN = 2.0f;

    private static final int CHAKRA_REGEN_DELAY = 20;

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
        float cost = this.masteryCost(ninjaData, 2.0f);
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
        if (ticksActive % 20 != 0) {
            return;
        }
        float drain = this.masteryCost(ninjaData, PASSIVE_DRAIN);
        if (ninjaData.getChakra() < drain) {
            ToggleState.deactivate(player, ninjaData, this);
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return;
        }
        ninjaData.useChakra(drain, CHAKRA_REGEN_DELAY);
    }

    @Override
    public void handleAbilityEnded(Player player, INinjaData ninjaData, int ticksActive) {
        ToggleState.clear(ninjaData, ID);
    }

    @Override
    public void performToggleClient(Player player, INinjaData ninjaData) {
    }

    public static boolean isActive(INinjaData ninjaData) {
        return ninjaData != null && ToggleState.isActive(ninjaData, ID);
    }
}