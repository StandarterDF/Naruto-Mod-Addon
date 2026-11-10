package com.sekwah.narutoaddon.abilities.jutsus;

import com.sekwah.narutoaddon.abilities.Ability;
import com.sekwah.narutoaddon.capabilities.INinjaData;
import com.sekwah.narutoaddon.capabilities.size.EntitySize;
import com.sekwah.narutoaddon.util.Targeting;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Dharma: grinds a three by three by three cube of space out of the world.
 *
 * <p>The drill is instant: every tick of the hold removes the whole cube instead of one cube every
 * second, which is what a technique that eats space should feel like. A tap switches between the two
 * modes it can run in:</p>
 * <ul>
 *   <li><b>Vanish</b> (default, cheap) - the blocks are simply gone.</li>
 *   <li><b>Drop</b> (expensive) - the blocks fall out as items.</li>
 * </ul>
 */
public class SpaceDrillAbility extends Ability implements Ability.Channeled {

    public static final long COMBO = 3112L;

    public static final ResourceLocation ID = new ResourceLocation("narutoaddon", "space_drill");

    /** Chakra per tick while the blocks vanish. */
    private static final float COST_VANISH = 0.5f;

    /** Chakra per tick while the blocks are kept as drops. */
    private static final float COST_DROP = 1.2f;

    private static final int CHAKRA_REGEN_DELAY = 20;

    /** How far the beam reaches before it lands, in blocks. */
    private static final double RANGE = 10.0d;

    /** Persistent flag: true when the drill keeps what it breaks. */
    private static final String DROP_MODE_KEY = "narutoaddon:space_drill_drop";

    @Override
    public ActivationType activationType() {
        return ActivationType.CHANNELED;
    }

    @Override
    public long defaultCombo() {
        return COMBO;
    }

    /** A tap switches the mode, so the press has to reach the server. */
    @Override
    public boolean canActivateBelowMinCharge() {
        return true;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        // A tap is the mode switch and costs nothing.
        if (chargeAmount == -1) {
            return true;
        }
        // A bigger body cuts a bigger hole, and the hole is paid for by the block.
        float cost = this.masteryCost(ninjaData,
                (dropMode(player) ? COST_DROP : COST_VANISH) * costFactor(player));
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

    /** {@code ticksActive == -1} is the tap: flip between vanishing and dropping. */
    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        if (ticksActive != -1) {
            return;
        }
        boolean drop = !dropMode(player);
        player.getPersistentData().putBoolean(DROP_MODE_KEY, drop);
        player.displayClientMessage(Component.translatable(
                drop ? "jutsu.space_drill.mode.drop" : "jutsu.space_drill.mode.vanish")
                .withStyle(drop ? ChatFormatting.GOLD : ChatFormatting.AQUA), true);
    }

    @Override
    public void handleChannelling(Player player, INinjaData ninjaData, int ticksChanneled) {
        this.recordMasteryCredit(player, ninjaData, MASTERY_CREDIT_PER_TICK);
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        BlockHitResult hit = Targeting.lookedAtBlock(player, RANGE);
        // A beam leaves the caster every tick, all the way to the face it lands on: the drill is a
        // visible cut through space, not blocks quietly vanishing somewhere ahead.
        Vec3 eye = player.getEyePosition();
        Vec3 impact = hit == null ? eye.add(player.getLookAngle().scale(RANGE)) : hit.getLocation();
        drawBeam(level, eye, impact, cutRadius(player));
        if (hit == null) {
            return;
        }
        boolean drop = dropMode(player);
        BlockPos center = hit.getBlockPos();
        int radius = cutRadius(player);
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius),
                center.offset(radius, radius, radius))) {
            BlockState state = level.getBlockState(pos);
            if (state.isAir()
                    || state.getDestroySpeed(level, pos) < 0
                    || state.getDestroySpeed(level, pos) > 5
                    || state.hasBlockEntity()
                    || state.is(Blocks.BEDROCK)) {
                continue;
            }
            // destroyBlock with drops either throws the item out or leaves nothing behind.
            level.destroyBlock(pos.immutable(), drop, player);
        }
    }

    /**
     * Draws the drill beam: a dense line of sparks from the caster's eyes to the face it cuts, with a
     * flare where it lands.
     */
    private static void drawBeam(ServerLevel level, Vec3 from, Vec3 to, int radius) {
        Vec3 span = to.subtract(from);
        double length = span.length();
        if (length < 1.0E-4d) {
            return;
        }
        Vec3 step = span.scale(1.0d / length);
        // Keep the first stretch clear: the beam starts at the eyes, and a line of glowing points
        // two centimetres from the camera is what made the cast blind the caster.
        double start = Math.min(2.0d, length * 0.5d);
        int points = Math.max(1, (int) ((length - start) * 1.5d));
        for (int i = 0; i <= points; i++) {
            Vec3 point = from.add(step.scale(start + (length - start) * i / points));
            level.sendParticles(new net.minecraft.core.particles.DustParticleOptions(
                            new org.joml.Vector3f(0.55f, 0.85f, 1.0f), 0.45f),
                    point.x, point.y, point.z, 1, 0.0d, 0.0d, 0.0d, 0.0d);
        }
        level.sendParticles(ParticleTypes.SMOKE, to.x, to.y, to.z, 4 + radius * 3,
                radius * 0.4d, radius * 0.4d, radius * 0.4d, 0.01d);
    }


    /**
     * Radius of the hole, in blocks: one block at normal size, one more for every two sizes of
     * growth. A five-times-tall caster cuts a seven-by-seven hole, not the same three-by-three a
     * normal body does.
     */
    private static int cutRadius(Player player) {
        float scale = EntitySize.get(player);
        return Math.max(0, 1 + (int) Math.floor((scale - 1.0f) * 0.5f));
    }

    /** @return how much more the cut costs than a normal-sized one. */
    private static float costFactor(Player player) {
        return 1.0f + (cutRadius(player) - 1) * 0.5f;
    }

    /** @return whether the drill keeps its blocks as drops. */
    private static boolean dropMode(Player player) {
        return player.getPersistentData().getBoolean(DROP_MODE_KEY);
    }

    @Override
    public void recordMastery(Player player, INinjaData ninjaData) {
    }
}
