package com.sekwah.narutoaddon.events;

import com.sekwah.narutoaddon.abilities.jutsus.SenriganAbility;
import com.sekwah.narutoaddon.capabilities.INinjaData;
import com.sekwah.narutoaddon.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutoaddon.network.PacketHandler;
import com.sekwah.narutoaddon.network.s2c.ClientboundThreatDropsPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Server-side Senrigan threat readout: rolls each nearby mob's loot table into a stable preview and
 * ships the result to every caster that has the eye open.
 */
@Mod.EventBusSubscriber(modid = "narutoaddon", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SenriganThreatEvents {

    /** Cached loot previews, keyed by entity id. Computed once per entity while it stays in range. */
    private static final Map<Integer, List<ItemStack>> PREVIEW = new HashMap<>();

    /** Last preview we sent to each player, keyed by player uuid then entity id. */
    private static final Map<UUID, Map<Integer, List<ItemStack>>> LAST_SENT = new HashMap<>();

    /** Tick counter used to throttle the scan to once every 20 ticks. */
    private static int tickCounter = 0;

    private SenriganThreatEvents() {
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (++tickCounter % 20 != 0) {
            return;
        }

        Set<Integer> seenThisTick = new HashSet<>();
        Map<UUID, Set<Integer>> seenByPlayer = new HashMap<>();

        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            INinjaData ninjaData = player.getCapability(NinjaCapabilityHandler.NINJA_DATA).resolve().orElse(null);
            if (ninjaData == null || !SenriganAbility.isActive(ninjaData)) {
                continue;
            }
            ServerLevel level = player.serverLevel();
            List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class,
                    player.getBoundingBox().inflate(32.0d),
                    entity -> entity != player && entity.isAlive());

            Set<Integer> playerSeen = new HashSet<>();
            seenByPlayer.put(player.getUUID(), playerSeen);

            for (LivingEntity entity : entities) {
                seenThisTick.add(entity.getId());
                playerSeen.add(entity.getId());

                ResourceLocation tableId = entity.getLootTable();
                if (tableId == null) {
                    continue;
                }

                List<ItemStack> preview = PREVIEW.get(entity.getId());
                if (preview == null) {
                    preview = computePreview(level, entity, player);
                    PREVIEW.put(entity.getId(), preview);
                }

                Map<Integer, List<ItemStack>> lastForPlayer = LAST_SENT.computeIfAbsent(player.getUUID(), k -> new HashMap<>());
                List<ItemStack> last = lastForPlayer.get(entity.getId());
                if (last == null || !listsEqual(last, preview)) {
                    lastForPlayer.put(entity.getId(), copyList(preview));
                    PacketHandler.NARUTO_CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                            new ClientboundThreatDropsPacket(entity.getId(), preview));
                }
            }
        }

        // Keep the cache from growing forever: drop previews for entities no one can see anymore.
        PREVIEW.keySet().removeIf(id -> !seenThisTick.contains(id));

        // Trim per-player bookkeeping to entities still in range for that player.
        for (Map.Entry<UUID, Map<Integer, List<ItemStack>>> entry : LAST_SENT.entrySet()) {
            Set<Integer> seen = seenByPlayer.get(entry.getKey());
            if (seen == null) {
                entry.getValue().clear();
            } else {
                entry.getValue().keySet().removeIf(id -> !seen.contains(id));
            }
        }
    }

    private static List<ItemStack> computePreview(ServerLevel level, LivingEntity entity, ServerPlayer player) {
        ResourceLocation tableId = entity.getLootTable();
        if (tableId == null) {
            return List.of();
        }
        LootTable table = level.getServer().getLootData().getLootTable(tableId);
        if (table == null) {
            return List.of();
        }

        // Each run aggregates the items dropped in one "kill"; we track how many of the 8 runs
        // contained each item and the largest quantity seen in a single run.
        List<Stat> stats = new ArrayList<>();

        for (int i = 0; i < 8; i++) {
            LootParams params = new LootParams.Builder(level)
                    .withParameter(LootContextParams.THIS_ENTITY, entity)
                    .withParameter(LootContextParams.ORIGIN, entity.position())
                    .withOptionalParameter(LootContextParams.DAMAGE_SOURCE, level.damageSources().playerAttack(player))
                    .withOptionalParameter(LootContextParams.KILLER_ENTITY, player)
                    .withOptionalParameter(LootContextParams.LAST_DAMAGE_PLAYER, player)
                    .withLuck(0.0F)
                    .create(LootContextParamSets.ENTITY);

            long seed = entity.getId() * 341873128712L + 132897987541L + i;
            if (seed == 0L) {
                seed = 1L;
            }
            LootContext context = new LootContext.Builder(params)
                    .withOptionalRandomSeed(seed)
                    .create(tableId);

            List<ItemStack> runItems = new ArrayList<>();
            table.getRandomItems(context, runItems::add);

            // Aggregate quantities within this single run.
            List<ItemStack> runUnique = new ArrayList<>();
            List<Integer> runCounts = new ArrayList<>();
            for (ItemStack stack : runItems) {
                if (stack == null || stack.isEmpty()) {
                    continue;
                }
                int found = -1;
                for (int j = 0; j < runUnique.size(); j++) {
                    if (ItemStack.isSameItemSameTags(runUnique.get(j), stack)) {
                        found = j;
                        break;
                    }
                }
                if (found < 0) {
                    runUnique.add(stack.copy());
                    runCounts.add(stack.getCount());
                } else {
                    runCounts.set(found, runCounts.get(found) + stack.getCount());
                }
            }

            for (int j = 0; j < runUnique.size(); j++) {
                ItemStack key = runUnique.get(j);
                int qty = runCounts.get(j);
                Stat stat = findStat(stats, key);
                if (stat == null) {
                    stat = new Stat(key.copy());
                    stat.key.setCount(1);
                    stats.add(stat);
                }
                stat.frequency++;
                if (qty > stat.maxCount) {
                    stat.maxCount = qty;
                }
            }
        }

        List<Stat> chosen = new ArrayList<>();
        for (Stat stat : stats) {
            if (stat.frequency >= 4) {
                chosen.add(stat);
            }
        }
        chosen.sort((a, b) -> {
            if (b.frequency != a.frequency) {
                return Integer.compare(b.frequency, a.frequency);
            }
            String na = a.key.getItem().getDescriptionId();
            String nb = b.key.getItem().getDescriptionId();
            return na.compareToIgnoreCase(nb);
        });
        if (chosen.size() > 4) {
            chosen = chosen.subList(0, 4);
        }

        List<ItemStack> result = new ArrayList<>();
        for (Stat stat : chosen) {
            ItemStack out = stat.key.copy();
            int count = stat.maxCount <= 0 ? 1 : stat.maxCount;
            out.setCount(count);
            result.add(out);
        }
        return result;
    }

    private static Stat findStat(List<Stat> stats, ItemStack key) {
        for (Stat stat : stats) {
            if (ItemStack.isSameItemSameTags(stat.key, key)) {
                return stat;
            }
        }
        return null;
    }

    private static boolean listsEqual(List<ItemStack> a, List<ItemStack> b) {
        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            ItemStack sa = a.get(i);
            ItemStack sb = b.get(i);
            if (sa.getCount() != sb.getCount()) {
                return false;
            }
            if (!ItemStack.isSameItemSameTags(sa, sb)) {
                return false;
            }
        }
        return true;
    }

    private static List<ItemStack> copyList(List<ItemStack> in) {
        List<ItemStack> out = new ArrayList<>();
        for (ItemStack s : in) {
            out.add(s.copy());
        }
        return out;
    }

    private static final class Stat {
        final ItemStack key;
        int frequency;
        int maxCount;

        Stat(ItemStack key) {
            this.key = key;
        }
    }
}
