package com.sekwah.narutoaddon.abilities;

import com.sekwah.narutoaddon.capabilities.INinjaData;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The combo groups of the three eyes.
 *
 * <p>An eye has four base combos, not one combo per technique. The same four keys mean three
 * different things: a plain press casts the click technique, the same press with shift held casts
 * the shift technique, and holding the last key down casts the held technique. The base mod's key
 * handler only knows combos that map to a single ability, so every group is registered there as a
 * channeled <em>carrier</em>; the addon's client mixin intercepts the cast and picks the variant.</p>
 *
 * <p>A slot may be empty: the jutsu menu lists only the click technique of a group and describes the
 * other two in its tooltip, so a branch does not have to invent a third technique just to fill a
 * row. A technique that is one idea rather than a group of three keeps its own combo instead: the
 * Byakugan eye sits in its group's click slot, and the Dharma size dial owns {@code 3211} alone.</p>
 */
public final class ComboGroup {

    /** How the base combo was pressed. */
    public enum Modifier {
        CLICK,
        SHIFT,
        HOLD
    }

    /** One base combo and the techniques behind it; any slot but the click one may be null. */
    public record Group(long combo, ResourceLocation click, ResourceLocation shift, ResourceLocation hold) {
    }

    /**
     * The carrier the base mod sees for every group. It has to be channeled: that is what makes the
     * base handler wait for a hold and report the release, instead of casting on a timer.
     */
    public static final ResourceLocation CARRIER = id("byakugan_farsight");

    private static final List<Group> GROUPS = new ArrayList<>();
    private static final Map<Long, List<Group>> BY_COMBO = new HashMap<>();
    private static final Map<ResourceLocation, Group> BY_MEMBER = new HashMap<>();
    private static final Map<ResourceLocation, ResourceLocation> REQUIRED = new HashMap<>();

    static {
        // Branch 1 - Byakugan.
        group(3112L, "juken_stance", "juken_purge", "sixty_four_palms");
        group(3113L, "rotation", "circular_block", "rotation");
        group(3121L, "air_palm", "eight_trigrams_collapse", "chakra_grip");
        group(3321L, "palm_lance", "byakugan_sonar", null);

        // Branch 2 - Dharma.
        group(3112L, "armor_collapse", null, "space_drill");
        group(3113L, "black_rod_throw", "dimension_banish", null);
        group(3121L, "sewing_pocket", "dimension_step", "sewing_lock");
        group(3321L, "size_shift", "cube_summon", "size_shift");

        // Branch 3 - Tenseigan.
        // Truth-Seeking Orbs are the same idea in both modes, so the slot is shared: the Six Paths
        // mode is listed first and wins while it is up.
        group(3113L, "gudodama_launch", "gudodama_guard", "gudodama_form");
        group(3112L, "flesh_rupture", null, "zero_gravity");
        // The cocoon is the shift arm, the held arm is the gathering: tap to throw one orb, hold to
        // pull the whole ring into one sphere and throw it, shift to wrap up in the cocoon.
        group(3113L, "orb_shot", "orb_cocoon", "orb_shot");
        group(3121L, "projectile_reflect", "gravity_press", "telekinesis_loot");
        group(3321L, "otsutsuki_will", "gravity_well", "golden_wheel");
        // Branch 4 - Sharingan.
        group(3112L, "kurai", "suggestion", "chakra_trail");
        group(3113L, "kagutsuchi", null, "amaterasu");
        group(3121L, "illusory_double", "izanagi", "kotoamatsukami");
        group(3321L, "susanoo", "tsukuyomi", "izanami");
        // Branch 5 - Rinnegan. Two channeled techniques keep their own combos: the grid has only
        // four hold slots and the branch has six channels.
        group(3112L, "black_rod", "naraka_path", "outer_chains");
        group(3113L, "asura_missiles", null, "universal_force");
        group(3121L, "outer_armor", "item_attraction", "momoshiki_absorption");
        group(3321L, "limbo", null, "chakra_transfer");
        // Branch 6 - Senrigan.
        group(3112L, "senrigan_pulse", "senrigan_deathmark", "senrigan_deathstep");
        group(3113L, "senrigan_sight", "senrigan_freecam", null);
        group(3121L, "senrigan_evasion", "senrigan_flight", null);
        group(3321L, "senrigan_world", "senrigan_guidance", null);
        // The grip under the feet and the line thrown from the hand are two halves of one cast:
        // a tap holds, a held press throws.
        group(23L, "chakra_adhesion", null, "chakra_thread");

        require("juken_stance", "byakugan");
        require("juken_purge", "byakugan");
        require("sixty_four_palms", "byakugan");
        require("rotation", "byakugan");
        require("circular_block", "byakugan");
        require("air_palm", "byakugan");
        require("eight_trigrams_collapse", "byakugan");
        require("chakra_grip", "byakugan");
        require("palm_lance", "byakugan");
        require("byakugan_sonar", "byakugan");
        require("flesh_rupture", "tenseigan");
        require("zero_gravity", "tenseigan");
        require("orb_shot", "tenseigan");
        require("orb_cocoon", "tenseigan");
        require("projectile_reflect", "tenseigan");
        require("gravity_press", "tenseigan");
        require("telekinesis_loot", "tenseigan");
        require("otsutsuki_will", "tenseigan");
        require("gravity_well", "tenseigan");
        require("golden_wheel", "tenseigan");
        require("gravity_drill", "tenseigan");
        require("gudodama_launch", "six_paths_mode");
        require("gudodama_guard", "six_paths_mode");
        require("gudodama_form", "six_paths_mode");
        require("kurai", "sharingan");
        require("suggestion", "sharingan");
        require("chakra_trail", "sharingan");
        require("kagutsuchi", "sharingan");
        require("amaterasu", "sharingan");
        require("illusory_double", "sharingan");
        require("izanagi", "sharingan");
        require("kotoamatsukami", "sharingan");
        require("susanoo", "sharingan");
        require("tsukuyomi", "sharingan");
        require("izanami", "sharingan");
        require("black_rod", "rinnegan");
        require("naraka_path", "rinnegan");
        require("outer_chains", "rinnegan");
        require("asura_missiles", "rinnegan");
        require("universal_force", "rinnegan");
        require("outer_armor", "rinnegan");
        require("item_attraction", "rinnegan");
        require("momoshiki_absorption", "rinnegan");
        require("limbo", "rinnegan");
        require("chakra_transfer", "rinnegan");
        require("chibaku_tensei", "rinnegan");
        require("rinnegan_prison", "rinnegan");
        require("armor_collapse", "dharma_eye");
        require("space_drill", "dharma_eye");
        require("black_rod_throw", "dharma_eye");
        require("dimension_banish", "dharma_eye");
        require("sewing_pocket", "dharma_eye");
        require("dimension_step", "dharma_eye");
        require("sewing_lock", "dharma_eye");
        require("size_shift", "dharma_eye");
        require("cube_summon", "dharma_eye");
        require("senrigan_pulse", "senrigan");
        require("senrigan_deathmark", "senrigan");
        require("senrigan_deathstep", "senrigan");
        require("senrigan_sight", "senrigan");
        require("senrigan_freecam", "senrigan");
        require("senrigan_evasion", "senrigan");
        require("senrigan_flight", "senrigan");
        require("senrigan_world", "senrigan");
        require("senrigan_guidance", "senrigan");
    }

    private ComboGroup() {
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation("narutoaddon", path);
    }

    private static void group(long combo, String click, String shift, String hold) {
        Group group = new Group(combo, id(click),
                shift == null ? null : id(shift),
                hold == null ? null : id(hold));
        GROUPS.add(group);
        BY_COMBO.computeIfAbsent(combo, key -> new ArrayList<>()).add(group);
        index(group, group.click());
        index(group, group.shift());
        index(group, group.hold());
    }

    private static void index(Group group, ResourceLocation member) {
        if (member != null) {
            BY_MEMBER.put(member, group);
        }
    }

    public static List<Group> groups() {
        return List.copyOf(GROUPS);
    }

    public static Group byCombo(long combo) {
        List<Group> list = BY_COMBO.get(combo);
        return list == null || list.isEmpty() ? null : list.get(0);
    }

    /** Every group bound to one base combo, in registration order (first match wins). */
    public static List<Group> groupsFor(long combo) {
        List<Group> list = BY_COMBO.get(combo);
        return list == null ? List.of() : List.copyOf(list);
    }

    /** Marks a technique as usable only while the given stance or eye is running. */
    private static void require(String ability, String toggle) {
        REQUIRED.put(id(ability), id(toggle));
    }

    /** @return the stance or eye a technique needs, or null when it needs none. */
    public static ResourceLocation requiredFor(ResourceLocation abilityId) {
        return abilityId == null ? null : REQUIRED.get(abilityId);
    }

    public static ResourceLocation resolve(long combo, Modifier modifier) {
        return resolve(combo, modifier, null);
    }

    /**
     * Picks the technique of a shared combo whose eye or mode is running right now. Groups are
     * tried in registration order; a group whose slot is empty or whose mode is closed is skipped.
     */
    public static ResourceLocation resolve(long combo, Modifier modifier, @Nullable INinjaData data) {
        for (Group group : groupsFor(combo)) {
            ResourceLocation member = switch (modifier) {
                case CLICK -> group.click();
                case SHIFT -> group.shift();
                case HOLD -> group.hold();
            };
            if (member == null) {
                continue;
            }
            ResourceLocation required = requiredFor(member);
            if (required != null && !ToggleState.isActive(data, required)) {
                continue;
            }
            return member;
        }
        return null;
    }

    /** @return the base combo a technique belongs to, or 0 when it is a plain combo of its own. */
    public static long comboOf(ResourceLocation abilityId) {
        Group group = abilityId == null ? null : BY_MEMBER.get(abilityId);
        return group == null ? 0L : group.combo();
    }

    /** Whether the technique is cast through a group (and must not claim a combo of its own). */
    public static boolean isMember(ResourceLocation abilityId) {
        return abilityId != null && BY_MEMBER.containsKey(abilityId);
    }

    /** @return the group a technique belongs to, or null when it is a plain combo of its own. */
    public static Group groupOf(ResourceLocation abilityId) {
        return abilityId == null ? null : BY_MEMBER.get(abilityId);
    }

    /** @return the modifier a technique answers to inside its group, or null. */
    public static Modifier modifierOf(ResourceLocation abilityId) {
        Group group = abilityId == null ? null : BY_MEMBER.get(abilityId);
        if (group == null) {
            return null;
        }
        if (group.click().equals(abilityId)) {
            return Modifier.CLICK;
        }
        if (abilityId.equals(group.shift())) {
            return Modifier.SHIFT;
        }
        return group.hold() != null && group.hold().equals(abilityId) ? Modifier.HOLD : null;
    }
}
