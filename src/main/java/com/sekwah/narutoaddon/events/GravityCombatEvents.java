package com.sekwah.narutoaddon.events;

import com.sekwah.narutoaddon.abilities.jutsus.ProjectileReflectAbility;
import com.sekwah.narutoaddon.capabilities.INinjaData;
import com.sekwah.narutoaddon.capabilities.NinjaCapabilityHandler;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Fireball;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "narutoaddon", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class GravityCombatEvents {
    private GravityCombatEvents() {
    }

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        Projectile projectile = event.getProjectile();
        if (!(projectile instanceof AbstractArrow)
                && !(projectile instanceof ThrownTrident)
                && !(projectile instanceof Fireball)) {
            return;
        }

        Level level = projectile.level();
        for (Player player : level.players()) {
            if (player.distanceTo(projectile) > 3.0d) {
                continue;
            }
            boolean active = player.getCapability(NinjaCapabilityHandler.NINJA_DATA)
                    .map(ProjectileReflectAbility::isActive)
                    .orElse(false);
            if (!active) {
                continue;
            }

            event.setCanceled(true);

            Entity shooter = projectile.getOwner();
            Vec3 dir = shooter != null
                    ? shooter.position().subtract(projectile.position()).normalize()
                    : projectile.getDeltaMovement().reverse().normalize();
            projectile.setDeltaMovement(dir.scale(1.5d));
            projectile.hurtMarked = true;

            if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.ENCHANTED_HIT,
                        projectile.getX(), projectile.getY(), projectile.getZ(),
                        12, 0.3d, 0.3d, 0.3d, 0.02d);
            }
            return;
        }
    }
}
