package com.sekwah.narutoaddon.network.s2c;

import com.sekwah.narutoaddon.client.ClientThreatDrops;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Carries the Senrigan loot preview for a single living entity from the server to the client.
 */
public class ClientboundThreatDropsPacket {

    private final int entityId;
    private final List<ItemStack> stacks;

    public ClientboundThreatDropsPacket(int entityId, List<ItemStack> stacks) {
        this.entityId = entityId;
        this.stacks = stacks == null ? List.of() : new ArrayList<>(stacks);
    }

    public int entityId() {
        return this.entityId;
    }

    public List<ItemStack> stacks() {
        return this.stacks;
    }

    public static void encode(ClientboundThreatDropsPacket msg, FriendlyByteBuf buffer) {
        buffer.writeVarInt(msg.entityId);
        buffer.writeVarInt(msg.stacks.size());
        for (ItemStack stack : msg.stacks) {
            buffer.writeItem(stack);
        }
    }

    public static ClientboundThreatDropsPacket decode(FriendlyByteBuf buffer) {
        int entityId = buffer.readVarInt();
        int count = buffer.readVarInt();
        List<ItemStack> stacks = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            stacks.add(buffer.readItem());
        }
        return new ClientboundThreatDropsPacket(entityId, stacks);
    }

    public static class Handler {
        public static void handle(ClientboundThreatDropsPacket msg, Supplier<NetworkEvent.Context> ctx) {
            NetworkEvent.Context context = ctx.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> ClientThreatDrops.apply(msg.entityId, msg.stacks)));
            context.setPacketHandled(true);
        }
    }
}
