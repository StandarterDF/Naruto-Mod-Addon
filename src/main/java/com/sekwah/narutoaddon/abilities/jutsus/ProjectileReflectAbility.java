package com.sekwah.narutoaddon.abilities.jutsus;

import com.sekwah.narutoaddon.abilities.ToggleState;
import com.sekwah.narutoaddon.abilities.Ability;
import com.sekwah.narutoaddon.capabilities.INinjaData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * Tenseigan branch: a stance that reflects incoming projectiles.
 */
public class ProjectileReflectAbility extends Ability implements Ability.Toggled, Ability.HandleEnded {

    public static final long COMBO = 3121L;

    public static final ResourceLocation ID = new ResourceLocation("narutoaddon", "projectile_reflect");

    private static final float COST_PER_TICK = 0.4f;
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
        if (!TenseiganAbility.requireOpen(player, ninjaData, this)) {
            return false;
        }
        float cost = this.masteryCost(ninjaData, COST_PER_TICK);
        if (ninjaData.getChakra() < cost) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(net.minecraft.ChatFormatting.YELLOW)), true);
            return false;
        }
        ninjaData.useChakra(cost, CHAKRA_REGEN_DELAY);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        this.recordMasteryCredit(player, ninjaData, MASTERY_CREDIT_PER_TICK);
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
