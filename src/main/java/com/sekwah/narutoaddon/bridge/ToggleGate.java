package com.sekwah.narutoaddon.bridge;

import com.sekwah.narutoaddon.abilities.ToggleState;
import com.sekwah.narutoaddon.abilities.Ability;
import com.sekwah.narutoaddon.abilities.AbilityUnlocks;
import com.sekwah.narutoaddon.abilities.ModeBus;
import com.sekwah.narutoaddon.abilities.jutsus.AbstractDojutsuAbility;
import com.sekwah.narutoaddon.abilities.jutsus.ChakraCloakAbility;
import com.sekwah.narutoaddon.capabilities.AddonNinjaData;
import com.sekwah.narutoaddon.events.ByakuganEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * Re-applies the addon's toggle entry gates on the base mod's combo-key path.
 *
 * <p>The base mod's {@code ServerAbilityActivatePacket} (combo keys 1..6) only adds/removes the
 * ability in {@code ToggleAbilityData} and never runs {@code handleCost}, {@code canStartToggle} or
 * the unlock/disabled/seal/cloak gates. This helper repeats exactly the same chain that
 * {@code com.sekwah.narutoaddon.network.c2s.ServerAbilityActivatePacket} runs (lines 88-118 plus the
 * switch-on calls at 166 and 169-173), in the same order and with the same messages, so a toggle
 * started through combo keys is subject to the same rules as one started through the jutsu menu.</p>
 */
public final class ToggleGate {

    private ToggleGate() {
    }

    /**
     * @return {@code true} when the toggle may start (and the dojutsu switch / mode close have been
     *         applied); {@code false} when a gate refused it (the player has already been told why).
     */
    public static boolean tryStart(Player player, AddonNinjaData ninjaData, Ability ability) {
        // The proxy is what lives in the base registry; resolve it from the delegate.
        AbilityProxy proxyAbility = AbilityProxy.getProxy(ability);
        ResourceLocation abilityId = null;
        if (proxyAbility != null) {
            abilityId = com.sekwah.narutomod.registries.NarutoRegistries.ABILITIES.getResourceKey(proxyAbility)
                    .map(ResourceKey::location).orElse(null);
        }

        // 1. Unlock gate (matches ServerAbilityActivatePacket 88-93).
        if (!AbilityUnlocks.isUnlocked(ninjaData, abilityId)) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notunlocked",
                    Component.translatable(ability.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW))
                    .withStyle(ChatFormatting.RED), true);
            return false;
        }
        // 2. Disabled in the jutsu menu (matches 96-101).
        if (ninjaData.isAbilityDisabled(abilityId)) {
            player.displayClientMessage(Component.translatable("jutsu.fail.techdisabled",
                    Component.translatable(ability.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW))
                    .withStyle(ChatFormatting.RED), true);
            return false;
        }
        // 3. Byakugan tenketsu seal (matches 105-108), waived when closing an already running toggle.
        boolean closesRunningToggle = ability.activationType() == Ability.ActivationType.TOGGLE
                && abilityId != null
                && ToggleState.isActive(ninjaData, abilityId);
        if (ByakuganEvents.isSealed(player) && !closesRunningToggle) {
            player.displayClientMessage(Component.translatable("jutsu.fail.tenketsu")
                    .withStyle(ChatFormatting.RED), true);
            return false;
        }
        // 4. Chakra cloak (matches 113-117), waived when closing an already running toggle.
        if (!ChakraCloakAbility.leavesRoomFor(player, ability) && !closesRunningToggle) {
            player.displayClientMessage(Component.translatable("jutsu.fail.chakra_cloak.locked",
                    Component.translatable(ability.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW))
                    .withStyle(ChatFormatting.RED), true);
            return false;
        }
        // 5. Toggle entry price / stance rules (matches 166).
        if (!ability.canStartToggle(player, ninjaData)) {
            return false;
        }
        // Success: eye hand-off and single-mode enforcement (matches 169-175).
        if (ability instanceof AbstractDojutsuAbility eye) {
            eye.switchFrom(player, ninjaData);
        }
        ModeBus.closeOthers(player, ninjaData, abilityId);
        // The jutsu menu path credits the cast too (ServerAbilityActivatePacket:175); without this
        // a stance switched on with the combo keys would earn no mastery for it.
        ability.recordMastery(player, ninjaData);
        return true;
    }
}
