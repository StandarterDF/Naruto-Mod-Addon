package com.sekwah.narutoaddon.abilities.jutsus;

import com.sekwah.narutoaddon.abilities.Ability;
import com.sekwah.narutoaddon.capabilities.INinjaData;
import com.sekwah.narutoaddon.entity.jutsuprojectile.DharmaCubeEntity;
import com.sekwah.narutoaddon.util.Targeting;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Dharma: calls a black cube down onto whatever the caster is looking at.
 *
 * <p>An instant cast, not a hold: the cube is a single heavy hit, and making the caster keep the keys
 * down for it only produced "I pressed it and nothing happened".</p>
 *
 * <p>The landing spot is a real ray cast, not a fixed point in front of the caster: the ray runs
 * from the eyes along the crosshair, stops at the first entity or the first block it meets, and the
 * cube drops exactly there. A beam of dark particles is drawn from the eyes to that spot, so the
 * caster sees where the cube is going before it lands.</p>
 */
public class CubeSummonAbility extends Ability implements Ability.Cooldown {

    /** A cube dropping is heard across the field. */
    @Override
    public int noiseRadius() {
        return 24;
    }

    public static final long COMBO = 3321L;

    public static final ResourceLocation ID = new ResourceLocation("narutoaddon", "cube_summon");

    private static final float COST = 60.0f;
    private static final int COOLDOWN = 200;

    /** How far the targeting ray reaches, in blocks. */
    private static final double RANGE = 40.0d;

    /** How high above the landing spot the cube appears, in blocks. */
    private static final double DROP_HEIGHT = 12.0d;

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
        if (this.checkCooldown(player, ninjaData, this.getTranslationKey(ninjaData))) {
            return false;
        }
        float cost = this.masteryCost(ninjaData, COST);
        if (ninjaData.getChakra() < cost) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW))
                    .withStyle(ChatFormatting.RED), true);
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
        Landing landing = aim(level, player);
        drawBeam(level, player.getEyePosition(), landing.beamPoint());
        DharmaCubeEntity cube = new DharmaCubeEntity(player, level,
                landing.x(), landing.spawnY(), landing.z());
        level.addFreshEntity(cube);
        level.sendParticles(ParticleTypes.SMOKE, landing.x(), landing.spawnY(), landing.z(),
                8, 0.4d, 0.2d, 0.4d, 0.02d);
        level.playSound(null, player, net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE,
                net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 0.8f);
    }

    @Override
    public void recordMastery(Player player, INinjaData ninjaData) {
    }

    /**
     * Casts the targeting ray and works out where the cube has to fall.
     *
     * <p>An entity under the crosshair is hit first, then a block; when the ray reaches into open
     * air the ground under its far end is used, so a cast at the horizon still lands somewhere
     * instead of dropping a cube into the void.</p>
     */
    private static Landing aim(ServerLevel level, Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0f);

        Entity looked = Targeting.lookedAtEntity(player, RANGE);
        if (looked instanceof LivingEntity target) {
            // Straight onto the head: the cube has to come down on the body, not on the feet, or
            // the hitbox misses it while the target is jumping.
            double top = target.getY() + target.getBbHeight();
            Vec3 beam = new Vec3(target.getX(), target.getY() + target.getBbHeight() * 0.5d, target.getZ());
            return new Landing(target.getX(), top + DROP_HEIGHT, target.getZ(), beam);
        }

        BlockHitResult hit = Targeting.lookedAtBlock(player, RANGE);
        if (hit != null) {
            // The cube lands in the open cell the crosshair points at, not inside the face it hit:
            // a wall gives the cell in front of it, the ground gives the cell above it.
            BlockPos block = hit.getBlockPos();
            BlockPos landing = block.relative(hit.getDirection());
            return new Landing(landing.getX() + 0.5d, landing.getY() + DROP_HEIGHT,
                    landing.getZ() + 0.5d, hit.getLocation());
        }

        Vec3 reach = eye.add(look.scale(RANGE));
        int x = (int) Math.floor(reach.x);
        int z = (int) Math.floor(reach.z);
        int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        // An unloaded column answers with the bottom of the world; keep the ray's own height there.
        double baseY = ground <= level.getMinBuildHeight() ? reach.y : ground;
        return new Landing(x + 0.5d, baseY + DROP_HEIGHT, z + 0.5d, reach);
    }

    /**
     * Draws the aim beam: a line of dark, sinking motes from the caster's eyes to the landing spot,
     * plus a flare there. Without it the technique was a silent hit with nothing to watch.
     */
    private static void drawBeam(ServerLevel level, Vec3 from, Vec3 to) {
        Vec3 span = to.subtract(from);
        double length = span.length();
        if (length < 1.0E-4d) {
            return;
        }
        Vec3 step = span.scale(1.0d / length);
        // Keep the first stretch clear, or the start of the beam is a bright blob on the camera.
        double start = Math.min(2.0d, length * 0.5d);
        int points = Math.max(1, (int) ((length - start) * 1.5d));
        DustParticleOptions mote = new DustParticleOptions(new Vector3f(0.05f, 0.05f, 0.08f), 0.7f);
        for (int i = 0; i <= points; i++) {
            Vec3 point = from.add(step.scale(start + (length - start) * i / points));
            level.sendParticles(mote, point.x, point.y, point.z, 1, 0.0d, 0.0d, 0.0d, 0.0d);
        }
        level.sendParticles(ParticleTypes.SQUID_INK, to.x, to.y, to.z, 10, 0.4d, 0.4d, 0.4d, 0.02d);
    }

    /**
     * Where the cube appears and what the beam points at.
     *
     * @param x      centre of the cube on the x axis
     * @param spawnY bottom of the cube, already lifted to the drop height
     * @param z      centre of the cube on the z axis
     * @param beamPoint the spot on the surface the crosshair hit, for the beam's flare
     */
    private record Landing(double x, double spawnY, double z, Vec3 beamPoint) {
    }
}
