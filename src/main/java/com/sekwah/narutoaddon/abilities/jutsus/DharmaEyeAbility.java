package com.sekwah.narutoaddon.abilities.jutsus;

import com.sekwah.narutoaddon.abilities.Ability;
import com.sekwah.narutoaddon.abilities.EyeRequirement;
import com.sekwah.narutoaddon.abilities.NarutoAbilities;
import com.sekwah.narutoaddon.capabilities.INinjaData;
import com.sekwah.narutoaddon.capabilities.size.EntitySize;
import com.sekwah.narutoaddon.config.NarutoConfig;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;


/**
 * The Dharma Eye: the base stance of the "mastery over matter" branch.
 *
 * <p>On its own this eye grants nothing beyond the stance itself - no bonus, no vision, no
 * capacity. It is the root node the rest of the Dharma branch builds on, and opening it only
 * keeps the caster's attention fixed on the world of matter around them.</p>
 */
public class DharmaEyeAbility extends AbstractDojutsuAbility {

    /**
     * V B B B - the eye's own combo, kept apart from the four technique groups (3211, 3212, 3221,
     * 3222): the eye is the stance the other twelve keys stand on, so it must be reachable without
     * spending a slot of their own.
     */
    public static final long COMBO = 3223L;

    public static final ResourceLocation ID = new ResourceLocation("narutoaddon", "dharma_eye");

    /** Chakra burnt for every tick the eye stays open. */
    private static final float CHAKRA_PER_TICK = 0.4f;

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
        return NarutoConfig.dharmaEyeEnabled;
    }

    @Override
    protected String disabledKey() {
        return "jutsu.fail.dharma_eye.disabled";
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
        if (player.tickCount % 6 == 0) {
            level.sendParticles(ParticleTypes.ENCHANT, player.getX(),
                    player.getY() + player.getBbHeight() * 0.92d, player.getZ(),
                    3, 0.15d, 0.05d, 0.15d, 0.01d);
        }
    }

    /**
     * Closing the eye takes back everything it was holding open. The upkeep of the branch is paid by
     * the eye, so a hold that keeps running after the eye went dark - because it ran out of chakra,
     * was switched off or lost to another eye - would be a technique paid by nothing.
     */
    @Override
    public void handleAbilityEnded(Player player, INinjaData ninjaData, int ticksActive) {
        super.handleAbilityEnded(player, ninjaData, ticksActive);
        // Channels can be ended right here: they are not iterated by the toggle loop. Running
        // stances are handled from the player tick instead - removing one from inside the loop that
        // is walking the toggle set would break that loop (see SizeEvents#enforceEye).
        endOpenChannels(player, ninjaData);
        if (player.level().isClientSide()) {
            return;
        }
        // A closed eye takes the branch with it: the body returns to its normal size and anyone the
        // eye was holding inside the pocket dimension is put back where they came from.
        if (EntitySize.get(player) != EntitySize.DEFAULT) {
            EntitySize.set(player, EntitySize.DEFAULT);
        }
        DimensionStepAbility.leaveDharmaCube(player);
    }

    private static void endOpenChannels(Player player, INinjaData ninjaData) {
        ResourceLocation current = ninjaData.getCurrentlyChanneledAbility();
        if (current == null || !EyeRequirement.isDharmaTechnique(current)) {
            return;
        }
        int ticks = ninjaData.getCurrentlyChanneledTicks();
        // Clearing the channel first: the ending itself may look at what is currently held.
        ninjaData.setCurrentlyChanneledAbility(player, null);
        Ability ability = NarutoAbilities.byId(current);
        if (ability instanceof Ability.HandleEnded ended) {
            ended.handleAbilityEnded(player, ninjaData, ticks);
        }
    }

    /** @return whether the caster has the Dharma Eye open right now. */
    public static boolean isActive(INinjaData ninjaData) {
        return isEyeActive(ninjaData, ID);
    }
}
