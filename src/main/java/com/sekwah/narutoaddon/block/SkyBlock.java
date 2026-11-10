package com.sekwah.narutoaddon.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The wall of the Dharma cube: a window into the end sky.
 *
 * <p>It has no model of its own - the drawing is done by {@code SkyBlockRenderer}, which fills the
 * cube with the vanilla end-portal pass. That pass is the only thing in the game that already knows
 * how to draw a parallax starfield, so the block borrows it instead of pretending with a texture.</p>
 */
public class SkyBlock extends Block implements EntityBlock {

    public SkyBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SkyBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }
}
