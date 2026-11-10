package com.sekwah.narutoaddon.abilities.jutsus;

import com.sekwah.narutoaddon.abilities.Ability;
import com.sekwah.narutoaddon.capabilities.INinjaData;
import com.sekwah.narutoaddon.capabilities.size.EntitySize;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

/**
 * Dharma: the size dial.
 *
 * <p>One technique, not a set of forms, and the two ways of pressing it do the two things a size
 * control needs:</p>
 * <ul>
 *   <li><b>Hold</b> the combo - the dial opens and the mouse wheel slides the body anywhere between
 *       half and five times its size. Letting go leaves the size as it is.</li>
 *   <li><b>Tap</b> the combo - the body snaps back to its normal size.</li>
 * </ul>
 *
 * <p>The split is the base key handler's own: a press that never reaches the hold threshold arrives
 * as {@code MIN_ACTIVATE}, a real hold as {@code START}/{@code STOP}. That is why this is a channel
 * rather than a toggle - a toggle cannot tell a tap from a hold, and a size control with no way back
 * to normal except scrolling all the way is a trap.</p>
 */
public class SizeShiftAbility extends Ability implements Ability.Channeled {

    /** V B V V - the size slot of the Dharma branch. */
    public static final long COMBO = 3321L;

    public static final ResourceLocation ID = new ResourceLocation("narutoaddon", "size_shift");

    /** Chakra burnt for every tick the dial stays open. */
    private static final float COST_PER_TICK = 0.15f;

    private static final int CHAKRA_REGEN_DELAY = 20;

    /** Persistent key of the last time the wheel reminder was shown. */
    private static final String HINT_KEY = "narutoaddon:size_hint_at";

    /** Minimum gap between two wheel reminders, in ticks. */
    private static final int HINT_INTERVAL = 100;

    @Override
    public int noiseRadius() {
        return 0;
    }

    @Override
    public ActivationType activationType() {
        return ActivationType.CHANNELED;
    }

    @Override
    public long defaultCombo() {
        return COMBO;
    }

    /** A tap is a reset, not a cast: it still has to reach the server. */
    @Override
    public boolean canActivateBelowMinCharge() {
        return true;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        // A tap is the way back to normal size: it must never be refused for want of chakra, or a
        // caster who ran dry while enlarged would be stuck at that size.
        if (chargeAmount == -1) {
            return true;
        }
        float cost = this.masteryCost(ninjaData, COST_PER_TICK);
        if (ninjaData.getChakra() < cost) {
            if (chargeAmount == 0) {
                player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                        Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW))
                        .withStyle(ChatFormatting.RED), true);
            }
            return false;
        }
        ninjaData.useChakra(cost, CHAKRA_REGEN_DELAY);
        return true;
    }

    /**
     * {@code ticksActive == -1} is the tap arm of the packet (the press never became a hold): that is
     * the way back to normal size. A real release arrives with the number of ticks held and leaves
     * the body as the dial left it.
     */
    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        if (ticksActive != -1) {
            return;
        }
        if (EntitySize.get(player) == EntitySize.DEFAULT) {
            return;
        }
        EntitySize.set(player, EntitySize.DEFAULT);
        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + player.getBbHeight() * 0.5d,
                    player.getZ(), 16, 0.4d, 0.4d, 0.4d, 0.02d);
        }
    }

    /** Per tick of an open dial: mastery, the wheel reminder, and a quiet ring of chakra. */
    @Override
    public void handleChannelling(Player player, INinjaData ninjaData, int ticksChanneled) {
        this.recordMasteryCredit(player, ninjaData, MASTERY_CREDIT_PER_TICK);
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        long now = level.getGameTime();
        long shownAt = player.getPersistentData().getLong(HINT_KEY);
        if (now - shownAt > HINT_INTERVAL) {
            player.getPersistentData().putLong(HINT_KEY, now);
            player.displayClientMessage(Component.translatable("jutsu.size_shift.hint"), true);
        }
        if (player.tickCount % 8 == 0) {
            level.sendParticles(ParticleTypes.END_ROD, player.getX(),
                    player.getY() + player.getBbHeight() * 0.5d, player.getZ(),
                    6, player.getBbWidth() * 0.5d, player.getBbHeight() * 0.3d, player.getBbWidth() * 0.5d, 0.01d);
        }
    }

    /** A dial is mastered by being held open, so the credit is paid per tick. */
    @Override
    public void recordMastery(Player player, INinjaData ninjaData) {
    }

    /** @return whether the size dial is open for this caster right now. */
    public static boolean isDialOpen(INinjaData ninjaData) {
        return ninjaData != null && ID.equals(ninjaData.getCurrentlyChanneledAbility());
    }
}
