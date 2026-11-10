package com.sekwah.narutoaddon.events;

import com.sekwah.narutoaddon.abilities.NarutoAbilities;
import com.sekwah.narutoaddon.abilities.jutsus.ByakuganAbility;
import com.sekwah.narutoaddon.abilities.jutsus.JukenStanceAbility;
import com.sekwah.narutoaddon.abilities.mastery.Mastery;
import com.sekwah.narutoaddon.capabilities.INinjaData;
import com.sekwah.narutoaddon.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutoaddon.capabilities.enemychakra.EnemyChakra;
import com.sekwah.narutoaddon.entity.jutsuprojectile.TravellingArcEntity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Gentle Fist hooks: while a player has the Juken stance up, their melee hits carry the
 * anatomical-scale multiplier, an armor-piercing bonus and drain the target's chakra. Every swing
 * also throws a narrow crescent of chakra that carries the hand's own blow a few blocks further.
 */
@Mod.EventBusSubscriber(modid = "narutoaddon", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class JukenEvents {

    /** How far the chakra wave thrown by a strike reaches, in blocks. */
    private static final double WAVE_RANGE = 5.0d;

    /** Half-angle of the wave's crescent, so the full fan is 30 degrees: a palm, not a wall. */
    private static final double WAVE_HALF_ANGLE_DEGREES = 15.0d;

    /** The body the hand itself struck this swing, so the wave does not bill it a second time. */
    private static final String HIT_TARGET = "narutoaddon:juken_hit_target";
    private static final String HIT_AT = "narutoaddon:juken_hit_at";

    /** The charge the hand's own blow was struck with; vanilla resets it right after the hit. */
    private static final String HIT_CHARGE = "narutoaddon:juken_hit_charge";

    /** How long after the hand's own blow the wave still counts as its carry, in ticks. */
    private static final long EXEMPT_WINDOW = 4L;

    /** Stamina one palm strike takes, before mastery makes the art cheaper. */
    private static final float STAMINA_PER_STRIKE = 8.0f;

    /** Ticks the stamina bar waits before it starts coming back after a strike. */
    private static final int STAMINA_REGEN_DELAY = 20;

    private JukenEvents() {
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getSource().getDirectEntity() instanceof Player attacker) {
            LivingEntity target = event.getEntity();
            // The gentle fist is a bare-handed art: a blade keeps its own rules.
            if (!attacker.getMainHandItem().isEmpty()) {
                return;
            }
            if (target == attacker || target.isSpectator()
                    || com.sekwah.narutoaddon.friends.Friends.isOwnSide(attacker, target)) {
                return;
            }
            INinjaData data = attacker.getCapability(NinjaCapabilityHandler.NINJA_DATA).resolve().orElse(null);
            if (data == null) {
                return;
            }
            if (!JukenStanceAbility.isActive(data)) {
                return;
            }
            // Remember the body for a few ticks: the wave thrown by the same swing carries this
            // blow outward, and the body already at arm's length must not be struck twice. The
            // charge is taken now because Player#attack resets the ticker before the wave spawns.
            attacker.getPersistentData().putInt(HIT_TARGET, target.getId());
            attacker.getPersistentData().putLong(HIT_AT, attacker.level().getGameTime());
            attacker.getPersistentData().putFloat(HIT_CHARGE, attacker.getAttackStrengthScale(0.5f));
            event.setAmount(event.getAmount() * AnatomicalScaleEvents.damageMultiplier(attacker, target));
            // Mastery of the stance adds on top of the anatomical knowledge: a palm that has been
            // practised lands harder than the first one ever thrown, and the wave sent by the same
            // swing inherits it exactly once - the wave's own hit runs through this same handler.
            event.setAmount(event.getAmount() * (1.0f + ByakuganAbility.MASTERY_DAMAGE_BONUS
                    * Mastery.level(data, NarutoAbilities.JUKEN_STANCE.get())));
            event.setAmount(event.getAmount() + target.getArmorValue() * 0.15f);
            EnemyChakra.drain(target, 6.0f);
            if (attacker.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.END_ROD, target.getX(),
                        target.getY() + target.getBbHeight() * 0.5d, target.getZ(),
                        4, 0.1d, 0.1d, 0.1d, 0.05d);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (player.level().isClientSide) {
            return;
        }
        // The server sees every swing - at a body or at empty air - as exactly one tick with
        // swingTime == 1, so this is the one place both kinds of strike pass through.
        if (player.swingTime != 1) {
            return;
        }
        // The gentle fist is a bare-handed art: a blade keeps its own rules.
        if (!player.getMainHandItem().isEmpty()) {
            return;
        }
        INinjaData data = player.getCapability(NinjaCapabilityHandler.NINJA_DATA).resolve().orElse(null);
        if (data == null) {
            return;
        }
        if (!(JukenStanceAbility.isActive(data) && ByakuganAbility.isActive(data))) {
            return;
        }
        ServerLevel level = (ServerLevel) player.level();
        // Every swing costs stamina, whether it lands on a body or on empty air. The gentle fist is
        // chakra spent through the palms: without a price the stance was a free permanent buff.
        // Mastery makes the art cheaper, as it does for every other priced technique.
        float cost = Mastery.cost(data, NarutoAbilities.byId(JukenStanceAbility.ID), STAMINA_PER_STRIKE);
        if (data.getStamina() < cost) {
            // Too spent to push chakra through the hands: the palm still swings, but the wave that
            // carries it does not leave the arm. No message, or a fast attacker would be spammed.
            return;
        }
        data.useStamina(cost, STAMINA_REGEN_DELAY);
        long hitAt = player.getPersistentData().getLong(HIT_AT);
        boolean carriesHandBlow = hitAt != 0L && level.getGameTime() - hitAt <= EXEMPT_WINDOW;
        // The wave carries the blow the hand would have landed: the same attack value, scaled by
        // the same charge the vanilla strike uses (0.2 + strength^2 * 0.8). A hand hit resets the
        // charge ticker, so its own value is taken from the strike itself. The Juken multipliers
        // are not added here - the wave's hit runs through the same LivingHurtEvent handler as a
        // punch, so it inherits them exactly once.
        float charge = carriesHandBlow
                ? player.getPersistentData().getFloat(HIT_CHARGE)
                : player.getAttackStrengthScale(0.5f);
        float damage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE)
                * (0.2f + charge * charge * 0.8f);
        // A crescent that leaves the hand and crosses the ground in front of the caster: the same
        // flight the vacuum blade and the grass cut use, so the wave is a body moving through the
        // world rather than a fan of motes that hangs in the air.
        TravellingArcEntity wave = new TravellingArcEntity(level, player, WAVE_RANGE,
                Math.cos(Math.toRadians(WAVE_HALF_ANGLE_DEGREES)), damage,
                TravellingArcEntity.KIND_JUKEN, false, 0, 0.0f);
        if (carriesHandBlow) {
            wave.setExemptEntity(player.getPersistentData().getInt(HIT_TARGET));
        }
        level.addFreshEntity(wave);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.4f, 1.2f);
    }
}
