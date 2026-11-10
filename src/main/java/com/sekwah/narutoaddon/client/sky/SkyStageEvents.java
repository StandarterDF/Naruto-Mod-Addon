package com.sekwah.narutoaddon.client.sky;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Forge-bus half of {@link SkyBlockSky}: refreshes the off-screen sky while a sky block is visible.
 */
@Mod.EventBusSubscriber(modid = "narutoaddon", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class SkyStageEvents {

    private SkyStageEvents() {
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        SkyBlockSky.onRenderLevelStage(event);
    }
}
