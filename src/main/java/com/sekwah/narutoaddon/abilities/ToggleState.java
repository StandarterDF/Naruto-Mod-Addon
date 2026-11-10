package com.sekwah.narutoaddon.abilities;

import com.sekwah.narutoaddon.capabilities.AddonNinjaData;
import com.sekwah.narutoaddon.capabilities.INinjaData;
import com.sekwah.narutoaddon.registries.NarutoRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.HashSet;
import java.util.Set;

/**
 * The one place that answers "is this stance running right now?".
 *
 * <p>There are two toggle sets in play and both are real. A stance switched on from the jutsu menu
 * is written into the addon layer's set and ticked by the addon; a stance switched on with its key
 * combo goes through the base mod's packet and lives in the base capability's set, where the base
 * tick loop drives it. Asking only one of them was the bug behind several "the toggle burns chakra
 * but nothing happens" reports: the stance was running in the set nobody looked at.</p>
 *
 * <p>Reading both sets costs nothing and needs no synchronisation between them. Writing is rarer and
 * goes through {@link #deactivate}, which ends the ability once and clears it wherever it sits.</p>
 *
 * <p>Stance constants may be declared under either the {@code narutomod} or {@code narutoaddon}
 * namespace, so comparisons tolerate both.</p>
 */
public final class ToggleState {

    private ToggleState() {
    }

    /**
     * @return whether the stance is running in either set.
     * <p>Toggle keys may be stored under either the {@code narutomod} or {@code narutoaddon}
     * namespace, so a match is accepted across the two namespaces when the path is the same.</p>
     */
    public static boolean isActive(INinjaData ninjaData, ResourceLocation abilityId) {
        if (ninjaData == null || abilityId == null) {
            return false;
        }
        for (ResourceLocation entry : ninjaData.getToggleAbilityData().getAbilitiesHashSet()) {
            if (matches(entry, abilityId)) {
                return true;
            }
        }
        Set<ResourceLocation> base = baseToggles(ninjaData);
        if (base != null) {
            for (ResourceLocation entry : base) {
                if (matches(entry, abilityId)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * @return a snapshot of every running stance, addon layer and base layer together.
     * <p>Keys are canonicalised to the {@code narutoaddon} namespace when such a registry entry exists,
     * tolerating the legacy {@code narutomod} namespace used by some stance constants.</p>
     */
    public static Set<ResourceLocation> activeToggles(INinjaData ninjaData) {
        Set<ResourceLocation> all = new HashSet<>();
        if (ninjaData == null) {
            return all;
        }
        for (ResourceLocation key : ninjaData.getToggleAbilityData().getAbilitiesHashSet()) {
            addBothNamespaces(all, key);
        }
        Set<ResourceLocation> base = baseToggles(ninjaData);
        if (base != null) {
            for (ResourceLocation key : base) {
                addBothNamespaces(all, key);
            }
        }
        return all;
    }

    private static ResourceLocation canonicalize(ResourceLocation key) {
        if (key == null) {
            return null;
        }
        if ("narutomod".equals(key.getNamespace())) {
            if (NarutoRegistries.ABILITIES != null
                    && NarutoRegistries.ABILITIES.containsKey(new ResourceLocation("narutoaddon", key.getPath()))) {
                return new ResourceLocation("narutoaddon", key.getPath());
            }
        }
        return key;
    }

    /**
     * Adds the key under both namespaces when the stance exists in the registry: callers compare a
     * snapshot with {@code contains(SomeAbility.ID)}, and those constants are declared under either
     * namespace, so a single spelling would hide half of the running stances from the HUD.
     */
    private static void addBothNamespaces(Set<ResourceLocation> all, ResourceLocation key) {
        ResourceLocation canonical = canonicalize(key);
        if (canonical == null) {
            return;
        }
        all.add(canonical);
        if ("narutoaddon".equals(canonical.getNamespace())) {
            all.add(new ResourceLocation("narutomod", canonical.getPath()));
        }
    }

    /**
     * Ends the stance and takes it out of whichever set holds it. {@code handleAbilityEnded} runs
     * once, here, so the ability gives back what it took (the size dial returns the body to normal
     * size, a cloak drops its bonuses) regardless of how it was switched on.
     */
    public static void deactivate(Player player, INinjaData ninjaData, Ability ability) {
        if (ninjaData == null || ability == null) {
            return;
        }
        if (ability instanceof Ability.HandleEnded ended) {
            ended.handleAbilityEnded(player, ninjaData, 0);
        }
        clear(ninjaData, NarutoAbilities.idOf(ability));
    }

    /**
     * Takes the stance out of both toggle sets and does nothing else.
     *
     * <p>Each of the two pipelines only ever removes the entry it owns: the base ability packet
     * knows the base set, the jutsu menu knows the addon set. An ability switched on from one side
     * and off from the other therefore used to stay in the set nobody removed it from, and its own
     * tick loop kept it running - the eye was "closed" but its picture never went away. Whoever ends
     * a stance calls this so the other set cannot keep it alive.</p>
     */
    public static void clear(INinjaData ninjaData, ResourceLocation abilityId) {
        if (ninjaData == null || abilityId == null) {
            return;
        }
        Set<ResourceLocation> toRemove = new HashSet<>();
        for (ResourceLocation entry : ninjaData.getToggleAbilityData().getAbilitiesHashSet()) {
            if (matches(entry, abilityId)) {
                toRemove.add(entry);
            }
        }
        Set<ResourceLocation> base = baseToggles(ninjaData);
        if (base != null) {
            for (ResourceLocation entry : base) {
                if (matches(entry, abilityId)) {
                    toRemove.add(entry);
                }
            }
        }
        for (ResourceLocation each : toRemove) {
            ninjaData.getToggleAbilityData().removeAbility(each);
            if (base != null) {
                base.remove(each);
            }
        }
    }

    private static boolean matches(ResourceLocation stored, ResourceLocation id) {
        if (stored == null || id == null) return false;
        if (stored.equals(id)) return true;
        if (!stored.getPath().equals(id.getPath())) return false;
        String a = stored.getNamespace();
        String b = id.getNamespace();
        return ("narutomod".equals(a) && "narutoaddon".equals(b)) || ("narutoaddon".equals(a) && "narutomod".equals(b));
    }

    /** @return the base capability's live toggle set, or null when the addon runs on its own. */
    private static Set<ResourceLocation> baseToggles(INinjaData ninjaData) {
        if (ninjaData instanceof AddonNinjaData adapter) {
            return adapter.getBase().getToggleAbilityData().getAbilitiesHashSet();
        }
        return null;
    }
}
