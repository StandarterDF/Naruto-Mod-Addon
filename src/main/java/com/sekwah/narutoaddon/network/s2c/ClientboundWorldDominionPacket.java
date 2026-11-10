package com.sekwah.narutoaddon.network.s2c;

import com.sekwah.narutoaddon.client.gui.WorldDominionScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Empty server to client signal that opens the Dominion of the World screen.
 */
public class ClientboundWorldDominionPacket {

    public ClientboundWorldDominionPacket() {
    }

    public ClientboundWorldDominionPacket(FriendlyByteBuf buf) {
        // empty signal
    }

    public void toBytes(FriendlyByteBuf buf) {
        // empty signal
    }

    public static void encode(ClientboundWorldDominionPacket msg, FriendlyByteBuf buffer) {
        msg.toBytes(buffer);
    }

    public static ClientboundWorldDominionPacket decode(FriendlyByteBuf buffer) {
        return new ClientboundWorldDominionPacket(buffer);
    }

    public static class Handler {
        public static void handle(ClientboundWorldDominionPacket msg, Supplier<NetworkEvent.Context> ctx) {
            NetworkEvent.Context context = ctx.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                Minecraft.getInstance().setScreen(new WorldDominionScreen());
            }));
            context.setPacketHandled(true);
        }
    }
}