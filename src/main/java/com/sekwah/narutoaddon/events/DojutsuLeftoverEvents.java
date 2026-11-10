package com.sekwah.narutoaddon.events;

import com.sekwah.narutoaddon.abilities.jutsus.BlackRodAbility;
import com.sekwah.narutoaddon.abilities.jutsus.KamuiPhaseAbility;
import com.sekwah.narutoaddon.capabilities.NinjaCapabilityHandler;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * The consequences that outlive the cast of the dojutsu techniques in this block: a cut that lands,
 * a rod that keeps working, a body that stays out of phase, and a death the far eye refuses to
 * forget.
 *
 * <p>Every one of these is a mark first and an effect second. The techniques write a tag and walk
 * away; this class reads it on the next relevant tick and does the work. That keeps the ability
 * classes small, keeps the server the only authority for anything that changes a body, and means a
 * mark written just before a crash still means something when the world comes back.</p>
 *
 * <p>The phase flags are the one thing that has to run on both sides. Vanilla resets
 * {@code noPhysics} at the top of {@code Player#tick}, and the client would otherwise push the caster
 * back out of every block the server is letting them pass through. It runs at
 * {@link EventPriority#HIGHEST} so it lands after the Kamui upkeep, which also touches the same field
 * for a different stance.</p>
 */
@Mod.EventBusSubscriber(modid = "narutoaddon", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DojutsuLeftoverEvents {

    /** How often a body is tested against the rods around it, in ticks. */
    private static final int ROD_INTERVAL = 5;

    /** The glow a rod puts on a nearby ninja, refreshed rather than permanent. */
    private static final int ROD_GLOW_TICKS = 40;

    /** How long a rod keeps natural regeneration switched off after the last refresh. */
    private static final int ROD_REGEN_DELAY = 40;

    private DojutsuLeftoverEvents() {
    }

    /**
     * A rod of the outer path holds the chakra of every ninja standing near it still: regeneration is
     * pushed back every few ticks, and the eye that planted the rod can see the bodies it caught.
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) {
            return;
        }
        Player player = event.player;
        if (player.tickCount % ROD_INTERVAL != 0) {
            return;
        }
        List<Display.ItemDisplay> rods = player.level().getEntitiesOfClass(Display.ItemDisplay.class,
                player.getBoundingBox().inflate(BlackRodAbility.SUPPRESSION_RADIUS), BlackRodAbility::isRod);
        boolean withinReach = false;
        for (Display.ItemDisplay rod : rods) {
            if (rod.distanceToSqr(player) <= BlackRodAbility.SUPPRESSION_RADIUS
                    * BlackRodAbility.SUPPRESSION_RADIUS) {
                withinReach = true;
                break;
            }
        }
        if (!withinReach) {
            return;
        }
        player.addEffect(new MobEffectInstance(MobEffects.GLOWING, ROD_GLOW_TICKS, 0, false, false));
        // Zero is spent: only the regeneration delay is wanted, and this is the public way to set it.
        player.getCapability(NinjaCapabilityHandler.NINJA_DATA)
                .ifPresent(data -> data.useChakra(0.0f, ROD_REGEN_DELAY));
    }

}
