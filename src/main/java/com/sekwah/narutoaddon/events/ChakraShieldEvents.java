package com.sekwah.narutoaddon.events;

import com.sekwah.narutoaddon.NarutoAddonMod;
import com.sekwah.narutoaddon.abilities.jutsus.ChakraShieldAbility;
import com.sekwah.narutoaddon.abilities.mastery.Mastery;
import com.sekwah.narutoaddon.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutoaddon.damagetypes.NarutoDamageTypes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

/**
 * Spends the caster's chakra shield as damage comes in.
 *
 * <p>Damage is taken out of the shield pool before it ever reaches the caster, point for point, and
 * the armour display is kept in step so the caster can watch it being eaten away. See
 * {@link ChakraShieldAbility}.</p>
 */
@Mod.EventBusSubscriber(modid = NarutoAddonMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ChakraShieldEvents {

    private static final UUID TOUGHNESS_MODIFIER_ID = UUID.fromString("3f7a2c64-8b0d-4e59-a1c2-95d4e8f6b073");

    private static final double TOUGHNESS_ARMOUR = 8.0d;

    private static final float TOUGHNESS_MASTERY = 0.8f;

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) {
            return;
        }
        if (event.isCanceled() || event.getAmount() <= 0.0f) {
            return;
        }
        if (event.getSource().is(NarutoDamageTypes.GATE_BACKLASH)) {
            // The gate backlash is the caster's own body tearing itself apart. There is no shielding
            // it, or eight gates would be no more dangerous than any other buff.
            return;
        }

        player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
            float pool = ninjaData.getShieldPoints();
            if (pool <= 0.0f) {
                return;
            }

            float absorbed = Math.min(pool, event.getAmount());
            float remaining = pool - absorbed;
            ninjaData.setShieldPoints(remaining);
            event.setAmount(event.getAmount() - absorbed);

            if (remaining <= 0.0f) {
                ninjaData.setShieldPoints(0.0f);
                ChakraShieldAbility.recordBreakMastery(player, ninjaData);
                ChakraShieldAbility.removeShield(player);
                if (player.level() instanceof ServerLevel level) {
                    level.sendParticles(ParticleTypes.CRIT, player.getX(),
                            player.getY() + player.getBbHeight() / 2.0d, player.getZ(),
                            30, 0.5d, 0.7d, 0.5d, 0.1d);
                    level.playSound(null, player, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.8f, 0.9f);
                }
            } else {
                ChakraShieldAbility.applyShield(player, ninjaData.getShieldPoints());
            }
        });
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
            AttributeInstance toughness = player.getAttribute(Attributes.ARMOR_TOUGHNESS);
            if (toughness == null) {
                return;
            }

            boolean masteredShield = ninjaData.getShieldPoints() > 0.0f
                    && Mastery.level(ninjaData, ChakraShieldAbility.ID) >= TOUGHNESS_MASTERY;
            boolean worn = toughness.getModifier(TOUGHNESS_MODIFIER_ID) != null;
            if (masteredShield == worn) {
                return;
            }

            toughness.removeModifier(TOUGHNESS_MODIFIER_ID);
            if (masteredShield) {
                toughness.addTransientModifier(new AttributeModifier(TOUGHNESS_MODIFIER_ID,
                        "narutomod:chakra_shield_toughness", TOUGHNESS_ARMOUR,
                        AttributeModifier.Operation.ADDITION));
            }
        });
    }
}
