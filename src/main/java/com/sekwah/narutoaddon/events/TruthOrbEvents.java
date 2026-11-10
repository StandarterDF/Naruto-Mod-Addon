package com.sekwah.narutoaddon.events;

import com.sekwah.narutoaddon.abilities.jutsus.TenseiganAbility;
import com.sekwah.narutoaddon.capabilities.INinjaData;
import com.sekwah.narutoaddon.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutoaddon.skills.SkillTree;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * "Spheres of Truth" passive (Tenseigan branch): reduces damage taken from behind.
 *
 * <p>The orbs themselves are drawn by {@code TenseiganOrbRenderer} - the halo of the Six Paths mode
 * in the eye's turquoise - so this class only owns the mechanic. It uses the same two conditions the
 * renderer does (the eye open and the node bought), which is what keeps the picture and the effect
 * from ever disagreeing.</p>
 */
@Mod.EventBusSubscriber(modid = "narutoaddon", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TruthOrbEvents {

    private TruthOrbEvents() {
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player target)) {
            return;
        }
        if (target.level().isClientSide()) {
            return;
        }
        Entity attacker = event.getSource().getEntity();
        if (attacker == null) {
            attacker = event.getSource().getDirectEntity();
        }
        if (attacker == null || attacker == target) {
            return;
        }
        INinjaData data = target.getCapability(NinjaCapabilityHandler.NINJA_DATA).resolve().orElse(null);
        if (data == null) {
            return;
        }
        if (!SkillTree.hasUnlock(data.getSkillData().getUnlocked(), "mechanic:truth_orbs")) {
            return;
        }
        if (!TenseiganAbility.isActive(data)) {
            return;
        }
        Vec3 toSource = attacker.position().subtract(target.position()).normalize();
        if (target.getLookAngle().dot(toSource) < -0.35) {
            event.setAmount(event.getAmount() * 0.25f);
            if (target.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.END_ROD, target.getX(), target.getY() + target.getBbHeight() * 0.5d,
                        target.getZ(), 6, 0.4d, 0.4d, 0.4d, 0.02d);
            }
        }
    }
}
