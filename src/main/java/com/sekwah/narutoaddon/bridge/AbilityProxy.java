package com.sekwah.narutoaddon.bridge;

import com.sekwah.narutoaddon.abilities.Ability;
import com.sekwah.narutoaddon.capabilities.INinjaData;
import com.sekwah.narutoaddon.capabilities.AddonNinjaData;


import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Proxy that exposes one of our abilities to the base mod's ability pipeline.
 * The base mod only knows about {@link BaseAbility}; this class wraps our
 * {@link Ability} and delegates every call, converting the ninja data along
 * the way.
 */
public class AbilityProxy extends com.sekwah.narutomod.abilities.Ability
        implements com.sekwah.narutomod.abilities.Ability.Toggled, com.sekwah.narutomod.abilities.Ability.Channeled, com.sekwah.narutomod.abilities.Ability.Cooldown, com.sekwah.narutomod.abilities.Ability.HandleEnded {

    private static final Map<Ability, AbilityProxy> PROXY_BY_DELEGATE = new HashMap<>();

    private final Ability delegate;

    public AbilityProxy(Ability delegate) {
        this.delegate = delegate;
        PROXY_BY_DELEGATE.put(delegate, this);
    }

    public Ability getDelegate() {
        return delegate;
    }

    public static AbilityProxy getProxy(Ability ability) {
        return ability == null ? null : PROXY_BY_DELEGATE.get(ability);
    }

    @Override
    public long defaultCombo() {
        return delegate.defaultCombo();
    }

    @Override
    public com.sekwah.narutomod.abilities.Ability.ActivationType activationType() {
        Ability.ActivationType t = delegate.activationType();
        return switch (t) {
            case INSTANT -> com.sekwah.narutomod.abilities.Ability.ActivationType.INSTANT;
            case TOGGLE -> com.sekwah.narutomod.abilities.Ability.ActivationType.TOGGLE;
            case CHANNELED -> com.sekwah.narutomod.abilities.Ability.ActivationType.CHANNELED;
        };
    }

    /**
     * Registry of adapter wrappers around the base mod's ninja data.
     *
     * <p>Stored as a {@link WeakHashMap}: Forge recreates the base capability
     * instance on respawn / clone (and the player object itself can be replaced),
     * so a strong reference would pin the dead capability (and its adapter)
     * in memory forever. With weak keys the entry becomes eligible for
     * collection as soon as no live code holds the base data anymore.
     */
    private static final Map<com.sekwah.narutomod.capabilities.INinjaData, AddonNinjaData> ADAPTERS = new WeakHashMap<>();

    public static void registerAdapter(com.sekwah.narutomod.capabilities.INinjaData base, AddonNinjaData adapter) {
        if (base != null && adapter != null) {
            ADAPTERS.put(base, adapter);
        }
    }

    public static void unregisterAdapter(com.sekwah.narutomod.capabilities.INinjaData base) {
        if (base != null) {
            ADAPTERS.remove(base);
        }
    }

    /**
     * Returns the adapter already registered for a base capability, or {@code null} when that
     * capability has none (not attached yet, or already unregistered).
     *
     * <p>Cheaper than resolving the addon capability off the player, so the base capability's own
     * tick can ask for its addon layer through this. Callers that do have the player at hand should
     * still fall back to {@code player.getCapability(NinjaCapabilityHandler.NINJA_DATA)}.</p>
     */
    public static AddonNinjaData adapterFor(com.sekwah.narutomod.capabilities.INinjaData base) {
        if (base == null) {
            return null;
        }
        return ADAPTERS.get(base);
    }

    private AddonNinjaData adapt(com.sekwah.narutomod.capabilities.INinjaData baseData) {
        if (baseData == null) {
            return null;
        }
        AddonNinjaData cached = ADAPTERS.get(baseData);
        if (cached == null) {
            cached = new AddonNinjaData(baseData, false);
            ADAPTERS.put(baseData, cached);
        }
        return cached;
    }

    @Override
    public boolean handleCost(Player player, com.sekwah.narutomod.capabilities.INinjaData ninjaData, int chargeAmount) {
        AddonNinjaData data = adapt(ninjaData);
        if (!com.sekwah.narutoaddon.abilities.CastGate.allows(player, data, delegate)) {
            return false;
        }
        return delegate.handleCost(player, data, chargeAmount);
    }

    @Override
    public boolean handleCost(Player player, com.sekwah.narutomod.capabilities.INinjaData ninjaData) {
        AddonNinjaData data = adapt(ninjaData);
        if (!com.sekwah.narutoaddon.abilities.CastGate.allows(player, data, delegate)) {
            return false;
        }
        return delegate.handleCost(player, data);
    }

    @Override
    public boolean logInChat() {
        return delegate.logInChat();
    }

    @Override
    public SoundEvent castingSound() {
        return delegate.castingSound();
    }

    @Override
    public SoundEvent castingFailSound() {
        return delegate.castingFailSound();
    }

    @Override
    public String getTranslationKey(com.sekwah.narutomod.capabilities.INinjaData ninjaData) {
        return delegate.getTranslationKey(adapt(ninjaData));
    }

    @Override
    public String getTranslationKey(com.sekwah.narutomod.capabilities.INinjaData ninjaData, int ticksActive) {
        return delegate.getTranslationKey(adapt(ninjaData), ticksActive);
    }

    @Override
    public void performServer(Player player, com.sekwah.narutomod.capabilities.INinjaData ninjaData, int ticksActive) {
        delegate.performServer(player, adapt(ninjaData), ticksActive);
    }

    @Override
    public void performServer(Player player, com.sekwah.narutomod.capabilities.INinjaData ninjaData) {
        delegate.performServer(player, adapt(ninjaData));
    }

    public boolean canStartToggle(Player player, com.sekwah.narutomod.capabilities.INinjaData ninjaData) {
        return delegate.canStartToggle(player, adapt(ninjaData));
    }

    // Toggled
    @Override
    public void performToggleClient(Player player, com.sekwah.narutomod.capabilities.INinjaData ninjaData) {
        if (delegate instanceof Ability.Toggled t) {
            t.performToggleClient(player, adapt(ninjaData));
        }
    }

    // Channeled
    @Override
    public boolean canActivateBelowMinCharge() {
        if (delegate instanceof Ability.Channeled c) {
            return c.canActivateBelowMinCharge();
        }
        return com.sekwah.narutomod.abilities.Ability.Channeled.super.canActivateBelowMinCharge();
    }

    @Override
    public boolean useChargedMessages() {
        if (delegate instanceof Ability.Channeled c) {
            return c.useChargedMessages();
        }
        return com.sekwah.narutomod.abilities.Ability.Channeled.super.useChargedMessages();
    }

    @Override
    public boolean hideChannelMessages() {
        if (delegate instanceof Ability.Channeled c) {
            return c.hideChannelMessages();
        }
        return com.sekwah.narutomod.abilities.Ability.Channeled.super.hideChannelMessages();
    }

    @Override
    public void handleChannelling(Player player, com.sekwah.narutomod.capabilities.INinjaData ninjaData, int ticksChanneled) {
        if (delegate instanceof Ability.Channeled c) {
            c.handleChannelling(player, adapt(ninjaData), ticksChanneled);
        }
    }

    // Cooldown
    @Override
    public int getCooldown() {
        if (delegate instanceof Ability.Cooldown c) {
            return c.getCooldown();
        }
        return 0;
    }

    @Override
    public boolean checkCooldown(Player player, com.sekwah.narutomod.capabilities.INinjaData ninjaData, String translationKey) {
        if (delegate instanceof Ability.Cooldown c) {
            return c.checkCooldown(player, adapt(ninjaData), translationKey);
        }
        return com.sekwah.narutomod.abilities.Ability.Cooldown.super.checkCooldown(player, ninjaData, translationKey);
    }

    @Override
    public void registerCooldown(com.sekwah.narutomod.capabilities.INinjaData ninjaData, String translationKey) {
        if (delegate instanceof Ability.Cooldown c) {
            c.registerCooldown(null, adapt(ninjaData), translationKey);
        } else {
            com.sekwah.narutomod.abilities.Ability.Cooldown.super.registerCooldown(ninjaData, translationKey);
        }
    }

    // HandleEnded
    @Override
    public void handleAbilityEnded(Player player, com.sekwah.narutomod.capabilities.INinjaData ninjaData, int ticksActive) {
        AddonNinjaData data = adapt(ninjaData);
        if (delegate instanceof Ability.HandleEnded h) {
            h.handleAbilityEnded(player, data, ticksActive);
        }
        // The base packet only removes the entry in its own set. A stance switched on from the jutsu
        // menu lives in the addon layer's set instead, so the same press would only add it to both
        // sets and the next one would stop paying but never stop showing. Clear this side too.
        com.sekwah.narutoaddon.abilities.ToggleState.clear(
                data, com.sekwah.narutoaddon.abilities.NarutoAbilities.idOf(delegate));
    }
}
