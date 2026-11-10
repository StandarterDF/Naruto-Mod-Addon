package com.sekwah.narutoaddon.abilities.jutsus;

import com.sekwah.narutoaddon.abilities.Ability;
import com.sekwah.narutoaddon.abilities.jutsus.SenriganAbility;
import com.sekwah.narutoaddon.capabilities.INinjaData;
import com.sekwah.narutoaddon.config.NarutoConfig;
import com.sekwah.narutoaddon.entity.ChakraMarkerEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Senrigan Deathstep: steps straight to the marker left at the place of death and consumes it.
 */
public class SenriganDeathstepAbility extends Ability implements Ability.Cooldown {

    /** B V V B ... the step back to the mark. */
    public static final long COMBO = 3112L;

    public static final ResourceLocation ID = new ResourceLocation("narutomod", "senrigan_deathstep");

    /** Cooldown in ticks: thirty seconds. */
    private static final int COOLDOWN = 20 * 30;

    private static final int CHAKRA_REGEN_DELAY = 20;

    private static final double SEARCH_RADIUS = 2.0d;

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
        if (ninjaData.getMarkerLoc() == null) {
            player.displayClientMessage(Component.translatable("jutsu.fail.nomarker",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW))
                            .withStyle(ChatFormatting.RED), true);
            return false;
        }
        ResourceLocation markerDim = ninjaData.getMarkerDimension();
        if (markerDim == null || !markerDim.equals(player.level().dimension().location())) {
            player.displayClientMessage(Component.translatable("jutsu.fail.wrongdimension",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW))
                            .withStyle(ChatFormatting.RED), true);
            return false;
        }
        float cost = this.masteryCost(ninjaData, 40.0f);
        if (ninjaData.getChakra() < cost) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
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
        Vec3 target = ninjaData.getMarkerLoc();
        ResourceLocation dim = ninjaData.getMarkerDimension();
        if (target == null || dim == null) {
            return;
        }
        ServerLevel targetLevel = this.markerLevel(serverPlayer, dim);
        if (targetLevel == null) {
            return;
        }
        // Force-load the chunk holding the marker so its entity is available
        // even when the player (and thus the chunk) is far away.
        ChunkPos chunkPos = new ChunkPos(BlockPos.containing(target));
        targetLevel.getChunk(chunkPos.x, chunkPos.z);
        // Consume the marker log at the recorded spot.
        this.discardOwnedMarkers(targetLevel, target, SEARCH_RADIUS, serverPlayer);
        // Teleport to the mark, crossing dimensions when needed.
        serverPlayer.teleportTo(targetLevel, target.x, target.y, target.z, serverPlayer.getYRot(), serverPlayer.getXRot());

        // Insurance pass: discard any marker that loaded only after teleport.
        this.discardOwnedMarkers(targetLevel, target, 4.0d, serverPlayer);

        ninjaData.setMarkerLoc(null, null);

        ServerLevel level = serverPlayer.serverLevel();
        level.sendParticles(ParticleTypes.PORTAL, serverPlayer.getX(), serverPlayer.getY() + 1.0d, serverPlayer.getZ(),
                32, 0.4d, 0.8d, 0.4d, 0.1d);
        level.playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8f, 1.2f);
        serverPlayer.displayClientMessage(Component.translatable("jutsu.senrigan.deathstep"), true);
    }

    private void discardOwnedMarkers(ServerLevel level, Vec3 center, double radius, ServerPlayer owner) {
        for (ChakraMarkerEntity marker : level.getEntitiesOfClass(ChakraMarkerEntity.class,
                new AABB(center, center).inflate(radius))) {
            if (marker.isOwnedBy(owner)) {
                marker.discard();
            }
        }
    }

    private ServerLevel markerLevel(Player player, ResourceLocation dimension) {
        if (dimension == null || !(player.level() instanceof ServerLevel level)) {
            return null;
        }
        return level.getServer() == null ? null : level.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, dimension));
    }
}