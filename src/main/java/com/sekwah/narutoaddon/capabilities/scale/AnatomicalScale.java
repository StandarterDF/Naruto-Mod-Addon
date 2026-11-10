package com.sekwah.narutoaddon.capabilities.scale;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * Per-mob-type kill counters for the owner of the Byakugan, stored in the player's persistent data.
 *
 * <p>The counters are keyed by the registry name of the slain entity type (e.g. {@code minecraft:zombie})
 * inside a nested {@code CompoundTag} under {@code narutoaddon:anatomical_kills}. They are used to scale
 * Juken damage against a given mob type up to {@link #MAX_MULTIPLIER}.</p>
 */
public final class AnatomicalScale {

    public static final int KILLS_FOR_MAX = 50;
    public static final float MAX_MULTIPLIER = 5.0f;

    private static final String ROOT_KEY = "narutoaddon:anatomical_kills";

    private AnatomicalScale() {
    }

    private static String typeKey(EntityType<?> type) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
    }

    private static CompoundTag root(Player p) {
        CompoundTag data = p.getPersistentData();
        if (!data.contains(ROOT_KEY)) {
            return null;
        }
        return data.getCompound(ROOT_KEY);
    }

    public static int kills(Player p, EntityType<?> type) {
        CompoundTag r = root(p);
        if (r == null || type == null) {
            return 0;
        }
        String k = typeKey(type);
        if (!r.contains(k)) {
            return 0;
        }
        return r.getInt(k);
    }

    public static float multiplier(Player p, EntityType<?> type) {
        int k = kills(p, type);
        return 1f + (MAX_MULTIPLIER - 1f) * Math.min(1f, k / (float) KILLS_FOR_MAX);
    }

    public static void addKill(Player p, EntityType<?> type) {
        if (p == null || type == null) {
            return;
        }
        CompoundTag data = p.getPersistentData();
        CompoundTag r = data.contains(ROOT_KEY) ? data.getCompound(ROOT_KEY) : new CompoundTag();
        String k = typeKey(type);
        int current = r.contains(k) ? r.getInt(k) : 0;
        if (current >= KILLS_FOR_MAX) {
            data.put(ROOT_KEY, r);
            return;
        }
        r.putInt(k, Math.min(KILLS_FOR_MAX, current + 1));
        data.put(ROOT_KEY, r);
    }

    public static void reset(Player p) {
        if (p == null) {
            return;
        }
        p.getPersistentData().remove(ROOT_KEY);
    }

    public static Map<String, Integer> all(Player p) {
        Map<String, Integer> result = new HashMap<>();
        CompoundTag r = root(p);
        if (r == null) {
            return result;
        }
        for (String k : r.getAllKeys()) {
            result.put(k, r.getInt(k));
        }
        return result;
    }
}
