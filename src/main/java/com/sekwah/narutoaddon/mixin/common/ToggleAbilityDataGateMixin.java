package com.sekwah.narutoaddon.mixin.common;

import com.sekwah.narutoaddon.abilities.Ability;
import com.sekwah.narutoaddon.abilities.NarutoAbilities;
import com.sekwah.narutoaddon.abilities.ToggleState;
import com.sekwah.narutoaddon.bridge.AbilityProxy;
import com.sekwah.narutoaddon.bridge.ToggleGate;
import com.sekwah.narutoaddon.capabilities.AddonNinjaData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(com.sekwah.narutomod.capabilities.toggleabilitydata.ToggleAbilityData.class)
public class ToggleAbilityDataGateMixin {

    @Inject(method = "addAbilityStarted", at = @At("HEAD"), cancellable = true, remap = false)
    private void narutoaddon$gateToggleStart(Player player,
                                            com.sekwah.narutomod.capabilities.INinjaData ninjaData,
                                            com.sekwah.narutomod.abilities.Ability ability,
                                            CallbackInfoReturnable<Boolean> cir) {
        if (ability instanceof AbilityProxy proxy) {
            AddonNinjaData addon = AbilityProxy.adapterFor(ninjaData);
            if (addon == null) {
                return;
            }
            // A stance switched on from the jutsu menu is written into the addon layer's own set,
            // which the base packet that owns this one never reads. That packet therefore thinks the
            // stance is off and starts a second copy, so one press opened the eye and the next press
            // merely duplicated it - the player had to press twice to close what one press opened.
            // Ending it here makes the press a close, whatever set it was started from.
            ResourceLocation id = NarutoAbilities.idOf(proxy.getDelegate());
            if (id != null && ToggleState.isActive(addon, id)) {
                ToggleState.deactivate(player, addon, proxy.getDelegate());
                cir.setReturnValue(false);
                return;
            }
            if (!ToggleGate.tryStart(player, addon, proxy.getDelegate())) {
                cir.setReturnValue(false);
            }
        }
    }

    @Inject(method = "removeAbilityEnded", at = @At("HEAD"), cancellable = true, remap = false)
    private void narutoaddon$retoggleOnEnd(Player player,
                                          com.sekwah.narutomod.capabilities.INinjaData ninjaData,
                                          com.sekwah.narutomod.abilities.Ability ability,
                                          CallbackInfoReturnable<Boolean> cir) {
        if (ability instanceof AbilityProxy proxy) {
            if (proxy.getDelegate() instanceof Ability.Retoggle retoggle) {
                AddonNinjaData addon = AbilityProxy.adapterFor(ninjaData);
                if (addon == null) {
                    return;
                }
                if (retoggle.onRetoggle(player, addon)) {
                    cir.setReturnValue(true);
                }
            }
        }
    }
}
