package com.sekwah.narutoaddon.events;

import com.sekwah.narutoaddon.abilities.jutsus.SenriganAbility;
import com.sekwah.narutoaddon.capabilities.INinjaData;
import com.sekwah.narutoaddon.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutoaddon.config.NarutoConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * Passive Senrigan perk: "Merchant Sympathy".
 *
 * <p>While the Senrigan is open, villagers read the caster's intent and offer their goods at the
 * best possible price. This is implemented by clamping every offer's effective cost A to a single
 * item of its base currency whenever the player interacts with a villager, so the trade screen
 * that {@code mobInteract} opens afterwards shows the discounted prices. The interaction itself is
 * never cancelled: the villager still trades normally, only the numbers change.</p>
 *
 * <p>The discount is applied through {@link MerchantOffer#addToSpecialPriceDiff(int)} rather than by
 * rewriting the offer, so the underlying trades and their demand/stock state stay intact and the
 * original prices are restored on the next restock cycle.</p>
 */
@Mod.EventBusSubscriber(modid = "narutoaddon", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SenriganTradeEvents {

    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(event.getTarget() instanceof AbstractVillager villager)) return;
        if (!NarutoConfig.senriganEnabled) return;

        INinjaData ninjaData = player.getCapability(NinjaCapabilityHandler.NINJA_DATA).resolve().orElse(null);
        if (ninjaData == null || !SenriganAbility.isActive(ninjaData)) return;

        MerchantOffers offers = villager.getOffers();
        if (offers.isEmpty()) return;

        for (MerchantOffer offer : offers) {
            ItemStack base = offer.getBaseCostA();
            if (base.isEmpty()) continue;
            int target = 1;
            int delta = target - base.getCount() - offer.getSpecialPriceDiff();
            if (delta != 0) {
                offer.addToSpecialPriceDiff(delta);
            }
        }
    }
}