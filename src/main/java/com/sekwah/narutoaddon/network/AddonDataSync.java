package com.sekwah.narutoaddon.network;

import com.sekwah.narutoaddon.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutoaddon.network.s2c.ClientboundAddonDataPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

import java.util.WeakHashMap;
import java.util.Map;

/**
 * Keeps the client's addon data in sync with the server.
 *
 * <p>The sekclib capability sync does not cover our capability: {@code NinjaCapabilityHandler}
 * registers {@code AddonNinjaData.class}, but sekclib only copies fields annotated with {@code @Sync},
 * and those fields live in the nested {@code NinjaData} rather than in {@code AddonNinjaData} itself.
 * Because of that, addon state (skill tree unlocks, skill points, disabled abilities, senin mana pool,
 * ...) is pushed manually: an NBT snapshot is sent on demand and as a delta every ten ticks.</p>
 */
public class AddonDataSync {

    private static final Map<ServerPlayer, CompoundTag> LAST_SENT = new WeakHashMap<>();
    private static final Map<ServerPlayer, Integer> TICK_COUNTER = new WeakHashMap<>();

    /**
     * Sends a full NBT snapshot of the player's addon data to the client right away.
     */
    public static void send(ServerPlayer player) {
        player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(data -> {
            CompoundTag tag = (CompoundTag) data.serializeSyncNBT();
            PacketHandler.sendToPlayer(new ClientboundAddonDataPacket(tag), player);
            LAST_SENT.put(player, tag.copy());
        });
    }

    /**
     * Periodic delta tick: bumps the per-player counter and, every tenth tick, re-serializes the
     * addon data and sends it only when it changed since the last send.
     */
    public static void tick(ServerPlayer player) {
        int counter = TICK_COUNTER.getOrDefault(player, 0) + 1;
        TICK_COUNTER.put(player, counter);
        if (counter % 10 != 0) {
            return;
        }
        player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(data -> {
            CompoundTag tag = (CompoundTag) data.serializeSyncNBT();
            CompoundTag last = LAST_SENT.get(player);
            if (last == null || !last.equals(tag)) {
                PacketHandler.sendToPlayer(new ClientboundAddonDataPacket(tag), player);
                LAST_SENT.put(player, tag.copy());
            }
        });
    }
}