package com.sekwah.narutoaddon.abilities;

import com.sekwah.narutoaddon.capabilities.INinjaData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * Which eye a technique needs open before it may start.
 *
 * <p>A dojutsu branch is not a list of spells that happen to share a tree: the eye is the stance
 * that lets the body do any of it. The tree already refuses to unlock a branch technique without
 * its eye, but an unlock is permanent while the eye is a stance the caster closes, runs dry or
 * swaps for another. Without this check a caster could learn the Dharma branch, close the eye and
 * still run every technique of it for free - and the branch would be a plain spell list again.</p>
 *
 * <p>Only the Dharma branch is listed here. The Byakugan techniques each test their own eye in
 * their own {@code handleCost}, because they also carry per-technique numbers; this table is the
 * place for a branch whose techniques share the same requirement with nothing else to say.</p>
 */
public final class EyeRequirement {

    /** The id of the Dharma Eye, the stance every technique below runs on. */
    public static final ResourceLocation DHARMA_EYE_ID = new ResourceLocation("narutoaddon", "dharma_eye");

    /**
     * Every Dharma technique, i.e. every member of the four Dharma combo groups. The eye itself is
     * deliberately absent: it is the thing that has to be open.
     */
    private static final String[] DHARMA_TECHNIQUES = {
            "size_shift",
            "dimension_step", "dimension_banish", "sewing_pocket",
            "sewing_lock", "armor_collapse", "space_drill",
            "black_rod_throw", "cube_summon",
    };

    private static final Map<ResourceLocation, ResourceLocation> REQUIRED = build();

    private EyeRequirement() {
    }

    private static Map<ResourceLocation, ResourceLocation> build() {
        Map<ResourceLocation, ResourceLocation> map = new HashMap<>();
        for (String path : DHARMA_TECHNIQUES) {
            map.put(new ResourceLocation("narutoaddon", path), DHARMA_EYE_ID);
        }
        return Map.copyOf(map);
    }

    /** @return the eye this technique needs open, or null when it needs none. */
    public static ResourceLocation eyeFor(ResourceLocation abilityId) {
        return abilityId == null ? null : REQUIRED.get(abilityId);
    }

    /** @return whether the technique belongs to the Dharma branch. */
    public static boolean isDharmaTechnique(ResourceLocation abilityId) {
        return DHARMA_EYE_ID.equals(eyeFor(abilityId));
    }

    /**
     * Checks the eye of a technique about to start and tells the player why it refused.
     *
     * @return true when the technique may start, including every technique that needs no eye.
     */
    public static boolean allows(Player player, INinjaData ninjaData, ResourceLocation abilityId) {
        if (eyeIsOpen(ninjaData, abilityId)) {
            return true;
        }
        player.displayClientMessage(refusalMessage(ninjaData, abilityId), true);
        return false;
    }

    /**
     * The eye half of {@link #allows(Player, INinjaData, ResourceLocation)}, without any message:
     * callers that throttle their own refusals (see {@code CastGate}) ask this instead.
     *
     * @return true when the technique needs no eye or its eye is open right now.
     */
    public static boolean eyeIsOpen(INinjaData ninjaData, ResourceLocation abilityId) {
        ResourceLocation eyeId = eyeFor(abilityId);
        if (eyeId == null) {
            return true;
        }
        return ninjaData != null && ToggleState.isActive(ninjaData, eyeId);
    }

    /** The message a closed eye produces, so every cast road can refuse in the same words. */
    public static MutableComponent refusalMessage(INinjaData ninjaData, ResourceLocation abilityId) {
        Ability ability = NarutoAbilities.byId(abilityId);
        MutableComponent name = Component.empty();
        if (ability != null) {
            name = Component.translatable(ability.getTranslationKey(ninjaData))
                    .withStyle(ChatFormatting.YELLOW);
        }
        return Component.translatable("jutsu.fail.dharma_eye.required", name)
                .withStyle(ChatFormatting.RED);
    }
}
