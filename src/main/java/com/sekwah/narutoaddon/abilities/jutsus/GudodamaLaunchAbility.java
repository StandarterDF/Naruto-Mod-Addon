package com.sekwah.narutoaddon.abilities.jutsus;

import com.sekwah.narutoaddon.abilities.Ability;
import com.sekwah.narutoaddon.abilities.mastery.Mastery;
import com.sekwah.narutoaddon.capabilities.INinjaData;
import com.sekwah.narutoaddon.config.NarutoConfig;
import com.sekwah.narutoaddon.entity.jutsuprojectile.ChakraOrbEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

/**
 * A Truth-Seeking Orb thrown as a bolt.
 *
 * <p>The orbs are the mode's ammunition, so this is the plainest way to spend one: the black sphere
 * goes out along the caster's look, stops at the first wall or body in the way, and comes apart there.
 * Unlike a fireball it does not care what the ground is made of - it leaves no crater and no fire, it
 * simply erases what it touches. Everything within four blocks of the impact takes the same blow,
 * allies excluded.</p>
 *
 * <p>A thrown orb is gone. The count on the caster goes down by one, and when it reaches zero the
 * mode itself closes; see {@link SixPathsModeAbility}.</p>
 */
public class GudodamaLaunchAbility extends Ability implements Ability.Cooldown {

    /** The black sphere is quiet until it lands, and then the whole field knows. */
    @Override
    public int noiseRadius() {
        return 20;
    }

    /** C C C C B C - the orb seal. */
    public static final long COMBO = 3113L;

    public static final ResourceLocation ID = new ResourceLocation("narutomod", "gudodama_launch");

    /** What one orb costs to throw, and how long before another can follow. */
    private static final float COST = 40.0f;
    private static final int COOLDOWN = 20;

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public long defaultCombo() {
        return COMBO;
    }

    @Override
    public int getCooldown() {
        return COOLDOWN;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (!NarutoConfig.rinneganEnabled) {
            player.displayClientMessage(Component.translatable("jutsu.fail.rinnegan.disabled")
                    .withStyle(ChatFormatting.RED), true);
            return false;
        }
        if (!SixPathsModeAbility.isActive(player)) {
            player.displayClientMessage(Component.translatable("jutsu.fail.six_paths_mode.required",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW))
                    .withStyle(ChatFormatting.RED), true);
            return false;
        }
        if (SixPathsModeAbility.orbs(player) <= 0) {
            player.displayClientMessage(Component.translatable("jutsu.fail.gudodama.no_orbs",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW))
                    .withStyle(ChatFormatting.RED), true);
            return false;
        }
        float cost = this.masteryCost(ninjaData, COST);
        if (ninjaData.getChakra() < cost) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        ninjaData.useChakra(cost, 20);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        // The cost was paid against a check a moment ago; the orb is taken only if there is one, so a
        // double packet or a stale count cannot turn into a free throw.
        if (SixPathsModeAbility.orbs(player) <= 0) {
            return;
        }

        float mastery = Mastery.level(ninjaData, this);
        float damage = (float) (50.0d + 200.0d * mastery * mastery);
        ChakraOrbEntity orb = ChakraOrbEntity.launch(player, 1.8d, 0.45f, (byte) 0,
                damage, 4.0f, 1, true, mastery);
        level.addFreshEntity(orb);

        SixPathsModeAbility.spendOrb(player);
    }
}
