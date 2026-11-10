package com.sekwah.narutoaddon.abilities.jutsus;

import com.sekwah.narutoaddon.capabilities.INinjaData;
import com.sekwah.narutoaddon.config.NarutoConfig;
import com.sekwah.narutoaddon.skills.SkillEffect;
import com.sekwah.narutoaddon.skills.SkillTree;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/**
 * The Byakugan: the all-seeing eye of the Hyuga, worn as a stance.
 *
 * <p>It is the eye of the circle. Almost everything the caster could want to know is already in the
 * world around them - a mob behind a wall, a player in a hollow under the floor, a hunt gathering at
 * their back - and the Byakugan simply stops pretending those things are hidden. The picture itself
 * is drawn on the client by the same renderer the chakra sense uses: while the eye is open every
 * living body within {@link #VISION_RADIUS} blocks is shown as its own outline through the terrain,
 * which is exactly the "circular vision" the eye is famous for.</p>
 *
 * <p>It hands out no capacity and no damage. What it buys is information, and information has a
 * price like anything else: a cheap trickle of chakra for as long as the eye is open.</p>
 *
 * <p>Its close-quarters half is {@link RotationAbility}.</p>
 */
public class ByakuganAbility extends AbstractDojutsuAbility {

    /** An eye that only watches is quiet. */
    @Override
    public int noiseRadius() {
        return 0;
    }

    /** V B V V V V - the base eye, and the root of the Byakugan branch. */
    public static final long COMBO = 3111L;

    public static final ResourceLocation ID = new ResourceLocation("narutomod", "byakugan");

    /**
     * How far the circular vision reaches before any training, in blocks. Deliberately short: the eye
     * shows everything in that bubble through walls, so a large number would turn every fight into a
     * map. The tree widens it a step at a time; see {@link #visionRadius(INinjaData)}.
     */
    public static final float VISION_RADIUS = 16.0f;

    /** Chakra burnt for every tick the eye stays open. */
    private static final float CHAKRA_PER_TICK = 0.4f;

    /**
     * How much harder a gentle-fist technique lands once it is mastered: nothing at the first cast,
     * seventy-five per cent more at full mastery. The eye's arts are paid for by the tick or by a
     * long cooldown, so their damage is the one thing that has to grow visibly with use - otherwise
     * a mastered palm and the first palm anyone ever threw are the same blow.
     */
    public static final float MASTERY_DAMAGE_BONUS = 0.75f;

    /**
     * The damage a Byakugan technique lands, for how well its caster knows it. Mastery is read
     * through the registry key, never through the class's own {@code ID} constant: several of these
     * abilities still spell their id under the legacy {@code narutomod} namespace and the casts are
     * stored under {@code narutoaddon}.
     */
    public static float masteredDamage(INinjaData ninjaData, com.sekwah.narutoaddon.abilities.Ability ability,
                                       float base) {
        if (ninjaData == null || ability == null) {
            return base;
        }
        return base * (1.0f + MASTERY_DAMAGE_BONUS
                * com.sekwah.narutoaddon.abilities.mastery.Mastery.level(ninjaData, ability));
    }

    @Override
    public long defaultCombo() {
        return COMBO;
    }

    @Override
    protected ResourceLocation eyeId() {
        return ID;
    }

    @Override
    protected boolean enabled() {
        return NarutoConfig.byakuganEnabled;
    }

    @Override
    protected String disabledKey() {
        return "jutsu.fail.byakugan.disabled";
    }

    @Override
    protected float chakraPerTick() {
        return CHAKRA_PER_TICK;
    }

    /**
     * How much of the upkeep is still paid at full mastery. A fresh eye costs its full trickle; a
     * mastered one is a whisper - the eye is worn all the time by a Hyuga who has lived with it, so
     * a drain that empties a bar over a fight was the wrong shape for it. The generic mastery
     * discount still applies on top; this is the eye's own curve.
     */
    private static final float UPKEEP_AT_FULL_MASTERY = 0.1f;

    @Override
    protected float upkeepCost(INinjaData ninjaData) {
        float cost = super.upkeepCost(ninjaData);
        float mastery = com.sekwah.narutoaddon.abilities.mastery.Mastery.level(ninjaData, this);
        return cost * Mth.lerp(mastery, 1.0f, UPKEEP_AT_FULL_MASTERY);
    }

    /**
     * A mastered eye does not hold the caster's own regeneration back any more: once the upkeep is no
     * larger than the trickle the body makes by itself, switching that trickle off would cost far
     * more than the upkeep does. That is the difference between "cheap" and "nearly free".
     */
    @Override
    protected boolean upkeepInterruptsRegen(float cost) {
        return cost > NarutoConfig.chakraRegen;
    }

    @Override
    protected void aura(ServerLevel level, Player player, INinjaData ninjaData) {
        // A pale ring of chakra around the head, thin enough that the vision itself stays the
        // noticeable part of the eye.
        if (player.tickCount % 6 == 0) {
            level.sendParticles(ParticleTypes.END_ROD, player.getX(),
                    player.getY() + player.getBbHeight() * 0.92d, player.getZ(),
                    2, 0.2d, 0.05d, 0.2d, 0.005d);
        }
    }

    /**
     * How far this caster's circular vision actually reaches: the base bubble plus whatever the two
     * widening nodes of the branch add. Read on the client by {@code ClientSensingData}, which draws
     * the picture, so the tree widens the view itself rather than only the numbers on a tooltip.
     */
    public static float visionRadius(INinjaData ninjaData) {
        return VISION_RADIUS + SkillTree.bonus(ninjaData.getSkillData().getUnlocked(),
                SkillEffect.BYAKUGAN_RANGE);
    }

    /** @return whether the caster has the Byakugan open right now. */
    public static boolean isActive(INinjaData ninjaData) {
        return isEyeActive(ninjaData, ID);
    }
}
