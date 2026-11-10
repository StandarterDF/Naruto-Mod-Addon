package com.sekwah.narutoaddon.client;

import com.sekwah.narutoaddon.client.gui.SupplyChestScreen;
import com.sekwah.narutoaddon.menu.NarutoMenus;
import net.minecraft.client.gui.screens.MenuScreens;

/**
 * Hooks the mod's menus up to their screens. Called from client setup, so it never loads on a
 * dedicated server.
 */
public final class NarutoClientScreens {

    private NarutoClientScreens() {
    }

    public static void register() {
        MenuScreens.register(NarutoMenus.SUPPLY_CHEST.get(), SupplyChestScreen::new);
    }
}
