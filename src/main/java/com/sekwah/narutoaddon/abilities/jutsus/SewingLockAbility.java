package com.sekwah.narutoaddon.abilities.jutsus;

import com.sekwah.narutoaddon.abilities.Ability;
import com.sekwah.narutoaddon.capabilities.INinjaData;
import com.sekwah.narutoaddon.events.SewingEvents;
import com.sekwah.narutoaddon.util.Targeting;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * Dharma Sewing Lock: the eye stitches a door, a gate or a lid to its frame.
 *
 * <p>Two ways to press one technique, and the base key handler already tells them apart:</p>
 * <ul>
 *   <li><b>Hold</b> the combo - the seam is drawn, paid for once, and then held for as long as the
 *       caster keeps the keys down. It is refreshed every tick, so a held seam never times out and
 *       costs nothing more: it holds indefinitely, however long the caster stands there. Letting go
 *       leaves it standing for another half minute.</li>
 *   <li><b>Tap</b> the combo - the seam is cut loose again. A tap costs nothing, so a caster can
 *       always undo their own stitch even with an empty chakra bar.</li>
 * </ul>
 *
 * <p>A locked thing is still the thing it was - the caster can walk through their own door - but the
 * seam will not let anyone else move it. That is what makes the technique a guard rather than a
 * barricade: nothing is placed, nothing is broken, and when the seam lets go the door is exactly as
 * it was built.</p>
 *
 * <p>Doors and chests are sewn on both halves, because the upper half of a door opens it just as
 * well as the lower one and the far half of a double chest is the same chest. The owner is recorded
 * rather than the whole player, so the lock survives the caster walking out of range.</p>
 */
public class SewingLockAbility extends Ability implements Ability.Channeled, Ability.Cooldown {

    /** A seam tightening is a small sound. */
    @Override
    public int noiseRadius() {
        return 4;
    }

    /** B V V V V V V. */
    public static final long COMBO = 3121L;

    public static final ResourceLocation ID = new ResourceLocation("narutoaddon", "sewing_lock");

    /** How far down the look ray a door or a lid can be sewn. */
    private static final double RANGE = 8.0d;

    /** One-off chakra price of a seam; the hold itself is free, so a held seam never drains dry. */
    private static final float COST = 25.0f;

    private static final int CHAKRA_REGEN_DELAY = 20;

    /** Persistent mark that the seam of the current hold has already been paid for. */
    private static final String PAID_KEY = "narutoaddon:sewing_lock_paid";

    /** How long the seam survives once the caster stops holding it, in ticks. */
    public static final int LOCK_TICKS = 600;

    private static final int COOLDOWN = 300;

    @Override
    public ActivationType activationType() {
        return ActivationType.CHANNELED;
    }

    @Override
    public long defaultCombo() {
        return COMBO;
    }

    @Override
    public int getCooldown() {
        return COOLDOWN;
    }

    /** A tap is the way to undo a seam: it must reach the server or the caster is stuck with it. */
    @Override
    public boolean canActivateBelowMinCharge() {
        return true;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        // A tap cuts a seam: it is free and is never refused for want of chakra, otherwise a caster
        // who ran dry while sewing could not take their own stitch back.
        if (chargeAmount == -1) {
            return true;
        }
        // First tick of a hold: nothing has been paid for this seam yet. Re-marking here is also what
        // heals a stale mark left behind by a hold that ended without its closing call.
        if (chargeAmount == 0) {
            player.getPersistentData().putBoolean(PAID_KEY, false);
        }
        // A running cooldown has to refuse every tick of the hold, not only its first one: a seam
        // started mid-cooldown because the second tick stopped asking would be a free bypass.
        String key = this.getTranslationKey(ninjaData);
        if (this.getCooldown() > 0 && ninjaData.getCooldownEvents().containsKey(key)) {
            if (chargeAmount == 0) {
                this.checkCooldown(player, ninjaData, key);
            }
            return false;
        }
        if (!hasLockableTarget(player)) {
            // Only on the first tick: a refused channel keeps asking every tick while the keys are
            // down, and a message per tick is unreadable.
            if (chargeAmount == 0) {
                player.displayClientMessage(Component.translatable("jutsu.fail.sewing_lock.nothing",
                                Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW))
                        .withStyle(ChatFormatting.RED), true);
            }
            return false;
        }
        // The seam is paid for once and then holds for free: a hold that drained chakra per tick
        // would drop the door open on its own after a few seconds, and the seam is supposed to keep
        // standing for as long as the caster stands there holding it.
        if (!player.getPersistentData().getBoolean(PAID_KEY)) {
            float cost = this.masteryCost(ninjaData, COST);
            if (ninjaData.getChakra() < cost) {
                if (chargeAmount == 0) {
                    player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW))
                            .withStyle(ChatFormatting.RED), true);
                }
                return false;
            }
            ninjaData.useChakra(cost, CHAKRA_REGEN_DELAY);
            player.getPersistentData().putBoolean(PAID_KEY, true);
        }
        return true;
    }

    /**
     * {@code ticksActive == -1} is the tap arm of the packet (the press never became a hold): that is
     * the seam being cut. A real release arrives with the number of ticks held and leaves the seam
     * standing for {@link #LOCK_TICKS} more.
     */
    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        // The hold is over either way: the next seam is paid for again.
        player.getPersistentData().putBoolean(PAID_KEY, false);
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        BlockHitResult hit = Targeting.lookedAtBlock(player, RANGE);
        if (ticksActive == -1) {
            this.cutSeam(level, player, hit);
            return;
        }

        // The seam was being refreshed every tick of the hold; this just gives the last one a full
        // life of its own, so releasing does not open the door the instant the keys come up.
        if (hit == null) {
            return;
        }
        BlockPos pos = hit.getBlockPos();
        BlockState state = level.getBlockState(pos);
        if (!SewingEvents.isLockable(state) || !SewingEvents.hasLock(level, pos, state)) {
            return;
        }
        SewingEvents.lock(level, pos, state, player.getUUID(), level.getGameTime() + LOCK_TICKS);
        this.registerCooldown(player, ninjaData, this.getTranslationKey(ninjaData));
    }

    /**
     * Keeps the seam standing while the caster holds the technique: the lock is re-stamped every
     * tick, so a held seam never expires, however long the caster stands there.
     */
    @Override
    public void handleChannelling(Player player, INinjaData ninjaData, int ticksChanneled) {
        this.recordMasteryCredit(player, ninjaData, MASTERY_CREDIT_PER_TICK);
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        BlockHitResult hit = Targeting.lookedAtBlock(player, RANGE);
        if (hit == null) {
            return;
        }
        BlockPos pos = hit.getBlockPos();
        BlockState state = level.getBlockState(pos);
        if (!SewingEvents.isLockable(state)) {
            return;
        }
        SewingEvents.lock(level, pos, state, player.getUUID(), level.getGameTime() + LOCK_TICKS);
        if (ticksChanneled % 4 == 0) {
            Vec3 centre = Vec3.atCenterOf(pos);
            level.sendParticles(ParticleTypes.ENCHANT, centre.x, centre.y, centre.z,
                    4, 0.3d, 0.3d, 0.3d, 0.01d);
        }
    }

    /** A held seam is mastered by the holding, not by the press. */
    @Override
    public void recordMastery(Player player, INinjaData ninjaData) {
    }

    /**
     * Cuts the seam off whatever the caster is looking at: their own stitch comes loose, and a seam
     * somebody else made is not theirs to cut.
     */
    private void cutSeam(ServerLevel level, Player player, BlockHitResult hit) {
        if (hit == null) {
            player.displayClientMessage(Component.translatable("jutsu.sewing_lock.noseam")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }
        BlockPos pos = hit.getBlockPos();
        BlockState state = level.getBlockState(pos);
        if (SewingEvents.isLockable(state) && SewingEvents.hasLock(level, pos, state)) {
            UUID owner = player.getUUID();
            if (!SewingEvents.unlock(level, pos, state, owner)) {
                player.displayClientMessage(Component.translatable("jutsu.fail.sewing_lock.held")
                        .withStyle(ChatFormatting.RED), true);
                return;
            }
            Vec3 centre = Vec3.atCenterOf(pos);
            level.sendParticles(ParticleTypes.PORTAL, centre.x, centre.y, centre.z,
                    16, 0.4d, 0.4d, 0.4d, 0.05d);
            level.playSound(null, centre.x, centre.y, centre.z,
                    SoundEvents.BUNDLE_DROP_CONTENTS, SoundSource.PLAYERS, 0.6f, 1.4f);
            player.displayClientMessage(Component.translatable("jutsu.sewing_lock.cut")
                    .withStyle(ChatFormatting.AQUA), true);
            return;
        }
        shearBlock(level, player, pos, state);
    }

    /**
     * a tap on a block that cannot be sewn cuts the block itself, which comes away whole (silk-touch style).
     */
    private void shearBlock(ServerLevel level, Player player, BlockPos pos, BlockState state) {
        if (state.isAir()
                || state.getBlock() instanceof LiquidBlock
                || state.getDestroySpeed(level, pos) < 0f
                || state.hasBlockEntity()
                || state.is(Blocks.BEDROCK)
                || state.is(Blocks.BARRIER)
                || state.is(Blocks.COMMAND_BLOCK)
                || state.is(Blocks.END_PORTAL_FRAME)
                || state.is(Blocks.END_PORTAL)
                || state.is(Blocks.NETHER_PORTAL)) {
            player.displayClientMessage(Component.translatable("jutsu.sewing_lock.noseam")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }
        level.levelEvent(2001, pos, Block.getId(state));
        level.removeBlock(pos, false);
        ItemStack drop = new ItemStack(state.getBlock().asItem());
        if (!drop.isEmpty()) {
            Block.popResource(level, pos, drop);
        }
        Vec3 centre = Vec3.atCenterOf(pos);
        level.sendParticles(ParticleTypes.PORTAL, centre.x, centre.y, centre.z, 16, 0.4d, 0.4d, 0.4d, 0.05d);
        level.playSound(null, centre.x, centre.y, centre.z,
                SoundEvents.BUNDLE_DROP_CONTENTS, SoundSource.PLAYERS, 0.6f, 1.4f);
    }

    /** @return whether the crosshair is on something a seam can hold shut. */
    private static boolean hasLockableTarget(Player player) {
        BlockHitResult hit = Targeting.lookedAtBlock(player, RANGE);
        return hit != null && SewingEvents.isLockable(player.level().getBlockState(hit.getBlockPos()));
    }
}
