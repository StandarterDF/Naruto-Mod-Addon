package com.sekwah.narutoaddon.events;

import com.sekwah.narutoaddon.capabilities.INinjaData;
import com.sekwah.narutoaddon.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutoaddon.config.NarutoConfig;
import com.sekwah.narutoaddon.entity.ChakraMarkerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Leaves a chakra marker log behind wherever a caster with the Senrigan Deathmark mode dies, so the
 * deathstep technique can walk them back to that spot.
 */
@Mod.EventBusSubscriber(modid = "narutoaddon", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SenriganDeathstepEvents {

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!NarutoConfig.senriganEnabled) {
            return;
        }
        if (!player.getPersistentData().getBoolean("narutomod:senriganDeathmark")) {
            return;
        }
        ServerLevel level = player.serverLevel();
        Vec3 spot = player.position();
        ChakraMarkerEntity marker = new ChakraMarkerEntity(level, player);
        marker.moveTo(spot.x, spot.y, spot.z, player.getYRot(), 0.0f);
        level.addFreshEntity(marker);

        INinjaData ninjaData = player.getCapability(NinjaCapabilityHandler.NINJA_DATA).resolve().orElse(null);
        if (ninjaData != null) {
            ninjaData.setMarkerLoc(spot, level.dimension().location());
        }
        player.getPersistentData().putBoolean("narutomod:senriganDeathmark", false);
    }
}