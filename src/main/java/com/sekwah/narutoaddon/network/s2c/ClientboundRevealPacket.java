package com.sekwah.narutoaddon.network.s2c;

import com.sekwah.narutoaddon.client.ClientRevealData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Hands the caster of an eye technique the blocks the server found behind the stone.
 *
 * <p>The Byakugan sees what the client cannot: a hollow chamber, a lake under a mountain, a slab of
 * lava, a pressure plate on the other side of a wall. The scan runs on the server because the chunks
 * may not even be loaded on the client, and only the result crosses over. The client owns the fade,
 * exactly like the ore pulse it is modelled on.</p>
 */
public class ClientboundRevealPacket {

    /** Air pockets and cave mouths. */
    public static final byte KIND_VOID = 0;
    /** Water behind the stone. */
    public static final byte KIND_WATER = 1;
    /** Lava behind the stone. */
    public static final byte KIND_LAVA = 2;
    /** Plates, tripwires, charges, spawners and containers. */
    public static final byte KIND_MECHANISM = 3;
    public static final byte KIND_ORE_COAL = 4;
    public static final byte KIND_ORE_IRON = 5;
    public static final byte KIND_ORE_COPPER = 6;
    public static final byte KIND_ORE_GOLD = 7;
    public static final byte KIND_ORE_REDSTONE = 8;
    public static final byte KIND_ORE_LAPIS = 9;
    public static final byte KIND_ORE_DIAMOND = 10;
    public static final byte KIND_ORE_EMERALD = 11;
    public static final byte KIND_ORE_QUARTZ = 12;
    public static final byte KIND_ORE_DEBRIS = 13;
    public static final byte KIND_ORE_OTHER = 14;

    private final List<Marker> markers;
    private final int duration;

    public ClientboundRevealPacket(List<Marker> markers, int duration) {
        this.markers = markers;
        this.duration = Math.max(1, duration);
    }

    public List<Marker> markers() {
        return this.markers;
    }

    public int duration() {
        return this.duration;
    }

    public static void encode(ClientboundRevealPacket msg, FriendlyByteBuf buffer) {
        buffer.writeVarInt(msg.markers.size());
        for (Marker marker : msg.markers) {
            buffer.writeBlockPos(marker.pos());
            buffer.writeByte(marker.kind());
        }
        buffer.writeVarInt(msg.duration);
    }

    public static ClientboundRevealPacket decode(FriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        List<Marker> markers = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            markers.add(new Marker(buffer.readBlockPos(), buffer.readByte()));
        }
        return new ClientboundRevealPacket(markers, buffer.readVarInt());
    }

    public static class Handler {
        public static void handle(ClientboundRevealPacket msg, Supplier<NetworkEvent.Context> ctx) {
            NetworkEvent.Context context = ctx.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> ClientRevealData.apply(msg.markers, msg.duration)));
            context.setPacketHandled(true);
        }
    }

    /** One block the eye found, and which colour it should be drawn in. */
    public record Marker(BlockPos pos, byte kind) {
    }
}
