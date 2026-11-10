package com.sekwah.narutoaddon.abilities.jutsus;

import com.sekwah.narutoaddon.abilities.Ability;
import com.sekwah.narutoaddon.capabilities.INinjaData;
import com.sekwah.narutoaddon.config.NarutoConfig;
import com.sekwah.narutoaddon.network.PacketHandler;
import com.sekwah.narutoaddon.network.s2c.ClientboundRevealPacket;
import net.minecraftforge.network.PacketDistributor;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.DaylightDetectorBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.DropperBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.ObserverBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.RepeaterBlock;
import net.minecraft.world.level.block.TargetBlock;
import net.minecraft.world.level.block.TripWireHookBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Senrigan Pulse: one sweep of the far eye that lights up containers, spawners and mechanisms
 * through the stone for ten seconds.
 */
public class SenriganPulseAbility extends Ability implements Ability.Cooldown {

    /** B V B B V ... pulse of the far eye. */
    public static final long COMBO = 3112L;

    public static final ResourceLocation ID = new ResourceLocation("narutomod", "senrigan_pulse");

    /** Chakra paid for the pulse. */
    private static final float PULSE_COST = 80.0f;

    /** Cooldown in ticks: fifteen seconds. */
    private static final int COOLDOWN = 20 * 15;

    private static final int CHAKRA_REGEN_DELAY = 20;

    private static final TagKey<Block> FORGE_ORES = BlockTags.create(new ResourceLocation("forge", "ores"));

    /** Structure paths whose display name is provided through lang (naruto.structure.<path>). */
    private static final Set<String> KNOWN_STRUCTURES = Set.of(
            "village_plains", "village_desert", "village_savanna", "village_snowy", "village_taiga",
            "mineshaft", "mineshaft_mesa", "stronghold", "fortress", "bastion", "monument", "mansion",
            "ancient_city", "ruined_portal", "shipwreck", "desert_pyramid", "jungle_temple", "witch_hut",
            "ocean_ruin", "trail_ruins", "end_city", "igloo", "pillager_outpost", "woodland_mansion");

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
    public int getCooldown() {
        return COOLDOWN;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (this.checkCooldown(player, ninjaData, this.getTranslationKey(ninjaData))) {
            return false;
        }
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
        float cost = this.masteryCost(ninjaData, PULSE_COST);
        if (ninjaData.getChakra() < cost) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW))
                            .withStyle(ChatFormatting.RED), true);
            return false;
        }
        ninjaData.useChakra(cost, CHAKRA_REGEN_DELAY);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        ServerLevel level = serverPlayer.serverLevel();
        BlockPos center = serverPlayer.blockPosition();

        List<ClientboundRevealPacket.Marker> mechanisms = new ArrayList<>();
        List<ClientboundRevealPacket.Marker> ores = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-32, -32, -32), center.offset(32, 32, 32))) {
            if (center.distSqr(pos) > 32 * 32) {
                continue;
            }
            boolean target = false;
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof Container || be instanceof SpawnerBlockEntity) {
                target = true;
            } else {
                BlockState state = level.getBlockState(pos);
                Block block = state.getBlock();
                if (block instanceof LeverBlock || block instanceof ButtonBlock
                        || block instanceof PressurePlateBlock || block instanceof TripWireHookBlock
                        || block instanceof PistonBaseBlock || block instanceof PistonHeadBlock
                        || block instanceof DispenserBlock || block instanceof DropperBlock
                        || block instanceof ObserverBlock || block instanceof HopperBlock
                        || block instanceof RedstoneLampBlock || block instanceof ComparatorBlock
                        || block instanceof RepeaterBlock || block instanceof DaylightDetectorBlock
                        || block instanceof TargetBlock) {
                    target = true;
                }
            }
            if (target) {
                mechanisms.add(new ClientboundRevealPacket.Marker(pos.immutable(), ClientboundRevealPacket.KIND_MECHANISM));
                continue;
            }
            BlockState state = level.getBlockState(pos);
            Block block = state.getBlock();
            if (state.is(FORGE_ORES) || block == Blocks.ANCIENT_DEBRIS) {
                ores.add(new ClientboundRevealPacket.Marker(pos.immutable(), (byte) oreKind(block)));
            }
        }

        List<ClientboundRevealPacket.Marker> markers = new ArrayList<>(mechanisms.size() + ores.size());
        markers.addAll(mechanisms);
        markers.addAll(ores);
        if (markers.size() > 1024) {
            markers = new ArrayList<>(markers.subList(0, 1024));
        }

        if (!markers.isEmpty()) {
            PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                    new ClientboundRevealPacket(markers, 20 * 10));
        }

        level.playSound(null, center, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8f, 1.6f);

        for (int i = 0; i < 24; i++) {
            double angle = i * Math.PI / 12.0;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            level.sendParticles(ParticleTypes.END_ROD,
                    serverPlayer.getX() + cos * 2.0d,
                    serverPlayer.getY() + 1.0d,
                    serverPlayer.getZ() + sin * 2.0d,
                    1, 0.0d, 0.05d, 0.0d, 0.0d);
        }

        serverPlayer.displayClientMessage(Component.translatable("jutsu.senrigan.pulse"), true);

        // Structures nearby: chat message listing up to five unique structures within 5x5 chunks.
        sendStructures(serverPlayer, level);
    }

    /** Maps an ore block to its reveal kind so the colour tells the caster what it is. */
    private static int oreKind(Block block) {
        if (block == Blocks.COAL_ORE || block == Blocks.DEEPSLATE_COAL_ORE) {
            return ClientboundRevealPacket.KIND_ORE_COAL;
        }
        if (block == Blocks.IRON_ORE || block == Blocks.DEEPSLATE_IRON_ORE) {
            return ClientboundRevealPacket.KIND_ORE_IRON;
        }
        if (block == Blocks.COPPER_ORE || block == Blocks.DEEPSLATE_COPPER_ORE) {
            return ClientboundRevealPacket.KIND_ORE_COPPER;
        }
        if (block == Blocks.GOLD_ORE || block == Blocks.DEEPSLATE_GOLD_ORE || block == Blocks.NETHER_GOLD_ORE) {
            return ClientboundRevealPacket.KIND_ORE_GOLD;
        }
        if (block == Blocks.REDSTONE_ORE || block == Blocks.DEEPSLATE_REDSTONE_ORE) {
            return ClientboundRevealPacket.KIND_ORE_REDSTONE;
        }
        if (block == Blocks.LAPIS_ORE || block == Blocks.DEEPSLATE_LAPIS_ORE) {
            return ClientboundRevealPacket.KIND_ORE_LAPIS;
        }
        if (block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE) {
            return ClientboundRevealPacket.KIND_ORE_DIAMOND;
        }
        if (block == Blocks.EMERALD_ORE || block == Blocks.DEEPSLATE_EMERALD_ORE) {
            return ClientboundRevealPacket.KIND_ORE_EMERALD;
        }
        if (block == Blocks.NETHER_QUARTZ_ORE) {
            return ClientboundRevealPacket.KIND_ORE_QUARTZ;
        }
        if (block == Blocks.ANCIENT_DEBRIS) {
            return ClientboundRevealPacket.KIND_ORE_DEBRIS;
        }
        return ClientboundRevealPacket.KIND_ORE_OTHER;
    }

    private static void sendStructures(ServerPlayer serverPlayer, ServerLevel level) {
        ChunkPos centerChunk = serverPlayer.chunkPosition();
        BlockPos origin = serverPlayer.blockPosition();
        Map<String, StructureEntry> byPath = new HashMap<>();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                int cx = centerChunk.x + dx;
                int cz = centerChunk.z + dz;
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) {
                    continue;
                }
                for (Map.Entry<Structure, StructureStart> entry : chunk.getAllStarts().entrySet()) {
                    Structure structure = entry.getKey();
                    StructureStart start = entry.getValue();
                    if (start == null || !start.isValid()) {
                        continue;
                    }
                    ResourceLocation key = level.registryAccess()
                            .registryOrThrow(Registries.STRUCTURE).getKey(structure);
                    if (key == null) {
                        continue;
                    }
                    String path = key.getPath();
                    BlockPos centerPos = start.getBoundingBox().getCenter();
                    double dist = Math.sqrt(origin.distSqr(centerPos));
                    String dirSuffix = directionSuffix(centerPos.getX() - origin.getX(),
                            centerPos.getZ() - origin.getZ());
                    StructureEntry candidate = new StructureEntry(path, dist, dirSuffix);
                    StructureEntry existing = byPath.get(path);
                    if (existing == null || dist < existing.dist) {
                        byPath.put(path, candidate);
                    }
                }
            }
        }
        if (byPath.isEmpty()) {
            return;
        }
        List<StructureEntry> entries = new ArrayList<>(byPath.values());
        entries.sort(Comparator.comparingDouble(e -> e.dist));
        if (entries.size() > 5) {
            entries = new ArrayList<>(entries.subList(0, 5));
        }

        net.minecraft.network.chat.MutableComponent body = net.minecraft.network.chat.Component.empty();
        boolean first = true;
        for (StructureEntry e : entries) {
            if (!first) {
                body.append(net.minecraft.network.chat.Component.literal(", "));
            }
            first = false;
            net.minecraft.network.chat.Component nameComponent = KNOWN_STRUCTURES.contains(e.path)
                    ? net.minecraft.network.chat.Component.translatable("naruto.structure." + e.path)
                    : net.minecraft.network.chat.Component.literal(formatPath(e.path));
            int meters = (int) Math.round(e.dist);
            net.minecraft.network.chat.Component entryComp = net.minecraft.network.chat.Component.translatable(
                    "jutsu.senrigan.pulse.structure.entry",
                    nameComponent,
                    net.minecraft.network.chat.Component.translatable("jutsu.senrigan.pulse.distance", meters),
                    net.minecraft.network.chat.Component.translatable("jutsu.senrigan.pulse.dir." + e.dirSuffix));
            body.append(entryComp);
        }
        serverPlayer.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                "jutsu.senrigan.pulse.structures", body));
    }

    private static String directionSuffix(int dx, int dz) {
        double angle = Math.toDegrees(Math.atan2(dx, -dz));
        if (angle < 0) {
            angle += 360.0;
        }
        int oct = (int) Math.round(angle / 45.0) % 8;
        return switch (oct) {
            case 0 -> "n";
            case 1 -> "ne";
            case 2 -> "e";
            case 3 -> "se";
            case 4 -> "s";
            case 5 -> "sw";
            case 6 -> "w";
            case 7 -> "nw";
            default -> "n";
        };
    }

    private static String formatPath(String path) {
        String s = path.replace('_', ' ');
        if (s.isEmpty()) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private record StructureEntry(String path, double dist, String dirSuffix) {
    }
}
