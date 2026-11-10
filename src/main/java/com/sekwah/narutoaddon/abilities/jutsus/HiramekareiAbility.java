package com.sekwah.narutoaddon.abilities.jutsus;

import com.sekwah.narutoaddon.abilities.Ability;
import com.sekwah.narutoaddon.abilities.AbilityUnlocks;
import com.sekwah.narutoaddon.abilities.JutsuEffects;
import com.sekwah.narutoaddon.abilities.NarutoAbilities;
import com.sekwah.narutoaddon.abilities.mastery.Mastery;
import com.sekwah.narutoaddon.capabilities.INinjaData;
import com.sekwah.narutoaddon.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutoaddon.entity.ShadowCloneEntity;
import com.sekwah.narutoaddon.friends.Friends;
import com.sekwah.narutoaddon.item.ComboSwordItem;
import com.sekwah.narutoaddon.item.weapons.TechniqueSwordItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Hiramekarei: the great hammer-blade held over the shoulder and brought down with the whole body.
 *
 * <p>The sword is not a sword in the usual sense. It is a slab of a blade wide enough to be a shield,
 * and what it is for is weight: everything the caster lands while it is up in both hands comes down
 * half again as hard, because the mass behind the blow is the technique. A ninja who can hold it up
 * long enough to matter can also swing it into the ground, and anything standing on stone or ore
 * loses the ground it was standing on.</p>
 *
 * <p>The slabs also keep what they take. Every blow that lands with the blade drinks a little of the
 * wielder's chakra into the steel, the store deepens with every kill the blade is credited with, and
 * a caster who sneaks while right-clicking lets the whole charge go at once as a wide light-hammer
 * blast.</p>
 *
 * <p>The technique belongs to the sword now: right-clicking a Hiramekarei raises it for ten seconds,
 * and the node on the tree is what teaches the move. There is no combo to remember and no row in
 * the jutsu menu.</p>
 */
public class HiramekareiAbility extends Ability {

    /** A slab of a blade coming down is heard across a courtyard. */
    @Override
    public int noiseRadius() {
        return 14;
    }

    /** No combo: the move lives on the sword's right-click. */
    public static final long COMBO = -1L;

    public static final ResourceLocation ID = new ResourceLocation("narutomod", "hiramekarei");

    /** The mark the melee hook reads: kept a few ticks ahead for as long as the hammer is up. */
    public static final String UNTIL_TAG = "narutomod:hiramekarei_until";

    /** What the hammer does to a blow: the weight behind it. */
    public static final float DAMAGE_FACTOR = 1.5f;

    /** What raising it costs, and how long the arm can keep it up. */
    private static final float OPEN_COST = 50.0f;
    private static final int HOLD_TICKS = 200;

    private static final int CHAKRA_REGEN_DELAY = 20;

    /** Where the stored chakra rides on the blade. */
    public static final String CHAKRA_TAG = "narutomod:hiramekarei_chakra";

    /** Chakra the blade takes from every blow it lands. */
    public static final float CHARGE_PER_HIT = 10.0f;

    /** What the blade can hold, and how much every kill it is credited with adds to that. */
    public static final float MAX_CHAKRA_BASE = 200.0f;
    public static final float MAX_CHAKRA_PER_KILL = 10.0f;

    /** The blast the store becomes: damage, reach and how hard it throws, all growing with the charge. */
    private static final float BLAST_DAMAGE_BASE = 4.0f;
    private static final float BLAST_DAMAGE_RANGE = 26.0f;
    private static final double BLAST_RADIUS_BASE = 6.0d;
    private static final double BLAST_RADIUS_RANGE = 4.0d;
    private static final float BLAST_FACTOR_BASE = 0.6f;
    private static final float BLAST_FACTOR_RANGE = 0.4f;

    /** How hard the blast throws what it does not kill, and how much it lifts it. */
    private static final double BLAST_PUSH = 1.2d;
    private static final double BLAST_LIFT = 0.4d;

    /** Said to the caster who lets go of a blade with nothing in it. */
    private static final String EMPTY_KEY = "jutsu.fail.hiramekarei.empty";

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public long defaultCombo() {
        return COMBO;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        return false;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
    }

    /**
     * Raises the hammer from the sword's right-click. The node on the kenjutsu branch is the gate,
     * and the price is paid once for the whole ten-second hold.
     *
     * @return true when the blade actually went up.
     */
    public static boolean raise(Player player) {
        INinjaData ninjaData = player.getCapability(NinjaCapabilityHandler.NINJA_DATA).resolve().orElse(null);
        if (ninjaData == null || !AbilityUnlocks.isUnlocked(ninjaData, ID)) {
            return false;
        }
        if (!(player.getMainHandItem().getItem() instanceof SwordItem) || isUp(player)) {
            return false;
        }
        float cost = Mastery.cost(ninjaData, NarutoAbilities.HIRAMEKAREI.get(), OPEN_COST);
        if (ninjaData.getChakra() < cost) {
            return false;
        }
        ninjaData.useChakra(cost, CHAKRA_REGEN_DELAY);
        player.getPersistentData().putLong(UNTIL_TAG, player.level().getGameTime() + HOLD_TICKS);
        player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        if (player.level() instanceof ServerLevel level) {
            Vec3 hand = JutsuEffects.handPosition(player);
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, hand.x, hand.y, hand.z,
                    18, 0.3d, 0.4d, 0.3d, 0.02d);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    net.minecraft.sounds.SoundEvents.ANVIL_LAND,
                    net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 0.6f);
        }
        return true;
    }

    /** @return whether the hammer is up right now, asked by the melee hook. */
    public static boolean isUp(Player player) {
        return player.getPersistentData().getLong(UNTIL_TAG) > player.level().getGameTime();
    }

    /**
     * The hammer brought down into the ground under a target: the block a body is standing on is
     * broken out from beneath it, which is the second use of a blade this heavy.
     */
    public static void crushUnderfoot(Player caster, LivingEntity victim) {
        if (!(caster.level() instanceof ServerLevel level)) {
            return;
        }
        BlockPos pos = victim.getOnPos();
        BlockState state = level.getBlockState(pos);
        if (!breakable(state) || !level.mayInteract(caster, pos)) {
            return;
        }
        level.destroyBlock(pos, true, caster);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.getX() + 0.5d, pos.getY() + 0.5d,
                pos.getZ() + 0.5d, 8, 0.3d, 0.2d, 0.3d, 0.02d);
    }

    /** @return whether the block is stone or ore, the two things a hammer of this size breaks. */
    private static boolean breakable(BlockState state) {
        if (state.isAir() || state.hasBlockEntity()) {
            return false;
        }
        return state.is(BlockTags.BASE_STONE_OVERWORLD)
                || state.is(BlockTags.STONE_ORE_REPLACEABLES)
                || state.getBlock() instanceof DropExperienceBlock;
    }

    /** @return the chakra the blade is holding. */
    public static float chakra(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? 0.0f : tag.getFloat(CHAKRA_TAG);
    }

    /** @return how much the blade can hold: a base store, plus a little for every kill it has taken. */
    public static int maxChakra(ItemStack stack) {
        return (int) (MAX_CHAKRA_BASE + MAX_CHAKRA_PER_KILL * ComboSwordItem.getKills(stack));
    }

    /** Puts chakra into the blade, never past what it can hold and never below nothing. */
    public static void setChakra(ItemStack stack, float value) {
        float clamped = Math.min(Math.max(value, 0.0f), maxChakra(stack));
        stack.getOrCreateTag().putFloat(CHAKRA_TAG, clamped);
    }

    /** Melee hook: every blow the blade lands puts a little more into the slabs. */
    public static void charge(ItemStack stack) {
        charge(stack, CHARGE_PER_HIT);
    }

    /**
     * Puts a given amount into the slabs, clipped by {@link #setChakra}.
     *
     * <p>A blow the blade lands pays {@link #CHARGE_PER_HIT}; a blow the blade takes on the guard
     * pays what the guard swallowed instead, which is worth more the harder the blow was.</p>
     */
    public static void charge(ItemStack stack, float amount) {
        if (amount <= 0.0f) {
            return;
        }
        setChakra(stack, chakra(stack) + amount);
    }

    /** @return whether the stack is the hammer-blade itself, asked by name rather than by instance. */
    public static boolean isBlade(ItemStack stack) {
        return stack.getItem() instanceof TechniqueSwordItem sword
                && sword.art() == TechniqueSwordItem.Art.HIRAMEKAREI;
    }

    /** @return how full the blade is, which is what its blast is worth. */
    public static float chargeRatio(ItemStack stack) {
        return chakra(stack) / Math.max(1.0f, maxChakra(stack));
    }

    /** @return what the stored charge is worth as damage if it is let go right now. */
    public static float releaseDamage(ItemStack stack) {
        return BLAST_DAMAGE_BASE + BLAST_DAMAGE_RANGE * chargeRatio(stack);
    }

    /** @return how far the stored charge reaches if it is let go right now. */
    public static double releaseRadius(ItemStack stack) {
        return BLAST_RADIUS_BASE + BLAST_RADIUS_RANGE * chargeRatio(stack);
    }

    /**
     * Lets the stored chakra go: a wide light-hammer blast around the caster that spends the whole
     * store at once. This is the sneaking right-click of the blade, so it costs nothing beyond the
     * chakra that was already in the slabs.
     *
     * @return true when the blast actually went off.
     */
    public static boolean release(Player player) {
        ItemStack stack = player.getMainHandItem();
        if (!isBlade(stack)) {
            return false;
        }
        INinjaData ninjaData = player.getCapability(NinjaCapabilityHandler.NINJA_DATA).resolve().orElse(null);
        if (ninjaData == null || !AbilityUnlocks.isUnlocked(ninjaData, ID)) {
            return false;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }
        if (chakra(stack) <= 0.0f) {
            player.displayClientMessage(Component.translatable(EMPTY_KEY), true);
            return false;
        }

        float ratio = chargeRatio(stack);
        float damage = releaseDamage(stack);
        double radius = releaseRadius(stack);
        float factor = BLAST_FACTOR_BASE + BLAST_FACTOR_RANGE * ratio;

        // Spend the whole charge.
        setChakra(stack, 0.0f);

        // Damage and knock back every enemy in range.
        Vec3 centre = player.position();
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(radius),
                e -> e.isAlive() && e != player);
        for (LivingEntity target : targets) {
            if (!canHit(player, target) || target.position().distanceTo(centre) > radius) {
                continue;
            }
            target.hurt(player.damageSources().playerAttack(player), damage);
            Vec3 away = target.position().subtract(centre);
            if (away.lengthSqr() < 0.01d) {
                away = player.getLookAngle().reverse();
            }
            away = new Vec3(away.x, 0.0d, away.z).normalize().scale(BLAST_PUSH * factor);
            target.push(away.x, BLAST_LIFT * factor, away.z);
            target.hurtMarked = true;
            target.fallDistance = 0.0f;
        }

        // The light hammer: a ring at the edge of the blast, spokes back to the caster, and a burst
        // in the middle, all of it wider and thicker the more the blade had in it.
        int ringCount = Math.max(1, (int) (36 * factor));
        for (int i = 0; i < ringCount; i++) {
            double angle = i * Math.PI * 2.0d / ringCount;
            double x = centre.x + Math.cos(angle) * radius;
            double z = centre.z + Math.sin(angle) * radius;
            level.sendParticles(ParticleTypes.END_ROD, x, centre.y + 1.0d, z,
                    1, 0.0d, 0.0d, 0.0d, 0.0d);
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, x, centre.y + 1.0d, z,
                    1, 0.0d, 0.0d, 0.0d, 0.0d);
            level.sendParticles(ParticleTypes.CLOUD, x, centre.y + 0.5d, z,
                    Math.max(1, (int) (2 * factor)), 0.2d, 0.1d, 0.2d, 0.02d);
        }
        int spokeCount = Math.max(1, (int) (12 * factor));
        int spokeSteps = Math.max(1, (int) (9 * factor));
        for (int i = 0; i < spokeCount; i++) {
            double angle = i * Math.PI * 2.0d / spokeCount;
            for (int r = 1; r <= spokeSteps; r++) {
                double x = centre.x + Math.cos(angle) * r;
                double z = centre.z + Math.sin(angle) * r;
                level.sendParticles(ParticleTypes.CLOUD, x, centre.y + 0.5d, z,
                        1, 0.05d, 0.05d, 0.05d, 0.0d);
            }
        }
        level.sendParticles(ParticleTypes.END_ROD, centre.x, centre.y + 1.0d, centre.z,
                Math.max(1, (int) (24 * factor)), 0.6d, 0.8d, 0.6d, 0.05d);
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, centre.x, centre.y + 1.0d, centre.z,
                Math.max(1, (int) (6 * factor)), 0.8d, 0.4d, 0.8d, 0.0d);
        level.sendParticles(ParticleTypes.CLOUD, centre.x, centre.y + 0.5d, centre.z,
                Math.max(1, (int) (20 * factor)), 0.8d, 0.3d, 0.8d, 0.03d);

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0f, 1.2f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 0.8f);
        return true;
    }

    /** Anything alive is fair game except the caster, their allies and the untouchable. */
    private static boolean canHit(Player caster, LivingEntity target) {
        if (target == caster || target.isRemoved() || !target.isAlive() || target.isSpectator()) {
            return false;
        }
        if (target instanceof Player other && (other.isCreative() || other.isSpectator())) {
            return false;
        }
        if (target.isAlliedTo(caster) || Friends.areFriends(caster, target)) {
            return false;
        }
        return !(target instanceof ShadowCloneEntity clone && clone.isOwnedBy(caster));
    }
}
