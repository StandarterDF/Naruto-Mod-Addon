package com.sekwah.narutoaddon.abilities.jutsus;

import com.sekwah.narutoaddon.abilities.Ability;
import com.sekwah.narutoaddon.abilities.NarutoAbilities;
import com.sekwah.narutoaddon.abilities.ToggleState;
import com.sekwah.narutoaddon.capabilities.INinjaData;
import com.sekwah.narutoaddon.config.NarutoConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/**
 * The Tenseigan: the eye that opens the chakra mode.
 *
 * <p>While the eye is open the caster gains the chakra mode - flight, strength, reduced incoming
 * damage and the ability to stand on water and lava. The upkeep is paid per tick like any other eye.</p>
 */
public class TenseiganAbility extends AbstractDojutsuAbility {

    /** The combo that opens the Tenseigan. */
    public static final long COMBO = 3311L;

    public static final ResourceLocation ID = new ResourceLocation("narutoaddon", "tenseigan");

    /** Truth-Seeking Orbs the halo behind the caster carries while the eye is open. */
    public static final int MAX_ORBS = 6;

    /** Ticks it takes the halo to regrow one spent orb. */
    public static final int ORB_REGEN_TICKS = 100;

    /** @return orbs left in the halo, clamped to the ring's size. */
    public static int orbs(INinjaData ninjaData) {
        if (ninjaData == null) {
            return 0;
        }
        return Mth.clamp(ninjaData.getTenseiganOrbs(), 0, MAX_ORBS);
    }

    /** @return orbs left for a player, clamped like {@link #orbs(INinjaData)}. */
    public static int orbs(Player player) {
        if (player == null) {
            return 0;
        }
        return orbs(player.getCapability(
                com.sekwah.narutoaddon.capabilities.NinjaCapabilityHandler.NINJA_DATA)
                .resolve().orElse(null));
    }

    /** Writes the orb count, keeping anything from putting more out than the ring holds. */
    public static void setOrbs(INinjaData ninjaData, int orbs) {
        if (ninjaData != null) {
            ninjaData.setTenseiganOrbs(Mth.clamp(orbs, 0, MAX_ORBS));
        }
    }

    /**
     * Takes one orb out of the halo.
     *
     * @return whether there was one to take; the caller must not fire when this is false
     */
    public static boolean spendOrb(INinjaData ninjaData) {
        int left = orbs(ninjaData);
        if (left <= 0) {
            return false;
        }
        setOrbs(ninjaData, left - 1);
        return true;
    }

    /** Chakra burnt for every tick the eye stays open. */
    private static final float CHAKRA_PER_TICK = 0.8f;

    /**
     * The stances of the branch, closed together with the eye.
     *
     * <p>Built lazily in a holder for the same reason {@code AbstractDojutsuAbility.EyeIds} is:
     * reading another ability's {@code ID} from this class's static initializer would run that
     * class's initializer in the middle of ours, and an id could come back null.</p>
     */
    private static final class BranchStances {
        private static final java.util.List<ResourceLocation> ALL = java.util.List.of(
                ProjectileReflectAbility.ID,
                OtsutsukiWillAbility.ID);
    }

    @Override
    public long defaultCombo() {
        return COMBO;
    }

    @Override
    protected ResourceLocation eyeId() {
        return ID;
    }

    @Override
    protected boolean enabled() {
        return NarutoConfig.tenseiganEnabled;
    }

    @Override
    protected String disabledKey() {
        return "jutsu.fail.tenseigan.disabled";
    }

    @Override
    protected float chakraPerTick() {
        return CHAKRA_PER_TICK;
    }

    @Override
    public int noiseRadius() {
        return 0;
    }

    @Override
    protected void aura(ServerLevel level, Player player, INinjaData ninjaData) {
        // Pale chakra only, no cold blue flames: the white ring reads as an open eye, the blue
        // one read as a buff burning on the body and was mistaken for leftover particles.
        if (player.tickCount % 3 == 0) {
            level.sendParticles(ParticleTypes.END_ROD, player.getX(),
                    player.getY() + player.getBbHeight() * 0.5d, player.getZ(),
                    2, 0.4d, 0.6d, 0.4d, 0.01d);
        }
    }

    @Override
    public void handleAbilityEnded(Player player, INinjaData ninjaData, int ticksActive) {
        super.handleAbilityEnded(player, ninjaData, ticksActive);
        // The ring is the eye's, not the caster's: closing the eye dissolves it, and the next time
        // the eye opens the halo is whole again.
        setOrbs(ninjaData, MAX_ORBS);
        // Branch stances exist only while the eye does: leaving them running after the eye closes
        // would let a caster keep the effect without its source.
        for (ResourceLocation stanceId : BranchStances.ALL) {
            if (!ToggleState.isActive(ninjaData, stanceId)) {
                continue;
            }
            Ability stance = NarutoAbilities.byId(stanceId);
            if (stance != null) {
                ToggleState.deactivate(player, ninjaData, stance);
            }
        }
        if (!player.isCreative() && !SixPathsModeAbility.isActive(player)
                && !KamuiPhaseAbility.isActive(ninjaData)) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
    }

    /** @return whether the caster has the Tenseigan open right now. */
    public static boolean isActive(INinjaData ninjaData) {
        return isEyeActive(ninjaData, ID);
    }

    /**
     * Gate for every technique of the branch: the eye has to be open before anything it grants can
     * be used. Returns true when the technique may start; otherwise it tells the caster why on the
     * action bar and the caller returns false.
     *
     * <p>Kept here rather than repeated in twelve classes so the rule and its wording live in one
     * place: a branch technique that forgot the check was usable without the eye at all.</p>
     */
    public static boolean requireOpen(Player player, INinjaData ninjaData, Ability ability) {
        if (isActive(ninjaData)) {
            return true;
        }
        if (player != null && ability != null) {
            player.displayClientMessage(Component.translatable("jutsu.fail.tenseigan.required",
                            Component.translatable(ability.getTranslationKey(ninjaData))
                                    .withStyle(ChatFormatting.YELLOW))
                    .withStyle(ChatFormatting.RED), true);
        }
        return false;
    }
}