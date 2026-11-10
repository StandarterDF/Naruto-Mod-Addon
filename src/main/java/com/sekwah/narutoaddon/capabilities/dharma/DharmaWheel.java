package com.sekwah.narutoaddon.capabilities.dharma;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;

/**
 * Tracks per-damage-type adaptation ("Wheel of Dharma" passive). After the first hit of a given
 * damage type, every subsequent hit of the same type within the next 10 seconds deals half damage.
 */
public final class DharmaWheel {

    private static final String ROOT_KEY = "narutoaddon:dharma_wheel";

    private DharmaWheel() {
    }

    /** Stable identifier for a damage type, used as the key inside the stored compound. */
    private static String typeId(DamageSource source) {
        return source.typeHolder().unwrapKey()
                .map(key -> key.location().toString())
                .orElse(source.getMsgId());
    }

    public static boolean isAdapted(Player p, DamageSource source) {
        if (p == null || source == null) {
            return false;
        }
        CompoundTag root = p.getPersistentData().getCompound(ROOT_KEY);
        String id = typeId(source);
        if (!root.contains(id)) {
            return false;
        }
        CompoundTag entry = root.getCompound(id);
        int hits = entry.getInt("hits");
        long until = entry.getLong("until");
        return hits >= 2 && until > p.level().getGameTime();
    }

    public static void recordHit(Player p, DamageSource source) {
        if (p == null || source == null) {
            return;
        }
        long now = p.level().getGameTime();
        CompoundTag root = p.getPersistentData().getCompound(ROOT_KEY);
        // Drop expired entries while we are here.
        for (String key : new ArrayList<>(root.getAllKeys())) {
            CompoundTag entry = root.getCompound(key);
            if (entry.getLong("until") <= now) {
                root.remove(key);
            }
        }
        String id = typeId(source);
        CompoundTag entry = root.contains(id) ? root.getCompound(id) : new CompoundTag();
        entry.putInt("hits", entry.getInt("hits") + 1);
        entry.putLong("until", now + 200L);
        root.put(id, entry);
        p.getPersistentData().put(ROOT_KEY, root);
    }

    public static float multiplier(Player p, DamageSource source) {
        return isAdapted(p, source) ? 0.5f : 1.0f;
    }

    public static void reset(Player p) {
        if (p == null) {
            return;
        }
        p.getPersistentData().remove(ROOT_KEY);
    }

    public static void copy(Player from, Player to) {
        if (from == null || to == null) {
            return;
        }
        if (from.getPersistentData().contains(ROOT_KEY)) {
            to.getPersistentData().put(ROOT_KEY, from.getPersistentData().getCompound(ROOT_KEY).copy());
        } else {
            to.getPersistentData().remove(ROOT_KEY);
        }
    }
}
