package com.sekwah.narutoaddon.mixin.common;

import com.sekwah.narutoaddon.bridge.AbilityProxy;
import com.sekwah.narutoaddon.capabilities.AddonNinjaData;
import com.sekwah.narutoaddon.capabilities.NinjaCapabilityHandler;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Raises the base pool's per-player maximum with the capacity the addon layer adds.
 *
 * <p>The base mod owns chakra, stamina and the substitution budget, and it keeps their ceilings in
 * private fields that it re-reads from its <em>global</em> config every server tick:</p>
 *
 * <pre>
 *     // NinjaData#updateDataServer(Player), called once per tick
 *     this.getConfigData();          // maxChakra/maxStamina/maxSubstitutions = NarutoConfig.*
 *     ...
 *     this.substitutions = Math.min(Math.max(this.substitutions, 0), this.maxSubstitutions);
 *     this.stamina = Math.min(Math.max(this.stamina, 0), this.maxStamina);
 *     this.chakra = Math.min(Math.max(this.chakra, 0), this.maxChakra);
 *
 *     private void getConfigData() {
 *         this.maxChakra = NarutoConfig.maxChakra;
 *         this.maxStamina = NarutoConfig.maxStamina;
 *         this.maxSubstitutions = NarutoConfig.maxSubstitutions;
 *     }
 * </pre>
 *
 * <p>So the addon's capacity rules (the tree's capacity and all-rounded nodes, the charge
 * technique's mastery, the armour and eye bonuses, scroll bonuses, the tree's substitution node)
 * could never be felt: both the getters and every clamp read those fields, and every tick put them
 * back to the configured value. The interface gives no way to raise them and there is no access
 * transformer for them, so the raise happens here, on the base's own tick.</p>
 *
 * <p>The injection point is the {@code getConfigData()} call inside {@code updateDataServer}, with
 * {@code shift = AFTER}. That is the one place where the ordering is guaranteed to be right:</p>
 * <ul>
 *     <li>the base has just overwritten the three fields with the configured values, so the bonus is
 *     added onto a freshly reset number - it can never accumulate across ticks, which is exactly
 *     what a {@code HEAD} injection or a plain tail injection would have allowed;</li>
 *     <li>the clamps further down the same method (and {@code addChakra}/{@code addStamina}, and the
 *     {@code getMaxChakra()}/{@code getMaxStamina()} getters the HUD reads) all run after it, so
 *     every one of them sees the raised ceiling;</li>
 *     <li>{@code Player} is a parameter of the method, so the addon layer can be resolved without
 *     guessing which player is being ticked.</li>
 * </ul>
 *
 * <p>{@code updateDataServer} returns early when ninja mode is off; nothing regenerates or clamps in
 * that case, so leaving the configured value alone there changes nothing.</p>
 */
@Mixin(com.sekwah.narutomod.capabilities.NinjaData.class)
public abstract class BaseNinjaDataCapacityMixin {

    // remap = false throughout: this mixin targets the base mod, whose classes are not in this
    // workspace's mapping set (same rule as KeyHandlerMixin).
    @Shadow(remap = false)
    private float maxChakra;

    @Shadow(remap = false)
    private float maxStamina;

    @Shadow(remap = false)
    private float maxSubstitutions;

    @Inject(
            method = "updateDataServer",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/sekwah/narutomod/capabilities/NinjaData;getConfigData()V",
                    shift = At.Shift.AFTER
            ),
            remap = false
    )
    private void narutoaddon$applyCapacityBonuses(Player player, CallbackInfo ci) {
        AddonNinjaData addon = this.narutoaddon$addonLayer(player);
        if (addon == null) {
            return;
        }
        // Added, not assigned: the call above has just put the configured value back into each
        // field, so this is the configured maximum plus the addon's bonus, once per tick.
        this.maxChakra += addon.capacityBonus();
        this.maxStamina += addon.staminaCapacityBonus();
        this.maxSubstitutions += addon.substitutionBonus();
    }

    /**
     * The adapter for this very capability. The registry lookup is the cheap path; the capability on
     * the player is the fallback for the ticks before the registry entry exists (the adapter is only
     * registered once capabilities are attached).
     */
    private AddonNinjaData narutoaddon$addonLayer(Player player) {
        AddonNinjaData addon = AbilityProxy.adapterFor(
                (com.sekwah.narutomod.capabilities.INinjaData) (Object) this);
        if (addon != null) {
            return addon;
        }
        com.sekwah.narutoaddon.capabilities.INinjaData found =
                player.getCapability(NinjaCapabilityHandler.NINJA_DATA).orElse(null);
        return found instanceof AddonNinjaData addonData ? addonData : null;
    }
}
