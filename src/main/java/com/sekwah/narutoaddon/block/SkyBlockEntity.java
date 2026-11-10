package com.sekwah.narutoaddon.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Marker entity for {@link SkyBlock}: the renderer needs a block entity to be called at all, it
 * carries no data of its own.
 */
public class SkyBlockEntity extends BlockEntity {

    public SkyBlockEntity(BlockPos pos, BlockState state) {
        super(NarutoBlocks.SKY_BLOCK_ENTITY.get(), pos, state);
    }
}
