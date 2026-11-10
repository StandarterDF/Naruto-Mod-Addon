package com.sekwah.narutoaddon.network.c2s;

import com.sekwah.narutoaddon.events.SenriganWorldDominionEvents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client to server request carrying one of the Dominion of the World actions (a time of day or a
 * weather state). The server re-checks the eye and the chakra cost before applying anything.
 * <p>
 * {@code value} holds the target day time in ticks for time actions (0 for weather actions).
 */
public class ServerWorldDominionPacket {

    private final byte action;
    private final int value;

    public ServerWorldDominionPacket(byte action) {
        this(action, 0);
    }

    public ServerWorldDominionPacket(byte action, int value) {
        this.action = action;
        this.value = value;
    }

    /** Decoding constructor, matching the {@link #toBytes(FriendlyByteBuf)} writer. */
    public ServerWorldDominionPacket(FriendlyByteBuf buf) {
        this.action = buf.readByte();
        this.value = buf.readInt();
    }

    /** Writes this packet's payload into the buffer. */
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeByte(this.action);
        buf.writeInt(this.value);
    }

    public byte action() {
        return this.action;
    }

    public int value() {
        return this.value;
    }

    public static void encode(ServerWorldDominionPacket msg, FriendlyByteBuf buffer) {
        msg.toBytes(buffer);
    }

    public static ServerWorldDominionPacket decode(FriendlyByteBuf buffer) {
        return new ServerWorldDominionPacket(buffer);
    }

    public static class Handler {
        public static void handle(ServerWorldDominionPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player != null) {
                    SenriganWorldDominionEvents.apply(player, msg.action(), msg.value());
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
}
