package com.sekwah.narutoaddon.abilities.jutsus;

import com.sekwah.narutoaddon.abilities.Ability;
import com.sekwah.narutoaddon.capabilities.INinjaData;
import com.sekwah.narutoaddon.config.NarutoConfig;
import com.sekwah.narutoaddon.network.PacketHandler;
import com.sekwah.narutoaddon.network.s2c.ClientboundOreVeinPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;

/**
 * The Byakugan's sonar: a tap on a block releases a warden-like sonic pulse that highlights the
 * nearest ores (forge:ores) around the struck point.
 *
 * <p>Instead of tracing a single connected vein, the server scans the area around the targeted
 * block for ore blocks and hands the client the list, which is drawn for a few seconds and then
 * fades. It costs chakra up front and sits on a short cooldown.</p>
 */
public class ByakuganSonarAbility extends Ability implements Ability.Cooldown {

    /** An eye that only listens is quiet. */
    @Override
    public int noiseRadius() {
        return 0;
    }

    /** V B V V B V - the sonar tap. */
    public static final long COMBO = 3321L;

    public static final ResourceLocation ID = new ResourceLocation("narutoaddon", "byakugan_sonar");

    /** Chakra paid for the tap. */
    private static final float COST = 30.0f;

    /** Cooldown in ticks: five seconds. */
    private static final int COOLDOWN = 100;

    /** Scan radius in blocks around the tapped block. */
    private static final int SCAN_RADIUS = 24;

    /** Maximum number of ore positions the scan may collect. */
    private static final int MAX_ORES = 256;

    private static final int CHAKRA_REGEN_DELAY = 20;

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
        if (!NarutoConfig.byakuganEnabled) {
            player.displayClientMessage(Component.translatable("jutsu.fail.byakugan.disabled")
                    .withStyle(ChatFormatting.RED), true);
            return false;
        }
        float cost = this.masteryCost(ninjaData, COST);
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
        if (!(player instanceof ServerPlayer serverPlayer) || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        net.minecraft.world.phys.BlockHitResult hit = com.sekwah.narutoaddon.util.Targeting.lookedAtBlock(player, 40.0d);
        if (hit == null) {
            player.displayClientMessage(Component.translatable("jutsu.fail.byakugan_sonar.noblock"), true);
            return;
        }
        BlockPos center = hit.getBlockPos();

        level.playSound(null, center, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.5f, 1.4f);
        level.playSound(null, center, SoundEvents.WARDEN_LISTENING, SoundSource.PLAYERS, 0.8f, 1.0f);
        level.sendParticles(ParticleTypes.SONIC_BOOM, center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5, 1, 0.0, 0.0, 0.0, 0.0);
        level.sendParticles(ParticleTypes.SCULK_CHARGE_POP, center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5, 8, 0.4, 0.4, 0.4, 0.0);
        level.sendParticles(ParticleTypes.SCULK_SOUL, center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5, 6, 0.4, 0.4, 0.4, 0.01);

        ArrayList<BlockPos> positions = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-SCAN_RADIUS, -SCAN_RADIUS, -SCAN_RADIUS),
                center.offset(SCAN_RADIUS, SCAN_RADIUS, SCAN_RADIUS))) {
            if (Math.abs(pos.getY() - center.getY()) > 16) {
                continue;
            }
            if (pos.distSqr(center) > (long) SCAN_RADIUS * SCAN_RADIUS) {
                continue;
            }
            if (!level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) {
                continue;
            }
            if (level.getBlockState(pos).is(net.minecraftforge.common.Tags.Blocks.ORES)) {
                positions.add(new BlockPos(pos));
                if (positions.size() >= MAX_ORES) {
                    break;
                }
            }
        }

        PacketHandler.sendToPlayer(new ClientboundOreVeinPacket(positions, 200), serverPlayer);
    }
}