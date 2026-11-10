package com.sekwah.narutoaddon.util;

import com.sekwah.narutoaddon.capabilities.INinjaData;

/**
 * The frog a failed sage becomes: when nature energy runs dry before the mode is mastered, the body
 * is worn as a toad for a minute. A frog has no hands for seals and no room for a stance, so every
 * cast path asks this first.
 */
public final class FrogState {

    /** The entity type the sage-mode failure wears. */
    private static final String FROG_ENTITY = "minecraft:frog";

    private FrogState() {
    }

    public static boolean isFrog(INinjaData ninjaData) {
        if (ninjaData == null) {
            return false;
        }
        return ninjaData.getHengeTicks() > 0 && FROG_ENTITY.equals(ninjaData.getHengeEntityType());
    }
}
