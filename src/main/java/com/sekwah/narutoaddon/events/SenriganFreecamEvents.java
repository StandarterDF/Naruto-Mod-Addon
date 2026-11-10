package com.sekwah.narutoaddon.events;

import com.sekwah.narutoaddon.abilities.ToggleState;
import com.sekwah.narutoaddon.abilities.jutsus.SenriganFreecamAbility;
import com.sekwah.narutoaddon.capabilities.NinjaCapabilityHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Server-side support for the Senrigan freecam stance: pins the caster's body to its starting
 * position, cancels incoming damage (except void and /kill) and cleans up stale anchors.
 */
@Mod.EventBusSubscriber(modid = "narutoaddon", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SenriganFreecamEvents {

    private SenriganFreecamEvents() {
    }

    /** Last known body position for each caster while the eye is out. */
    private static final Map<UUID, Vec3> ANCHORS = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!(event.player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        serverPlayer.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
            boolean on = ToggleState.isActive(ninjaData, SenriganFreecamAbility.ID);
            if (on) {
                Vec3 anchor = ANCHORS.get(serverPlayer.getUUID());
                if (anchor == null) {
                    anchor = new Vec3(serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ());
                    ANCHORS.put(serverPlayer.getUUID(), anchor);
                }
                serverPlayer.setDeltaMovement(Vec3.ZERO);
                serverPlayer.fallDistance = 0.0F;
                serverPlayer.hurtMarked = true;
                double dx = serverPlayer.getX() - anchor.x;
                double dy = serverPlayer.getY() - anchor.y;
                double dz = serverPlayer.getZ() - anchor.z;
                if (Math.sqrt(dx * dx + dy * dy + dz * dz) > 1.0D) {
                    serverPlayer.teleportTo(anchor.x, anchor.y, anchor.z);
                }
            } else {
                ANCHORS.remove(serverPlayer.getUUID());
            }
        });
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
            if (!ToggleState.isActive(ninjaData, SenriganFreecamAbility.ID)) {
                return;
            }
            // Void damage and /kill keep working.
            String msgId = event.getSource().getMsgId();
            if ("outOfWorld".equals(msgId) || "genericKill".equals(msgId)) {
                return;
            }
            event.setCanceled(true);
        });
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            ANCHORS.remove(serverPlayer.getUUID());
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            ANCHORS.remove(serverPlayer.getUUID());
        }
    }
}