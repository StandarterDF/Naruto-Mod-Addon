package com.sekwah.narutoaddon.abilities.jutsus;

import com.sekwah.narutoaddon.abilities.Ability;
import com.sekwah.narutoaddon.abilities.AbilityUnlocks;
import com.sekwah.narutoaddon.capabilities.INinjaData;
import com.sekwah.narutoaddon.config.NarutoConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

/**
 * Carrier-only stub for the far sight's combo group.
 *
 * <p>This class remains registered <b>only</b> as an internal carrier of combo groups
 * ({@code ComboGroup.CARRIER}); it is not castable by itself. The zoom itself is part of the
 * {@link ByakuganAbility} stance, and there is no separate "far sight" technique any more.</p>
 */
public class ByakuganFarsightAbility extends Ability implements Ability.Channeled, Ability.HandleEnded {

    /** An eye that only looks is quiet. */
    @Override
    public int noiseRadius() {
        return 0;
    }

    /** V B V V B B B - the far sight, channeled. */
    public static final long COMBO = -1L;

    public static final ResourceLocation ID = new ResourceLocation("narutomod", "byakugan_farsight");

    /** Chakra burnt for every tick the view is pulled in. */
    private static final float CHAKRA_PER_TICK = 0.1f;

    private static final int CHAKRA_REGEN_DELAY = 20;

    /** How often the faint ring around the head is drawn while the stance is up, in ticks. */
    private static final int AURA_INTERVAL = 8;

    @Override
    public ActivationType activationType() {
        return ActivationType.CHANNELED;
    }

    @Override
    public long defaultCombo() {
        return COMBO;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (!NarutoConfig.byakuganEnabled) {
            player.displayClientMessage(Component.translatable("jutsu.fail.byakugan.disabled")
                    .withStyle(ChatFormatting.RED), true);
            return false;
        }
        // Reading far away is still reading with the Byakugan, so the eye has to be known first. The
        // stance itself does not need the eye open; it is the eye's use, not another eye.
        if (!AbilityUnlocks.isUnlocked(ninjaData, ByakuganAbility.ID)) {
            player.displayClientMessage(Component.translatable("jutsu.fail.byakugan.notlearned",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW))
                    .withStyle(ChatFormatting.RED), true);
            return false;
        }
        float cost = this.masteryCost(ninjaData, CHAKRA_PER_TICK);
        if (ninjaData.getChakra() < cost) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW))
                    .withStyle(ChatFormatting.RED), true);
            return false;
        }
        ninjaData.useChakra(cost, CHAKRA_REGEN_DELAY);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        // The far sight leaves no mark on the world; the channel's upkeep is paid in handleCost.
        if (player.level() instanceof ServerLevel level && player.tickCount % AURA_INTERVAL == 0) {
            level.sendParticles(ParticleTypes.END_ROD, player.getX(),
                    player.getY() + player.getBbHeight() * 0.92d, player.getZ(),
                    1, 0.12d, 0.04d, 0.12d, 0.002d);
        }
    }

    @Override
    public void handleChannelling(Player player, INinjaData ninjaData, int ticksChanneled) {
        // Mastery is earned by holding the view, not by flicking it on.
        this.recordMasteryCredit(player, ninjaData, MASTERY_CREDIT_PER_TICK);
    }

    /**
     * A stance is mastered by holding it, so the credit is paid per active tick from
     * {@link #performServer} and this records nothing.
     */
    @Override
    public void recordMastery(Player player, INinjaData ninjaData) {
    }

    /**
     * A channelled technique is mastered by holding it, which {@link #handleChannelling} already
     * paid for, so a tap is not a cast.
     */
    @Override
    public void handleAbilityEnded(Player player, INinjaData ninjaData, int ticksActive) {
        // The stance gave the body nothing, so there is nothing to hand back when it ends.
    }
}
