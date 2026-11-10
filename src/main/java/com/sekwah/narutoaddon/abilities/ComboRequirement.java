package com.sekwah.narutoaddon.abilities;

import com.sekwah.narutoaddon.capabilities.INinjaData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * The stance half of a context combo: a technique collected on a shared combo may only start
 * while the eye or mode it belongs to is running.
 */
public final class ComboRequirement {

    private ComboRequirement() {
    }

    /** @return true when the technique needs no mode or the mode it needs is active. */
    public static boolean allows(Player player, INinjaData ninjaData, ResourceLocation abilityId) {
        ResourceLocation required = ComboGroup.requiredFor(abilityId);
        if (required == null || ToggleState.isActive(ninjaData, required)) {
            return true;
        }
        Ability ability = NarutoAbilities.byId(abilityId);
        Component technique = ability == null ? Component.empty()
                : Component.translatable(ability.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW);
        Ability mode = NarutoAbilities.byId(required);
        Component modeName = mode == null ? Component.empty()
                : Component.translatable(mode.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW);
        MutableComponent message = Component.translatable("jutsu.fail.mode.required", technique, modeName)
                .withStyle(ChatFormatting.RED);
        if (player != null) {
            player.displayClientMessage(message, true);
        }
        return false;
    }
}