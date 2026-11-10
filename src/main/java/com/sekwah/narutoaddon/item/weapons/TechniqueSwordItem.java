package com.sekwah.narutoaddon.item.weapons;

import com.sekwah.narutoaddon.abilities.jutsus.HiramekareiAbility;
import com.sekwah.narutoaddon.abilities.jutsus.ShibukiAbility;
import com.sekwah.narutoaddon.entity.ShadowCloneEntity;
import com.sekwah.narutoaddon.friends.Friends;
import com.sekwah.narutoaddon.item.ComboSwordItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A great blade whose technique lives in the sword rather than in a combo.
 *
 * <p>Hiramekarei and Shibuki are not spells a ninja learns to cast; they are things a particular
 * blade does. Right-clicking the blade uses its art, the same way the guard is raised from any of
 * the mod's swords, so the two no longer take up a combo slot or a row in the jutsu menu.</p>
 *
 * <p>Both blades also carry a tally. Hiramekarei stores chakra in its slabs and lets it go in one
 * wide blast; Shibuki keeps a kill count and its scroll grows heavier with every one. The tally
 * rides on the stack, so it survives a relog, a death and a handover.</p>
 */
public class TechniqueSwordItem extends ComboSwordItem {

    /** Which blade's art this item carries. */
    public enum Art {
        HIRAMEKAREI,
        SHIBUKI
    }

    /** Power of the controlled detonation the blast sword sets off on every hit. */
    private static final float HIT_POWER = 2.0f;

    /** How far around the wielder the blast sword looks for bodies to keep out of its own blast. */
    private static final double PROTECT_RADIUS = 8.0d;

    private final Art art;

    /** Kenjutsu skill nodes the wielder must have unlocked to swing this blade at full speed. */
    private final int kenjutsuPointsRequired;

    public TechniqueSwordItem(Tier tier, int attackDamage, float attackSpeed, Properties properties,
                              float damagePerStep, int maxCombo, int comboWindow, Art art) {
        this(tier, attackDamage, attackSpeed, properties, damagePerStep, maxCombo, comboWindow, art, 3);
    }

    public TechniqueSwordItem(Tier tier, int attackDamage, float attackSpeed, Properties properties,
                              float damagePerStep, int maxCombo, int comboWindow, Art art,
                              int kenjutsuPointsRequired) {
        super(tier, attackDamage, attackSpeed, properties, damagePerStep, maxCombo, comboWindow, 0.0f,
                kenjutsuPointsRequired);
        this.art = art;
        this.kenjutsuPointsRequired = kenjutsuPointsRequired;
    }

    public Art art() {
        return this.art;
    }

    public int kenjutsuPointsRequired() {
        return this.kenjutsuPointsRequired;
    }

    /**
     * Both blades keep a kill tally, so {@code ComboSwordEvents} records every kill they take: the
     * hammer's store deepens with each one and the scroll's charge grows heavier. The flat
     * damage-per-kill of the other great swords is not wanted here - these two grow in their own
     * way - and it is not granted, because {@link ComboSwordItem} reads its own field for that.
     */
    @Override
    public boolean gainsKills() {
        return true;
    }

    /**
     * What the blade does to a blow it has just landed.
     *
     * <p>Hiramekarei drinks a little of the caster's chakra into its slabs on every hit; Shibuki's
     * scroll is set off where the blade lands, a controlled blast that breaks nothing and leaves the
     * wielder and their allies out of it.</p>
     */
    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean result = super.hurtEnemy(stack, target, attacker);
        if (attacker.level().isClientSide || !(attacker instanceof Player player)) {
            return result;
        }
        if (this.art == Art.HIRAMEKAREI) {
            HiramekareiAbility.charge(stack);
        } else {
            explodeOnHit(player, target);
        }
        return result;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        // A shield takes priority over the blade's art, the same way it does for every other great
        // sword: if the caster is carrying one, right-click must raise the shield instead of firing
        // the technique (see ComboSwordItem#use and the fork's ShibukiItem#use).
        ItemStack offHand = player.getItemInHand(InteractionHand.OFF_HAND);
        ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (offHand.getItem() instanceof ShieldItem || mainHand.getItem() instanceof ShieldItem) {
            return InteractionResultHolder.pass(stack);
        }
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResultHolder.pass(stack);
        }

        if (this.art == Art.HIRAMEKAREI) {
            // Sneaking lets the stored chakra go in one wide blast instead of shouldering the blade.
            if (player.isShiftKeyDown()) {
                if (!level.isClientSide) {
                    HiramekareiAbility.release(player);
                }
                return InteractionResultHolder.success(stack);
            }
            if (!level.isClientSide) {
                HiramekareiAbility.raise(player);
            }
            // The blade is also held up as a guard while the button stays down: the hit hook reads
            // the stance off the used stack, the same way it does for every other great sword.
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }

        if (!level.isClientSide) {
            ShibukiAbility.swing(player);
        }
        return InteractionResultHolder.pass(stack);
    }

    /**
     * The blast sword's scroll going off at the point of impact: a controlled explosion of the
     * sword's own power that breaks no block and spares the wielder, their allies and their clones.
     */
    private static void explodeOnHit(Player player, LivingEntity target) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        // Keep the owner and their allies out of the blast.
        List<LivingEntity> protectedEntities = new ArrayList<>();
        player.setInvulnerable(true);
        protectedEntities.add(player);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(PROTECT_RADIUS))) {
            if (entity == player || entity == target) {
                continue;
            }
            if (isProtected(player, entity)) {
                entity.setInvulnerable(true);
                protectedEntities.add(entity);
            }
        }

        // The kill belongs to the sword, so the blast has to remember who threw it.
        player.setLastHurtMob(target);
        Vec3 pos = target.position();
        level.explode(player, pos.x, pos.y, pos.z, HIT_POWER, ExplosionInteraction.NONE);

        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, pos.x, pos.y + target.getBbHeight() * 0.5d,
                pos.z, 1, 0.0d, 0.0d, 0.0d, 0.0d);
        level.sendParticles(ParticleTypes.EXPLOSION, pos.x, pos.y + target.getBbHeight() * 0.5d,
                pos.z, 3, 0.4d, 0.4d, 0.4d, 0.0d);
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS,
                1.0f, 1.2f);

        for (LivingEntity entity : protectedEntities) {
            entity.setInvulnerable(false);
        }
    }

    /** @return whether the blast must leave this body alone. */
    private static boolean isProtected(Player player, LivingEntity target) {
        if (target == player) {
            return true;
        }
        if (target instanceof Player other && (other.isCreative() || other.isSpectator())) {
            return true;
        }
        if (target.isAlliedTo(player) || Friends.areFriends(player, target)) {
            return true;
        }
        return target instanceof ShadowCloneEntity clone && clone.isOwnedBy(player);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        int kills = ComboSwordItem.getKills(stack);
        if (this.art == Art.HIRAMEKAREI) {
            tooltip.add(Component.translatable("item.narutoaddon.hiramekarei.charge",
                    (int) HiramekareiAbility.chakra(stack), HiramekareiAbility.maxChakra(stack))
                    .withStyle(ChatFormatting.AQUA));
            tooltip.add(Component.translatable("item.narutoaddon.hiramekarei.kills", kills)
                    .withStyle(ChatFormatting.GOLD));
            tooltip.add(Component.translatable("item.narutoaddon.hiramekarei.release",
                    (int) HiramekareiAbility.releaseDamage(stack),
                    (int) HiramekareiAbility.releaseRadius(stack)).withStyle(ChatFormatting.LIGHT_PURPLE));
        } else {
            tooltip.add(Component.translatable("item.narutoaddon.shibuki.kills", kills)
                    .withStyle(ChatFormatting.GOLD));
            tooltip.add(Component.translatable("item.narutoaddon.shibuki.power",
                    Math.round(ShibukiAbility.burstDamage(kills))).withStyle(ChatFormatting.RED));
        }
    }
}
