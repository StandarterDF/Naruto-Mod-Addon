package com.sekwah.narutoaddon.client.events;

import com.sekwah.narutoaddon.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutoaddon.item.interfaces.IShouldHideNameplate;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = "narutoaddon", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class RenderEvents {

    /**
     * {@link net.minecraftforge.client.event.RenderPlayerEvent}
     *
     * @param event
     */
    @SubscribeEvent
    public static void playerRenderEvent(RenderPlayerEvent.Pre event) {
        event.getEntity().getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
            if(ninjaData.getInvisible()) {
                event.setCanceled(true);
            }
        });
        //event.setCanceled(true);
    }

    @SubscribeEvent
    public static void renderNameplateEvent(RenderNameTagEvent event) {
        if (event.getResult() != Event.Result.DENY) {
            Entity entity = event.getEntity();
            if (entity instanceof Player player) {
                ItemStack itemStack = player.getItemBySlot(EquipmentSlot.HEAD);
                Item item = itemStack.getItem();
                // Two interfaces answer the same question: the addon's own copy, used by the pieces it
                // registers (headbands), and the base mod's, implemented by the armour it registers
                // (the ANBU masks among them). Checking only ours left every base mask showing the
                // wearer's name.
                boolean hideNameplate = item instanceof IShouldHideNameplate own && own.shouldHideNameplate(entity);
                if (!hideNameplate
                        && item instanceof com.sekwah.narutomod.item.interfaces.IShouldHideNameplate base
                        && base.shouldHideNameplate(entity)) {
                    hideNameplate = true;
                }
                if (hideNameplate) {
                    event.setResult(Event.Result.DENY);
                }
            }
        }
    }

}
