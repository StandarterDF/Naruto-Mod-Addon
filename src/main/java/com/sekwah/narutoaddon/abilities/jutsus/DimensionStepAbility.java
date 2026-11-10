package com.sekwah.narutoaddon.abilities.jutsus;

import com.sekwah.narutoaddon.abilities.Ability;
import com.sekwah.narutoaddon.capabilities.CooldownTickEvent;
import com.sekwah.narutoaddon.capabilities.INinjaData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Dharma: Gate: opens a portal into the caster's personal bedrock cube. Casting it again while standing
 * inside the cube steps back out onto the point the portal was opened from.
 */
@Mod.EventBusSubscriber(modid = "narutoaddon", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DimensionStepAbility extends Ability {

    /** The pocket dimension this technique opens onto. */
    public static final ResourceKey<Level> DHARMA_DIMENSION =
            ResourceKey.create(Registries.DIMENSION, new ResourceLocation("narutoaddon", "dharma_cube"));

    /** Persistent-data key holding the way back out of the cube. */
    public static final String RETURN_KEY = "narutoaddon:dharma_return";

    /** B V V C B - the dharma eye, then one more key. */
    public static final long COMBO = 3121L;

    public static final ResourceLocation ID = new ResourceLocation("narutoaddon", "dimension_step");

    private static final float COST = 60.0f;
    private static final int COOLDOWN = 1200;
    private static final int PORTAL_LIFETIME_TICKS = 300;

    /** Open portals waiting for someone to walk through them. */
    public static final List<Portal> PORTALS = new ArrayList<>();

    public record Portal(ResourceKey<Level> dim, Vec3 pos, long expiresAt) {
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
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        // Stepping back out must never be blocked: a caster inside the cube is let out for free,
        // even on cooldown - otherwise a gate on cooldown becomes a locked cell.
        if (player.level().dimension().equals(DHARMA_DIMENSION)) {
            return true;
        }
        if (onCooldown(player, ninjaData)) {
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

    /** @return whether the gate is still cooling down, telling the caster how long is left. */
    private boolean onCooldown(Player player, INinjaData ninjaData) {
        String key = this.getTranslationKey(ninjaData);
        CooldownTickEvent event = ninjaData.getCooldownEvents().get(key);
        if (event == null) {
            return false;
        }
        player.displayClientMessage(Component.translatable("jutsu.fail.cooldown",
                Component.translatable(key).withStyle(ChatFormatting.YELLOW),
                Component.literal(String.valueOf((int) Math.ceil(event.ticks / 20f)))
                        .withStyle(ChatFormatting.YELLOW)), true);
        return true;
    }

    /** Starts the one-minute cooldown of a freshly opened gate. */
    private void startCooldown(Player player, INinjaData ninjaData) {
        String key = this.getTranslationKey(ninjaData);
        ninjaData.getCooldownEvents().put(key,
                new CooldownTickEvent(com.sekwah.narutoaddon.abilities.mastery.Mastery.reduceCooldown(
                        ninjaData, key, COOLDOWN)));
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        if (!(player instanceof ServerPlayer sp)) {
            return;
        }

        if (player.level().dimension().equals(DHARMA_DIMENSION)) {
            leaveDharmaCube(player);
            // The way out clears the gate's cooldown: it was paid for when the gate opened.
            ninjaData.getCooldownEvents().remove(this.getTranslationKey(ninjaData));
            return;
        }

        // Open a portal: remember the way home first, then place the door.
        CompoundTag data = player.getPersistentData();
        CompoundTag ret = new CompoundTag();
        ret.putString("dim", level.dimension().location().toString());
        ret.putDouble("x", player.getX());
        ret.putDouble("y", player.getY());
        ret.putDouble("z", player.getZ());
        ret.putFloat("yaw", player.getYRot());
        ret.putFloat("pitch", player.getXRot());
        data.put(RETURN_KEY, ret);

        Vec3 look = player.getLookAngle().normalize();
        Vec3 base = player.position().add(look.scale(3.0d));
        Vec3 portalPos = findFreePlace(level, base, player);

        // A portal opened on top of another portal is just a way to lose the way back.
        for (Portal existing : PORTALS) {
            if (existing.dim().equals(level.dimension())
                    && existing.pos().distanceToSqr(portalPos) < 4.0d
                    && existing.expiresAt() > level.getGameTime()) {
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                        "jutsu.fail.dimension_step.nospace"), true);
                return;
            }
        }
        PORTALS.add(new Portal(level.dimension(), portalPos, level.getGameTime() + PORTAL_LIFETIME_TICKS));
        startCooldown(player, ninjaData);
        level.sendParticles(ParticleTypes.PORTAL, portalPos.x, portalPos.y + 1.0d, portalPos.z,
                32, 0.3d, 0.5d, 0.3d, 0.4d);
    }


    /**
     * Sends a caster out of the cube: to the point they opened the gate from, or to the overworld
     * spawn when that point is gone. Used both by the gate itself and by the Dharma Eye closing -
     * the pocket dimension belongs to the eye, so it must not keep anyone inside once the eye shuts.
     */
    public static void leaveDharmaCube(Player player) {
        if (!(player instanceof ServerPlayer sp) || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        if (!level.dimension().equals(DHARMA_DIMENSION)) {
            return;
        }
        CompoundTag data = player.getPersistentData();
        ServerLevel destination = level.getServer().overworld();
        double x = destination.getSharedSpawnPos().getX() + 0.5d;
        double y = destination.getSharedSpawnPos().getY();
        double z = destination.getSharedSpawnPos().getZ() + 0.5d;
        float yaw = player.getYRot();
        float pitch = player.getXRot();
        if (data.contains(RETURN_KEY)) {
            CompoundTag ret = data.getCompound(RETURN_KEY);
            ResourceLocation dimLoc = ResourceLocation.tryParse(ret.getString("dim"));
            if (dimLoc != null) {
                ServerLevel resolved = level.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, dimLoc));
                if (resolved != null) {
                    destination = resolved;
                }
            }
            x = ret.getDouble("x");
            y = ret.getDouble("y");
            z = ret.getDouble("z");
            yaw = ret.getFloat("yaw");
            pitch = ret.getFloat("pitch");
            data.remove(RETURN_KEY);
        }
        if (destination == null) {
            return;
        }
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1.0d, player.getZ(),
                32, 0.3d, 0.5d, 0.3d, 0.4d);
        sp.teleportTo(destination, x, y, z, yaw, pitch);
        destination.sendParticles(ParticleTypes.PORTAL, x, y + 1.0d, z, 32, 0.3d, 0.5d, 0.3d, 0.4d);
        player.fallDistance = 0.0f;
    }

    @SubscribeEvent
    public static void onServerStopping(net.minecraftforge.event.server.ServerStoppingEvent event) {
        PORTALS.clear();
    }

    private static Vec3 findFreePlace(ServerLevel level, Vec3 base, Player player) {
        Vec3 pos = base;
        for (int i = 0; i < 6; i++) {
            BlockPos feet = new BlockPos((int) Math.floor(pos.x), (int) Math.floor(pos.y), (int) Math.floor(pos.z));
            if (level.getBlockState(feet).isAir() && level.getBlockState(feet.above()).isAir()
                    && level.noCollision(player, player.getBoundingBox().move(pos.subtract(player.position())))) {
                break;
            }
            pos = pos.add(0.0d, 1.0d, 0.0d);
        }
        return pos;
    }

    public static boolean isInDharmaCube(Entity entity) {
        return entity.level().dimension().equals(DHARMA_DIMENSION);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Player player = event.player;
        if (player.level().isClientSide) {
            return;
        }
        if (!(player instanceof ServerPlayer sp)) {
            return;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        long now = level.getGameTime();

        // Particles for open portals in this dimension.
        Iterator<Portal> pit = PORTALS.iterator();
        while (pit.hasNext()) {
            Portal portal = pit.next();
            if (portal.expiresAt() <= now) {
                pit.remove();
                continue;
            }
            if (!portal.dim().equals(level.dimension())) {
                continue;
            }
            if (now % 2 == 0) {
                for (double dy = 0.0d; dy <= 2.0d; dy += 0.5d) {
                    level.sendParticles(ParticleTypes.PORTAL, portal.pos().x, portal.pos().y + dy, portal.pos().z,
                            2, 0.25d, 0.05d, 0.25d, 0.02d);
                    level.sendParticles(ParticleTypes.REVERSE_PORTAL, portal.pos().x, portal.pos().y + dy, portal.pos().z,
                            1, 0.25d, 0.05d, 0.25d, 0.02d);
                }
            }
        }

        // Entry check every five ticks.
        if (sp.tickCount % 5 != 0) {
            return;
        }
        Iterator<Portal> eit = PORTALS.iterator();
        while (eit.hasNext()) {
            Portal portal = eit.next();
            if (!portal.dim().equals(level.dimension())) {
                continue;
            }
            if (portal.expiresAt() <= now) {
                eit.remove();
                continue;
            }
            if (sp.position().distanceTo(portal.pos()) < 1.5d) {
                ServerLevel dharma = level.getServer().getLevel(DHARMA_DIMENSION);
                if (dharma != null) {
                    sp.teleportTo(dharma, 8.0d, 64.0d, 8.0d, sp.getYRot(), sp.getXRot());
                }
                eit.remove();
                break;
            }
        }
    }
}