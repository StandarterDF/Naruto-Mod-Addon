package com.sekwah.narutoaddon.events;

import com.sekwah.narutoaddon.capabilities.size.EntitySize;
import com.sekwah.narutoaddon.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutoaddon.abilities.jutsus.DharmaEyeAbility;
import com.sekwah.narutoaddon.abilities.jutsus.SizeShiftAbility;
import com.sekwah.narutoaddon.network.PacketHandler;
import com.sekwah.narutoaddon.network.s2c.ClientboundSizePacket;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

@Mod.EventBusSubscriber(modid = "narutoaddon", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SizeEvents {

    private SizeEvents() {
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        EntitySize.applyAttributes(player);
        restoreHealthShare(player);
        if (player instanceof ServerPlayer sp) {
            PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> sp),
                    new ClientboundSizePacket(EntitySize.get(sp)));
        }
    }

    /**
     * A caster who logs out enlarged comes back with the same share of the bar: the saved health is
     * read against the normal pool before the size modifier is applied, so without this a five-times
     * body would log in at a fifth of its health.
     */
    private static void restoreHealthShare(Player player) {
        if (EntitySize.get(player) == EntitySize.DEFAULT) {
            return;
        }
        float share = player.getPersistentData().getFloat(EntitySize.HEALTH_SHARE_KEY);
        if (share <= 0.0f) {
            return;
        }
        float max = player.getMaxHealth();
        player.setHealth(Math.max(1.0f, Math.min(max, share * max)));
    }

    /**
     * A different level means a fresh client-side body: the client recomputes its hitbox and its
     * camera eye height from {@code ClientSizeData}, and that cache is only filled by the size packet.
     * Without a resend the server keeps the tall hitbox while the client camera and box snap back to
     * normal size - the "camera breaks after travelling" bug.
     */
    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) {
            return;
        }
        float size = EntitySize.get(sp);
        // Re-applying the same size re-sends the packet and rebuilds the bounding box on both sides.
        EntitySize.set(sp, size);
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        Player original = event.getOriginal();
        Player clone = event.getEntity();
        if (event.isWasDeath() && original.getPersistentData().contains("narutoaddon:size")) {
            clone.getPersistentData().putFloat("narutoaddon:size", original.getPersistentData().getFloat("narutoaddon:size"));
        }
        EntitySize.applyAttributes(clone);
        if (clone instanceof ServerPlayer sp) {
            PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> sp),
                    new ClientboundSizePacket(EntitySize.get(sp)));
        }
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (player.level().isClientSide()) {
            return;
        }
        enforceEye(player);
        if (player.tickCount % 40 != 0) {
            return;
        }
        if (EntitySize.get(player) == EntitySize.DEFAULT) {
            return;
        }
        EntitySize.applyAttributes(player);
    }

    /**
     * The size dial is a Dharma technique, so it may not keep running after the eye that pays for it
     * closes. A hold that outlives its eye is simply dropped, leaving the body at whatever size the
     * dial last set.
     */
    private static void enforceEye(Player player) {
        player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
            if (!SizeShiftAbility.isDialOpen(ninjaData) || DharmaEyeAbility.isActive(ninjaData)) {
                return;
            }
            ninjaData.setCurrentlyChanneledAbility(player, null);
        });
    }
}