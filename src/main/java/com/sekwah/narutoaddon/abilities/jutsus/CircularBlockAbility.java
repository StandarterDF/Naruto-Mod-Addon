package com.sekwah.narutoaddon.abilities.jutsus;

import com.sekwah.narutoaddon.abilities.Ability;
import com.sekwah.narutoaddon.abilities.jutsus.ByakuganAbility;
import com.sekwah.narutoaddon.capabilities.INinjaData;
import com.sekwah.narutoaddon.config.NarutoConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Eight Trigrams: Circular Block, the guard the eye keeps while the hands are busy.
 *
 * <p>Parry version: on cast the caster's persistent data is stamped with a short window
 * ({@value #PARRY_WINDOW} ticks) during which the next blow coming from the front is refused
 * outright by {@link com.sekwah.narutoaddon.events.ParryEvents}.</p>
 */
public class CircularBlockAbility extends Ability implements Ability.Cooldown {

    /** A parry is silent: the circle is drawn in chakra, not sound. */
    @Override
    public int noiseRadius() {
        return 0;
    }

    /** V B V V B V B V - the eye's seal, then the four keys of the circle. */
    public static final long COMBO = 3113L;

    public static final ResourceLocation ID = new ResourceLocation("narutomod", "circular_block");

    /** Persistent-data key for the parry window; read by {@code ParryEvents}. */
    public static final String PARRY_UNTIL_TAG = "narutoaddon:parry_until";

    /** How long the parry window stays open after the cast, in ticks. */
    private static final int PARRY_WINDOW = 10;

    /** What the parry costs and how long the eye needs before it can parry again. */
    private static final float COST = 20.0f;
    private static final int COOLDOWN = 100;

    private static final int CHAKRA_REGEN_DELAY = 10;

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
        if (!ByakuganAbility.isActive(ninjaData)) {
            player.displayClientMessage(Component.translatable("jutsu.fail.byakugan.required",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW))
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
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        player.getPersistentData().putLong(PARRY_UNTIL_TAG, level.getGameTime() + PARRY_WINDOW);

        Vec3 look = player.getLookAngle();
        double x = player.getX() + look.x * 1.5d;
        double y = player.getY() + player.getBbHeight() * 0.55d + look.y * 1.5d;
        double z = player.getZ() + look.z * 1.5d;
        level.sendParticles(ParticleTypes.ENCHANTED_HIT, x, y, z,
                12, 0.4d, 0.4d, 0.4d, 0.0d);
    }

    /** @return whether the parry window is open for this caster right now. */
    public static boolean isActive(Player player) {
        long until = player.getPersistentData().getLong(PARRY_UNTIL_TAG);
        return until > 0L && until > player.level().getGameTime();
    }
}