package com.sekwah.narutoaddon.events;

import com.sekwah.narutoaddon.abilities.jutsus.ByakuganAbility;
import com.sekwah.narutoaddon.capabilities.INinjaData;
import com.sekwah.narutoaddon.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutoaddon.skills.SkillTree;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * When an open Byakugan holder with the surgical extraction node digs a block
 * bare-handed, the block drops as a single whole item instead of being mined
 * normally (no tool wear, no experience).
 */
@Mod.EventBusSubscriber(modid = "narutoaddon", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SurgicalExtractionEvents {

    private SurgicalExtractionEvents() {
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        if (player == null || player.level().isClientSide()) {
            return;
        }
        if (!player.getMainHandItem().isEmpty()) {
            return;
        }
        player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(data -> {
            if (!ByakuganAbility.isActive(data)) {
                return;
            }
            if (!SkillTree.hasUnlock(data.getSkillData().getUnlocked(), "mechanic:surgical_extraction")) {
                return;
            }
            LevelAccessor level = event.getLevel();
            if (!(level instanceof ServerLevel serverLevel)) {
                return;
            }
            BlockPos pos = event.getPos();
            BlockState state = event.getState();
            if (state.hasBlockEntity() || state.isAir()) {
                return;
            }
            Block block = state.getBlock();
            if (block == Blocks.BEDROCK || block == Blocks.BARRIER || block == Blocks.COMMAND_BLOCK
                    || block == Blocks.END_PORTAL_FRAME || block == Blocks.END_PORTAL
                    || block == Blocks.NETHER_PORTAL) {
                return;
            }
            event.setCanceled(true);
            level.levelEvent(2001, pos, Block.getId(state));
            level.removeBlock(pos, false);
            ItemStack drop = new ItemStack(state.getBlock().asItem());
            if (!drop.isEmpty()) {
                Block.popResource(serverLevel, pos, drop);
            }
        });
    }
}