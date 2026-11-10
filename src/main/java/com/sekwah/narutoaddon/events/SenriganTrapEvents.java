package com.sekwah.narutoaddon.events;

import com.sekwah.narutoaddon.abilities.jutsus.SenriganAbility;
import com.sekwah.narutoaddon.capabilities.INinjaData;
import com.sekwah.narutoaddon.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutoaddon.config.NarutoConfig;
import com.sekwah.narutomod.entity.item.PaperBombEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * While the Senrigan is open, a hidden paper-bomb trap is seen before it fires: the explosion of a
 * {@link PaperBombEntity} is cancelled, the bomb is discarded, and the caster is warned. Other
 * explosions (TNT, creepers, ...) are left untouched.
 */
@Mod.EventBusSubscriber(modid = "narutoaddon", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SenriganTrapEvents {

    @SubscribeEvent
    public static void onExplosionStart(ExplosionEvent.Start event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        Explosion explosion = event.getExplosion();

        PaperBombEntity bomb = null;
        Entity direct = explosion.getDirectSourceEntity();
        Entity indirect = explosion.getIndirectSourceEntity();
        Entity exploder = explosion.getExploder();
        if (direct instanceof PaperBombEntity paperBomb) {
            bomb = paperBomb;
        } else if (indirect instanceof PaperBombEntity paperBomb) {
            bomb = paperBomb;
        } else if (exploder instanceof PaperBombEntity paperBomb) {
            bomb = paperBomb;
        }
        if (bomb == null) {
            return;
        }

        Vec3 center = explosion.getPosition();
        List<ServerPlayer> near = level.getEntitiesOfClass(ServerPlayer.class, new AABB(center, center).inflate(6.0d));

        ServerPlayer found = null;
        for (ServerPlayer player : near) {
            if (!NarutoConfig.senriganEnabled) {
                continue;
            }
            INinjaData ninjaData = player.getCapability(NinjaCapabilityHandler.NINJA_DATA).resolve().orElse(null);
            if (ninjaData == null) {
                continue;
            }
            if (!SenriganAbility.isActive(ninjaData)) {
                continue;
            }
            found = player;
            break;
        }

        if (found == null) {
            return;
        }

        event.setCanceled(true);
        bomb.discard();
        found.displayClientMessage(Component.translatable("jutsu.senrigan.trap"), true);
        level.sendParticles(ParticleTypes.CLOUD, center.x, center.y, center.z, 16, 0.4d, 0.4d, 0.4d, 0.02d);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.8f, 1.2f);
    }
}