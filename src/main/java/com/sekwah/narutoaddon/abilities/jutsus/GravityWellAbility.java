package com.sekwah.narutoaddon.abilities.jutsus;

import com.sekwah.narutoaddon.abilities.Ability;
import com.sekwah.narutoaddon.capabilities.INinjaData;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Tenseigan branch: a gravity well that pulls everything nearby and detonates after a short time.
 */
@Mod.EventBusSubscriber(modid = "narutoaddon", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class GravityWellAbility extends Ability implements Ability.Cooldown {

    public static final long COMBO = 3321L;

    public static final ResourceLocation ID = new ResourceLocation("narutoaddon", "gravity_well");

    private static final float COST = 45.0f;
    private static final int COOLDOWN = 300;
    private static final int CHAKRA_REGEN_DELAY = 20;
    private static final double PULL_RADIUS = 8.0d;
    private static final double DAMAGE_RADIUS = 4.0d;
    private static final float DAMAGE = 12.0f;
    private static final int LIFETIME_TICKS = 100;

    private static final Map<UUID, Well> WELLS = new HashMap<>();
    private static int tickCounter = 0;

    private static final class Well {
        final Vec3 pos;
        final UUID owner;
        final ResourceKey<Level> dimension;
        final long expiresAt;

        Well(Vec3 pos, UUID owner, ResourceKey<Level> dimension, long expiresAt) {
            this.pos = pos;
            this.owner = owner;
            this.dimension = dimension;
            this.expiresAt = expiresAt;
        }
    }

    @Override
    public int noiseRadius() {
        return 16;
    }

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
        if (!TenseiganAbility.requireOpen(player, ninjaData, this)) {
            return false;
        }
        float cost = this.masteryCost(ninjaData, COST);
        if (ninjaData.getChakra() < cost) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("jutsu.fail.notenoughchakra",
                    net.minecraft.network.chat.Component.translatable(this.getTranslationKey(ninjaData)).withStyle(net.minecraft.ChatFormatting.YELLOW)), true);
            return false;
        }
        ninjaData.useChakra(cost, CHAKRA_REGEN_DELAY);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 look = player.getLookAngle();
        Vec3 pos = player.getEyePosition().add(look.scale(6.0d));
        WELLS.put(UUID.randomUUID(), new Well(pos, player.getUUID(), level.dimension(),
                level.getGameTime() + LIFETIME_TICKS));
    }

    /**
     * The well drags enemies only: the caster is standing in its own radius, and an ally should not
     * be pulled into a hole the caster opened.
     */
    private static boolean isHostile(ServerLevel level, Well well, LivingEntity target) {
        if (target.getUUID().equals(well.owner)) {
            return false;
        }
        Player owner = level.getServer() == null ? null : level.getServer().getPlayerList().getPlayer(well.owner);
        return owner == null || !com.sekwah.narutoaddon.friends.Friends.isOwnSide(owner, target);
    }

    @SubscribeEvent
    public static void onServerStopping(net.minecraftforge.event.server.ServerStoppingEvent event) {
        WELLS.clear();
        tickCounter = 0;
    }

    @Override
    public void recordMastery(Player player, INinjaData ninjaData) {
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (++tickCounter % 2 != 0) {
            return;
        }
        MinecraftServer server = event.getServer();
        Iterator<Map.Entry<UUID, Well>> it = WELLS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Well> entry = it.next();
            Well well = entry.getValue();
            ServerLevel level = server.getLevel(well.dimension);
            if (level == null) {
                it.remove();
                continue;
            }
            long now = level.getGameTime();
            if (now >= well.expiresAt) {
                Player owner = server.getPlayerList().getPlayer(well.owner);
                AABB box = new AABB(
                        well.pos.x - DAMAGE_RADIUS, well.pos.y - DAMAGE_RADIUS, well.pos.z - DAMAGE_RADIUS,
                        well.pos.x + DAMAGE_RADIUS, well.pos.y + DAMAGE_RADIUS, well.pos.z + DAMAGE_RADIUS);
                for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box,
                        e -> e.isAlive() && !e.isRemoved())) {
                    if (target.distanceToSqr(well.pos) > DAMAGE_RADIUS * DAMAGE_RADIUS) {
                        continue;
                    }
                    if (!isHostile(level, well, target)) {
                        continue;
                    }
                    if (owner != null) {
                        target.hurt(owner.damageSources().playerAttack(owner), DAMAGE);
                    } else {
                        target.hurt(level.damageSources().magic(), DAMAGE);
                    }
                }
                level.sendParticles(ParticleTypes.PORTAL, well.pos.x, well.pos.y, well.pos.z,
                        40, 1.0d, 1.0d, 1.0d, 0.05d);
                level.sendParticles(ParticleTypes.SMOKE, well.pos.x, well.pos.y, well.pos.z,
                        40, 1.0d, 1.0d, 1.0d, 0.05d);
                it.remove();
            } else {
                AABB box = new AABB(
                        well.pos.x - PULL_RADIUS, well.pos.y - PULL_RADIUS, well.pos.z - PULL_RADIUS,
                        well.pos.x + PULL_RADIUS, well.pos.y + PULL_RADIUS, well.pos.z + PULL_RADIUS);
                for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box,
                        e -> e.isAlive() && !e.isRemoved())) {
                    if (target.distanceToSqr(well.pos) > PULL_RADIUS * PULL_RADIUS) {
                        continue;
                    }
                    if (!isHostile(level, well, target)) {
                        continue;
                    }
                    Vec3 dir = well.pos.subtract(target.position());
                    if (dir.lengthSqr() > 0.001d) {
                        target.setDeltaMovement(dir.normalize().scale(0.35d));
                        target.hurtMarked = true;
                    }
                }
            }
        }
    }
}