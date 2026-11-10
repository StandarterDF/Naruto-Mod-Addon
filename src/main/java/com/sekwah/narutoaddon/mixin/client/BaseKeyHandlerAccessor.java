package com.sekwah.narutoaddon.mixin.client;

import com.sekwah.narutomod.client.keybinds.KeyBindingTickHeld;
import com.sekwah.narutomod.client.keybinds.NarutoKeyHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Reads the base mod's jutsu key mappings.
 *
 * <p>The three combo keys are private static fields of the base key handler and the access transformer
 * of this project does not reach the compile classpath, so they are exposed as static accessors
 * instead of by reflection on a field name. The names are the mod's own, not Minecraft's, so they do
 * not move between mappings.</p>
 */
@Mixin(NarutoKeyHandler.class)
public interface BaseKeyHandlerAccessor {

    @Accessor("JUTSU_C_KEY")
    static KeyBindingTickHeld narutoaddon$jutsuCKey() {
        throw new AssertionError();
    }

    @Accessor("JUTSU_V_KEY")
    static KeyBindingTickHeld narutoaddon$jutsuVKey() {
        throw new AssertionError();
    }

    @Accessor("JUTSU_B_KEY")
    static KeyBindingTickHeld narutoaddon$jutsuBKey() {
        throw new AssertionError();
    }
}
