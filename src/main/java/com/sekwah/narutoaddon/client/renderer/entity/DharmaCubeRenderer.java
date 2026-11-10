package com.sekwah.narutoaddon.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sekwah.narutoaddon.entity.jutsuprojectile.DharmaCubeEntity;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Draws the dharma cube as what it is: a two-by-two-by-two block of black stone falling out of the
 * sky.
 *
 * <p>The renderer used to be deliberately empty - the cube was meant to be a server-side projectile
 * only - so in game the technique looked like an invisible hit somewhere ahead of the caster. The
 * body is now built from eight ordinary black-concrete blocks at their real size, which keeps the
 * cube sharp, lit by the level at every corner and blocked by geometry exactly where the hitbox is.
 * </p>
 */
public class DharmaCubeRenderer extends EntityRenderer<DharmaCubeEntity> {

    /** The stuff of the cube: solid black, so it reads as a hole in the world rather than as rock. */
    private static final BlockState BODY = Blocks.BLACK_CONCRETE.defaultBlockState();

    /** How many blocks across one side of the cube; the entity hitbox is 2x2 blocks. */
    private static final int SIDE = 2;

    private final BlockRenderDispatcher blockRenderer;

    public DharmaCubeRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.blockRenderer = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(DharmaCubeEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight) {
        Level level = entity.level();
        // The entity is centred on its column and stands on its own feet, so the block grid starts
        // one block to the -x and -z side of the centre.
        double originX = entity.getX() - SIDE * 0.5d;
        double originZ = entity.getZ() - SIDE * 0.5d;
        double originY = entity.getY();

        poseStack.pushPose();
        poseStack.translate(-SIDE * 0.5d, 0.0d, -SIDE * 0.5d);
        for (int x = 0; x < SIDE; x++) {
            for (int y = 0; y < SIDE; y++) {
                for (int z = 0; z < SIDE; z++) {
                    // Each of the eight blocks is lit on its own: a cube lit as a single sample came
                    // out flat, and a black cube without corners reads as a screen smudge.
                    int light = LevelRenderer.getLightColor(level, BlockPos.containing(
                            originX + x, originY + y, originZ + z));
                    poseStack.pushPose();
                    poseStack.translate(x, y, z);
                    this.blockRenderer.renderSingleBlock(BODY, poseStack, buffers, light,
                            OverlayTexture.NO_OVERLAY);
                    poseStack.popPose();
                }
            }
        }
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(DharmaCubeEntity entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
