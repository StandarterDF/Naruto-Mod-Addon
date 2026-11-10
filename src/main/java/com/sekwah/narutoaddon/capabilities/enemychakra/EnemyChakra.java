package com.sekwah.narutoaddon.capabilities.enemychakra;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;

/**
 * Hidden chakra pool attached to any living entity (mob or player), stored in the entity's persistent
 * data. The pool scales with maximum health and drives debuffs at low ratios.
 */
public final class EnemyChakra {

    public static final float CHAKRA_PER_HEALTH = 4.0f;

    private static final String KEY_CHAKRA = "narutoaddon:enemy_chakra";
    private static final String KEY_MAX = "narutoaddon:enemy_chakra_max";
    private static final String KEY_HIT_AT = "narutoaddon:enemy_chakra_hit_at";
    private static final String KEY_SILENCED_UNTIL = "narutoaddon:silenced_until";

    private EnemyChakra() {
    }

    public static float max(LivingEntity e) {
        CompoundTag data = e.getPersistentData();
        if (data.contains(KEY_MAX)) {
            return data.getFloat(KEY_MAX);
        }
        float value = e.getMaxHealth() * CHAKRA_PER_HEALTH;
        data.putFloat(KEY_MAX, value);
        return value;
    }

    public static float pool(LivingEntity e) {
        CompoundTag data = e.getPersistentData();
        if (!data.contains(KEY_CHAKRA)) {
            float value = max(e);
            data.putFloat(KEY_CHAKRA, value);
            return value;
        }
        return data.getFloat(KEY_CHAKRA);
    }

    public static float ratio(LivingEntity e) {
        float m = max(e);
        if (m <= 0.0f) {
            return 1.0f;
        }
        return pool(e) / m;
    }

    public static void drain(LivingEntity e, float amount) {
        float current = pool(e);
        current = Math.max(0.0f, current - amount);
        e.getPersistentData().putFloat(KEY_CHAKRA, current);
        e.getPersistentData().putLong(KEY_HIT_AT, e.level().getGameTime());
    }

    public static void restore(LivingEntity e, float amount) {
        float current = pool(e);
        float m = max(e);
        current = Math.min(m, current + amount);
        e.getPersistentData().putFloat(KEY_CHAKRA, current);
    }

    public static void reset(LivingEntity e) {
        e.getPersistentData().putFloat(KEY_CHAKRA, max(e));
    }

    public static int tier(float ratio) {
        if (ratio <= 0.0f) {
            return 5;
        }
        if (ratio < 0.10f) {
            return 4;
        }
        if (ratio < 0.25f) {
            return 3;
        }
        if (ratio < 0.50f) {
            return 2;
        }
        if (ratio < 0.75f) {
            return 1;
        }
        return 0;
    }

    public static long lastHitAt(LivingEntity e) {
        return e.getPersistentData().getLong(KEY_HIT_AT);
    }

    public static void silence(LivingEntity e, int ticks) {
        e.getPersistentData().putLong(KEY_SILENCED_UNTIL, e.level().getGameTime() + ticks);
    }

    public static boolean isSilenced(LivingEntity e) {
        return e.getPersistentData().getLong(KEY_SILENCED_UNTIL) > e.level().getGameTime();
    }
}