package com.sekwah.narutoaddon.events;

import com.sekwah.narutoaddon.NarutoAddonMod;
import com.sekwah.narutoaddon.abilities.jutsus.HiramekareiAbility;
import com.sekwah.narutoaddon.item.ComboSwordItem;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Hiramekarei on the defensive side: the slabs keep what they take.
 *
 * <p>A blow turned aside by a great sword belongs to the kenjutsu guard, and all of it - taking the
 * edge off the hit, spending the stamina for it, throwing the spark where the two met and knocking
 * an arrow back out of the air - is done in {@code KenjutsuTaijutsuEvents}. This class is the
 * hammer's own half of that guard and touches nothing but the charge riding on the blade: the blade
 * is not a shield that stops a blow, it is a slab that drinks one.</p>
 *
 * <p>In the fork this was the same listener as the damage cut. Here the cut already exists and is
 * not repeated: a second {@code setAmount} on the same event would take the blow down twice over,
 * once for the guard and once again by this class. So the amount is only read, never written.</p>
 */
@Mod.EventBusSubscriber(modid = NarutoAddonMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class HiramekareiEvents {

    /**
     * How much of a blow the great-sword guard leaves behind: {@code KenjutsuTaijutsuEvents} keeps
     * {@link KenjutsuTaijutsuEvents#GUARD_DAMAGE_LEFT} of the damage and swallows the rest. That
     * listener sits at the default priority, so the amount read here has already been cut; this share
     * is what brings the blow back to its original force, because only the part the guard swallowed is
     * worth storing.
     */
    private static final float GUARD_LEAVES = KenjutsuTaijutsuEvents.GUARD_DAMAGE_LEFT;

    /** How much of what the guard swallowed runs into the slabs: the fork's 90%. */
    private static final float ABSORBED_TO_CHARGE = 0.9f;

    private HiramekareiEvents() {
    }

    /**
     * A blow taken on the raised blade charges the slabs with a share of what the guard swallowed.
     *
     * <p>Registered last, so that the outcome of every other listener is already known: the amount
     * seen here is what the caster is finally going to lose, and a blow somebody else turned aside
     * outright - a parry, an arrow knocked back, a life the caster did not have to spend - charges
     * nothing at all. The fork's two refusals are kept as well: a caster's own hand cannot be met by
     * their own guard, and falling is not a blow.</p>
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.isCanceled() || event.getAmount() <= 0.0f) {
            return;
        }
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) {
            return;
        }
        // A technique the caster's own hand threw is not something the guard can meet, and a fall is
        // not a blow: neither is stored.
        if (event.getSource().getEntity() == player) {
            return;
        }
        if (event.getSource().is(DamageTypeTags.IS_FALL)) {
            return;
        }
        // The guard reads the used stack, so the blade has to be the one held up in the main hand.
        if (!player.isUsingItem() || player.getUsedItemHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        ItemStack blade = player.getUseItem();
        if (!HiramekareiAbility.isBlade(blade)
                || !(player.getMainHandItem().getItem() instanceof ComboSwordItem)) {
            return;
        }
        // What is left of the blow, brought back up to its original force: the difference is the part
        // the guard swallowed, and 90% of that is what the slabs drink.
        float left = event.getAmount();
        float absorbed = left / GUARD_LEAVES - left;
        HiramekareiAbility.charge(blade, absorbed * ABSORBED_TO_CHARGE);
    }
}
