package com.sekwah.narutoaddon.mixin.client;

import com.sekwah.narutoaddon.abilities.jutsus.ByakuganAbility;
import com.sekwah.narutoaddon.capabilities.INinjaData;
import com.sekwah.narutoaddon.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutoaddon.client.gui.JutsuScreen;
import com.sekwah.narutomod.client.keybinds.KeyBindingTickHeld;
import com.sekwah.narutomod.client.keybinds.NarutoKeyHandler;
import com.sekwah.narutomod.config.NarutoConfig;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NarutoKeyHandler.class)
public class KeyHandlerMixin {

    @Shadow(remap = false)
    private static KeyBindingTickHeld JUTSU_MENU_KEY;

    @Shadow(remap = false)
    private static long currentJutsuCombo;

    @Shadow(remap = false)
    private static int ticksSinceLastKey;

    @Shadow(remap = false)
    private static void resetJutsuCasting() {
        throw new AssertionError();
    }

    @Inject(method = "registerKeyBinds", at = @At("TAIL"), remap = false)
    private static void narutoaddon$swapJutsuMenu(CallbackInfo ci) {
        if (JUTSU_MENU_KEY != null) {
            JUTSU_MENU_KEY.registerClickConsumer(() -> Minecraft.getInstance().setScreen(new JutsuScreen()));
        }
    }

    /**
     * Holding one jutsu key is not a technique: it is how every combo starts, and for the Byakugan
     * it is also how the far sight is held. The base handler treats an idle combo as a failed cast
     * and scolds the player with "combo not found" once the cast delay runs out; a lone digit is
     * dropped silently so a held key does not spam the action bar. The delay is respected exactly
     * as the base checks it, or a combo in progress would be cut after its first digit.
     */
    @Inject(method = "checkNonCharging", at = @At("HEAD"), cancellable = true, remap = false)
    private static void narutoaddon$dropLoneDigit(CallbackInfo ci) {
        if (currentJutsuCombo > 0L && currentJutsuCombo < 10L
                && ticksSinceLastKey > NarutoConfig.jutsuCastDelay) {
            resetJutsuCasting();
            ci.cancel();
        }
    }

    /**
     * A held eye combo belongs to the far sight, not to the toggle.
     *
     * <p>The Byakugan is a toggle, so typing its combo and keeping the last key down used to switch
     * the eye - usually off, since the caster had just opened it - and the zoom it was meant to ask
     * for never happened. A tap still toggles; a hold past the cast delay leaves the eye exactly as
     * it is and hands the key to the lens.</p>
     */
    @Inject(method = "checkNonCharging", at = @At("HEAD"), cancellable = true, remap = false)
    private static void narutoaddon$holdEyeForFarsight(CallbackInfo ci) {
        if (ticksSinceLastKey <= NarutoConfig.jutsuCastDelay) {
            return;
        }
        if (currentJutsuCombo != ByakuganAbility.COMBO) {
            return;
        }
        if (!narutoaddon$eyeOpen() || !narutoaddon$comboKeyHeld()) {
            return;
        }
        // Held, not tapped: the eye stays as it is and the key now drives the zoom.
        resetJutsuCasting();
        ci.cancel();
    }

    private static boolean narutoaddon$eyeOpen() {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return false;
        }
        INinjaData data = player.getCapability(NinjaCapabilityHandler.NINJA_DATA).resolve().orElse(null);
        return data != null && ByakuganAbility.isActive(data);
    }

    private static boolean narutoaddon$comboKeyHeld() {
        KeyMapping key = com.sekwah.narutoaddon.client.keybinds.NarutoKeyHandler
                .getJutsuKey((int) (ByakuganAbility.COMBO % 10L));
        return key != null && key.isDown();
    }
}
