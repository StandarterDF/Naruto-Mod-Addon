package com.sekwah.narutoaddon.network.s2c;

import com.sekwah.narutoaddon.capabilities.NinjaCapabilityHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ClientboundAddonDataPacket {

    private final CompoundTag data;

    public ClientboundAddonDataPacket(CompoundTag data) {
        this.data = data;
    }

    public static void encode(ClientboundAddonDataPacket msg, FriendlyByteBuf buffer) {
        buffer.writeNbt(msg.data);
    }

    public static ClientboundAddonDataPacket decode(FriendlyByteBuf buffer) {
        CompoundTag tag = buffer.readNbt();
        return new ClientboundAddonDataPacket(tag == null ? new CompoundTag() : tag);
    }

    public static class Handler {
        public static void handle(ClientboundAddonDataPacket msg, Supplier<NetworkEvent.Context> ctx) {
            NetworkEvent.Context context = ctx.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                Player player = net.minecraft.client.Minecraft.getInstance().player;
                if (player == null) return;
                player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(data -> {
                    data.deserializeSyncNBT(msg.data);
                });
            }));
            context.setPacketHandled(true);
        }
    }
}