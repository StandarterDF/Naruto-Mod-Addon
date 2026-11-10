package com.sekwah.narutoaddon.capabilities;

import com.mojang.logging.LogUtils;
import com.sekwah.narutoaddon.abilities.Ability;
import com.sekwah.narutoaddon.abilities.NarutoAbilities;
import com.sekwah.narutoaddon.abilities.AbilityUnlocks;
import com.sekwah.narutoaddon.abilities.utility.ChakraChargeAbility;
import com.sekwah.narutoaddon.capabilities.absorption.AbsorptionData;
import com.sekwah.narutoaddon.capabilities.disabledabilitydata.DisabledAbilityData;
import com.sekwah.narutoaddon.capabilities.mastery.MasteryData;
import com.sekwah.narutoaddon.capabilities.mastery.MeditationData;
import com.sekwah.narutoaddon.capabilities.skills.SkillData;
import com.sekwah.narutoaddon.capabilities.toggleabilitydata.ToggleAbilityData;
import com.sekwah.narutoaddon.config.NarutoConfig;
import com.sekwah.narutoaddon.effect.NarutoEffects;
import com.sekwah.narutoaddon.events.GenjutsuEvents;
import com.sekwah.narutoaddon.gameevents.NarutoGameEvents;
import com.sekwah.narutoaddon.item.ChakraBatteryItem;
import com.sekwah.narutoaddon.menu.SupplyChestContainer;
import com.sekwah.narutoaddon.registries.NarutoRegistries;
import com.sekwah.narutoaddon.skills.SkillEffect;
import com.sekwah.narutoaddon.skills.SkillTree;
import com.sekwah.sekclib.capabilitysync.capabilitysync.annotation.Sync;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Consumer;

public class NinjaData implements INinjaData, ICapabilityProvider {

    private static final Logger LOGGER = LogUtils.getLogger();

    @Sync(minTicks = 1)
    private float chakra;

    @Sync(minTicks = 1)
    private float stamina;

    @Sync(minTicks = 1)
    private float substitutions;

    @Sync
    private float maxChakra;

    /**
     * Chakra capacity granted on top of the configured maximum, see
     * {@code INinjaData#getChakraCapacityBonus()}. Kept out of the save file: it is recomputed every
     * tick by whatever is handing it out (the meditation clones) and means nothing without them.
     */
    @Sync(minTicks = 1)
    private float chakraCapacityBonus;

    /**
     * Chakra capacity handed out by the armour the player is wearing, see
     * {@code events/NarutoArmorEvents}. Kept apart from {@link #chakraCapacityBonus} so the
     * meditation clones and the armour do not fight over one field, and out of the save file like it.
     */
    @Sync(minTicks = 1)
    private float armorChakraBonus;
    @Sync(minTicks = 1)
    private float enchantChakraBonus;
    @Sync(minTicks = 1)
    private float enchantStaminaBonus;
    private float enchantSeninBonus;
    private boolean gudodamaForm;

    /**
     * Chakra capacity handed out by an open eye, see {@code RinneganAbility}. A third separate field
     * for the same reason the armour has one: the eye and the armour must be able to stop giving
     * their bonus without wiping the other's. Not saved, not synced for the HUD's sake but for the
     * bar's maximum, which the client draws from the same number.
     */
    @Sync(minTicks = 1)
    private float eyeChakraBonus;

    /**
     * Nature energy capacity handed out by an open eye, see {@code RinneganAbility}. Same rules as
     * {@link #eyeChakraBonus}.
     */
    private float eyeSeninBonus;

    /**
     * The max health bonus the worn armour last wrote onto the player's attribute. Server side only,
     * used to tell whether the attribute modifier needs rebuilding.
     */
    private float armorHealthBonus;

    /**
     * Shield bought with chakra, see {@code ChakraShieldAbility}. Shown as armour points and spent
     * point for point as damage comes in, so it is really a second health bar the armour display
     * happens to render.
     */
    @Sync(minTicks = 1)
    private float shieldPoints;

    /**
     * Whether the second breath trick is armed. Synced so the jutsu menu can show the technique as
     * readied without the client having to guess from an effect that only appears on the killing
     * blow.
     */
    @Sync(minTicks = 1)
    private boolean secondBreathArmed;

    /**
     * How many of the eight inner gates are open. Synced so the client can paint the aura of the
     * current gate and the HUD can show the count; the numbers of the gates themselves stay on the
     * server.
     */
    @Sync(minTicks = 1)
    private int gateCount;

    /**
     * Fraction of a point of health the open gates have ground away but not yet been paid. The
     * backlash is charged in whole points of health, so the remainder is carried here between
     * drain ticks. Server side only and not written to disk, exactly like {@link #gateCount}.
     */
    private float gateBacklashCarry;

    /**
     * If the player should have access to all the ninja shit
     */
    @Sync(syncGlobally = true)
    private boolean ninjaModeEnabled;

    @Sync
    private float maxStamina;

    @Sync(minTicks = 1)
    private float trainingWeightsStaminaBonus = 0.0f;

    @Sync(minTicks = 1)
    private float trainingWeightsRegenBonus = 0.0f;

    // Stat scrolls: permanent bonuses from scrolls found in the world.
    @Sync
    private float chakraBonus = 0.0f;

    @Sync
    private float staminaBonus = 0.0f;

    @Sync
    private float chakraRegenBonus = 0.0f;

    @Sync
    private float staminaRegenBonus = 0.0f;

    @Sync
    private int vitalityBonus = 0;

    @Sync
    private int strengthBonus = 0;

    // Unless the player needs to know the max for rendering, no point in rendering.
    private float maxSubstitutions;

    @Sync
    private Vec3 substitutionLocation;

    @Sync
    private ResourceLocation substitutionDimension;

    /**
     * Where this player's chakra marker log stands. Kept server side only: nothing on the client
     * needs to know, and unlike the substitution marker it is written to disk so a return marker
     * survives logging out.
     */
    private Vec3 markerLocation;

    private ResourceLocation markerDimension;

    /**
     * The supply chest a worker clone deposits into, see {@code StorageChestAbility}. It is not a
     * block: it lives right here in the caster's own data, so a worker can reach it from anywhere and
     * it survives a logout. There is always exactly one.
     */
    private final SupplyChestContainer supplyChest = new SupplyChestContainer(SupplyChestContainer.MIN_SIZE);

    /**
     * Where a caster who walked into the Kamui pocket dimension is to come back out, see
     * {@code KamuiJutsuAbility}. Server side only and written to disk, so logging out inside the
     * void is not a one way trip.
     */
    private Vec3 kamuiReturnLocation;

    private ResourceLocation kamuiReturnDimension;

    /**
     * Nature energy gathered by standing still and burned by sage mode, see {@code SageModeAbility}.
     * Written to disk (a gathered pool should not evaporate on logout) and synced for the HUD.
     */
    @Sync(minTicks = 1)
    private float seninMana;

    /**
     * Whether sage mode is up. Synced for the HUD and the client side effects, and never written to
     * disk: death and logging out both end it.
     */
    @Sync(minTicks = 1)
    private boolean sageMode;

    private int sixPathsOrbs;

    /**
     * Truth-Seeking Orbs left in the Tenseigan halo, 0..6. Synced because the halo is drawn on the
     * client; transient like {@link #sixPathsOrbs} - the orbs are part of the open eye, not a saved
     * resource.
     */
    @Sync(minTicks = 1)
    private int tenseiganOrbs = 6;

    private int sealVesselIntegrity;

    /**
     * Reach of the held chakra sense, or zero while it is not up. Synced because the client is what
     * draws the sense - the glow on creatures and the structure markers - and never written to disk:
     * it is a held state, gone the moment the keys are let go. See {@code ChakraSensingAbility}.
     */
    private float sensingRadius;

    /**
     * Chakra funded night vision left, in ticks. Not written to disk, like {@link #sageMode}; it is
     * synced at a slow cadence so the jutsu menu can show the sensing row as active while the tap is
     * running, even though the effect itself is a vanilla one the client already sees.
     */
    private int nightVisionTicks;

    /**
     * UUID of the player whose form the caster is wearing, or an empty string. Synced because the
     * client renderer is what turns it into a copied skin and name tag, and deliberately not written
     * to disk: logging out drops the disguise.
     */
    private String hengeTarget = "";

    /**
     * Registry id of the mob whose form the caster is wearing, or an empty string for a player (or no
     * disguise at all). Synced for the same reason as {@link #hengeTarget}: the client renderer is
     * the only thing that reads it.
     */
    private String hengeEntityType = "";

    /** Display name that goes with {@link #hengeTarget}, synced for the same reason. */
    private String hengeName = "";

    /** Ticks of disguise left, server side only. */
    private int hengeTicks;

    /**
     * If the player can double jump, will be updated by underlying values server side.
     *
     * Will be true if the player has enough chakra as well as
     */
    private DoubleJumpData doubleJumpData;

    /**
     * The current ability being charged/channeled
     * <p>
     * TODO make this global then expand the channeled logic to be able to handle any visual effects easier.
     * TODO possibly swap the type of this over to an Ability type so that it needs to be looked up less.
     */
    @Sync(minTicks = 1)
    private ResourceLocation currentlyChanneled;

    @Sync(minTicks = 1)
    private int ticksChanneled;

    private ToggleAbilityData toggleAbilityData;

    /**
     * Per-technique cast counts, the raw material mastery is worked out from, see
     * {@code abilities/mastery/Mastery}. Synced, because the jutsu menu draws a progress bar for
     * every technique from it.
     */
    @Sync(minTicks = 1)
    private MasteryData masteryData = new MasteryData();

    /**
     * The skill tree nodes this caster has bought with skill points, plus the points themselves.
     * Synced because the skill screen and the HUD both read it, and saved so neither is ever lost.
     */
    @Sync(minTicks = 1)
    private SkillData skillData = new SkillData();

    /**
     * Techniques switched off from the jutsu menu, see {@link DisabledAbilityData}. Synced, because
     * the menu greys the rows from it and skips the cast packet, and saved, because a technique the
     * player deliberately put aside should stay aside after a relog.
     */
    @Sync(minTicks = 1)
    private DisabledAbilityData disabledAbilityData = new DisabledAbilityData();

    /**
     * Whether clicks in the jutsu menu may cast any technique, not only the ones that stay on, see
     * {@code MenuClickCommand}. Off by default: the menu is a reference, and a stray click firing a
     * one-shot cast is a nuisance. Synced so the menu itself knows which rows it may mark as
     * clickable, and saved so a player who asked for it keeps it after a relog.
     */
    @Sync(minTicks = 1)
    private boolean menuClickAll;

    /**
     * Chakra swallowed through the palm Rinnegan and not yet spent, per nature. Synced for the HUD
     * and never written to disk: it is a held stance, and dying or logging out drops the stance.
     */
    private AbsorptionData absorptionData = new AbsorptionData();

    /**
     * Bosses whose one-time milestone reward this ninja has already collected, by short id. Kept
     * server side and saved so a milestone cannot be farmed by killing the same boss again.
     */
    private final Set<String> claimedBossMilestones = new HashSet<>();

    /** The ninja's rank id, see {@code ranks/NinjaRank}. Server side only, saved with the rest. */
    private String ninjaRank = "";

    /** The element the ninja was born to, see {@code abilities/Nature}; empty before the choice. */
    @Sync
    private String natureAffinity = "";

    /**
     * Whether the starting handbook has already been handed out. Ninja mode can be switched off and
     * on again, so "became a ninja just now" is not enough to decide: without a flag of its own the
     * awakening path would give a second handbook on every toggle, which used to be a free book
     * printer. See {@code ServerToggleNinjaPacket}.
     */
    private boolean handbookGiven;

    /**
     * The standing meditating clones of this caster, see {@code MeditationData}. Server side only,
     * deliberately neither synced nor saved: a caster who dies or logs out keeps no meditations.
     */
    private final MeditationData meditationData = new MeditationData();


    /**
     * Depending on what's going might want to swap to a potion effect.
     * This will make the player truly invisible.
     */
    @Sync(minTicks = 1, syncGlobally = true)
    private boolean isInvisible = false;

    private int invisibleTicks = 0;


    private ArrayList<DelayedPlayerTickEvent> delayedTickEvents = new ArrayList<>();
    // CooldownTickEvent to follow the same style you have for DelayedPlayerTickEvent
    private HashMap<String, CooldownTickEvent> cooldownTickEvents = new HashMap<>();

    public NinjaData(boolean isServer) {
        if (isServer) {
            this.getConfigData();
            this.stamina = this.maxStamina;
            this.chakra = this.maxChakra;
        }
        this.toggleAbilityData = new ToggleAbilityData();
        this.doubleJumpData = new DoubleJumpData(false);
    }

    static class RegenInfo {
        public int cooldown;

        public RegenInfo() {
        }

        /**
         * Tick down when checked
         *
         * @return if regen should take place
         */
        public boolean canRegen() {
            if (this.cooldown > 0) {
                this.cooldown--;
                return false;
            }
            return true;
        }
    }

    private final RegenInfo chakraRegenInfo = new RegenInfo();
    private final RegenInfo staminaRegenInfo = new RegenInfo();

    private static final String CHAKRA_TAG = "chakra";
    private static final String STAMINA_TAG = "stamina";
    private static final String NINJA_MODE_ENABLED = "ninjaModeEnabled";
    private static final String DOUBLE_JUMP_TAG = "doubleJumpReady";
    private static final String SAVE_TIME = "save_time";
    private static final String COOLDOWN_TAG = "cooldowns";
    private static final String SUBSTITUTION_TAG = "substitutions";
    private static final String MARKER_TAG = "chakraMarker";
    private static final String MARKER_DIMENSION_TAG = "chakraMarkerDimension";
    private static final String SUPPLY_CHEST_TAG = "supplyChest";
    private static final String KAMUI_RETURN_TAG = "kamuiReturn";
    private static final String KAMUI_RETURN_DIMENSION_TAG = "kamuiReturnDimension";
    private static final String SENIN_MANA_TAG = "seninMana";
    /**
     * The bought chakra shield and the armed second breath live in the bar rather than in a block or
     * an effect, so they have to be written down: a caster who spent the whole bar on the shield must
     * not find it gone after a relogin, and the same goes for a readied second breath.
     */
    private static final String SHIELD_TAG = "shieldPoints";
    private static final String SECOND_BREATH_TAG = "secondBreathArmed";
    private static final String MASTERY_TAG = "mastery";
    private static final String SKILLS_TAG = "skills";
    private static final String SKILL_POINTS_TAG = "skillPoints";
    private static final String SCROLLS_TAG = "scrolls";
    private static final String BOSS_MILESTONES_TAG = "bossMilestones";
    private static final String NINJA_RANK_TAG = "ninjaRank";
    private static final String CHAKRA_BONUS_TAG = "chakraBonus";
    private static final String STAMINA_BONUS_TAG = "staminaBonus";
    private static final String CHAKRA_REGEN_BONUS_TAG = "chakraRegenBonus";
    private static final String STAMINA_REGEN_BONUS_TAG = "staminaRegenBonus";
    private static final String VITALITY_BONUS_TAG = "vitalityBonus";
    private static final String STRENGTH_BONUS_TAG = "strengthBonus";
    private static final String NATURE_AFFINITY_TAG = "natureAffinity";
    private static final String HANDBOOK_GIVEN_TAG = "handbookGiven";
    private static final String DISABLED_ABILITIES_TAG = "disabledAbilities";
    private static final String MENU_CLICK_ALL_TAG = "menuClickAll";
    private static final String GUDODAMA_FORM_TAG = "gudodamaForm";
    private static final String GATE_COUNT_TAG = "gateCount";
    private static final String SAGE_MODE_TAG = "sageMode";
    private static final String SIX_PATHS_ORBS_TAG = "sixPathsOrbs";
    private static final String TENSEIGAN_ORBS_TAG = "tenseiganOrbs";
    private static final String SEAL_VESSEL_TAG = "sealVesselIntegrity";
    private static final String SENSING_RADIUS_TAG = "sensingRadius";
    private static final String NIGHT_VISION_TAG = "nightVisionTicks";
    private static final String HENGE_TARGET_TAG = "hengeTarget";
    private static final String HENGE_ENTITY_TYPE_TAG = "hengeEntityType";
    private static final String HENGE_NAME_TAG = "hengeName";
    private static final String ABSORPTION_TAG = "absorption";
    private static final String TOGGLE_ABILITIES_TAG = "toggleAbilities";
    private static final String EYE_SENIN_BONUS_TAG = "eyeSeninBonus";
    private static final String ENCHANT_SENIN_BONUS_TAG = "enchantSeninBonus";
    /** Below this share of the pool the caster is running on fumes and moves slower. */
    private static final float EXHAUSTION_THRESHOLD = 0.15f;

    /** Short and refreshed every tick, so it drops the moment the pool recovers. */
    private static final int EXHAUSTION_TICKS = 40;

    /** How much faster the passive trickle runs while the chakra flow effect is up. */
    private static final float CHAKRA_FLOW_MULTIPLIER = 4.0f;

    /**
     * Creative casts are not free, they are fast: the pool still empties, it just comes back ten
     * times quicker. That keeps creative mode useful for testing a technique's cost without making
     * every bar meaningless.
     */
    private static final float CREATIVE_REGEN_MULTIPLIER = 10.0f;    private final LazyOptional<INinjaData> holder = LazyOptional.of(() -> this);

    @Override
    public float getChakra() {
        return this.chakra;
    }

    @Override
    public float getMaxChakra() {
        return this.maxChakra + this.chakraCapacityBonus + this.armorChakraBonus + this.eyeChakraBonus
                + this.enchantChakraBonus
                + ChakraChargeAbility.masteryChakraBonus(this)
                + SkillTree.bonus(this.skillData.getUnlocked(), SkillEffect.MAX_CHAKRA)
                + SkillTree.bonus(this.skillData.getUnlocked(), SkillEffect.ALL_ROUNDED)
                + this.chakraBonus;
    }

    @Override
    public float getChakraCapacityBonus() {
        return this.chakraCapacityBonus;
    }

    @Override
    public void setChakraCapacityBonus(float bonus) {
        this.chakraCapacityBonus = bonus;
    }

    @Override
    public float getArmorChakraBonus() {
        return this.armorChakraBonus;
    }

    @Override
    public void setArmorChakraBonus(float bonus) {
        this.armorChakraBonus = bonus;
    }

    @Override
    public float getArmorHealthBonus() {
        return this.armorHealthBonus;
    }

    @Override
    public void setArmorHealthBonus(float bonus) {
        this.armorHealthBonus = bonus;
    }

    @Override
    public float getEyeChakraBonus() {
        return this.eyeChakraBonus;
    }

    @Override
    public void setEyeChakraBonus(float bonus) {
        this.eyeChakraBonus = bonus;
    }

    @Override
    public float getEyeSeninBonus() {
        return this.eyeSeninBonus;
    }

    @Override
    public void setEyeSeninBonus(float bonus) {
        this.eyeSeninBonus = bonus;
    }

    @Override
    public void setEnchantChakraBonus(float bonus) {
        this.enchantChakraBonus = bonus;
    }

    @Override
    public void setEnchantStaminaBonus(float bonus) {
        this.enchantStaminaBonus = bonus;
    }

    @Override
    public void setEnchantSeninBonus(float bonus) {
        this.enchantSeninBonus = bonus;
    }

    @Override
    public boolean isGudodamaForm() {
        return this.gudodamaForm;
    }

    @Override
    public void setGudodamaForm(boolean active) {
        this.gudodamaForm = active;
    }
    @Override
    public float getShieldPoints() {
        return this.shieldPoints;
    }

    @Override
    public void setShieldPoints(float points) {
        this.shieldPoints = points;
    }

    @Override
    public boolean isSecondBreathArmed() {
        return this.secondBreathArmed;
    }

    @Override
    public void setSecondBreathArmed(boolean armed) {
        this.secondBreathArmed = armed;
    }

    @Override
    public int getGateCount() {
        return this.gateCount;
    }

    @Override
    public void setGateCount(int gates) {
        this.gateCount = gates;
    }

    @Override
    public float getGateBacklashCarry() {
        return this.gateBacklashCarry;
    }

    @Override
    public void setGateBacklashCarry(float carry) {
        this.gateBacklashCarry = carry;
    }

    @Override
    public float getStamina() {
        return this.stamina;
    }

    @Override
    public float getSubstitutionCount() {
        return this.substitutions;
    }

    @Override
    public float getMaxSubstitutions() {
        return this.maxSubstitutions
                + SkillTree.bonus(this.skillData.getUnlocked(), SkillEffect.MAX_SUBSTITUTIONS);
    }

    @Override
    public float getMaxStamina() {
        return this.maxStamina
                + SkillTree.bonus(this.skillData.getUnlocked(), SkillEffect.MAX_STAMINA)
                + SkillTree.bonus(this.skillData.getUnlocked(), SkillEffect.ALL_ROUNDED)
                + this.trainingWeightsStaminaBonus
                + this.staminaBonus
                + this.enchantStaminaBonus;
    }

    @Override
    public void setChakra(float chakra) {
        this.chakra = chakra;
    }

    @Override
    public void setStamina(float stamina) {
        this.stamina = stamina;
    }

    @Override
    public void useChakra(float amount, int cooldown) {
        this.chakra -= amount;
        this.chakraRegenInfo.cooldown = Math.max(cooldown, this.chakraRegenInfo.cooldown);
    }

    /** Extra stamina every action costs, from the training weights the body is carrying. */
    private float staminaCostMultiplier = 1.0f;

    @Override
    public void useStamina(float amount, int cooldown) {
        this.stamina -= amount * this.staminaCostMultiplier;
        this.staminaRegenInfo.cooldown = Math.max(cooldown, this.staminaRegenInfo.cooldown);
    }

    @Override
    public void useSubstitution(float amount) {
        this.substitutions -= amount;
    }

    @Override
    public void addChakra(float amount) {
        this.chakra = Math.min(Math.max(this.chakra + amount, 0), this.getMaxChakra());
    }

    @Override
    public void addStamina(float amount) {
        this.stamina = Math.min(Math.max(this.stamina + amount, 0), this.getMaxStamina());
    }

    @Override
    public float getChakraBonus() {
        return this.chakraBonus;
    }

    @Override
    public void addChakraBonus(float amount) {
        this.chakraBonus += amount;
    }

    @Override
    public float getStaminaBonus() {
        return this.staminaBonus;
    }

    @Override
    public void addStaminaBonus(float amount) {
        this.staminaBonus += amount;
    }

    @Override
    public float getChakraRegenBonus() {
        return this.chakraRegenBonus;
    }

    @Override
    public void addChakraRegenBonus(float amount) {
        this.chakraRegenBonus += amount;
    }

    @Override
    public float getStaminaRegenBonus() {
        return this.staminaRegenBonus;
    }

    @Override
    public void addStaminaRegenBonus(float amount) {
        this.staminaRegenBonus += amount;
    }

    @Override
    public int getVitalityBonus() {
        return this.vitalityBonus;
    }

    @Override
    public void addVitalityBonus(int amount) {
        this.vitalityBonus += amount;
    }

    @Override
    public int getStrengthBonus() {
        return this.strengthBonus;
    }

    @Override
    public void addStrengthBonus(int amount) {
        this.strengthBonus += amount;
    }

    @Override
    public void setInvisibleTicks(int ticks) {
        this.invisibleTicks = ticks;
    }

    @Override
    public boolean getInvisible() {
        return this.isInvisible;
    }

    @Override
    public Vec3 getSubstitutionLoc() {
        return this.substitutionLocation;
    }

    @Override
    public ResourceLocation getSubstitutionDimension() {
        return this.substitutionDimension;
    }

    @Override
    public void setSubstitutionLoc(Vec3 loc, ResourceLocation dimension) {
        this.substitutionLocation = loc;
        this.substitutionDimension = dimension;
    }

    @Override
    public Vec3 getMarkerLoc() {
        return this.markerLocation;
    }

    @Override
    public ResourceLocation getMarkerDimension() {
        return this.markerDimension;
    }

    @Override
    public void setMarkerLoc(Vec3 loc, ResourceLocation dimension) {
        this.markerLocation = loc;
        this.markerDimension = dimension;
    }

    @Override
    public SupplyChestContainer getSupplyChest() {
        return this.supplyChest;
    }

    @Override
    public Vec3 getKamuiReturnLoc() {
        return this.kamuiReturnLocation;
    }

    @Override
    public ResourceLocation getKamuiReturnDimension() {
        return this.kamuiReturnDimension;
    }

    @Override
    public void setKamuiReturnLoc(Vec3 loc, ResourceLocation dimension) {
        this.kamuiReturnLocation = loc;
        this.kamuiReturnDimension = dimension;
    }

    @Override
    public float getSeninMana() {
        return this.seninMana;
    }

    @Override
    public void setSeninMana(float mana) {
        this.seninMana = Math.min(Math.max(mana, 0.0f), this.getMaxSeninMana());
    }

    @Override
    public void addSeninMana(float mana) {
        // The open mode burns the pool and cannot be refuelled while it burns. Every source - the
        // passive trickle, the gathering technique, meditating clones, an armour enchantment - comes
        // through here, so this is the one place that guarantees a sage always runs out: without it,
        // any trickle at all (one clone's 0.2 a tick against a 0.02 burn) held the mode up forever
        // and the whole trade of the branch - gather first, then fight - disappeared.
        if (mana > 0.0f && this.isSageMode()) {
            return;
        }
        this.setSeninMana(this.seninMana + mana);
    }

    @Override
    public float getMaxSeninMana() {
        // Nature energy is learned, not innate: with no gathering technique there is no pool, so a
        // fresh ninja has no senin bar and cannot stock nature energy before buying the sage branch.
        if (!AbilityUnlocks.hasSenjutsu(this)) {
            return 0.0f;
        }
        return NarutoConfig.maxSeninMana + this.eyeSeninBonus
                + SkillTree.bonus(this.skillData.getUnlocked(), SkillEffect.SENIN_MANA)
                + this.enchantSeninBonus;
    }

    @Override
    public boolean isSageMode() {
        return this.sageMode;
    }

    @Override
    public void setSageMode(boolean sage) {
        this.sageMode = sage;
    }

    @Override
    public int getSixPathsOrbs() {
        return this.sixPathsOrbs;
    }

    @Override
    public void setSixPathsOrbs(int orbs) {
        this.sixPathsOrbs = orbs;
    }

    @Override
    public int getTenseiganOrbs() {
        return this.tenseiganOrbs;
    }

    @Override
    public void setTenseiganOrbs(int orbs) {
        this.tenseiganOrbs = orbs;
    }

    @Override
    public int getSealVesselIntegrity() {
        return this.sealVesselIntegrity;
    }

    @Override
    public void setSealVesselIntegrity(int integrity) {
        this.sealVesselIntegrity = integrity;
    }

    @Override
    public float getSensingRadius() {
        return this.sensingRadius;
    }

    @Override
    public void setSensingRadius(float radius) {
        this.sensingRadius = Math.max(radius, 0f);
    }

    @Override
    public int getNightVisionTicks() {
        return this.nightVisionTicks;
    }

    @Override
    public void setNightVisionTicks(int ticks) {
        this.nightVisionTicks = Math.max(ticks, 0);
    }

    @Override
    public String getHengeTarget() {
        return this.hengeTarget == null ? "" : this.hengeTarget;
    }

    @Override
    public void setHengeTarget(String uuid) {
        this.hengeTarget = uuid == null ? "" : uuid;
    }

    @Override
    public String getHengeEntityType() {
        return this.hengeEntityType == null ? "" : this.hengeEntityType;
    }

    @Override
    public void setHengeEntityType(String entityType) {
        this.hengeEntityType = entityType == null ? "" : entityType;
    }

    @Override
    public String getHengeName() {
        return this.hengeName == null ? "" : this.hengeName;
    }

    @Override
    public void setHengeName(String name) {
        this.hengeName = name == null ? "" : name;
    }

    @Override
    public int getHengeTicks() {
        return this.hengeTicks;
    }

    @Override
    public void setHengeTicks(int ticks) {
        this.hengeTicks = Math.max(ticks, 0);
    }

    @Override
    public boolean isHenged() {
        return this.hengeTicks > 0
                && (!this.getHengeTarget().isEmpty() || !this.getHengeEntityType().isEmpty());
    }

    @Override
    public DoubleJumpData getDoubleJumpData() {
        if (this.doubleJumpData == null) {
            this.doubleJumpData = new DoubleJumpData(false);
        }
        return this.doubleJumpData;
    }

    @Override
    public ResourceLocation getCurrentlyChanneledAbility() {
        return this.currentlyChanneled;
    }

    @Override
    public int getCurrentlyChanneledTicks() {
        return this.ticksChanneled;
    }

    @Override
    public void setCurrentlyChanneledAbility(Player player, Ability ability) {
        if (ability != null) {
            if (ability.castingSound() != null) {
                player.level().playSound(null, player, ability.castingSound(), SoundSource.PLAYERS, 0.5f, 1.0f);
            }
            ability.emitNoise(player);

            if(!(ability instanceof Ability.Channeled channeled && channeled.hideChannelMessages())) {
                if (ability instanceof Ability.Channeled channeled && channeled.useChargedMessages()) {
                    player.displayClientMessage(Component.translatable("jutsu.charge.start", Component.translatable(ability.getTranslationKey(this, 1)).withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.GREEN), true);
                } else {
                    player.displayClientMessage(Component.translatable("jutsu.channel.start", Component.translatable(ability.getTranslationKey(this, 1)).withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.GREEN), true);
                }
            }

            NarutoRegistries.ABILITIES.getResourceKey(ability)
                    .ifPresent(abilityResourceKey -> this.currentlyChanneled = abilityResourceKey.location());
        } else {
            if (this.currentlyChanneled != null) {
                Ability currentAbility = NarutoAbilities.byId(this.currentlyChanneled);
                if( currentAbility != null) {
                    if(!(currentAbility instanceof Ability.Channeled channeled && channeled.hideChannelMessages())) {
                        if (currentAbility instanceof Ability.Channeled channeled && channeled.useChargedMessages()) {
                            player.displayClientMessage(Component.translatable("jutsu.cast", Component.translatable(currentAbility.getTranslationKey(this, this.ticksChanneled - 1)).withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.GREEN), true);
                        } else {
                            player.displayClientMessage(Component.translatable("jutsu.channel.stop", Component.translatable(currentAbility.getTranslationKey(this)).withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.RED), true);
                        }
                    }
                }
            }
            this.currentlyChanneled = null;
        }
        this.ticksChanneled = 0;
    }

    /*
     * The nested data below is contractually never null, and every getter repairs a missing object
     * instead of handing it out.
     *
     * sekclib replays a player's globally synced fields to whoever starts tracking them, and it
     * replays the raw tracker values - a tracker that has not ticked yet sends "no data", which the
     * packet spells as null. The client handler then writes that null straight into the field through
     * a method handle, so a player who logs in during the same tick another player starts watching
     * them arrives with these objects wiped. A null here used to fall straight into a renderer and
     * take the game down (see SixPathsOrbRenderer). Starting from an empty object is safe: the next
     * real update of the field replaces it.
     */

    @Override
    public ToggleAbilityData getToggleAbilityData() {
        if (this.toggleAbilityData == null) {
            this.toggleAbilityData = new ToggleAbilityData();
        }
        return this.toggleAbilityData;
    }

    @Override
    public MasteryData getMasteryData() {
        if (this.masteryData == null) {
            this.masteryData = new MasteryData();
        }
        return this.masteryData;
    }

    @Override
    public SkillData getSkillData() {
        if (this.skillData == null) {
            this.skillData = new SkillData();
        }
        return this.skillData;
    }

    @Override
    public AbsorptionData getAbsorptionData() {
        if (this.absorptionData == null) {
            this.absorptionData = new AbsorptionData();
        }
        return this.absorptionData;
    }

    @Override
    public DisabledAbilityData getDisabledAbilityData() {
        if (this.disabledAbilityData == null) {
            this.disabledAbilityData = new DisabledAbilityData();
        }
        return this.disabledAbilityData;
    }

    @Override
    public boolean isAbilityDisabled(ResourceLocation abilityId) {
        if (abilityId == null || !this.disabledAbilityData.contains(abilityId)) {
            return false;
        }
        // Only techniques the menu controls can be put aside. A save from an older build may list
        // anything, and a stale entry must not silently block a fireball.
        Ability ability = NarutoAbilities.byId(abilityId);
        return ability != null && ability.isMenuControllable();
    }

    @Override
    public boolean isMenuClickAll() {
        return this.menuClickAll;
    }

    @Override
    public void setMenuClickAll(boolean enabled) {
        this.menuClickAll = enabled;
    }

    @Override
    public boolean hasClaimedBossMilestone(String bossId) {
        return this.claimedBossMilestones.contains(bossId);
    }

    @Override
    public void claimBossMilestone(String bossId) {
        this.claimedBossMilestones.add(bossId);
    }

    @Override
    public String getNinjaRank() {
        return this.ninjaRank;
    }

    @Override
    public void setNinjaRank(String rank) {
        this.ninjaRank = rank == null ? "" : rank;
    }

    @Override
    public String getNatureAffinity() {
        return this.natureAffinity;
    }

    @Override
    public void setNatureAffinity(String affinity) {
        this.natureAffinity = affinity == null ? "" : affinity;
    }

    @Override
    public boolean isHandbookGiven() {
        return this.handbookGiven;
    }

    @Override
    public void setHandbookGiven(boolean given) {
        this.handbookGiven = given;
    }

    @Override
    public MeditationData getMeditationData() {
        return this.meditationData;
    }

    /**
     * The soft price of an empty pool: below {@link #EXHAUSTION_THRESHOLD} the caster moves a little
     * slower. It is refreshed every tick and removes itself as soon as the pool is back above the
     * line, so it never becomes a death spiral, and it is never applied in creative or spectator.
     *
     * <p>The numbers are handed in by the caller instead of being read off this layer's own fields:
     * since the bar moved into the base capability this layer's {@code chakra} is never written, so
     * checking it here would slow every player down whose dead pool happens to sit at zero.</p>
     */
    private void updateExhaustion(Player player, float chakra, float maxChakra) {
        boolean exhausted = NarutoConfig.chakraExhaustionEnabled
                && maxChakra > 0.0f
                && chakra < maxChakra * EXHAUSTION_THRESHOLD
                && !player.isCreative() && !player.isSpectator();
        if (exhausted) {
            player.addEffect(new MobEffectInstance(NarutoEffects.CHAKRA_EXHAUSTION.get(),
                    EXHAUSTION_TICKS, 0, true, false));
        } else {
            player.removeEffect(NarutoEffects.CHAKRA_EXHAUSTION.get());
        }
    }

    @Override
    public void updateDataServer(Player player) {
        if(this.invisibleTicks > 0) {
            this.invisibleTicks--;
            this.isInvisible = true;
        } else {
            this.isInvisible = false;
        }

        if(!this.isNinjaModeEnabled()) {
            return;
        }

        this.getConfigData();
        Iterator<DelayedPlayerTickEvent> iterator = this.delayedTickEvents.iterator();
        while (iterator.hasNext()) {
            DelayedPlayerTickEvent event = iterator.next();
            event.tick();
            if (event.shouldRun()) {
                event.run(player);
                iterator.remove();
            }
        }

        // Compile list of keys from cooldown map
        ArrayList<String> completeList = new ArrayList<>(cooldownTickEvents.keySet());
        //  loop through to tick and then remove cooldown if complete
        for (String name : completeList) {
            CooldownTickEvent event = cooldownTickEvents.get(name);
            event.tick();
            if (event.isComplete()) {
                cooldownTickEvents.remove(name);
            }
        }

        this.staminaCostMultiplier = com.sekwah.narutoaddon.item.TrainingWeightsItem
                .staminaMultiplier(player);
        this.trainingWeightsStaminaBonus = com.sekwah.narutoaddon.item.TrainingWeightsItem
                .staminaCapacityBonus(player);
        this.trainingWeightsRegenBonus = com.sekwah.narutoaddon.item.TrainingWeightsItem
                .staminaRegenBonus(player);
        // A sage's body is held up by nature: everything costs half the stamina it should.
        if (this.isSageMode() && NarutoConfig.sageBuffsEnabled) {
            this.staminaCostMultiplier *= 0.5f;
        }
        if (this.staminaRegenInfo.canRegen()) {
            // Open gates burn stamina faster than it can be made back: while any gate is open the
            // bar only goes down, which is the price of the stance.
            if (this.getGateCount() <= 3) {
                float staminaRegen = NarutoConfig.staminaRegen
                        + SkillTree.bonus(this.skillData.getUnlocked(), SkillEffect.STAMINA_REGEN) / 20.0f
                        + this.trainingWeightsRegenBonus / 20.0f
                        + this.staminaRegenBonus / 20.0f;
                // Running dry leaves the body winded for a few seconds: the bar comes back at half rate,
                // which is what makes spending the last point a decision rather than a formality.
                if (this.stamina <= 0.0f) {
                    player.getPersistentData().putLong("narutomod:winded_until",
                            player.level().getGameTime() + 100L);
                }
                if (player.getPersistentData().getLong("narutomod:winded_until")
                        > player.level().getGameTime()) {
                    staminaRegen *= 0.5f;
                }
                // Weight is real: every point of armour takes a little off the recovery. A sage
                // does not feel the weight - nature carries it - so the penalty is waived in sage mode.
                if (!(this.isSageMode() && NarutoConfig.sageBuffsEnabled)) {
                    staminaRegen *= 1.0f - Math.min(0.3f, player.getArmorValue() * 0.01f);
                }
                if (player.isCreative()) {
                    staminaRegen *= CREATIVE_REGEN_MULTIPLIER;
                }
                this.stamina += staminaRegen;
            }
        }
        if (this.chakraRegenInfo.canRegen()
                && player.getPersistentData().getLong("narutomod:node_rupture_until")
                <= player.level().getGameTime()
                && !GenjutsuEvents.isUnderGenjutsu(player)) {
            // Open gates burn chakra faster than it can be made back: while any gate is open the
            // pool only goes down, which is the price of the stance.
            if (this.getGateCount() <= 3) {
                float regen = NarutoConfig.chakraRegen
                        + SkillTree.bonus(this.skillData.getUnlocked(), SkillEffect.CHAKRA_REGEN) / 20.0f
                        + this.chakraRegenBonus / 20.0f;
                // A freshly filled pool keeps trickling in faster for a while, which is what makes
                // topping the bar up before a fight worth the time, see ChakraChargeAbility.
                if (player.hasEffect(NarutoEffects.CHAKRA_FLOW.get()) && NarutoConfig.chakraFlowEnabled) {
                    regen *= CHAKRA_FLOW_MULTIPLIER;
                }
                // Creative is faster rather than free, see CREATIVE_REGEN_MULTIPLIER.
                if (player.isCreative()) {
                    regen *= CREATIVE_REGEN_MULTIPLIER;
                }
                this.chakra += regen;
            }
        }
        this.substitutions += NarutoConfig.substitutionRegenRate
                * (1.0f + SkillTree.bonus(this.skillData.getUnlocked(), SkillEffect.SUBSTITUTION_REGEN));
        this.substitutions = Math.min(Math.max(this.substitutions, 0), this.getMaxSubstitutions());
        this.stamina = Math.min(Math.max(this.stamina, 0), this.getMaxStamina());
        this.chakra = Math.min(Math.max(this.chakra, 0), this.getMaxChakra());
        // The exhaustion rule is not applied here: it is about the real bar, and the numbers above
        // belong to this layer's unused pools. {@link #updateAddonLayer} applies it against the
        // base capability, which is the pool the player actually casts from.

        if (this.currentlyChanneled != null) {
            Ability ability = NarutoAbilities.byId(this.currentlyChanneled);
            if (ability != null && ability.activationType() == Ability.ActivationType.CHANNELED) {
                // A cast held open is paid for tick by tick too, so it also gets to run on battery
                // power before its upkeep check decides the caster cannot afford it any more.
                ChakraBatteryItem.fuelUpkeep(player, this);
                if(ability.handleCost(player, this, this.ticksChanneled)) {
                    if (ability instanceof Ability.Channeled channeled) {
                        channeled.handleChannelling(player, this, this.ticksChanneled);
                    }
                } else {
                    // A channel that could not even pay its first tick must end quietly: calling
                    // performServer(0) here would hand the caster the release effect (a free earth
                    // wall, a free rasengan hit) for a cast the server just refused, and leaving the
                    // channel set would keep re-checking it every tick.
                    if (this.ticksChanneled > 0) {
                        int finalTicksChanneled = this.ticksChanneled - 1;
                        ability.performServer(player, this, finalTicksChanneled);
                    }
                    this.setCurrentlyChanneledAbility(player, null);
                }
            } else {
                LOGGER.error("Somehow non channeled ability has been set to ninja data {}", this.currentlyChanneled);
            }
            this.ticksChanneled++;
        }

        if(player.onGround()) {
            this.doubleJumpData.canDoubleJumpServer = true;
        }
    }

    /**
     * The half of {@link #updateDataServer(Player)} that a player actually reaches through
     * {@link AddonNinjaData}. The adapter keeps chakra, stamina, substitutions, the channeled cast and
     * the delayed tick events in the base capability (it delegates every one of them), so those must
     * not be ticked here as well - doing so would regenerate a pool that gameplay never reads and
     * would double-handle a channel. What stays on this layer, and is therefore the only thing ticked
     * here, is state nothing else knows about: the cooldown map the abilities write into, delayed
     * events scheduled straight on this layer, the training weight multipliers and the double jump
     * flag this layer hands to the adapter - plus the parts of chakra and stamina the base pool has
     * no notion of, which are paid into it instead of being ticked here (see
     * {@link #creditBonusRegen}).
     *
     * @param base the base capability, i.e. the pool the player actually casts from
     */
    public void updateAddonLayer(Player player, com.sekwah.narutomod.capabilities.INinjaData base) {
        this.getConfigData();
        // The adapter forwards its own delayed events to the base capability, so this list is
        // normally empty; ticking it keeps the layer consistent if anything schedules on it directly.
        Iterator<DelayedPlayerTickEvent> iterator = this.delayedTickEvents.iterator();
        while (iterator.hasNext()) {
            DelayedPlayerTickEvent event = iterator.next();
            event.tick();
            if (event.shouldRun()) {
                event.run(player);
                iterator.remove();
            }
        }

        // Cooldowns are this layer's alone: Ability.Cooldown#registerCooldown writes into this map via
        // INinjaData#getCooldownEvents and nothing else ever ticks it, so without this loop a finished
        // cooldown is never dropped and checkCooldown keeps refusing the technique forever.
        ArrayList<String> completeList = new ArrayList<>(this.cooldownTickEvents.keySet());
        for (String name : completeList) {
            CooldownTickEvent event = this.cooldownTickEvents.get(name);
            event.tick();
            if (event.isComplete()) {
                this.cooldownTickEvents.remove(name);
            }
        }

        // Weight is only worth scanning for when some is actually carried: the three bonuses below
        // are all zero without it.
        if (com.sekwah.narutoaddon.item.TrainingWeightsItem.isCarrying(player)) {
            this.staminaCostMultiplier = com.sekwah.narutoaddon.item.TrainingWeightsItem
                    .staminaMultiplier(player);
            this.trainingWeightsStaminaBonus = com.sekwah.narutoaddon.item.TrainingWeightsItem
                    .staminaCapacityBonus(player);
            this.trainingWeightsRegenBonus = com.sekwah.narutoaddon.item.TrainingWeightsItem
                    .staminaRegenBonus(player);
        } else {
            this.staminaCostMultiplier = 1.0f;
            this.trainingWeightsStaminaBonus = 0.0f;
            this.trainingWeightsRegenBonus = 0.0f;
        }
        // A sage's body is held up by nature: everything costs half the stamina it should.
        if (this.isSageMode() && NarutoConfig.sageBuffsEnabled) {
            this.staminaCostMultiplier *= 0.5f;
        }

        // This layer owns the double jump flag the adapter hands out, so re-arming it on the ground
        // has to happen in its tick.
        if(player.onGround()) {
            this.doubleJumpData.canDoubleJumpServer = true;
        }

        // Ordered last on purpose: the weight multipliers scanned just above feed the stamina half of
        // this, and the base pool has already regenerated itself by now, so what is paid in here is
        // always an addition on top of it.
        this.creditBonusRegen(player, base);
        // Exhaustion is about the real bar, so it is judged on the base pool's numbers.
        this.updateExhaustion(player, base.getChakra(), base.getMaxChakra());
    }

    /**
     * Chakra and stamina the addon adds on top of the base pool's own trickle.
     *
     * <p>Neither bar is duplicated: the base capability holds both, and it regenerates them itself
     * at its configured rate. All this method does is pay in what the base cannot know about - the
     * tree's regeneration nodes, the regeneration bonuses picked up from scrolls and the training
     * weights - through the base's own {@code addChakra}/{@code addStamina}, once per tick. The
     * bar's ceiling stays the base's: the capacity the addon's rules add is folded into the base's
     * own maximum by {@code BaseNinjaDataCapacityMixin}, so the base's clamp remains the real
     * ceiling and nothing credited here can push a pool past what it can hold, and nothing here
     * re-runs the base rate (that would count it twice).</p>
     */
    private void creditBonusRegen(Player player, com.sekwah.narutomod.capabilities.INinjaData base) {
        // The base only regenerates in ninja mode, so a bonus trickle outside it would be the only
        // thing moving the bar; keep both out together.
        if (base == null || !base.isNinjaModeEnabled()) {
            return;
        }
        // Open gates burn both pools faster than they come back: while any gate is open the tree's
        // trickle is off too, which is the price of the stance.
        if (this.getGateCount() > 0) {
            return;
        }
        // The tree stores regeneration in points per second, the tick works in points per tick.
        float chakraRegen = (SkillTree.bonus(this.skillData.getUnlocked(), SkillEffect.CHAKRA_REGEN)
                + this.chakraRegenBonus) / 20.0f;
        // Ruptured tenketsu and a genjutsu both stop the caster from gathering chakra at all, and a
        // node in the tree does not change that.
        if (chakraRegen > 0.0f
                && player.getPersistentData().getLong("narutomod:node_rupture_until")
                        <= player.level().getGameTime()
                && !GenjutsuEvents.isUnderGenjutsu(player)) {
            base.addChakra(chakraRegen);
        }
        float staminaRegen = (SkillTree.bonus(this.skillData.getUnlocked(), SkillEffect.STAMINA_REGEN)
                + this.trainingWeightsRegenBonus
                + this.staminaRegenBonus) / 20.0f;
        if (staminaRegen > 0.0f) {
            base.addStamina(staminaRegen);
        }
    }

    private void getConfigData() {
        this.maxChakra = NarutoConfig.maxChakra;
        this.maxStamina = NarutoConfig.maxStamina;
        this.maxSubstitutions = NarutoConfig.maxSubstitutions;
    }

    @Override
    public void scheduleDelayedTickEvent(Consumer<Player> consumer, int tickDelay) {
        this.delayedTickEvents.add(new DelayedPlayerTickEvent(consumer, tickDelay));
    }

    @Override
    public void updateDataClient(Player player) {
        this.doubleJumpData.stuckCheck();
    }

    @Override
    public void setIsNinja(boolean enableNinja) {
        this.ninjaModeEnabled = enableNinja;
    }

    @Override
    public boolean isNinjaModeEnabled() {
        return this.ninjaModeEnabled;
    }

    @Override
    public Tag serializeNBT() {
        final CompoundTag nbt = new CompoundTag();
        nbt.putFloat(CHAKRA_TAG, this.chakra);
        nbt.putFloat(STAMINA_TAG, this.stamina);
        nbt.putBoolean(NINJA_MODE_ENABLED, this.ninjaModeEnabled);
        nbt.putBoolean(DOUBLE_JUMP_TAG, this.doubleJumpData.canDoubleJumpServer);
        long currentTime = System.currentTimeMillis();
        nbt.putLong(SAVE_TIME, currentTime);
        final CompoundTag cooldownData = new CompoundTag();
        for (String key : this.cooldownTickEvents.keySet()) {
            CooldownTickEvent event = this.cooldownTickEvents.get(key);
            cooldownData.putInt(key, event.ticks);
        }
        nbt.put(COOLDOWN_TAG, cooldownData);
        nbt.putFloat(SUBSTITUTION_TAG, this.substitutions);
        if (this.markerLocation != null) {
            CompoundTag marker = new CompoundTag();
            marker.putDouble("x", this.markerLocation.x);
            marker.putDouble("y", this.markerLocation.y);
            marker.putDouble("z", this.markerLocation.z);
            nbt.put(MARKER_TAG, marker);
        }
        if (this.markerDimension != null) {
            nbt.putString(MARKER_DIMENSION_TAG, this.markerDimension.toString());
        }
        CompoundTag supplyChestTag = new CompoundTag();
        this.supplyChest.save(supplyChestTag);
        nbt.put(SUPPLY_CHEST_TAG, supplyChestTag);
        if (this.kamuiReturnLocation != null) {
            CompoundTag kamui = new CompoundTag();
            kamui.putDouble("x", this.kamuiReturnLocation.x);
            kamui.putDouble("y", this.kamuiReturnLocation.y);
            kamui.putDouble("z", this.kamuiReturnLocation.z);
            nbt.put(KAMUI_RETURN_TAG, kamui);
        }
        if (this.kamuiReturnDimension != null) {
            nbt.putString(KAMUI_RETURN_DIMENSION_TAG, this.kamuiReturnDimension.toString());
        }
        nbt.putFloat(SENIN_MANA_TAG, this.seninMana);
        nbt.putFloat(SHIELD_TAG, this.shieldPoints);
        nbt.putBoolean(SECOND_BREATH_TAG, this.secondBreathArmed);
        CompoundTag masteryTag = new CompoundTag();
        this.masteryData.getCasts().forEach((ability, casts) -> masteryTag.putInt(ability.toString(), casts));
        nbt.put(MASTERY_TAG, masteryTag);
        CompoundTag skillTag = new CompoundTag();
        for (ResourceLocation skill : this.skillData.getUnlocked()) {
            skillTag.putBoolean(skill.toString(), true);
        }
        nbt.put(SKILLS_TAG, skillTag);
        nbt.putInt(SKILL_POINTS_TAG, this.skillData.getPoints());
        CompoundTag scrollTag = new CompoundTag();
        for (ResourceLocation knowledge : this.skillData.getScrolls()) {
            scrollTag.putBoolean(knowledge.toString(), true);
        }
        nbt.put(SCROLLS_TAG, scrollTag);
        CompoundTag bossTag = new CompoundTag();
        for (String boss : this.claimedBossMilestones) {
            bossTag.putBoolean(boss, true);
        }
        nbt.put(BOSS_MILESTONES_TAG, bossTag);
        nbt.putString(NINJA_RANK_TAG, this.ninjaRank);
        nbt.putString(NATURE_AFFINITY_TAG, this.natureAffinity);
        nbt.putBoolean(HANDBOOK_GIVEN_TAG, this.handbookGiven);
        CompoundTag disabledTag = new CompoundTag();
        for (ResourceLocation id : this.disabledAbilityData.getAbilities()) {
            disabledTag.putBoolean(id.toString(), true);
        }
        nbt.put(DISABLED_ABILITIES_TAG, disabledTag);
        nbt.putBoolean(MENU_CLICK_ALL_TAG, this.menuClickAll);
        nbt.putFloat(CHAKRA_BONUS_TAG, this.chakraBonus);
        nbt.putFloat(STAMINA_BONUS_TAG, this.staminaBonus);
        nbt.putFloat(CHAKRA_REGEN_BONUS_TAG, this.chakraRegenBonus);
        nbt.putFloat(STAMINA_REGEN_BONUS_TAG, this.staminaRegenBonus);
        nbt.putInt(VITALITY_BONUS_TAG, this.vitalityBonus);
        nbt.putInt(STRENGTH_BONUS_TAG, this.strengthBonus);
        return nbt;
    }

    /**
     * Snapshot sent to the client. Unlike {@link #serializeNBT()} this one also carries the
     * session-only state ({@code gateCount}, {@code sageMode}, toggles, ...) that the client needs
     * for its HUD and overlays but that must not survive a relog.
     */
    public CompoundTag serializeSyncNBT() {
        CompoundTag tag = (CompoundTag) this.serializeNBT();
        // Fields the client reads but that sekclib cannot sync because the capability provider is
        // AddonNinjaData rather than NinjaData: push them through the NBT snapshot too.
        tag.putBoolean(GUDODAMA_FORM_TAG, this.gudodamaForm);
        tag.putInt(GATE_COUNT_TAG, this.gateCount);
        tag.putBoolean(SAGE_MODE_TAG, this.sageMode);
        tag.putInt(SIX_PATHS_ORBS_TAG, this.sixPathsOrbs);
        tag.putInt(TENSEIGAN_ORBS_TAG, this.tenseiganOrbs);
        tag.putInt(SEAL_VESSEL_TAG, this.sealVesselIntegrity);
        tag.putFloat(SENSING_RADIUS_TAG, this.sensingRadius);
        tag.putInt(NIGHT_VISION_TAG, this.nightVisionTicks);
        tag.putString(HENGE_TARGET_TAG, this.hengeTarget);
        tag.putString(HENGE_ENTITY_TYPE_TAG, this.hengeEntityType);
        tag.putString(HENGE_NAME_TAG, this.hengeName);
        CompoundTag absorptionTag = new CompoundTag();
        for (java.util.Map.Entry<AbsorptionData.Nature, Integer> entry : this.absorptionData.snapshot().entrySet()) {
            absorptionTag.putInt(entry.getKey().name(), entry.getValue());
        }
        tag.put(ABSORPTION_TAG, absorptionTag);
        CompoundTag toggleTag = new CompoundTag();
        for (ResourceLocation id : this.toggleAbilityData.getAbilitiesHashSet()) {
            toggleTag.putBoolean(id.toString(), true);
        }
        tag.put(TOGGLE_ABILITIES_TAG, toggleTag);
        tag.putFloat(EYE_SENIN_BONUS_TAG, this.eyeSeninBonus);
        tag.putFloat(ENCHANT_SENIN_BONUS_TAG, this.enchantSeninBonus);
        return tag;
    }

    @Override
    public void deserializeNBT(Tag tag) {
        if (tag instanceof CompoundTag compoundTag) {
            long currentTime = System.currentTimeMillis();
            long saveTime = compoundTag.getLong(SAVE_TIME);
            int ticksPassed = Math.max((int) ((currentTime - saveTime) / 1000 * 20), 0);
            this.chakra = compoundTag.getFloat(CHAKRA_TAG);
            this.stamina = compoundTag.getFloat(STAMINA_TAG);
            this.ninjaModeEnabled = compoundTag.getBoolean(NINJA_MODE_ENABLED);
            this.doubleJumpData.canDoubleJumpServer = compoundTag.getBoolean(DOUBLE_JUMP_TAG);
            if (this.doubleJumpData.canDoubleJumpServer) {
                this.doubleJumpData.canDoubleJumpClient = true;
                this.doubleJumpData.diffUpdateTicksClient = 0;
            }
            CompoundTag cooldownData = compoundTag.getCompound(COOLDOWN_TAG);
            for (String key : cooldownData.getAllKeys()) {
                this.cooldownTickEvents.put(key, new CooldownTickEvent(cooldownData.getInt(key) - ticksPassed));
            }
            this.substitutions = compoundTag.getFloat(SUBSTITUTION_TAG);
            if (compoundTag.contains(MARKER_TAG)) {
                CompoundTag marker = compoundTag.getCompound(MARKER_TAG);
                this.markerLocation = new Vec3(marker.getDouble("x"), marker.getDouble("y"),
                        marker.getDouble("z"));
            }
            if (compoundTag.contains(MARKER_DIMENSION_TAG)) {
                this.markerDimension = new ResourceLocation(compoundTag.getString(MARKER_DIMENSION_TAG));
            }
            Tag supplyChestTag = compoundTag.get(SUPPLY_CHEST_TAG);
            if (supplyChestTag instanceof CompoundTag supplyChestCompound) {
                this.supplyChest.load(supplyChestCompound);
            } else if (supplyChestTag instanceof ListTag supplyChestList) {
                // Contents written before the chest grew amounts, still a plain item list.
                this.supplyChest.loadLegacy(supplyChestList);
            }
            if (compoundTag.contains(KAMUI_RETURN_TAG)) {
                CompoundTag kamui = compoundTag.getCompound(KAMUI_RETURN_TAG);
                this.kamuiReturnLocation = new Vec3(kamui.getDouble("x"), kamui.getDouble("y"),
                        kamui.getDouble("z"));
            }
            if (compoundTag.contains(KAMUI_RETURN_DIMENSION_TAG)) {
                this.kamuiReturnDimension = new ResourceLocation(compoundTag.getString(KAMUI_RETURN_DIMENSION_TAG));
            }
            this.seninMana = compoundTag.getFloat(SENIN_MANA_TAG);
            this.shieldPoints = compoundTag.getFloat(SHIELD_TAG);
            this.secondBreathArmed = compoundTag.getBoolean(SECOND_BREATH_TAG);
            CompoundTag masteryTag = compoundTag.getCompound(MASTERY_TAG);
            // Counts are authoritative, so a load replaces whatever was there rather than adding to it.
            this.masteryData.getCasts().clear();
            for (String key : masteryTag.getAllKeys()) {
                ResourceLocation ability = ResourceLocation.tryParse(key);
                if (ability != null) {
                    this.masteryData.setCasts(ability, masteryTag.getInt(key));
                }
            }
            CompoundTag skillTag = compoundTag.getCompound(SKILLS_TAG);
            // Learned nodes are authoritative too, so a load replaces the set.
            this.skillData.reset();
            for (String key : skillTag.getAllKeys()) {
                ResourceLocation skill = ResourceLocation.tryParse(key);
                if (skill != null) {
                    this.skillData.unlock(skill);
                }
            }
            this.skillData.addPoints(compoundTag.getInt(SKILL_POINTS_TAG));
            CompoundTag scrollTag = compoundTag.getCompound(SCROLLS_TAG);
            for (String key : scrollTag.getAllKeys()) {
                ResourceLocation knowledge = ResourceLocation.tryParse(key);
                if (knowledge != null) {
                    this.skillData.learnScroll(knowledge);
                }
            }
            // Clamped once the learned nodes are back: without the gathering technique the pool is
            // zero, so a value saved by an older build cannot linger as a bar a fresh ninja owns.
            this.setSeninMana(this.seninMana);
            this.claimedBossMilestones.clear();
            CompoundTag bossTag = compoundTag.getCompound(BOSS_MILESTONES_TAG);
            for (String boss : bossTag.getAllKeys()) {
                this.claimedBossMilestones.add(boss);
            }
            this.ninjaRank = compoundTag.getString(NINJA_RANK_TAG);
            this.natureAffinity = compoundTag.getString(NATURE_AFFINITY_TAG);
            this.handbookGiven = compoundTag.getBoolean(HANDBOOK_GIVEN_TAG);
            this.menuClickAll = compoundTag.getBoolean(MENU_CLICK_ALL_TAG);
            this.chakraBonus = compoundTag.getFloat(CHAKRA_BONUS_TAG);
            this.staminaBonus = compoundTag.getFloat(STAMINA_BONUS_TAG);
            this.chakraRegenBonus = compoundTag.getFloat(CHAKRA_REGEN_BONUS_TAG);
            this.staminaRegenBonus = compoundTag.getFloat(STAMINA_REGEN_BONUS_TAG);
            this.vitalityBonus = compoundTag.getInt(VITALITY_BONUS_TAG);
            this.strengthBonus = compoundTag.getInt(STRENGTH_BONUS_TAG);
            this.disabledAbilityData.getAbilities().clear();
            CompoundTag disabledTag = compoundTag.getCompound(DISABLED_ABILITIES_TAG);
            for (String key : disabledTag.getAllKeys()) {
                ResourceLocation id = ResourceLocation.tryParse(key);
                if (id != null) {
                    this.disabledAbilityData.getAbilities().add(id);
                }
            }
            // Older builds let any technique be switched off. Only the ones the menu controls are
            // honoured now, so anything else is dropped instead of lingering as a dead entry.
            this.disabledAbilityData.getAbilities().removeIf(id -> {
                Ability ability = NarutoAbilities.byId(id);
                return ability == null || !ability.isMenuControllable();
            });
        }
    }

    /** Reads the client snapshot produced by {@link #serializeSyncNBT()}. */
    public void deserializeSyncNBT(Tag tag) {
        this.deserializeNBT(tag);
        if (tag instanceof CompoundTag compoundTag) {
            this.gudodamaForm = compoundTag.getBoolean(GUDODAMA_FORM_TAG);
            this.gateCount = compoundTag.getInt(GATE_COUNT_TAG);
            this.sageMode = compoundTag.getBoolean(SAGE_MODE_TAG);
            this.sixPathsOrbs = compoundTag.getInt(SIX_PATHS_ORBS_TAG);
            this.tenseiganOrbs = compoundTag.getInt(TENSEIGAN_ORBS_TAG);
            this.sealVesselIntegrity = compoundTag.getInt(SEAL_VESSEL_TAG);
            this.sensingRadius = compoundTag.getFloat(SENSING_RADIUS_TAG);
            this.nightVisionTicks = compoundTag.getInt(NIGHT_VISION_TAG);
            this.hengeTarget = compoundTag.getString(HENGE_TARGET_TAG);
            this.hengeEntityType = compoundTag.getString(HENGE_ENTITY_TYPE_TAG);
            this.hengeName = compoundTag.getString(HENGE_NAME_TAG);
            CompoundTag absorptionTag = compoundTag.getCompound(ABSORPTION_TAG);
            java.util.Map<AbsorptionData.Nature, Integer> allCounts = new java.util.EnumMap<>(AbsorptionData.Nature.class);
            for (String key : absorptionTag.getAllKeys()) {
                try {
                    allCounts.put(AbsorptionData.Nature.valueOf(key), absorptionTag.getInt(key));
                } catch (IllegalArgumentException ignored) {
                    // Unknown nature key from an older save; drop it.
                }
            }
            this.absorptionData.clear();
            this.absorptionData.apply(allCounts);
            this.toggleAbilityData.getAbilitiesHashSet().clear();
            CompoundTag toggleTag = compoundTag.getCompound(TOGGLE_ABILITIES_TAG);
            for (String key : toggleTag.getAllKeys()) {
                ResourceLocation id = ResourceLocation.tryParse(key);
                if (id != null) {
                    this.toggleAbilityData.getAbilitiesHashSet().add(id);
                }
            }
            this.eyeSeninBonus = compoundTag.getFloat(EYE_SENIN_BONUS_TAG);
            this.enchantSeninBonus = compoundTag.getFloat(ENCHANT_SENIN_BONUS_TAG);
        }
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        return NinjaCapabilityHandler.NINJA_DATA.orEmpty(cap, holder);
    }

    @Override
    public HashMap<String, CooldownTickEvent> getCooldownEvents() {
        return cooldownTickEvents;
    }
}
