package com.sekwah.narutoaddon.mixin.client;

import com.sekwah.narutoaddon.abilities.Ability;
import com.sekwah.narutoaddon.abilities.ComboGroup;
import com.sekwah.narutoaddon.abilities.NarutoAbilities;
import com.sekwah.narutoaddon.network.PacketHandler;
import com.sekwah.narutoaddon.network.c2s.ServerAbilityActivatePacket;
import com.sekwah.narutoaddon.network.c2s.ServerAbilityChannelPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Turns the base mod's single-ability combos into modifier groups.
 *
 * <p>The base key handler resolves one ability per combo and then either casts it (a tap) or drives
 * a channel (held key). The addon registers one channeled carrier per group, so the handler always
 * comes here; the actual technique is chosen by the modifier: plain press, shift held, or the last
 * key kept down.</p>
 */
@Mixin(com.sekwah.narutomod.abilities.NarutoAbilities.class)
public class ComboModifierMixin {

    /** A tap that the base handler resolved before the hold threshold: click or shift technique. */
    @Inject(method = "triggerAbility(J)Z", at = @At("HEAD"), cancellable = true, remap = false)
    private static void narutoaddon$trigger(long combo, CallbackInfoReturnable<Boolean> ci) {
        if (ComboGroup.byCombo(combo) == null) {
            return;
        }
        ResourceLocation id = ComboGroup.resolve(combo, modifier(), data());
        if (id != null) {
            sendTap(id);
        }
        ci.setReturnValue(true);
    }

    /** Hold start, hold release and the tap that never became a hold all arrive here. */
    @Inject(method = "handleCharging(JLcom/sekwah/narutomod/network/c2s/ServerAbilityChannelPacket$ChannelStatus;)Z",
            at = @At("HEAD"), cancellable = true, remap = false)
    private static void narutoaddon$channel(long combo,
                                            com.sekwah.narutomod.network.c2s.ServerAbilityChannelPacket.ChannelStatus status,
                                            CallbackInfoReturnable<Boolean> ci) {
        if (ComboGroup.byCombo(combo) == null) {
            return;
        }
        switch (status) {
            case START -> {
                ResourceLocation id = ComboGroup.resolve(combo, ComboGroup.Modifier.HOLD, data());
                if (id != null && isChanneled(id)) {
                    PacketHandler.sendToServer(new ServerAbilityChannelPacket(id,
                            ServerAbilityChannelPacket.ChannelStatus.START));
                } else if (id != null) {
                    // A held technique that is not a channel (the final will) is a toggle: the hold
                    // switches it on, and the release below leaves it running.
                    PacketHandler.sendToServer(new ServerAbilityActivatePacket(id));
                }
            }
            case STOP -> {
                ResourceLocation id = ComboGroup.resolve(combo, ComboGroup.Modifier.HOLD, data());
                if (id != null) {
                    PacketHandler.sendToServer(new ServerAbilityChannelPacket(id,
                            ServerAbilityChannelPacket.ChannelStatus.STOP));
                }
            }
            case MIN_ACTIVATE -> {
                ResourceLocation id = ComboGroup.resolve(combo, modifier(), data());
                if (id != null) {
                    sendTap(id);
                }
            }
        }
        ci.setReturnValue(true);
    }

    /**
     * Sends a tap. An instant or toggle technique goes through the activate packet; a channeled one
     * has no instant form at all, so it is sent as a below-minimum channel instead - otherwise the
     * server silently ignored the cast and the technique did nothing.
     */
    private static void sendTap(ResourceLocation id) {
        if (isChanneled(id)) {
            PacketHandler.sendToServer(new ServerAbilityChannelPacket(id,
                    ServerAbilityChannelPacket.ChannelStatus.MIN_ACTIVATE));
        } else {
            PacketHandler.sendToServer(new ServerAbilityActivatePacket(id));
        }
    }

    private static ComboGroup.Modifier modifier() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player != null && minecraft.player.isShiftKeyDown()
                ? ComboGroup.Modifier.SHIFT
                : ComboGroup.Modifier.CLICK;
    }

    private static com.sekwah.narutoaddon.capabilities.INinjaData data() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player == null ? null
                : minecraft.player.getCapability(
                        com.sekwah.narutoaddon.capabilities.NinjaCapabilityHandler.NINJA_DATA)
                        .orElse(null);
    }

    private static boolean isChanneled(ResourceLocation id) {
        Ability ability = NarutoAbilities.byId(id);
        return ability != null && ability.activationType() == Ability.ActivationType.CHANNELED;
    }
}
