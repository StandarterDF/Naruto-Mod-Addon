package com.sekwah.narutoaddon.events;

import com.sekwah.narutoaddon.abilities.jutsus.DimensionStepAbility;
import com.sekwah.narutoaddon.abilities.jutsus.SewingPocketAbility;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.EndGatewayBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.EndPortalBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The consequences of the dharma sewing techniques: a portal that has been sewn shut, a door that
 * has been sewn to its frame, and a caster whose steps the world does not hear.
 *
 * <p>Blocks cannot carry data the way an entity can, so the two block techniques keep their state in
 * the server's memory: the position of every seam, what was there before it was sewn, and when the
 * seam lets go. The sweeps here are the other half of those casts - the techniques write, this class
 * restores, expires and refuses. Nothing is persisted on purpose: a seal is a moment, not a change to
 * the world, and losing the table on shutdown only means a portal keeps whatever state the save has.</p>
 *
 * <p>The silent step belongs here for the same reason: it is a tag the game event dispatcher reads,
 * and the tag is only true while the eye that pays for it is open.</p>
 */
@Mod.EventBusSubscriber(modid = "narutoaddon", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SewingEvents {

    /** How close an ender pearl has to land to a sewn portal before the journey is refused. */
    private static final double PEARL_REFUSAL_RADIUS = 4.0d;

    /** Seals and locks are checked once a second; none of them has to expire within a tick. */
    private static final int SWEEP_INTERVAL = 20;

    /** Sewn portals, with what stood there and when the opening comes back. */
    private static final Map<BlockKey, Sealed> SEALED_PORTALS = new HashMap<>();

    /** Locks on doors, gates and lids: who may still work them, and for how long. */
    private static final Map<BlockKey, Lock> LOCKS = new HashMap<>();

    private static int sweepCounter;

    private SewingEvents() {
    }

    // ---------------------------------------------------------------------------------------------
    // What the techniques call
    // ---------------------------------------------------------------------------------------------

    /** @return whether this block is one of the seams a portal is cut along. */
    public static boolean isPortal(BlockState state) {
        Block block = state.getBlock();
        return block instanceof NetherPortalBlock
                || block instanceof EndPortalBlock
                || block instanceof EndGatewayBlock;
    }

    /** @return whether this block is something the sewing lock can hold shut. */
    public static boolean isLockable(BlockState state) {
        Block block = state.getBlock();
        return block instanceof DoorBlock
                || block instanceof TrapDoorBlock
                || block instanceof FenceGateBlock
                || block instanceof ChestBlock
                || block instanceof BarrelBlock;
    }

    /**
     * Takes every portal block within {@code radius} of {@code center} out of the world and remembers
     * what to put back. The frame is left alone: only the opening is sewn.
     *
     * @return the positions that were sewn, for the cast's own effects
     */
    public static List<BlockPos> sealPortals(ServerLevel level, BlockPos center, int radius, long until) {
        List<BlockPos> sewn = new ArrayList<>();
        BlockPos min = center.offset(-radius, -radius, -radius);
        BlockPos max = center.offset(radius, radius, radius);
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (level.isOutsideBuildHeight(pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (!isPortal(state)) {
                continue;
            }
            BlockPos immutable = pos.immutable();
            SEALED_PORTALS.put(key(level, immutable), new Sealed(state, until));
            level.setBlock(immutable, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            sewn.add(immutable);
        }
        return sewn;
    }

    /** Holds a block shut for everyone but {@code owner} until {@code until}. */
    public static void lock(ServerLevel level, BlockPos pos, UUID owner, long until) {
        LOCKS.put(key(level, pos), new Lock(owner, until));
    }

    /**
     * Holds a block shut, including the other half of a two-part block (a door's other half or a
     * double chest's neighbour), so the whole thing is sewn at once. Use this from the cast site so
     * the second-half logic lives in one place.
     *
     * <p>The seam is refreshed by calling this again with a later {@code until}: that is how the
     * sewing lock keeps a thing shut for as long as its caster holds the technique.</p>
     */
    public static void lock(ServerLevel level, BlockPos pos, BlockState state, UUID owner, long until) {
        for (BlockPos part : parts(level, pos, state)) {
            lock(level, part, owner, until);
        }
    }

    /** @return whether anything is sewn at this position, by anyone, and the seam has not expired. */
    public static boolean hasLock(ServerLevel level, BlockPos pos, BlockState state) {
        long now = level.getGameTime();
        for (BlockPos part : parts(level, pos, state)) {
            Lock lock = LOCKS.get(key(level, part));
            if (lock != null && lock.until() > now) {
                return true;
            }
        }
        return false;
    }

    /**
     * Cuts the seam off a block and its other half, but only if {@code owner} made it: a caster can
     * undo their own stitch, not somebody else's.
     *
     * @return true when at least one part of the thing was sewn by this owner and is now free
     */
    public static boolean unlock(ServerLevel level, BlockPos pos, BlockState state, UUID owner) {
        boolean removed = false;
        for (BlockPos part : parts(level, pos, state)) {
            BlockKey key = key(level, part);
            Lock lock = LOCKS.get(key);
            if (lock != null && lock.owner().equals(owner)) {
                LOCKS.remove(key);
                removed = true;
            }
        }
        return removed;
    }

    /**
     * The positions a single sewn thing occupies: a door's two halves, a double chest's two halves.
     * Everything that writes or reads a seam goes through this, so a lock and its removal can never
     * disagree about which blocks belong together.
     */
    private static List<BlockPos> parts(ServerLevel level, BlockPos pos, BlockState state) {
        List<BlockPos> parts = new ArrayList<>(2);
        parts.add(pos);
        Block block = state.getBlock();
        if (block instanceof DoorBlock) {
            Direction other = state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER
                    ? Direction.UP
                    : Direction.DOWN;
            parts.add(pos.relative(other));
        } else if (block instanceof ChestBlock) {
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos neighbour = pos.relative(direction);
                if (level.getBlockState(neighbour).getBlock() == block) {
                    parts.add(neighbour);
                    break;
                }
            }
        }
        return parts;
    }

    // ---------------------------------------------------------------------------------------------
    // Restoration and expiry
    // ---------------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (++sweepCounter < SWEEP_INTERVAL) {
            return;
        }
        sweepCounter = 0;
        MinecraftServer server = event.getServer();
        restorePortals(server);
        expireLocks(server);
    }

    /**
     * Puts every expired portal back. A chunk that is not loaded is left for a later sweep rather
     * than written into, and a position somebody has built over is not overwritten: the seal takes
     * the opening away, it does not claim the space.
     */
    private static void restorePortals(MinecraftServer server) {
        Iterator<Map.Entry<BlockKey, Sealed>> entries = SEALED_PORTALS.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<BlockKey, Sealed> entry = entries.next();
            BlockKey key = entry.getKey();
            ServerLevel level = server.getLevel(key.dimension());
            if (level == null) {
                entries.remove();
                continue;
            }
            if (level.getGameTime() < entry.getValue().until()) {
                continue;
            }
            if (!level.isLoaded(key.pos())) {
                continue;
            }
            if (level.getBlockState(key.pos()).isAir()) {
                level.setBlock(key.pos(), entry.getValue().state(), Block.UPDATE_ALL);
            }
            entries.remove();
        }
    }

    /** Drops the locks whose time is up, one level at a time so each keeps its own clock. */
    private static void expireLocks(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            long now = level.getGameTime();
            LOCKS.entrySet().removeIf(entry ->
                    entry.getKey().dimension().equals(level.dimension()) && entry.getValue().until() <= now);
        }
    }

    /**
     * Restores the seams before the server saves, so a restart does not leave a permanently open
     * frame behind. Only the shutdown path gets this; a crash loses the table and the portal blocks
     * with it, which is the accepted cost of keeping seals in memory.
     */
    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        MinecraftServer server = event.getServer();
        for (Map.Entry<BlockKey, Sealed> entry : SEALED_PORTALS.entrySet()) {
            BlockKey key = entry.getKey();
            ServerLevel level = server.getLevel(key.dimension());
            if (level == null || !level.isLoaded(key.pos())) {
                continue;
            }
            if (level.getBlockState(key.pos()).isAir()) {
                level.setBlock(key.pos(), entry.getValue().state(), Block.UPDATE_ALL);
            }
        }
        SEALED_PORTALS.clear();
        LOCKS.clear();
        sweepCounter = 0;
    }

    // ---------------------------------------------------------------------------------------------
    // Pearls and locks
    // ---------------------------------------------------------------------------------------------

    /**
     * A pearl thrown at a sewn portal is refused, which is the only part of the seal a caster can feel
     * directly: the seam is shut for travel as well as for walking.
     *
     * <p>This is the dimension-change path only; a pearl that never leaves its own level is handled
     * by vanilla's own teleport and is not seen here.</p>
     */
    @SubscribeEvent
    public static void onTravelToDimension(EntityTravelToDimensionEvent event) {
        if (!(event.getEntity() instanceof ThrownEnderpearl pearl)
                || !(pearl.level() instanceof ServerLevel level)) {
            return;
        }
        if (isSealedNear(level, pearl.position(), PEARL_REFUSAL_RADIUS)) {
            event.setCanceled(true);
        }
    }

    /** @return whether a live seal stands within {@code radius} of the given point. */
    private static boolean isSealedNear(ServerLevel level, Vec3 position, double radius) {
        long now = level.getGameTime();
        double limit = radius * radius;
        for (Map.Entry<BlockKey, Sealed> entry : SEALED_PORTALS.entrySet()) {
            if (!entry.getKey().dimension().equals(level.dimension()) || entry.getValue().until() <= now) {
                continue;
            }
            if (Vec3.atCenterOf(entry.getKey().pos()).distanceToSqr(position) <= limit) {
                return true;
            }
        }
        return false;
    }

    /**
     * Refuses the interaction when the block under the player's hand is sewn shut and somebody else
     * did the sewing. The owner is read from the lock rather than from the player, so the same rule
     * holds after a relog.
     */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        BlockKey key = key(level, event.getPos());
        Lock lock = LOCKS.get(key);
        if (lock == null) {
            return;
        }
        if (lock.until() <= level.getGameTime()) {
            LOCKS.remove(key);
            return;
        }
        Player player = event.getEntity();
        if (lock.owner().equals(player.getUUID())) {
            return;
        }
        event.setCanceled(true);
        player.displayClientMessage(Component.translatable("jutsu.fail.sewing_lock.held"), true);
    }

    // ---------------------------------------------------------------------------------------------
    // Silent step
    // ---------------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) {
            return;
        }
        tickSewingPocket(event.player);
    }

    /**
     * Closes the sewing pocket when its time is up. The menu id recorded by the cast is compared
     * first, so a container the caster opened in the meantime is left alone.
     */
    private static void tickSewingPocket(Player player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(SewingPocketAbility.OPEN_UNTIL_TAG)) {
            return;
        }
        if (player.level().getGameTime() < data.getLong(SewingPocketAbility.OPEN_UNTIL_TAG)) {
            return;
        }
        int menuId = data.getInt(SewingPocketAbility.OPEN_MENU_TAG);
        data.remove(SewingPocketAbility.OPEN_UNTIL_TAG);
        data.remove(SewingPocketAbility.OPEN_MENU_TAG);
        if (player.containerMenu != player.inventoryMenu && player.containerMenu.containerId == menuId) {
            player.closeContainer();
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Dharma cube death handling
    // ---------------------------------------------------------------------------------------------
    @SubscribeEvent
    public static void onDharmaCubeDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!player.level().dimension().equals(DimensionStepAbility.DHARMA_DIMENSION)) {
            return;
        }
        CompoundTag data = player.getPersistentData();
        ServerLevel level = (ServerLevel) player.level();
        ServerLevel destination = level.getServer() == null ? null : level.getServer().overworld();
        double x = 0.0d;
        double y = 0.0d;
        double z = 0.0d;
        float yaw = 0.0f;
        float pitch = 0.0f;
        if (data.contains(DimensionStepAbility.RETURN_KEY)) {
            CompoundTag ret = data.getCompound(DimensionStepAbility.RETURN_KEY);
            ResourceLocation dimLoc = ResourceLocation.tryParse(ret.getString("dim"));
            if (dimLoc != null && level.getServer() != null) {
                ServerLevel resolved = level.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, dimLoc));
                if (resolved != null) {
                    destination = resolved;
                }
            }
            x = ret.getDouble("x");
            y = ret.getDouble("y");
            z = ret.getDouble("z");
            yaw = ret.getFloat("yaw");
            pitch = ret.getFloat("pitch");
            data.remove(DimensionStepAbility.RETURN_KEY);
        } else if (destination != null) {
            x = destination.getSharedSpawnPos().getX() + 0.5d;
            y = destination.getSharedSpawnPos().getY();
            z = destination.getSharedSpawnPos().getZ() + 0.5d;
        }
        if (destination == null) {
            return;
        }
        event.setCanceled(true);
        player.setHealth(player.getMaxHealth());
        player.teleportTo(destination, x, y, z, yaw, pitch);
        player.fallDistance = 0.0f;
    }

    // ---------------------------------------------------------------------------------------------
    // Keys and values
    // ---------------------------------------------------------------------------------------------

    private static BlockKey key(ServerLevel level, BlockPos pos) {
        return new BlockKey(level.dimension(), pos.immutable());
    }

    /** A block is only meaningful together with its dimension; the same coordinates exist in every one. */
    private record BlockKey(ResourceKey<Level> dimension, BlockPos pos) {
    }

    /** An opening that was taken away, and the tick it is due back on. */
    private record Sealed(BlockState state, long until) {
    }

    /** Who sewed a block shut, and the tick the seam lets go. */
    private record Lock(UUID owner, long until) {
    }
}
