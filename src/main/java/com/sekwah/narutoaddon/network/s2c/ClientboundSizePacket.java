package com.sekwah.narutoaddon.network.s2c;

import com.sekwah.narutoaddon.client.ClientSizeData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Syncs the local player's size scale from the server to the client.
 */
public class ClientboundSizePacket {

    private final float scale;

    public ClientboundSizePacket(float scale) {
        this.scale = scale;
    }

    public float scale() {
        return this.scale;
    }

    public static void encode(ClientboundSizePacket msg, FriendlyByteBuf buffer) {
        buffer.writeFloat(msg.scale);
    }

    public static ClientboundSizePacket decode(FriendlyByteBuf buffer) {
        return new ClientboundSizePacket(buffer.readFloat());
    }

    public static class Handler {
        public static void handle(ClientboundSizePacket msg, Supplier<NetworkEvent.Context> ctx) {
            float scale = msg.scale;
            NetworkEvent.Context context = ctx.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                ClientSizeData.set(scale);
                // The client computes its own hitbox from this cache, so the box has to be rebuilt
                // the moment the number lands, not on the next pose change.
                net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
                if (minecraft.player != null) {
                    minecraft.player.refreshDimensions();
                }
            }));
            context.setPacketHandled(true);
        }
    }
}