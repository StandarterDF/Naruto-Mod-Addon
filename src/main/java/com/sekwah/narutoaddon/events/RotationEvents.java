package com.sekwah.narutoaddon.events;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import com.sekwah.narutoaddon.abilities.jutsus.RotationAbility;
import com.sekwah.narutoaddon.friends.Friends;
import com.sekwah.narutoaddon.capabilities.INinjaData;
import com.sekwah.narutoaddon.capabilities.NinjaCapabilityHandler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.FORGE;

/**
 * Kaiten (Rotation) projectile cancellation: a projectile entering the dome of a player who is
 * currently channeling Rotation is cancelled and discarded.
 */
@Mod.EventBusSubscriber(modid = "narutoaddon", bus = FORGE)
public final class RotationEvents {

    private RotationEvents() {
    }

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        Projectile projectile = event.getProjectile();
        if (projectile.level().isClientSide) {
            return;
        }
        if (!(projectile.level() instanceof ServerLevel level)) {
            return;
        }

        Vec3 pos = projectile.position();
        for (Player player : level.getEntitiesOfClass(Player.class,
                projectile.getBoundingBox().inflate(3.0d))) {
            if (player.distanceToSqr(pos) > 9.0d) {
                continue;
            }
            if (!RotationAbility.isActive(player)) {
                continue;
            }
            // The caster's own shots are not turned away by their own dome.
            if (projectile.getOwner() == player) {
                continue;
            }
            event.setCanceled(true);
            projectile.discard();
            return;
        }
    }

    /**
     * Carries stored debuffs from a Rotation tap onto the next melee target.
     */
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getSource().getDirectEntity() instanceof Player attacker)) {
            return;
        }
        LivingEntity target = event.getEntity();
        if (target == attacker) {
            return;
        }
        if (!RotationAbility.hasPending(attacker)) {
            return;
        }
        if (Friends.isOwnSide(attacker, target)) {
            return;
        }
        RotationAbility.transferPending(attacker, target);
    }
}