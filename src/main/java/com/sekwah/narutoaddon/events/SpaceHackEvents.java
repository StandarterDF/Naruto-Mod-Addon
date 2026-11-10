package com.sekwah.narutoaddon.events;

import com.sekwah.narutoaddon.abilities.jutsus.DharmaEyeAbility;
import com.sekwah.narutoaddon.capabilities.INinjaData;
import com.sekwah.narutoaddon.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutoaddon.skills.SkillTree;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * "Space Hack" passive (Dharma branch): lets the caster open a container that sits directly
 * underneath a solid block, while the Dharma Eye is open.
 */
@Mod.EventBusSubscriber(modid = "narutoaddon", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SpaceHackEvents {

    private SpaceHackEvents() {
    }

    @SubscribeEvent
    public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        if (level.isClientSide()) {
            return;
        }
        BlockPos up = event.getPos().above();
        BlockState upState = level.getBlockState(up);
        if (upState.isAir() || !upState.isSolidRender(level, up)) {
            return;
        }
        Player player = event.getEntity();
        INinjaData data = player.getCapability(NinjaCapabilityHandler.NINJA_DATA).resolve().orElse(null);
        if (data == null) {
            return;
        }
        if (!SkillTree.hasUnlock(data.getSkillData().getUnlocked(), "mechanic:space_hack")) {
            return;
        }
        if (!DharmaEyeAbility.isActive(data)) {
            return;
        }
        MenuProvider provider = level.getBlockState(event.getPos()).getMenuProvider(level, event.getPos());
        if (provider != null) {
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.openMenu(provider);
            }
            event.setCanceled(true);
            event.setUseBlock(Event.Result.DENY);
        }
    }
}
