package com.sekwah.narutoaddon.mixin.common;

import com.sekwah.narutoaddon.abilities.jutsus.SenriganAbility;
import com.sekwah.narutoaddon.capabilities.INinjaData;
import com.sekwah.narutoaddon.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutoaddon.config.NarutoConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Passive effect of the Senrigan stance "Luck's Gaze": grants Fortune III to
 * block drops while the stance is open.
 */
@Mixin(Block.class)
public class SenriganFortuneMixin {

    @Inject(method = "getDrops(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)Ljava/util/List;",
            at = @At("HEAD"), cancellable = true)
    private static void narutoaddon$fortune(BlockState state, ServerLevel level, BlockPos pos,
                                            BlockEntity blockEntity, Entity entity, ItemStack tool,
                                            CallbackInfoReturnable<List<ItemStack>> cir) {
        if (!(entity instanceof ServerPlayer player)) return;
        if (!NarutoConfig.senriganEnabled) return;
        if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BLOCK_FORTUNE, tool) >= 3) return;
        INinjaData ninjaData = player.getCapability(NinjaCapabilityHandler.NINJA_DATA).resolve().orElse(null);
        if (ninjaData == null || !SenriganAbility.isActive(ninjaData)) return;
        ItemStack lucky = tool.copy();
        lucky.enchant(Enchantments.BLOCK_FORTUNE, 3);
        cir.setReturnValue(Block.getDrops(state, level, pos, blockEntity, player, lucky));
    }
}