package com.sekwah.narutoaddon.events;

import com.sekwah.narutoaddon.entity.ShadowCloneEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Optional;
import java.util.UUID;

/**
 * Addon-side port of the clone totem-of-undying behaviour.
 *
 * <p>A clone carrying a totem in either hand survives the blow that would have popped it, once:
 * the lethal damage is cancelled before it is applied, the totem is consumed, and the clone is
 * left standing on one heart with regeneration, absorption and fire resistance. A blow coming
 * from the clone's own caster always pops it, totem or not.</p>
 *
 * <p>Implemented purely against the base mod's public API, without modifying ShadowCloneEntity.</p>
 */
@Mod.EventBusSubscriber(modid = "narutoaddon", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ShadowCloneTotemEvents {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLethalDamage(LivingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (!(victim instanceof ShadowCloneEntity clone)) {
            return;
        }
        if (clone.level().isClientSide() || event.isCanceled()) {
            return;
        }
        // Only a blow that would actually pop the clone spends a totem.
        if (event.getAmount() < clone.getHealth()) {
            return;
        }
        // The caster's own hand always pops a clone, totem or not.
        Entity source = event.getSource().getEntity();
        if (source instanceof Player caster) {
            Optional<UUID> owner = clone.getOwnerUUID();
            if (owner.isPresent() && owner.get().equals(caster.getUUID())) {
                return;
            }
        }
        if (!useTotem(clone)) {
            return;
        }
        event.setCanceled(true);
    }

    /**
     * Spends a totem from the clone's hands and pays out its protection, vanilla style.
     *
     * @return true if a totem was spent
     */
    private static boolean useTotem(ShadowCloneEntity clone) {
        for (net.minecraft.world.InteractionHand hand : net.minecraft.world.InteractionHand.values()) {
            ItemStack stack = clone.getItemInHand(hand);
            if (!stack.is(Items.TOTEM_OF_UNDYING)) {
                continue;
            }
            stack.shrink(1);
            clone.setHealth(1.0f);
            clone.setRemainingFireTicks(0);
            clone.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));
            clone.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
            clone.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));
            if (clone.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                        clone.getX(), clone.getY() + 1.0, clone.getZ(), 30, 0.2, 0.5, 0.2, 0.1);
                level.playSound(null, clone, SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0f, 1.0f);
            }
            // Vanilla totem animation byte for clients.
            clone.level().broadcastEntityEvent(clone, (byte) 35);
            return true;
        }
        return false;
    }
}
