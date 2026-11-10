package com.sekwah.narutoaddon.abilities.jutsus;

import com.sekwah.narutoaddon.abilities.ToggleState;
import com.sekwah.narutoaddon.abilities.Ability;
import com.sekwah.narutoaddon.abilities.mastery.Mastery;
import com.sekwah.narutoaddon.capabilities.INinjaData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * The Juken stance: the gentle fist, chakra wrapped around the hands.
 */
public class JukenStanceAbility extends Ability implements Ability.Toggled, Ability.HandleEnded {

    @Override
    public int noiseRadius() {
        return 0;
    }

    public static final long COMBO = 3112L;

    public static final ResourceLocation ID = new ResourceLocation("narutoaddon", "juken_stance");

    private static final float CHAKRA_PER_TICK = 0.3f;

    /** Persistent-data key that throttles the "the eye is closed" refusal, in game time. */
    private static final String REFUSED_AT = "narutoaddon:juken_refused_at";

    @Override
    public ActivationType activationType() {
        return ActivationType.TOGGLE;
    }

    @Override
    public long defaultCombo() {
        return COMBO;
    }

    /**
     * The stance is worn on the eye: a caster who has not opened the Byakugan cannot take it up.
     * This is the menu road, which refuses before the stance joins the toggle set.
     */
    @Override
    public boolean canStartToggle(Player player, INinjaData ninjaData) {
        if (ByakuganAbility.isActive(ninjaData)) {
            return true;
        }
        player.displayClientMessage(Component.translatable("jutsu.fail.byakugan.required",
                Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW))
                .withStyle(ChatFormatting.RED), true);
        return false;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        float cost = this.masteryCost(ninjaData, CHAKRA_PER_TICK);
        if (ninjaData.getChakra() < cost) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        if (!ByakuganAbility.isActive(ninjaData)) {
            // The key road adds the toggle before any check runs, so the stance is dropped on the
            // first tick without the eye. Tell the player once instead of once per tick.
            long now = player.level().getGameTime();
            if (now >= player.getPersistentData().getLong(REFUSED_AT)) {
                player.getPersistentData().putLong(REFUSED_AT, now + 40L);
                player.displayClientMessage(Component.translatable("jutsu.fail.byakugan.required",
                        Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW))
                        .withStyle(ChatFormatting.RED), true);
            }
            return false;
        }
        ninjaData.useChakra(cost, 20);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        this.recordMasteryCredit(player, ninjaData, Ability.MASTERY_CREDIT_PER_TICK);
        if (player.tickCount % 4 != 0) {
            return;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 look = player.getLookAngle();
        Vec3 flatLook = new Vec3(look.x, 0.0d, look.z);
        flatLook = flatLook.lengthSqr() < 1.0E-4d ? new Vec3(0.0d, 0.0d, 1.0d) : flatLook.normalize();
        Vec3 right = new Vec3(-flatLook.z, 0.0d, flatLook.x);
        Vec3 base = player.position().add(flatLook.scale(0.30d)).add(0.0d, 1.15d, 0.0d);
        Vec3 rightHand = base.add(right.scale(0.34d));
        Vec3 leftHand = base.add(right.scale(-0.34d));
        level.sendParticles(ParticleTypes.END_ROD, rightHand.x, rightHand.y, rightHand.z,
                1, 0.02d, 0.02d, 0.02d, 0.0d);
        level.sendParticles(ParticleTypes.END_ROD, leftHand.x, leftHand.y, leftHand.z,
                1, 0.02d, 0.02d, 0.02d, 0.0d);
    }

    @Override
    public void performToggleClient(Player player, INinjaData ninjaData) {
    }

    @Override
    public void handleAbilityEnded(Player player, INinjaData ninjaData, int ticksActive) {
    }

    @Override
    public void recordMastery(Player player, INinjaData ninjaData) {
    }

    public static boolean isActive(INinjaData ninjaData) {
        return ToggleState.isActive(ninjaData, ID);
    }
}
