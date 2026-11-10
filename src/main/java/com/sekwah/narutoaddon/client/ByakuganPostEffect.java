package com.sekwah.narutoaddon.client;

import com.sekwah.narutoaddon.abilities.jutsus.ByakuganAbility;
import com.sekwah.narutoaddon.capabilities.INinjaData;
import com.sekwah.narutoaddon.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutoaddon.config.NarutoConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Puts the world behind the Byakugan's cold, washed-out grade while the eye is open.
 *
 * <p>The grade is a vanilla {@code color_convolve} post chain shipped by the mod, so no custom GLSL
 * is compiled and a missing or broken shader only costs the tint: {@code GameRenderer.loadEffect}
 * logs the failure instead of throwing. The chain is loaded on the tick the eye opens and shut down
 * on the tick it closes, which keeps the cost off every other frame.</p>
 *
 * <p>Only the level goes through the chain - the HUD is drawn after post-processing and therefore
 * keeps its colours, which is what lets the veins and the chakra readouts stay legible.</p>
 */
@Mod.EventBusSubscriber(modid = "narutoaddon", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ByakuganPostEffect {

    private static final ResourceLocation EFFECT =
            new ResourceLocation("narutoaddon", "shaders/post/byakugan.json");

    /** Whether this class is the one that loaded the current effect. */
    private static boolean loaded;

    private ByakuganPostEffect() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            if (loaded) {
                minecraft.gameRenderer.shutdownEffect();
                loaded = false;
            }
            return;
        }
        // Ask the eye itself rather than the sensing snapshot: the snapshot is refreshed by another
        // tick listener and this one may run before it, which would delay both loading and closing
        // by a tick.
        if (eyeOpen(minecraft.player) == loaded) {
            return;
        }
        if (!loaded) {
            minecraft.gameRenderer.loadEffect(EFFECT);
            loaded = true;
        } else {
            // The effect was ours: nothing else put it there while the eye was open. Shutting it
            // down also restores the ordinary creeper/spider behaviour on the next camera change.
            minecraft.gameRenderer.shutdownEffect();
            loaded = false;
        }
    }

    private static boolean eyeOpen(Player player) {
        if (!NarutoConfig.byakuganScreenFx) {
            return false;
        }
        INinjaData data = player.getCapability(NinjaCapabilityHandler.NINJA_DATA).resolve().orElse(null);
        return data != null && ByakuganAbility.isActive(data);
    }
}
