package com.sekwah.narutoaddon.events;

import com.sekwah.narutoaddon.abilities.jutsus.TenseiganAbility;
import com.sekwah.narutoaddon.capabilities.INinjaData;
import com.sekwah.narutoaddon.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutoaddon.mixin.common.LivingEntityJumpingAccessor;
import com.sekwah.narutoaddon.skills.SkillTree;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Passive bonuses granted by the Tenseigan skill branch.
 *
 * <p>Three independent mechanics live here, one per unlocked node:
 * straightening the flight of thrown projectiles, an effectively infinite
 * mid-air jump and a meteor strike on landing from a real height.</p>
 */
@Mod.EventBusSubscriber(modid = "narutoaddon", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TenseiganPassiveEvents {

    /** Projectiles whose owner has {@code mechanic:trajectory_distortion}. */
    private static final Set<UUID> TRAJECTORY_PROJECTILES = new HashSet<>();

    /** Last game time a player used the infinite jump, for the 5-tick cooldown. */
    private static final Map<UUID, Long> INFINITE_JUMP_COOLDOWN = new HashMap<>();

    /**
     * Highest Y a player has reached since leaving the ground, keyed by player.
     *
     * <p>The chakra mode lends flight, and vanilla {@code Player#travel} clears {@code fallDistance}
     * on every tick spent flying, so a caster who lands straight out of flight would never look like
     * a faller. Tracking the peak height keeps the meteor strike honest in that case.</p>
     */
    private static final Map<UUID, Double> AIRBORNE_PEAK = new HashMap<>();

    /** Counts server ticks so the trajectory pass only runs every other tick. */
    private static int trajectoryTickCounter = 0;

    private TenseiganPassiveEvents() {
    }

    private static boolean isJumping(Player player) {
        return ((LivingEntityJumpingAccessor) player).narutoaddon$isJumping();
    }

    // ------------------------------------------------------------------ trajectory distortion

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel)) {
            return;
        }
        Entity entity = event.getEntity();
        if (!(entity instanceof ThrownEnderpearl) && !(entity instanceof AbstractArrow)) {
            return;
        }
        Entity owner = ((Projectile) entity).getOwner();
        if (!(owner instanceof Player player)) {
            return;
        }
        INinjaData ninjaData = player.getCapability(NinjaCapabilityHandler.NINJA_DATA)
                .resolve().orElse(null);
        if (ninjaData == null) {
            return;
        }
        if (!SkillTree.hasUnlock(ninjaData.getSkillData().getUnlocked(),
                "mechanic:trajectory_distortion")) {
            return;
        }
        if (!TenseiganAbility.isActive(ninjaData)) {
            return;
        }
        TRAJECTORY_PROJECTILES.add(entity.getUUID());
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (++trajectoryTickCounter % 2 != 0) {
            return;
        }
        MinecraftServer server = event.getServer();
        Iterator<UUID> iterator = TRAJECTORY_PROJECTILES.iterator();
        while (iterator.hasNext()) {
            UUID uuid = iterator.next();
            Entity projectile = null;
            for (ServerLevel level : server.getAllLevels()) {
                Entity candidate = level.getEntity(uuid);
                if (candidate != null) {
                    projectile = candidate;
                    break;
                }
            }
            if (projectile == null || !projectile.isAlive()) {
                iterator.remove();
                continue;
            }
            Vec3 direction = projectile.getDeltaMovement();
            if (direction.lengthSqr() > 0.01d) {
                projectile.setDeltaMovement(direction.normalize().scale(1.6d));
                projectile.setNoGravity(true);
            } else {
                iterator.remove();
            }
        }
    }

    // ------------------------------------------------------------------ infinite jump

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Player player = event.player;
        if (player.level().isClientSide()) {
            return;
        }
        INinjaData ninjaData = player.getCapability(NinjaCapabilityHandler.NINJA_DATA)
                .resolve().orElse(null);
        if (ninjaData == null) {
            return;
        }
        updateMeteorStrike(player, ninjaData);
        if (!SkillTree.hasUnlock(ninjaData.getSkillData().getUnlocked(),
                "mechanic:infinite_jump")) {
            return;
        }
        if (!TenseiganAbility.isActive(ninjaData)) {
            return;
        }
        if (player.onGround()) {
            return;
        }
        if (!isJumping(player)) {
            return;
        }
        Vec3 movement = player.getDeltaMovement();
        if (movement.y >= 0.4d) {
            return;
        }
        long now = player.level().getGameTime();
        Long last = INFINITE_JUMP_COOLDOWN.get(player.getUUID());
        if (last != null && now - last < 5L) {
            return;
        }
        player.setDeltaMovement(movement.x, 0.6d, movement.z);
        player.hasImpulse = true;
        player.fallDistance = 0.0f;
        INFINITE_JUMP_COOLDOWN.put(player.getUUID(), now);
    }

    // ------------------------------------------------------------------ meteor strike

    /** Minimum height of the drop that turns a landing into a meteor strike. */
    private static final double METEOR_MIN_DROP = 15.0d;

    /** Damage stops growing past this height, so a fall from orbit stays survivable for onlookers. */
    private static final double METEOR_MAX_HEIGHT = 60.0d;

    /**
     * Keeps the peak height of the current jump or flight and fires the meteor strike on touchdown.
     *
     * <p>This cannot be left to {@link LivingFallEvent} alone: vanilla only calls
     * {@code Block#fallOn} (and therefore {@code causeFallDamage}) when {@code fallDistance} is
     * positive, while the chakra mode's flight clears it every tick. Landing straight out of flight
     * would then produce no event at all, which is why the drop is tracked here instead.</p>
     */
    private static void updateMeteorStrike(Player player, INinjaData ninjaData) {
        UUID id = player.getUUID();
        // The passives of the branch belong to the open eye, not to the tree alone: a caster who
        // has bought the node but is not wearing the eye gets nothing and leaves no tracked height.
        if (!SkillTree.hasUnlock(ninjaData.getSkillData().getUnlocked(),
                "mechanic:meteor_strike")
                || !TenseiganAbility.isActive(ninjaData)) {
            AIRBORNE_PEAK.remove(id);
            return;
        }
        boolean grounded = player.onGround() || player.isPassenger()
                || player.isInWater() || player.isInLava();
        if (!grounded) {
            AIRBORNE_PEAK.merge(id, player.getY(), Math::max);
            return;
        }
        Double peak = AIRBORNE_PEAK.remove(id);
        if (peak == null) {
            return;
        }
        double drop = peak - player.getY();
        if (drop > METEOR_MIN_DROP) {
            strikeMeteor(player, drop);
        }
    }

    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (player.level().isClientSide()) {
            return;
        }
        INinjaData ninjaData = player.getCapability(NinjaCapabilityHandler.NINJA_DATA)
                .resolve().orElse(null);
        if (ninjaData == null) {
            return;
        }
        if (!SkillTree.hasUnlock(ninjaData.getSkillData().getUnlocked(),
                "mechanic:meteor_strike")) {
            return;
        }
        if (!TenseiganAbility.isActive(ninjaData)) {
            return;
        }
        // The peak tracked during the fall wins over fallDistance, which flight clears every tick.
        Double peak = AIRBORNE_PEAK.remove(player.getUUID());
        double drop = Math.max(event.getDistance(), player.fallDistance);
        if (peak != null) {
            drop = Math.max(drop, peak - player.getY());
        }
        if (drop <= METEOR_MIN_DROP) {
            return;
        }
        event.setCanceled(true);
        event.setDistance(0.0f);
        strikeMeteor(player, drop);
    }

    /** Damages and knocks back everything around the landing point and plays the shockwave. */
    private static void strikeMeteor(Player player, double drop) {
        double height = Math.min(METEOR_MAX_HEIGHT, drop);
        for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(6.0d))) {
            if (target == player) {
                continue;
            }
            target.hurt(player.level().damageSources().playerAttack(player),
                    (float) (height * 2.0d));
            target.knockback(1.0f, target.getX() - player.getX(),
                    target.getZ() - player.getZ());
        }

        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, player.getX(),
                    player.getY(), player.getZ(), 1, 0.0d, 0.0d, 0.0d, 0.0d);
            level.playSound(null, player, SoundEvents.GENERIC_EXPLODE,
                    SoundSource.PLAYERS, 1.0f, 1.0f);
            level.explode(player, player.getX(), player.getY(), player.getZ(),
                    0.0f, Level.ExplosionInteraction.NONE);
        }
        player.fallDistance = 0.0f;
    }
}