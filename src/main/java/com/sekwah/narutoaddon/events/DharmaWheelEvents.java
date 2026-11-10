package com.sekwah.narutoaddon.events;

import com.sekwah.narutoaddon.capabilities.INinjaData;
import com.sekwah.narutoaddon.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutoaddon.capabilities.dharma.DharmaWheel;
import com.sekwah.narutoaddon.skills.SkillTree;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.FORGE;

/**
 * Server-side handling for the "Wheel of Dharma" passive: adapt to a damage type after the first hit.
 */
@Mod.EventBusSubscriber(modid = "narutoaddon", bus = FORGE)
public final class DharmaWheelEvents {

    private DharmaWheelEvents() {
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof Player p)) {
            return;
        }
        INinjaData data = p.getCapability(NinjaCapabilityHandler.NINJA_DATA).resolve().orElse(null);
        if (data == null) {
            return;
        }
        if (!SkillTree.hasUnlock(data.getSkillData().getUnlocked(), "mechanic:dharma_wheel")) {
            return;
        }
        DharmaWheel.recordHit(p, event.getSource());
        if (DharmaWheel.isAdapted(p, event.getSource())) {
            event.setAmount(event.getAmount() * 0.5f);
            if (p.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.ENCHANT, p.getX(),
                        p.getY() + p.getBbHeight() * 0.5d, p.getZ(),
                        2, 0.2d, 0.2d, 0.2d, 0.05d);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) {
            return;
        }
        DharmaWheel.copy(event.getOriginal(), event.getEntity());
    }
}
