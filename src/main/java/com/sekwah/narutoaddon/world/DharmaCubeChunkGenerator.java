package com.sekwah.narutoaddon.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sekwah.narutoaddon.block.NarutoBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * The terrain of the Dharma pocket dimension: a sealed cube three chunks on a side.
 *
 * <p>The room is a grass floor under a sky that never ends: the shell is the sky block itself - it
 * is unbreakable, so one layer both seals the room and shows the sky - and the floor is grass over a
 * few layers of dirt. The sky is a block because the room is sealed: there is nothing above it to
 * look at, so the walls carry the picture.</p>
 */
public class DharmaCubeChunkGenerator extends ChunkGenerator {

    public static final Codec<DharmaCubeChunkGenerator> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(DharmaCubeChunkGenerator::getBiomeSource)
    ).apply(instance, DharmaCubeChunkGenerator::new));

    /** Inclusive bounds of the cube, three chunks (48 blocks) on each horizontal axis. */
    public static final int MIN_X = 0;
    public static final int MAX_X = 47;
    public static final int MIN_Z = 0;
    public static final int MAX_Z = 47;
    public static final int FLOOR_Y = 63;
    public static final int CEIL_Y = 110;

    /** How many dirt layers sit under the grass before the obsidian plug. */
    private static final int DIRT_DEPTH = 3;

    private static final int GEN_DEPTH = 256;
    private static final int MIN_Y = 0;

    public DharmaCubeChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    public static boolean inBounds(int x, int z) {
        return x >= MIN_X && x <= MAX_X && z >= MIN_Z && z <= MAX_Z;
    }

    public static int surfaceY(int x, int z) {
        return inBounds(x, z) ? FLOOR_Y + 1 : MIN_Y;
    }

    @Override
    protected Codec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor, Blender blender, RandomState random,
                                                        StructureManager structureManager, ChunkAccess chunk) {
        return CompletableFuture.supplyAsync(() -> {
            Heightmap oceanFloor = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
            Heightmap worldSurface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
            int minX = chunk.getPos().getMinBlockX();
            int minZ = chunk.getPos().getMinBlockZ();
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    int worldX = minX + x;
                    int worldZ = minZ + z;
                    if (!inBounds(worldX, worldZ)) {
                        continue;
                    }
                    for (int y = FLOOR_Y - DIRT_DEPTH - 1; y <= CEIL_Y; y++) {
                        BlockState state = stateAt(worldX, y, worldZ);
                        if (state == null) {
                            continue;
                        }
                        BlockPos pos = new BlockPos(worldX, y, worldZ);
                        chunk.setBlockState(pos, state, false);
                        // Worldgen writes states only: a block entity has to be added by hand, or
                        // the sky block stays a hole until it is broken and placed again.
                        if (state.getBlock() instanceof net.minecraft.world.level.block.EntityBlock entityBlock) {
                            net.minecraft.world.level.block.entity.BlockEntity blockEntity =
                                    entityBlock.newBlockEntity(pos, state);
                            if (blockEntity != null) {
                                chunk.setBlockEntity(blockEntity);
                            }
                        }
                        if (y == FLOOR_Y) {
                            oceanFloor.update(x, y, z, state);
                        }
                        if (y == CEIL_Y) {
                            worldSurface.update(x, y, z, state);
                        }
                    }
                }
            }
            return chunk;
        }, executor);
    }

    /**
     * The block at a position of the cube, or null for air.
     *
     * <p>The shell itself is the sky block: it is unbreakable, so one layer does both jobs - it
     * cannot be dug through, and every face turned into the room shows the sky. Inside it: a grass
     * floor over three layers of dirt, and air everywhere else.</p>
     */
    private static BlockState stateAt(int x, int y, int z) {
        if (!inBounds(x, z) || y < FLOOR_Y - DIRT_DEPTH - 1 || y > CEIL_Y) {
            return null;
        }
        boolean shell = x == MIN_X || x == MAX_X || z == MIN_Z || z == MAX_Z
                || y == CEIL_Y || y == FLOOR_Y - DIRT_DEPTH - 1;
        if (shell) {
            return NarutoBlocks.SKY_BLOCK.get().defaultBlockState();
        }
        if (y == FLOOR_Y) {
            return Blocks.GRASS_BLOCK.defaultBlockState();
        }
        if (y < FLOOR_Y) {
            return Blocks.DIRT.defaultBlockState();
        }
        return null;
    }

    @Override
    public void applyCarvers(WorldGenRegion level, long seed, RandomState random, BiomeManager biomeManager,
                             StructureManager structureManager, ChunkAccess chunk, GenerationStep.Carving step) {
    }

    @Override
    public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structureManager) {
    }

    @Override
    public void buildSurface(WorldGenRegion level, StructureManager structureManager, RandomState random,
                             ChunkAccess chunk) {
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion level) {
    }

    @Override
    public int getGenDepth() {
        return GEN_DEPTH;
    }

    @Override
    public int getSeaLevel() {
        return MIN_Y;
    }

    @Override
    public int getMinY() {
        return MIN_Y;
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState random) {
        return surfaceY(x, z);
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState random) {
        BlockState[] states = new BlockState[GEN_DEPTH];
        java.util.Arrays.fill(states, Blocks.AIR.defaultBlockState());
        if (inBounds(x, z)) {
            for (int y = FLOOR_Y - DIRT_DEPTH - 1; y <= CEIL_Y; y++) {
                BlockState state = stateAt(x, y, z);
                if (state != null) {
                    states[y - MIN_Y] = state;
                }
            }
        }
        return new NoiseColumn(MIN_Y, states);
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState random, BlockPos pos) {
        info.add("Dharma cube: " + (inBounds(pos.getX(), pos.getZ()) ? "inside" : "outside"));
    }
}
