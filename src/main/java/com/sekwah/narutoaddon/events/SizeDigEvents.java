package com.sekwah.narutoaddon.events;

import com.sekwah.narutoaddon.capabilities.size.EntitySize;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/**
 * Widens a single block break into an N-by-N-by-N cube centred on the broken block,
 * but only while the caster is enlarged by the "Dharma: Size" stance (scale greater than 1.0).
 *
 * <p>The cube side follows the caster's scale: 2.0 gives 2x2x2, 3.0 gives 3x3x3, 4.0 gives 4x4x4
 * and 5.0 gives 5x5x5. At scale 1.0 or below nothing changes and a single block is taken, exactly
 * as vanilla. Chakra is not charged here because the size stance already pays for itself every tick.
 * Every block is broken; blocks the held tool cannot harvest are broken without a drop and without
 * wearing the tool down. Blocks carrying a BlockEntity (chests, furnaces, ...), liquid, bedrock,
 * unbreakable blocks, air and blocks outside the build height are left untouched.</p>
 */
@Mod.EventBusSubscriber(modid = "narutoaddon", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SizeDigEvents {

    /** Guards against the extra breaks reporting themselves as new swings. */
    private static boolean breakingExtras;

    private SizeDigEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBlockBroken(BlockEvent.BreakEvent event) {
        if (breakingExtras || event.isCanceled()) {
            return;
        }
        Player player = event.getPlayer();
        if (player == null || player.level().isClientSide() || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        float scale = EntitySize.get(player);
        int side = Math.max(1, Math.min(5, Math.round(scale)));
        if (side <= 1) {
            return;
        }
        List<BlockPos> extras = cube(event.getPos(), side);

        breakingExtras = true;
        try {
            for (BlockPos pos : extras) {
                ItemStack held = player.getMainHandItem();
                if (level.isOutsideBuildHeight(pos)) {
                    continue;
                }
                BlockState state = level.getBlockState(pos);
                if (state.isAir() || state.getBlock() instanceof LiquidBlock
                        || state.getDestroySpeed(level, pos) < 0f || state.hasBlockEntity()
                        || state.is(Blocks.BEDROCK)) {
                    continue;
                }
                boolean keeps = !state.requiresCorrectToolForDrops() || held.isCorrectToolForDrops(state);
                if (level.destroyBlock(pos, keeps, player)) {
                    if (keeps) {
                        held.mineBlock(level, state, pos, player);
                    }
                }
            }
        } finally {
            breakingExtras = false;
        }
    }

    /**
     * The blocks of a cube of the given side around the centre, excluding the centre block itself.
     * Odd side lengths are centred on the origin block; even side lengths lean towards the negative
     * side so the total still matches the requested size.
     */
    private static List<BlockPos> cube(BlockPos centre, int side) {
        List<BlockPos> positions = new ArrayList<>();
        int start;
        int end;
        if (side % 2 == 1) {
            int half = side / 2;
            start = -half;
            end = half;
        } else {
            start = -(side / 2);
            end = (side / 2) - 1;
        }
        for (int a = start; a <= end; a++) {
            for (int b = start; b <= end; b++) {
                for (int c = start; c <= end; c++) {
                    if (a == 0 && b == 0 && c == 0) {
                        continue;
                    }
                    positions.add(centre.offset(a, b, c));
                }
            }
        }
        return positions;
    }
}