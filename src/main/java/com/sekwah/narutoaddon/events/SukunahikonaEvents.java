package com.sekwah.narutoaddon.events;

import com.sekwah.narutoaddon.capabilities.INinjaData;
import com.sekwah.narutoaddon.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutoaddon.friends.Friends;
import com.sekwah.narutoaddon.skills.SkillTree;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Passive aura "Sukunahikona": arrows and fireballs flying toward the owner vanish
 * within 8 blocks of a player who has unlocked the node.
 */
@Mod.EventBusSubscriber(modid = "narutoaddon", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SukunahikonaEvents {

    private SukunahikonaEvents() {
    }

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        var projectile = event.getProjectile();
        if (projectile.level().isClientSide) {
            return;
        }
        if (!(projectile instanceof AbstractArrow)
                && !(projectile instanceof LargeFireball)
                && !(projectile instanceof SmallFireball)) {
            return;
        }

        var players = projectile.level().getEntitiesOfClass(Player.class,
                projectile.getBoundingBox().inflate(12.0d));

        for (Player player : players) {
            if (player.distanceToSqr(projectile) > 64.0d) {
                continue;
            }
            if (projectile.getOwner() == player) {
                continue;
            }
            // Allies' projectiles are left alone.
            if (Friends.isOwnSide(player, projectile.getOwner())) {
                continue;
            }

            INinjaData data = player.getCapability(NinjaCapabilityHandler.NINJA_DATA).resolve().orElse(null);
            if (data == null) {
                continue;
            }
            if (!SkillTree.hasUnlock(data.getSkillData().getUnlocked(), "mechanic:sukunahikona_aura")) {
                continue;
            }

            event.setCanceled(true);
            projectile.discard();
            if (projectile.level() instanceof ServerLevel level) {
                var pos = projectile.position();
                level.sendParticles(ParticleTypes.END_ROD, pos.x, pos.y, pos.z, 8, 0.1d, 0.1d, 0.1d, 0.0d);
                level.sendParticles(ParticleTypes.SMOKE, pos.x, pos.y, pos.z, 4, 0.1d, 0.1d, 0.1d, 0.0d);
            }
            return;
        }
    }
}