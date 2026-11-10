package com.sekwah.narutoaddon.client.keybinds;

import com.sekwah.narutoaddon.mixin.client.BaseKeyHandlerAccessor;
import net.minecraft.client.KeyMapping;

/**
 * Client-side helpers around the base mod's key handler.
 *
 * <p>The addon does not register combo keys of its own: the base 1.8.1 already owns the three jutsu
 * keys and the menu key, and {@code mixin/client/KeyHandlerMixin} only swaps the menu screen. This
 * class exists so the jutsu menu can print which key a combo digit is on without touching the base
 * class directly.</p>
 */
public final class NarutoKeyHandler {

    private NarutoKeyHandler() {
    }

    /**
     * @param digit combo digit, 1..3
     * @return the matching key mapping, or {@code null} before the client registered the keys
     */
    public static KeyMapping getJutsuKey(int digit) {
        try {
            return switch (digit) {
                case 1 -> BaseKeyHandlerAccessor.narutoaddon$jutsuCKey();
                case 2 -> BaseKeyHandlerAccessor.narutoaddon$jutsuVKey();
                case 3 -> BaseKeyHandlerAccessor.narutoaddon$jutsuBKey();
                default -> null;
            };
        } catch (Throwable notRegisteredYet) {
            return null;
        }
    }
}
