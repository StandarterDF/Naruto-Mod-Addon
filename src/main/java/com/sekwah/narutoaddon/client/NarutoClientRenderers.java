package com.sekwah.narutoaddon.client;

import com.sekwah.narutoaddon.block.NarutoBlocks;
import com.sekwah.narutoaddon.client.renderer.SkyBlockRenderer;
import com.sekwah.narutoaddon.client.sky.SkyBlockSky;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-side renderers and shaders of the addon's own blocks.
 *
 * <p>Kept in its own client-only class so a dedicated server never loads a class that mentions a
 * renderer or shader type.</p>
 */
@Mod.EventBusSubscriber(modid = "narutoaddon", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class NarutoClientRenderers {

    private NarutoClientRenderers() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(NarutoBlocks.SKY_BLOCK_ENTITY.get(), SkyBlockRenderer::new);
    }

    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) {
        SkyBlockSky.onRegisterShaders(event);
    }
}
