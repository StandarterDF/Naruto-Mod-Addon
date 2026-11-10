package com.sekwah.narutoaddon.client.renderer;

import com.sekwah.narutoaddon.abilities.ToggleState;
import com.sekwah.narutoaddon.abilities.jutsus.ChakraCloakAbility;
import com.sekwah.narutoaddon.abilities.jutsus.CurseSealOneAbility;
import com.sekwah.narutoaddon.abilities.jutsus.CurseSealTwoAbility;
import com.sekwah.narutoaddon.abilities.jutsus.DividedCloakAbility;
import com.sekwah.narutoaddon.abilities.jutsus.LightningModeAbility;
import com.sekwah.narutoaddon.abilities.jutsus.PerfectCloakAbility;
import com.sekwah.narutoaddon.abilities.jutsus.SixPathsModeAbility;
import com.sekwah.narutoaddon.abilities.jutsus.TailedCloakAbility;
import com.sekwah.narutoaddon.abilities.jutsus.TenseiganAbility;
import com.sekwah.narutoaddon.capabilities.INinjaData;
import net.minecraft.util.Mth;

/**
 * The look of one body aura: a tint, how opaque it is, how much the shell swells and how it breathes.
 *
 * <p>Every stance that wraps the caster in chakra is the same picture with different numbers, so the
 * numbers live here and the drawing lives in {@link ModeAuraLayer}. Adding an aura to a new stance is
 * one line in {@link #resolve}; the layer does not know any stance by name and does not change.</p>
 *
 * @param red     red part of the shell tint
 * @param green   green part
 * @param blue    blue part
 * @param alpha   base opacity of the shell
 * @param scale   how much the shell is swollen over the body
 * @param emissive whether the shell lights itself instead of taking the world's light
 * @param pulse   how much the opacity breathes, as a fraction of {@code alpha}
 * @param speed   how fast that breath is, in radians per tick
 */
public record ModeAura(float red, float green, float blue, float alpha, float scale,
                       boolean emissive, float pulse, float speed) {

    /**
     * The strongest aura the caster is wearing right now, or {@code null} for none.
     *
     * <p>Ordered from the loudest shell to the quietest: two modes can be up at once (the eight gates
     * over a cloak, sage mode under the six paths), and the body should show the one that is doing
     * the most, not a blend of both. The gate count is synced for exactly this reason - the aura of
     * the eighth gate is not the aura of the first.</p>
     */
    public static ModeAura resolve(INinjaData ninjaData) {
        if (ninjaData == null) {
            return null;
        }
        int gates = ninjaData.getGateCount();
        if (gates > 0) {
            return gates(gates);
        }
        if (ToggleState.isActive(ninjaData, PerfectCloakAbility.ID)) {
            return new ModeAura(1.00f, 0.78f, 0.15f, 0.34f, 1.040f, true, 0.06f, 0.10f);
        }
        if (ToggleState.isActive(ninjaData, TailedCloakAbility.ID)) {
            return new ModeAura(0.72f, 0.06f, 0.04f, 0.32f, 1.038f, false, 0.05f, 0.12f);
        }
        if (ToggleState.isActive(ninjaData, SixPathsModeAbility.ID)) {
            return new ModeAura(1.00f, 0.96f, 0.72f, 0.34f, 1.045f, true, 0.10f, 0.09f);
        }
        if (ToggleState.isActive(ninjaData, DividedCloakAbility.ID)) {
            return new ModeAura(1.00f, 0.72f, 0.30f, 0.22f, 1.035f, false, 0.04f, 0.12f);
        }
        if (ToggleState.isActive(ninjaData, LightningModeAbility.ID)) {
            return new ModeAura(0.42f, 0.72f, 1.00f, 0.30f, 1.040f, true, 0.16f, 0.35f);
        }
        if (ToggleState.isActive(ninjaData, CurseSealTwoAbility.ID)) {
            return new ModeAura(0.52f, 0.16f, 0.62f, 0.32f, 1.042f, true, 0.07f, 0.10f);
        }
        if (ToggleState.isActive(ninjaData, CurseSealOneAbility.ID)) {
            return new ModeAura(0.58f, 0.28f, 0.48f, 0.22f, 1.035f, false, 0.05f, 0.10f);
        }
        if (ToggleState.isActive(ninjaData, ChakraCloakAbility.ID)) {
            return new ModeAura(1.00f, 0.32f, 0.08f, 0.30f, 1.035f, false, 0.05f, 0.12f);
        }
        if (ninjaData.isSageMode()) {
            return new ModeAura(0.95f, 0.82f, 0.35f, 0.25f, 1.036f, true, 0.05f, 0.08f);
        }
        if (ToggleState.isActive(ninjaData, TenseiganAbility.ID)) {
            return new ModeAura(0.36f, 1.00f, 0.92f, 0.22f, 1.040f, true, 0.08f, 0.10f);
        }
        return null;
    }

    /**
     * The eight gates: the shell starts as a thin amber shimmer and ends as a blood-red shroud that
     * pulses hard enough to read from across a field. The eighth gate cannot be sealed, so its look
     * is meant to be unmistakable.
     */
    private static ModeAura gates(int gates) {
        int open = Mth.clamp(gates, 1, 8);
        return switch (open) {
            case 1, 2 -> new ModeAura(1.00f, 0.62f, 0.18f, 0.26f, 1.035f, true, 0.05f, 0.10f);
            case 3, 4 -> new ModeAura(1.00f, 0.42f, 0.12f, 0.30f, 1.038f, true, 0.07f, 0.12f);
            case 5, 6 -> new ModeAura(1.00f, 0.20f, 0.10f, 0.34f, 1.042f, true, 0.09f, 0.14f);
            case 7 -> new ModeAura(0.95f, 0.08f, 0.22f, 0.38f, 1.046f, true, 0.11f, 0.16f);
            default -> new ModeAura(0.90f, 0.03f, 0.10f, 0.42f, 1.050f, true, 0.13f, 0.18f);
        };
    }

    /** @return the opacity for this frame, after the aura's own breath. */
    public float alphaAt(float ageInTicks) {
        return Mth.clamp(this.alpha * (1.0f + this.pulse * Mth.sin(ageInTicks * this.speed)), 0.0f, 1.0f);
    }
}
