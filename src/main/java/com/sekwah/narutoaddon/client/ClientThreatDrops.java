package com.sekwah.narutoaddon.client;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client-side cache of the Senrigan "threat readout" loot preview: the server rolls each mob's loot
 * table and ships the stable preview down, and the overlay reads it from here.
 */
public final class ClientThreatDrops {

    private static final Map<Integer, List<ItemStack>> DROPS = new HashMap<>();

    private ClientThreatDrops() {
    }

    /** Replace the preview for one entity. */
    public static void apply(int entityId, List<ItemStack> stacks) {
        if (stacks == null || stacks.isEmpty()) {
            DROPS.remove(entityId);
            return;
        }
        List<ItemStack> copy = new ArrayList<>();
        for (ItemStack stack : stacks) {
            if (stack != null && !stack.isEmpty()) {
                copy.add(stack.copy());
            }
        }
        if (copy.isEmpty()) {
            DROPS.remove(entityId);
        } else {
            DROPS.put(entityId, copy);
        }
    }

    /** @return the cached preview for an entity, or an empty list when nothing is known. */
    public static List<ItemStack> get(int entityId) {
        List<ItemStack> stacks = DROPS.get(entityId);
        return stacks == null ? List.of() : stacks;
    }

    /** Drop every cached preview (used when the eye closes or the world unloads). */
    public static void clear() {
        DROPS.clear();
    }
}
