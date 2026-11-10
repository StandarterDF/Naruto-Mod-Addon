package com.sekwah.narutoaddon.mixin.common;

import net.minecraftforge.network.NetworkRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Predicate;

/**
 * Accepts any protocol version on the base mod's packet channel.
 *
 * <p>In a dev build the base mod reports its version as {@code 0.0NONE} (see
 * {@code com.sekwah.narutomod.network.PacketHandler#PROTOCOL_VERSION}), while the client announces
 * the same channel as {@code ABSENT}. The stock version check then rejects the channel on both
 * sides, logging "Client attempted to connect with a different version of the mod" for every
 * player that joins, and every packet sent on the base channel - including the base "Become a
 * ninja" screen and the base combo casts - is silently dropped.</p>
 *
 * <p>The message protocol itself did not change between those versions, so the version gate only
 * gets in the way in this dev setup. This mixin relaxes both {@code serverAcceptedVersions} and
 * {@code clientAcceptedVersions} predicates to accept any version string. It touches only the
 * channel builder calls inside {@code PacketHandler}'s static initializer; the base mod, its
 * packet payloads and the addon's own network code are left untouched.</p>
 */
@Mixin(com.sekwah.narutomod.network.PacketHandler.class)
public class BasePacketChannelVersionMixin {

    @Redirect(method = "<clinit>",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraftforge/network/NetworkRegistry$ChannelBuilder;serverAcceptedVersions(Ljava/util/function/Predicate;)Lnet/minecraftforge/network/NetworkRegistry$ChannelBuilder;"),
            remap = false)
    private static NetworkRegistry.ChannelBuilder narutoaddon$acceptAnyServerVersion(NetworkRegistry.ChannelBuilder builder, Predicate<String> ignored) {
        return builder.serverAcceptedVersions(version -> true);
    }

    @Redirect(method = "<clinit>",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraftforge/network/NetworkRegistry$ChannelBuilder;clientAcceptedVersions(Ljava/util/function/Predicate;)Lnet/minecraftforge/network/NetworkRegistry$ChannelBuilder;"),
            remap = false)
    private static NetworkRegistry.ChannelBuilder narutoaddon$acceptAnyClientVersion(NetworkRegistry.ChannelBuilder builder, Predicate<String> ignored) {
        return builder.clientAcceptedVersions(version -> true);
    }
}