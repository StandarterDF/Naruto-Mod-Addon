package com.sekwah.narutoaddon.capabilities.size;

import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import com.sekwah.narutoaddon.client.ClientSizeData;
import com.sekwah.narutoaddon.network.PacketHandler;
import com.sekwah.narutoaddon.network.s2c.ClientboundSizePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.PacketDistributor;

import java.util.UUID;

/**
 * Core of the player size system: stores the size scale in persistent data and applies the
 * matching attribute modifiers / hitbox scaling. Scale range is {@link #MIN}..{@link #MAX}.
 */
public final class EntitySize {

    public static final float MIN = 0.5f;
    public static final float MAX = 5.0f;
    public static final float DEFAULT = 1.0f;

    private static final String NBT_KEY = "narutoaddon:size";

    /** Saved share of the health bar, so a relog at an unusual size comes back with the right bar. */
    public static final String HEALTH_SHARE_KEY = "narutoaddon:health_share";

    private static final UUID MOVEMENT_SPEED_MODIFIER = UUID.fromString("a1b2c3d4-1111-4000-8000-000000000001");
    private static final UUID ATTACK_DAMAGE_MODIFIER = UUID.fromString("a1b2c3d4-2222-4000-8000-000000000002");
    private static final UUID ATTACK_KNOCKBACK_MODIFIER = UUID.fromString("a1b2c3d4-3333-4000-8000-000000000003");
    private static final UUID STEP_HEIGHT_MODIFIER = UUID.fromString("a1b2c3d4-4444-4000-8000-000000000004");
    private static final UUID ENTITY_REACH_MODIFIER = UUID.fromString("a1b2c3d4-5555-4000-8000-000000000005");
    private static final UUID BLOCK_REACH_MODIFIER = UUID.fromString("a1b2c3d4-6666-4000-8000-000000000006");
    private static final UUID MAX_HEALTH_MODIFIER = UUID.fromString("a1b2c3d4-7777-4000-8000-000000000007");

    private EntitySize() {
    }

    public static float get(Player player) {
        if (player == null) {
            return DEFAULT;
        }
        if (player.level().isClientSide()) {
            // Only the local player's scale is synced to this client; another player's own client
            // knows their scale, and guessing ours would give them the wrong hitbox.
            return DistExecutor.unsafeRunForDist(
                    () -> () -> player == net.minecraft.client.Minecraft.getInstance().player
                            ? ClientSizeData.get() : DEFAULT,
                    () -> () -> DEFAULT);
        }
        float value = player.getPersistentData().getFloat(NBT_KEY);
        if (value <= 0.0f) {
            return DEFAULT;
        }
        return value;
    }

    public static float clamp(float value) {
        return Math.max(MIN, Math.min(MAX, value));
    }

    public static void set(Player player, float scale) {
        if (player == null) {
            return;
        }
        float clamped = clamp(scale);
        // The body keeps its share of health across a change: growing from half size (10 hearts)
        // to full size must not leave the caster at 10 out of 20, and shrinking from five times
        // (50 hearts) must not throw away four fifths of the bar. The share is taken before the
        // maximum moves, so no step of the dial can heal or wound the caster on its own.
        float oldMax = player.getMaxHealth();
        float healthShare = oldMax > 0.0f ? player.getHealth() / oldMax : 1.0f;
        if (!player.level().isClientSide()) {
            player.getPersistentData().putFloat(HEALTH_SHARE_KEY, healthShare);
        }
        player.getPersistentData().putFloat(NBT_KEY, clamped);
        player.refreshDimensions();
        applyAttributes(player);
        float newMax = player.getMaxHealth();
        if (!player.level().isClientSide() && newMax > 0.0f) {
            player.setHealth(Math.max(1.0f, Math.min(newMax, healthShare * newMax)));
        }
        if (player instanceof ServerPlayer sp) {
            PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> sp), new ClientboundSizePacket(clamped));
        }
    }

    public static void applyAttributes(Player player) {
        if (player == null) {
            return;
        }
        float scale = get(player);
        double s = scale;

        // Movement speed: a small body is quick and a grown one is heavy. Below normal size the
        // factor is the inverse square root (half size moves at about 141%), above it the size
        // raised to 0.8 (five times the body moves at about 3.6 times the speed) - full linear
        // scaling made a giant hard to steer around corners.
        double speedFactor = s >= 1.0d ? Math.pow(s, 0.8d) : 1.0d / Math.sqrt(s);
        AttributeInstance movement = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movement != null) {
            movement.removeModifier(MOVEMENT_SPEED_MODIFIER);
            movement.addTransientModifier(new AttributeModifier(MOVEMENT_SPEED_MODIFIER, "narutoaddon:size_movement",
                    speedFactor - 1.0d, AttributeModifier.Operation.MULTIPLY_TOTAL));
        }

        // Maximum health: the body is the size, so half size is half the pool and five times the
        // size is five times the pool.
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.removeModifier(MAX_HEALTH_MODIFIER);
            maxHealth.addTransientModifier(new AttributeModifier(MAX_HEALTH_MODIFIER, "narutoaddon:size_health",
                    s - 1.0d, AttributeModifier.Operation.MULTIPLY_TOTAL));
        }

        // Attack damage: (scale - 1) * 2
        AttributeInstance attackDamage = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackDamage != null) {
            attackDamage.removeModifier(ATTACK_DAMAGE_MODIFIER);
            attackDamage.addTransientModifier(new AttributeModifier(ATTACK_DAMAGE_MODIFIER, "narutoaddon:size_attack_damage",
                    (s - 1.0d) * 2.0d, AttributeModifier.Operation.ADDITION));
        }

        // Attack knockback: (scale - 1) * 0.5
        AttributeInstance attackKnockback = player.getAttribute(Attributes.ATTACK_KNOCKBACK);
        if (attackKnockback != null) {
            attackKnockback.removeModifier(ATTACK_KNOCKBACK_MODIFIER);
            attackKnockback.addTransientModifier(new AttributeModifier(ATTACK_KNOCKBACK_MODIFIER, "narutoaddon:size_attack_knockback",
                    (s - 1.0d) * 0.5d, AttributeModifier.Operation.ADDITION));
        }

        // Step height: (scale - 1) * 0.6
        Attribute stepAttr = ForgeMod.STEP_HEIGHT_ADDITION.get();
        AttributeInstance stepHeight = player.getAttribute(stepAttr);
        if (stepHeight != null) {
            stepHeight.removeModifier(STEP_HEIGHT_MODIFIER);
            stepHeight.addTransientModifier(new AttributeModifier(STEP_HEIGHT_MODIFIER, "narutoaddon:size_step_height",
                    (s - 1.0d) * 0.6d, AttributeModifier.Operation.ADDITION));
        }

        // Entity reach: (scale - 1) * 1.5
        Attribute entityReachAttr = ForgeMod.ENTITY_REACH.get();
        AttributeInstance entityReach = player.getAttribute(entityReachAttr);
        if (entityReach != null) {
            entityReach.removeModifier(ENTITY_REACH_MODIFIER);
            entityReach.addTransientModifier(new AttributeModifier(ENTITY_REACH_MODIFIER, "narutoaddon:size_entity_reach",
                    (s - 1.0d) * 1.5d, AttributeModifier.Operation.ADDITION));
        }

        // Block reach: (scale - 1) * 1.5
        Attribute blockReachAttr = ForgeMod.BLOCK_REACH.get();
        AttributeInstance blockReach = player.getAttribute(blockReachAttr);
        if (blockReach != null) {
            blockReach.removeModifier(BLOCK_REACH_MODIFIER);
            blockReach.addTransientModifier(new AttributeModifier(BLOCK_REACH_MODIFIER, "narutoaddon:size_block_reach",
                    (s - 1.0d) * 1.5d, AttributeModifier.Operation.ADDITION));
        }
    }
}