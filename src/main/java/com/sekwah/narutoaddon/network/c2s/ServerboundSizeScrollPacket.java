package com.sekwah.narutoaddon.network.c2s;

import com.sekwah.narutoaddon.abilities.jutsus.SizeShiftAbility;
import com.sekwah.narutoaddon.capabilities.INinjaData;
import com.sekwah.narutoaddon.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutoaddon.capabilities.size.EntitySize;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Carries one mouse-wheel step while the caster has the size dial open.
 */
public class ServerboundSizeScrollPacket {

    private final int direction;

    public ServerboundSizeScrollPacket(int direction) {
        this.direction = direction;
    }

    public static void encode(ServerboundSizeScrollPacket msg, FriendlyByteBuf buffer) {
        buffer.writeInt(msg.direction);
    }

    public static ServerboundSizeScrollPacket decode(FriendlyByteBuf buffer) {
        return new ServerboundSizeScrollPacket(buffer.readInt());
    }

    public static class Handler {
        public static void handle(ServerboundSizeScrollPacket msg, Supplier<NetworkEvent.Context> ctx) {
            NetworkEvent.Context context = ctx.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null) {
                    return;
                }
                player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
                    // The dial is a channel, so the wheel only works while it is actually held open.
                    if (!SizeShiftAbility.isDialOpen(ninjaData)) {
                        return;
                    }
                    int dir = msg.direction > 0 ? 1 : (msg.direction < 0 ? -1 : 0);
                    if (dir != 0) {
                        EntitySize.set(player, EntitySize.clamp(EntitySize.get(player) + dir * 0.25f));
                    }
                });
            });
            context.setPacketHandled(true);
        }
    }
}
