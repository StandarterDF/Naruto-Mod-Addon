package com.sekwah.narutoaddon.abilities.jutsus;

import com.sekwah.narutoaddon.abilities.Ability;
import com.sekwah.narutoaddon.abilities.NarutoAbilities;
import com.sekwah.narutoaddon.abilities.mastery.Mastery;
import com.sekwah.narutoaddon.capabilities.INinjaData;
import com.sekwah.narutoaddon.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutoaddon.config.NarutoConfig;
import com.sekwah.narutoaddon.friends.Friends;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * The Rinnegan handing chakra across a gap.
 *
 * <p>The eye of the sage is not only a hand that takes. This is the same path turned outwards: a
 * thread of chakra runs from the caster's pool to the people they call allies. A quick tap stamps
 * the seal - the list of every ally in reach, held on the caster and fed by
 * {@code ChakraSealEvents} once a second until the last thread snaps. Holding the keys instead aims
 * the beam at one body and pays for it tick by tick, so what leaves the caster is what the ally
 * receives. Pools are never averaged either way: helping a nearly empty ally never makes a full
 * caster poorer.</p>
 */
public class ChakraTransferAbility extends Ability implements Ability.Channeled, Ability.Cooldown {

    /** A shared thread of chakra is quiet work, not a shout. */
    @Override
    public int noiseRadius() {
        return 0;
    }

    /** V V C B B - the Rinnegan stance (V V C V) with the last key changed. */
    public static final long COMBO = 3321L;

    @Override
    public int getCooldown() {
        return 40;
    }

    public static final ResourceLocation ID = new ResourceLocation("narutomod", "chakra_transfer");

    /** The list of allies the seal currently feeds, stored on the caster. */
    public static final String ALLIES_TAG = "narutomod:chakra_seal_allies";

    /** How far the beam can reach before mastery extends it. */
    public static final double RANGE = 12.0d;

    /** Chakra fed to each sealed ally per second. Pools are not averaged. */
    public static final float ALLY_REGEN_PER_SECOND = 0.5f;

    /** Chakra the caster regains per second while the seal holds. Pools are not averaged. */
    public static final float CASTER_REGEN_PER_SECOND = 0.25f;

    /** Chakra drawn per tick; the ally receives the same amount, capped by their own pool. */
    private static final float PER_TICK = 8.0f;

    /** How long one cast may run before the caster has to start it again. */
    private static final int MAX_TICKS = 240;

    /** Ticks before the caster's own regeneration resumes. */
    private static final int REGEN_DELAY = 20;

    @Override
    public ActivationType activationType() {
        return ActivationType.CHANNELED;
    }

    @Override
    public long defaultCombo() {
        return COMBO;
    }

    /** Reach of the seal, widened by mastery. */
    public static double range(INinjaData ninjaData) {
        return RANGE + 24.0d * Mastery.level(ninjaData, NarutoAbilities.CHAKRA_TRANSFER.get());
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (!NarutoConfig.rinneganEnabled) {
            player.displayClientMessage(Component.translatable("jutsu.fail.rinnegan.disabled")
                    .withStyle(ChatFormatting.RED), true);
            return false;
        }
        if (!RinneganAbility.isActive(ninjaData)) {
            player.displayClientMessage(Component.translatable("jutsu.fail.rinnegan.required",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW))
                    .withStyle(ChatFormatting.RED), true);
            return false;
        }
        if (chargeAmount >= MAX_TICKS) {
            return false;
        }
        if (chargeAmount < 0) {
            // Seal tap: a cooldown-gated instant cast that latches every ally in reach.
            if (this.checkCooldown(player, ninjaData, this.getTranslationKey(ninjaData))) {
                return false;
            }
            if (allies(player, ninjaData).isEmpty()) {
                player.displayClientMessage(Component.translatable("jutsu.fail.chakra_transfer.noally",
                        Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW))
                        .withStyle(ChatFormatting.RED), true);
                return false;
            }
        } else if (recipient(player, ninjaData) == null) {
            if (chargeAmount == 0) {
                player.displayClientMessage(Component.translatable("jutsu.fail.chakra_transfer.noally",
                        Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW))
                        .withStyle(ChatFormatting.RED), true);
            }
            return false;
        }
        float cost = this.masteryCost(ninjaData, PER_TICK);
        if (ninjaData.getChakra() < cost) {
            if (chargeAmount == 0) {
                player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                        Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            }
            return false;
        }
        ninjaData.useChakra(cost, REGEN_DELAY);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        // A tap (min-activate) stamps the chakra seal: latch every ally in reach onto the caster so
        // ChakraSealEvents can feed them from one place. A full channel only records mastery.
        if (ticksActive < 0 && player.level() instanceof ServerLevel level) {
            List<Player> sealed = allies(player, ninjaData);
            if (!sealed.isEmpty()) {
                ListTag stored = new ListTag();
                for (Player ally : sealed) {
                    stored.add(StringTag.valueOf(ally.getUUID().toString()));
                    drawThread(level, player, ally);
                }
                player.getPersistentData().put(ALLIES_TAG, stored);
                this.registerCooldown(player, ninjaData, this.getTranslationKey(ninjaData));
            }
        }
        // The work happens per tick in handleChannelling; a completed channel is worth one cast.
        this.recordMastery(player, ninjaData);
    }

    @Override
    public void handleChannelling(Player player, INinjaData ninjaData, int ticksChanneled) {
        Player ally = recipient(player, ninjaData);
        if (ally == null || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        ally.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(allyData -> {
            float room = allyData.getMaxChakra() - allyData.getChakra();
            float moved = Math.min(PER_TICK, room);
            if (moved > 0.0f) {
                allyData.setChakra(allyData.getChakra() + moved);
            }
        });
        this.recordMasteryCredit(player, ninjaData, MASTERY_CREDIT_PER_TICK);

        if (ticksChanneled % 3 == 0) {
            for (Step step : Step.between(player, ally)) {
                level.sendParticles(ParticleTypes.END_ROD, step.x, step.y, step.z,
                        1, 0.02d, 0.02d, 0.02d, 0.0d);
            }
        }
        if (ticksChanneled % 20 == 0) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 0.35f, 1.6f);
        }
    }

    /**
     * The ally the beam is meant for: whoever is under the crosshair, or the nearest friend/team mate
     * if the caster is not aiming at anyone in particular. Mastery widens the search the same way it
     * widens the seal, so a practiced caster does not reach further with the group than with a
     * single body.
     */
    private Player recipient(Player caster, INinjaData ninjaData) {
        if (caster.level().isClientSide) {
            return null;
        }
        double reach = range(ninjaData);
        if (com.sekwah.narutoaddon.util.Targeting.lookedAtEntity(caster, reach) instanceof Player aimed
                && aimed != caster && isAlly(caster, aimed)) {
            return aimed;
        }
        Player best = null;
        double bestDistance = reach * reach;
        for (Player other : caster.level().players()) {
            if (other == caster || !isAlly(caster, other) || other.isSpectator()) {
                continue;
            }
            double distance = other.distanceToSqr(caster);
            if (distance <= bestDistance) {
                bestDistance = distance;
                best = other;
            }
        }
        return best;
    }

    /**
     * Whether the caster counts the other player as an ally: a team mate or a friend. Exposed because
     * the seal is kept alive elsewhere ({@code ChakraSealEvents}) and a thread has to snap the moment
     * the friendship ends or the team is left, not only when the cast happens.
     */
    public static boolean isAlly(Player caster, Player other) {
        return other.isAlliedTo(caster) || Friends.areFriends(caster, other);
    }

    /**
     * Every player in reach that the caster counts as an ally: teammates and friends, never the
     * caster themselves and never a spectator.
     */
    private static List<Player> allies(Player caster, INinjaData ninjaData) {
        List<Player> found = new ArrayList<>();
        if (caster.level().isClientSide) {
            return found;
        }
        double reach = range(ninjaData);
        double reachSqr = reach * reach;
        for (Player other : caster.level().players()) {
            if (other == caster || !other.isAlive() || other.isSpectator()) {
                continue;
            }
            if (!isAlly(caster, other)) {
                continue;
            }
            if (other.distanceToSqr(caster) > reachSqr) {
                continue;
            }
            found.add(other);
        }
        return found;
    }

    /** One thread of motes from the caster to an ally, so every connection is visible. */
    private static void drawThread(ServerLevel level, Player caster, Player ally) {
        for (Step step : Step.between(caster, ally)) {
            level.sendParticles(ParticleTypes.END_ROD, step.x, step.y, step.z,
                    1, 0.01d, 0.01d, 0.01d, 0.0d);
        }
    }

    /** One point along the beam, used to place the particles. */
    private record Step(double x, double y, double z) {

        private static final int POINTS = 10;

        static List<Step> between(Player from, Player to) {
            double startX = from.getX();
            double startY = from.getY() + from.getBbHeight() * 0.6d;
            double startZ = from.getZ();
            double endX = to.getX();
            double endY = to.getY() + to.getBbHeight() * 0.6d;
            double endZ = to.getZ();
            return java.util.stream.IntStream.rangeClosed(1, POINTS)
                    .mapToObj(i -> {
                        double t = (double) i / POINTS;
                        return new Step(startX + (endX - startX) * t,
                                startY + (endY - startY) * t,
                                startZ + (endZ - startZ) * t);
                    })
                    .toList();
        }
    }
}
