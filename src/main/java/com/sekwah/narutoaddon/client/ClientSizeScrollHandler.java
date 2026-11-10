package com.sekwah.narutoaddon.client;

import com.sekwah.narutoaddon.abilities.jutsus.SizeShiftAbility;
import com.sekwah.narutoaddon.capabilities.INinjaData;
import com.sekwah.narutoaddon.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutoaddon.network.PacketHandler;
import com.sekwah.narutoaddon.network.c2s.ServerboundSizeScrollPacket;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Swallows the mouse wheel while the size dial is open and turns it into size steps.
 *
 * <p>The dial's state is read from the synced toggle set rather than from a client-side flag: the
 * server is the only place the stance can end (chakra ran out, the eye closed, the caster switched
 * it off), and a local flag would keep eating the wheel after it did.</p>
 */
@Mod.EventBusSubscriber(modid = "narutoaddon", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientSizeScrollHandler {

    private ClientSizeScrollHandler() {
    }

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }
        INinjaData data = minecraft.player.getCapability(NinjaCapabilityHandler.NINJA_DATA).resolve().orElse(null);
        if (!SizeShiftAbility.isDialOpen(data)) {
            return;
        }
        int direction = event.getScrollDelta() > 0.0d ? 1 : (event.getScrollDelta() < 0.0d ? -1 : 0);
        if (direction == 0) {
            return;
        }
        // The dial owns the wheel while it is open: no hotbar switching in the middle of a step.
        event.setCanceled(true);
        PacketHandler.sendToServer(new ServerboundSizeScrollPacket(direction));
    }

    @SubscribeEvent
    public static void onLoggingOut(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
        ClientSizeData.set(1.0f);
    }

    /**
     * Traveling to another level replaces the client-side player object. Its box is rebuilt from
     * {@link ClientSizeData}, so it has to be asked to rebuild as soon as the new body exists -
     * otherwise the camera and the hitbox come back at normal size until the next size change.
     */
    @SubscribeEvent
    public static void onPlayerCloned(net.minecraftforge.client.event.ClientPlayerNetworkEvent.Clone event) {
        if (event.getNewPlayer() != null) {
            event.getNewPlayer().refreshDimensions();
        }
    }
}
