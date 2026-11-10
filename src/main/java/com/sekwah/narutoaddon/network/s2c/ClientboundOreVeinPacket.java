package com.sekwah.narutoaddon.network.s2c;

import com.sekwah.narutoaddon.client.ClientOreSenseData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Hands the client the list of blocks that make up a Byakugan sonar vein, plus how long to draw them.
 */
public class ClientboundOreVeinPacket {

    private final List<BlockPos> positions;
    private final int durationTicks;

    public ClientboundOreVeinPacket(List<BlockPos> positions) {
        this(positions, 200);
    }

    public ClientboundOreVeinPacket(List<BlockPos> positions, int durationTicks) {
        this.positions = positions;
        this.durationTicks = Math.max(1, durationTicks);
    }

    public List<BlockPos> positions() {
        return this.positions;
    }

    public int durationTicks() {
        return this.durationTicks;
    }

    public static void encode(ClientboundOreVeinPacket msg, FriendlyByteBuf buffer) {
        buffer.writeVarInt(msg.positions.size());
        for (BlockPos pos : msg.positions) {
            buffer.writeBlockPos(pos);
        }
        buffer.writeVarInt(msg.durationTicks);
    }

    public static ClientboundOreVeinPacket decode(FriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        List<BlockPos> positions = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            positions.add(buffer.readBlockPos());
        }
        return new ClientboundOreVeinPacket(positions, buffer.readVarInt());
    }

    public static class Handler {
        public static void handle(ClientboundOreVeinPacket msg, Supplier<NetworkEvent.Context> ctx) {
            NetworkEvent.Context context = ctx.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> ClientOreSenseData.applyVein(msg.positions, msg.durationTicks)));
            context.setPacketHandled(true);
        }
    }
}