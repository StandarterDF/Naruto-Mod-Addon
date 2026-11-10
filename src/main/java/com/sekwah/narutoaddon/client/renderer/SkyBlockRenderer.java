package com.sekwah.narutoaddon.client.renderer;

import com.sekwah.narutoaddon.block.NarutoBlocks;
import com.sekwah.narutoaddon.block.SkyBlockEntity;
import com.sekwah.narutoaddon.client.sky.SkyBlockSky;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;

/**
 * Draws the sky block as a window onto the real sky.
 *
 * <p>Each exposed face is filled with the addon's projected-sky shader, which samples the frame's
 * off-screen sky through the screen position of every vertex - so the face shows the piece of sky
 * exactly behind it, sun, moon, stars and clouds included, and the picture slides as the camera
 * moves. Faces buried behind another sky block or a solid neighbour are skipped, which keeps a wall
 * at one quad per visible side.</p>
 */
public class SkyBlockRenderer implements BlockEntityRenderer<SkyBlockEntity> {

    public SkyBlockRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(SkyBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        Level level = blockEntity.getLevel();
        if (level == null) {
            return;
        }
        BlockState state = blockEntity.getBlockState();
        if (!state.is(NarutoBlocks.SKY_BLOCK.get())) {
            return;
        }
        RenderType renderType = SkyBlockSky.renderType();
        if (renderType == null) {
            // The shader has not loaded yet: fall back to the vanilla end portal so the wall is
            // never an invisible hole.
            renderType = RenderType.endPortal();
        }
        BlockPos pos = blockEntity.getBlockPos();
        Matrix4f pose = poseStack.last().pose();
        VertexConsumer consumer = buffers.getBuffer(renderType);
        boolean drew = false;

        drew |= renderFace(level, pos, Direction.SOUTH, consumer, pose, 0.0F, 1.0F, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F);
        drew |= renderFace(level, pos, Direction.NORTH, consumer, pose, 0.0F, 1.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
        drew |= renderFace(level, pos, Direction.EAST, consumer, pose, 1.0F, 1.0F, 1.0F, 0.0F, 0.0F, 1.0F, 1.0F, 0.0F);
        drew |= renderFace(level, pos, Direction.WEST, consumer, pose, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 1.0F, 1.0F, 0.0F);
        drew |= renderFace(level, pos, Direction.DOWN, consumer, pose, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 1.0F);
        drew |= renderFace(level, pos, Direction.UP, consumer, pose, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 0.0F, 0.0F);

        if (drew) {
            SkyBlockSky.request();
        }
    }

    private static boolean renderFace(Level level, BlockPos pos, Direction direction, VertexConsumer consumer,
                                      Matrix4f pose, float x1, float x2, float y1, float y2,
                                      float z1, float z2, float z3, float z4) {
        BlockPos neighbour = pos.relative(direction);
        BlockState neighbourState = level.getBlockState(neighbour);
        // Adjacent sky blocks would draw the same picture twice; a solid neighbour (the bedrock
        // shell behind the wall) hides the face completely.
        if (neighbourState.is(NarutoBlocks.SKY_BLOCK.get()) || neighbourState.isSolidRender(level, neighbour)) {
            return false;
        }
        consumer.vertex(pose, x1, y1, z1).endVertex();
        consumer.vertex(pose, x2, y1, z2).endVertex();
        consumer.vertex(pose, x2, y2, z3).endVertex();
        consumer.vertex(pose, x1, y2, z4).endVertex();
        return true;
    }
}
