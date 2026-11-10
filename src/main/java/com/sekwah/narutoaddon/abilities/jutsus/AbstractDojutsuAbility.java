package com.sekwah.narutoaddon.abilities.jutsus;

import com.sekwah.narutoaddon.abilities.Ability;
import com.sekwah.narutoaddon.abilities.NarutoAbilities;
import com.sekwah.narutoaddon.abilities.ToggleState;
import com.sekwah.narutoaddon.capabilities.INinjaData;
import com.sekwah.narutoaddon.registries.NarutoRegistries;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/**
 * The shared half of every eye: a stance paid for by the tick, one at a time, closing cleanly.
 *
 * <p>The eyes are not separate mechanics that happen to look alike. They are one rule set - open an
 * eye, pay for it every tick, hold whatever it gives you for as long as you can afford it, close it
 * and give the bonus back in the same tick - and each eye only decides what that bonus is and what
 * kind of chakra it shows around the head. Keeping that here means a new eye is a small class: an
 * id, a config flag, its numbers and its aura.</p>
 *
 * <p>Only one eye can be open. Opening another one closes the one already burning, which is what the
 * eye bloodlines of the setting do anyway: a body that carries two of them at once is a different
 * kind of story. The check is constant work rather than a hidden rule, so a caster can switch mid
 * fight by paying the opening tick of the next eye.</p>
 */
public abstract class AbstractDojutsuAbility extends Ability implements Ability.Toggled, Ability.HandleEnded {

    /** Ticks before natural chakra regeneration resumes after a tick of upkeep. */
    protected static final int CHAKRA_REGEN_DELAY = 20;

    /**
     * Every eye in the mod, so opening one can close the others. Declaration order is the order they
     * are checked in, which only matters in the impossible case of two being open at once.
     *
     * <p>The list is built lazily in a holder instead of in this class's own static initializer on
     * purpose. Registering the abilities does `new SharinganAbility()` and so on, which initializes
     * the subclass, which initializes this class first, which would then read that very subclass's
     * {@code ID} before the subclass's initializer has run - a class initialization cycle, and the
     * id comes back null (crash in {@code List.of}). The holder only runs on the first call to
     * {@link #eyes()}, which is at runtime, long after every eye class is fully initialized.</p>
     */
    private static final class EyeIds {
        private static final List<ResourceLocation> ALL = List.of(
                SharinganAbility.ID,
                ByakuganAbility.ID,
                RinneganAbility.ID,
                SenriganAbility.ID,
                TenseiganAbility.ID,
                DharmaEyeAbility.ID);
    }

    /** @return every eye in the mod, in the order they are checked. */
    public static List<ResourceLocation> eyes() {
        return EyeIds.ALL;
    }

    /** The registry id of this eye, which is also its key in the toggle set. */
    protected abstract ResourceLocation eyeId();

    /** Whether this eye is allowed on the server at all. */
    protected abstract boolean enabled();

    /** Translation key of the "this eye is disabled" message. */
    protected abstract String disabledKey();

    /** Chakra burnt for every tick the eye stays open. */
    protected abstract float chakraPerTick();

    /**
     * Writes whatever capacity this eye hands out. Called on every active tick rather than once, so
     * a bonus cleared by a death or a reload comes back the moment the eye is running again.
     */
    protected void applyEyeBonus(INinjaData ninjaData) {
    }

    /**
     * Draws the eye's own chakra around the caster. Called on every active tick on the server; the
     * eyes use different colours and frequencies so two of them never read the same in a fight.
     */
    protected void aura(ServerLevel level, Player player, INinjaData ninjaData) {
    }

    @Override
    public ActivationType activationType() {
        return ActivationType.TOGGLE;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (!this.enabled()) {
            player.displayClientMessage(Component.translatable(this.disabledKey())
                    .withStyle(ChatFormatting.RED), true);
            return false;
        }
        float cost = this.upkeepCost(ninjaData);
        if (ninjaData.getChakra() < cost) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        ninjaData.useChakra(cost, this.upkeepInterruptsRegen(cost) ? CHAKRA_REGEN_DELAY : 0);
        return true;
    }

    /**
     * Whether paying the upkeep this tick should hold the body's own chakra regeneration back. It
     * normally should - a stance that costs anything at all is a stance that eats into the bar - but
     * an eye whose upkeep has faded to a whisper may say otherwise, and then the trickle the body
     * makes by itself keeps running under it.
     */
    protected boolean upkeepInterruptsRegen(float cost) {
        return true;
    }

    /**
     * What holding the eye open costs this tick. The ordinary answer is {@link #chakraPerTick()} after
     * mastery and the skill tree, but an eye may decide that its own upkeep fades with practice - a
     * mastered Byakugan is nearly free to keep open - so the price it is checked and paid against
     * comes from here and never from two different formulas.
     */
    protected float upkeepCost(INinjaData ninjaData) {
        return this.masteryCost(ninjaData, this.chakraPerTick());
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        closeOtherEyes(player, ninjaData);
        this.applyEyeBonus(ninjaData);
        // Mastery is earned by holding the eye open, not by flicking it on.
        this.recordMasteryCredit(player, ninjaData, MASTERY_CREDIT_PER_TICK);
        if (player.level() instanceof ServerLevel level) {
            this.aura(level, player, ninjaData);
        }
    }

    /**
     * A stance is mastered by holding it, so the credit is paid per active tick from
     * {@link #performServer} and this records nothing.
     */
    @Override
    public void recordMastery(Player player, INinjaData ninjaData) {
    }

    @Override
    public void performToggleClient(Player player, INinjaData ninjaData) {
        // Only state is synced; there is no client side body to maintain.
    }

    @Override
    public void handleAbilityEnded(Player player, INinjaData ninjaData, int ticksActive) {
        clearEyeBonus(ninjaData);
        // The pipeline that ended the eye only removed its own copy. An eye opened from the jutsu
        // menu and closed with its key (or the other way round) would otherwise stay in the other
        // set, and that set's tick loop would keep paying for it and keep the picture on screen.
        ToggleState.clear(ninjaData, eyeId());
    }

    /**
     * Takes the capacity the eye was handing out back off the caster. Public so that anything that
     * ends the stance on its own - death, a logout, a second eye taking over - can put the pools
     * back the way they were.
     */
    public static void clearEyeBonus(INinjaData ninjaData) {
        ninjaData.setEyeChakraBonus(0.0f);
        ninjaData.setEyeSeninBonus(0.0f);
    }

    /**
     * Runs the shared half of opening this eye: whichever other eye was open is closed here, before
     * the new one is added to the toggle set. This is what the activation packet calls, so a switch
     * happens in one step. Doing it a tick later from the upkeep was the bug: both eyes were in the
     * set for a tick, each upkeep closed the other, and the caster could end up with neither open.
     */
    public void switchFrom(Player player, INinjaData ninjaData) {
        closeOtherEyes(player, ninjaData);
    }

    /** @return whether any eye of the mod is open on this caster right now. */
    public static boolean anyEyeActive(INinjaData ninjaData) {
        for (ResourceLocation eyeId : eyes()) {
            if (isEyeActive(ninjaData, eyeId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Closes every other eye the caster has open, paying the little bookkeeping each of them needs:
     * its own bonus is cleared and its aura stops that same tick.
     */
    protected void closeOtherEyes(Player player, INinjaData ninjaData) {
        for (ResourceLocation otherId : eyes()) {
            if (otherId.equals(this.eyeId())) {
                continue;
            }
            if (!ToggleState.isActive(ninjaData, otherId)) {
                continue;
            }
            Ability other = NarutoAbilities.byId(otherId);
            if (other != null) {
                ninjaData.getToggleAbilityData().removeAbilityEnded(player, ninjaData, other);
            }
        }
    }

    /**
     * @return whether the caster has this eye open. Read from the synced toggle set, which is the
     *         same state the upkeep runs on, so the two can never disagree.
     */
    protected static boolean isEyeActive(INinjaData ninjaData, ResourceLocation eyeId) {
        // Read both toggle sets: an eye opened from the menu sits in the addon layer's set, one
        // opened with its keys sits in the base capability's set. See ToggleState.
        return ToggleState.isActive(ninjaData, eyeId);
    }
}
