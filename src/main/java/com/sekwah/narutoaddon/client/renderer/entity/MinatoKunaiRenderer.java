package com.sekwah.narutoaddon.client.renderer.entity;

import com.sekwah.narutoaddon.entity.projectile.MinatoKunaiEntity;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class MinatoKunaiRenderer extends ArrowRenderer<MinatoKunaiEntity> {
    // Reuse the base mod's projectile texture instead of shipping a copy of it.
    public static final ResourceLocation RES_ARROW = new ResourceLocation("narutomod", "textures/entity/projectiles/kunai.png");

    public MinatoKunaiRenderer(EntityRendererProvider.Context manager) {
        super(manager);
    }

    @Override
    public ResourceLocation getTextureLocation(MinatoKunaiEntity entity) {
        return RES_ARROW;
    }
}