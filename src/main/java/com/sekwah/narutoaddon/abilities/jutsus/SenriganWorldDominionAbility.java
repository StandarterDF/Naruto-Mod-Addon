package com.sekwah.narutoaddon.abilities.jutsus;

import com.sekwah.narutoaddon.abilities.Ability;
import com.sekwah.narutoaddon.abilities.jutsus.SenriganAbility;
import com.sekwah.narutoaddon.capabilities.INinjaData;
import com.sekwah.narutoaddon.config.NarutoConfig;
import com.sekwah.narutoaddon.network.PacketHandler;
import com.sekwah.narutoaddon.network.s2c.ClientboundWorldDominionPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.PacketDistributor;

/**
 * Senrigan: Dominion of the World. Opens a menu that bends the hour and the weather at a heavy
 * chakra price, usable only while the Senrigan is open.
 */
public class SenriganWorldDominionAbility extends Ability {

    /** B V B B B - the far eye reaching out over the world. */
    public static final long COMBO = 3321L;

    public static final ResourceLocation ID = new ResourceLocation("narutomod", "senrigan_world");

    /** Chakra paid to open the dominion menu. */
    private static final float MENU_COST = 20.0f;

    /** Public cost helper used by the server-side apply path, since {@link #masteryCost} is protected. */
    public static float worldCost(INinjaData ninjaData, boolean weather) {
        return new SenriganWorldDominionAbility().masteryCost(ninjaData, weather ? 500.0f : 300.0f);
    }

    @Override
    public int noiseRadius() {
        return 0;
    }

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public long defaultCombo() {
        return COMBO;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (!NarutoConfig.senriganEnabled) {
            player.displayClientMessage(Component.translatable("jutsu.fail.senrigan.disabled")
                    .withStyle(ChatFormatting.RED), true);
            return false;
        }
        if (!SenriganAbility.isActive(ninjaData)) {
            player.displayClientMessage(Component.translatable("jutsu.fail.senrigan.required",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW))
                            .withStyle(ChatFormatting.RED), true);
            return false;
        }
        float cost = this.masteryCost(ninjaData, MENU_COST);
        if (ninjaData.getChakra() < cost) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW))
                            .withStyle(ChatFormatting.RED), true);
            return false;
        }
        ninjaData.useChakra(cost, 20);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        if (player instanceof ServerPlayer sp) {
            PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> sp), new ClientboundWorldDominionPacket());
        }
    }
}