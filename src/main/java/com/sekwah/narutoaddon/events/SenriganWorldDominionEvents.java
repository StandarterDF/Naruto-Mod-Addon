package com.sekwah.narutoaddon.events;

import com.sekwah.narutoaddon.abilities.NarutoAbilities;
import com.sekwah.narutoaddon.abilities.jutsus.SenriganAbility;
import com.sekwah.narutoaddon.abilities.jutsus.SenriganWorldDominionAbility;
import com.sekwah.narutoaddon.capabilities.INinjaData;
import com.sekwah.narutoaddon.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutoaddon.config.NarutoConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Server-side application of the Dominion of the World actions. Not an event bus subscriber for the
 * application itself, but it does subscribe to the server tick to drive the smooth day-time shift.
 * Utility invoked by {@link com.sekwah.narutoaddon.network.c2s.ServerWorldDominionPacket}.
 */
@Mod.EventBusSubscriber(modid = "narutoaddon", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SenriganWorldDominionEvents {

    private SenriganWorldDominionEvents() {
    }

    /** Pending smooth time shifts, keyed by dimension. Each entry: [remaining ticks, target day time]. */
    private static final Map<ResourceKey<Level>, long[]> TIME_SHIFT = new HashMap<>();

    public static void apply(ServerPlayer player, byte action, int value) {
        if (!NarutoConfig.senriganEnabled) {
            return;
        }
        com.sekwah.narutoaddon.capabilities.INinjaData ninjaData =
                player.getCapability(NinjaCapabilityHandler.NINJA_DATA).resolve().orElse(null);
        if (ninjaData == null) {
            return;
        }
        if (!SenriganAbility.isActive(ninjaData)) {
            return;
        }

        boolean weather = action >= 4;
        float cost = SenriganWorldDominionAbility.worldCost(ninjaData, weather);
        if (ninjaData.getChakra() < cost) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(NarutoAbilities.SENRIGAN_WORLD.get().getTranslationKey(ninjaData))
                            .withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.RED), true);
            return;
        }
        ninjaData.useChakra(cost, 20);
        NarutoAbilities.SENRIGAN_WORLD.get().recordMastery(player, ninjaData);

        ServerLevel level = player.serverLevel();
        String messageKey = null;
        if (action <= 3) {
            long target;
            if (action == 0) {
                target = value;
            } else {
                target = switch (action) {
                    case 1 -> 6000L;
                    case 2 -> 12000L;
                    default -> 18000L;
                };
            }
            target = Math.max(0L, Math.min(23999L, target));
            long current = level.getDayTime();
            long delta = ((target - current) % 24000L + 24000L) % 24000L;
            if (delta <= 0) {
                // Already at the target: nothing to animate.
            } else {
                TIME_SHIFT.put(level.dimension(), new long[] { delta, target });
            }
            messageKey = switch (action) {
                case 1 -> "jutsu.senrigan.world.day";
                case 2 -> "jutsu.senrigan.world.sunset";
                case 3 -> "jutsu.senrigan.world.night";
                default -> null;
            };
        } else {
            switch (action) {
                case 4 -> level.setWeatherParameters(20 * 60 * 10, 0, false, false);
                case 5 -> level.setWeatherParameters(0, 20 * 60 * 5, true, false);
                default -> level.setWeatherParameters(0, 20 * 60 * 5, true, true);
            }
            messageKey = switch (action) {
                case 4 -> "jutsu.senrigan.world.clear";
                case 5 -> "jutsu.senrigan.world.rain";
                default -> "jutsu.senrigan.world.thunder";
            };
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                action == 6 ? SoundEvents.LIGHTNING_BOLT_THUNDER : SoundEvents.UI_BUTTON_CLICK.get(),
                SoundSource.PLAYERS, 1.0f, 1.0f);

        if (messageKey != null) {
            player.displayClientMessage(Component.translatable(messageKey), true);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftServer server = event.getServer();
        Iterator<Map.Entry<ResourceKey<Level>, long[]>> iterator = TIME_SHIFT.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<ResourceKey<Level>, long[]> entry = iterator.next();
            ServerLevel level = server.getLevel(entry.getKey());
            if (level == null) {
                iterator.remove();
                continue;
            }
            long[] data = entry.getValue();
            long target = data[1];
            long current = level.getDayTime();
            long delta = ((target - current) % 24000L + 24000L) % 24000L;
            if (delta <= 0) {
                iterator.remove();
                continue;
            }
            int step = (int) Math.ceil(delta / 100.0);
            if (step < 1) {
                step = 1;
            }
            level.setDayTime(current + step);
            // Vanilla only synchronises the clock once a second, which would make the shift look like
            // a handful of jumps. Push the new time every tick so the sky slides instead.
            server.getPlayerList().broadcastAll(new ClientboundSetTimePacket(level.getGameTime(),
                    level.getDayTime(), level.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT)),
                    level.dimension());
            if (delta <= step) {
                iterator.remove();
            } else {
                data[0] = delta - step;
            }
        }
    }
}
