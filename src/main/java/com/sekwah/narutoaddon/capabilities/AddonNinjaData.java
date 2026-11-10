package com.sekwah.narutoaddon.capabilities;

import com.sekwah.narutoaddon.abilities.Ability;
import com.sekwah.narutoaddon.bridge.AbilityProxy;
import com.sekwah.narutomod.config.NarutoConfig;


import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.function.Consumer;

/**
 * Adapter that makes the base mod's ninja data look like our {@link INinjaData}.
 * Common fields (chakra, stamina, substitution, cooldowns, ...) are delegated to
 * the base capability; our-only fields live in an internal {@link NinjaData}.
 */
public class AddonNinjaData implements INinjaData, net.minecraftforge.common.capabilities.ICapabilityProvider {

    private final com.sekwah.narutomod.capabilities.INinjaData base;
    private final NinjaData addon;

    public AddonNinjaData(com.sekwah.narutomod.capabilities.INinjaData base, boolean isServer) {
        this.base = base;
        this.addon = new NinjaData(isServer);
    }

    public com.sekwah.narutomod.capabilities.INinjaData getBase() {
        return base;
    }

    public NinjaData getAddon() {
        return addon;
    }

    // ---- common fields delegated to base ----
    @Override
    public float getChakra() { return base.getChakra(); }

    /**
     * The bar's maximum is the base pool's, and the capacity bonuses of this layer reach it through
     * {@code BaseNinjaDataCapacityMixin}: that mixin raises the base capability's own private
     * maximum once per server tick, right after the base has re-read its configured value, so the
     * pool, every rule that reads the maximum (the charge technique's "the bar is full" test,
     * mastery credit per point restored, chakra crystals and batteries working out their room, the
     * exhaustion line) and the synced bar all agree on the same, larger number. Delegating is
     * therefore enough here - adding the bonus again would count it twice.
     */
    @Override
    public float getMaxChakra() { return base.getMaxChakra(); }
    @Override
    public float getStamina() { return base.getStamina(); }
    @Override
    public float getSubstitutionCount() { return base.getSubstitutionCount(); }
    @Override
    public float getMaxSubstitutions() { return addon.getMaxSubstitutions(); }
    /** Same rule as {@link #getMaxChakra()}: the stamina bar's ceiling is the base pool's. */
    @Override
    public float getMaxStamina() { return base.getMaxStamina(); }

    // ---- capacity the addon layer adds on top of the configured pools ----

    /**
     * Chakra the addon's capacity rules put on top of the configured pool: the tree's capacity and
     * all-rounded nodes, the charge technique's mastery, the armour and eye bonuses and everything
     * picked up from scrolls.
     *
     * <p>Not spelled out as a sum of the individual fields on purpose: it is the difference between
     * the layer's own maximum ({@code NinjaData#getMaxChakra()}, which already holds the full
     * formula) and the value the layer read from its own config, so there is exactly one place to
     * extend when another capacity rule is added. The layer refreshes its copy of the configured
     * value every tick, so the difference is the bonus alone.</p>
     *
     * <p>The comparison is against the addon's config, not the base mod's: the base pool is whatever
     * the server configured for the base mod, and the addon only adds its own rules on top of it.
     * Comparing with the base value would silently force the base pool to the addon's number.</p>
     */
    public float capacityBonus() {
        return addon.getMaxChakra() - com.sekwah.narutoaddon.config.NarutoConfig.maxChakra;
    }

    /** Stamina the addon's rules add on top of the configured pool (same rule as {@link #capacityBonus()}). */
    public float staminaCapacityBonus() {
        return addon.getMaxStamina() - com.sekwah.narutoaddon.config.NarutoConfig.maxStamina;
    }

    /** Extra substitutions the tree hands out on top of the configured budget. */
    public float substitutionBonus() {
        return addon.getMaxSubstitutions()
                - (float) com.sekwah.narutoaddon.config.NarutoConfig.maxSubstitutions;
    }

    @Override
    public void setChakra(float amount) { base.setChakra(amount); }
    @Override
    public void setStamina(float amount) { base.setStamina(amount); }
    @Override
    public void useChakra(float amount, int cooldown) { base.useChakra(amount, cooldown); }
    @Override
    public void useStamina(float amount, int cooldown) { base.useStamina(amount, cooldown); }
    @Override
    public void useSubstitution(float amount) { base.useSubstitution(amount); }
    @Override
    public void addChakra(float amount) { base.addChakra(amount); }
    @Override
    public void addStamina(float amount) { base.addStamina(amount); }
    @Override
    public void setInvisibleTicks(int ticks) { base.setInvisibleTicks(ticks); }
    @Override
    public boolean getInvisible() { return base.getInvisible(); }
    @Override
    public Vec3 getSubstitutionLoc() { return base.getSubstitutionLoc(); }
    @Override
    public ResourceLocation getSubstitutionDimension() { return base.getSubstitutionDimension(); }
    @Override
    public void setSubstitutionLoc(Vec3 loc, ResourceLocation dimension) { base.setSubstitutionLoc(loc, dimension); }
    @Override
    public com.sekwah.narutoaddon.capabilities.DoubleJumpData getDoubleJumpData() { return addon.getDoubleJumpData(); }
    @Override
    public HashMap<String, com.sekwah.narutoaddon.capabilities.CooldownTickEvent> getCooldownEvents() { return addon.getCooldownEvents(); }
    @Override
    public ResourceLocation getCurrentlyChanneledAbility() { return base.getCurrentlyChanneledAbility(); }
    @Override
    public int getCurrentlyChanneledTicks() { return base.getCurrentlyChanneledTicks(); }
    @Override
    public void setCurrentlyChanneledAbility(Player player, Ability ability) {
        base.setCurrentlyChanneledAbility(player, AbilityProxy.getProxy(ability));
    }
    @Override
    public com.sekwah.narutoaddon.capabilities.toggleabilitydata.ToggleAbilityData getToggleAbilityData() { return addon.getToggleAbilityData(); }
    @Override
    public void updateDataServer(Player player) {
        // The base mod ticks its own capability itself, from
        // com.sekwah.narutomod.capabilities.NinjaCapabilityHandler#onPlayerUpdate (both server and
        // client side). This adapter therefore ticks only the addon layer
        // (NinjaData#updateAddonLayer), which owns the addon cooldown map and delayed events. Calling
        // the base layer here as well would run the base capability a second time, doubling
        // regen and speeding up channels (invisibleTicks, ticksChanneled).
        addon.updateAddonLayer(player, base);
    }
    @Override
    public void scheduleDelayedTickEvent(Consumer<Player> consumer, int tickDelay) { base.scheduleDelayedTickEvent(consumer, tickDelay); }
    @Override
    public void updateDataClient(Player player) {
        // The base mod ticks its own capability itself, from
        // com.sekwah.narutomod.capabilities.NinjaCapabilityHandler#onPlayerUpdate (client side), so
        // this adapter only ticks the addon layer to keep the double jump flag it owns from getting
        // stuck. Ticking the base layer here as well would run the base capability a second time.
        addon.updateDataClient(player);
    }
    @Override
    public void setIsNinja(boolean enableNinja) { base.setIsNinja(enableNinja); }
    @Override
    public boolean isNinjaModeEnabled() { return base.isNinjaModeEnabled(); }

    // ---- our-only fields delegated to internal NinjaData ----
    @Override
    public float getChakraCapacityBonus() { return addon.getChakraCapacityBonus(); }
    @Override
    public void setChakraCapacityBonus(float bonus) { addon.setChakraCapacityBonus(bonus); }
    @Override
    public float getArmorChakraBonus() { return addon.getArmorChakraBonus(); }
    @Override
    public void setArmorChakraBonus(float bonus) { addon.setArmorChakraBonus(bonus); }
    @Override
    public float getArmorHealthBonus() { return addon.getArmorHealthBonus(); }
    @Override
    public void setArmorHealthBonus(float bonus) { addon.setArmorHealthBonus(bonus); }
    @Override
    public float getEyeChakraBonus() { return addon.getEyeChakraBonus(); }
    @Override
    public void setEyeChakraBonus(float bonus) { addon.setEyeChakraBonus(bonus); }
    @Override
    public float getEyeSeninBonus() { return addon.getEyeSeninBonus(); }
    @Override
    public void setEyeSeninBonus(float bonus) { addon.setEyeSeninBonus(bonus); }
    @Override
    public void setEnchantChakraBonus(float bonus) { addon.setEnchantChakraBonus(bonus); }
    @Override
    public void setEnchantStaminaBonus(float bonus) { addon.setEnchantStaminaBonus(bonus); }
    @Override
    public void setEnchantSeninBonus(float bonus) { addon.setEnchantSeninBonus(bonus); }
    @Override
    public boolean isGudodamaForm() { return addon.isGudodamaForm(); }
    @Override
    public void setGudodamaForm(boolean active) { addon.setGudodamaForm(active); }
    @Override
    public float getShieldPoints() { return addon.getShieldPoints(); }
    @Override
    public void setShieldPoints(float points) { addon.setShieldPoints(points); }
    @Override
    public boolean isSecondBreathArmed() { return addon.isSecondBreathArmed(); }
    @Override
    public void setSecondBreathArmed(boolean armed) { addon.setSecondBreathArmed(armed); }
    @Override
    public int getGateCount() { return addon.getGateCount(); }
    @Override
    public void setGateCount(int gates) { addon.setGateCount(gates); }
    @Override
    public float getGateBacklashCarry() { return addon.getGateBacklashCarry(); }
    @Override
    public void setGateBacklashCarry(float carry) { addon.setGateBacklashCarry(carry); }
    @Override
    public float getChakraBonus() { return addon.getChakraBonus(); }
    @Override
    public void addChakraBonus(float amount) { addon.addChakraBonus(amount); }
    @Override
    public float getStaminaBonus() { return addon.getStaminaBonus(); }
    @Override
    public void addStaminaBonus(float amount) { addon.addStaminaBonus(amount); }
    @Override
    public float getChakraRegenBonus() { return addon.getChakraRegenBonus(); }
    @Override
    public void addChakraRegenBonus(float amount) { addon.addChakraRegenBonus(amount); }
    @Override
    public float getStaminaRegenBonus() { return addon.getStaminaRegenBonus(); }
    @Override
    public void addStaminaRegenBonus(float amount) { addon.addStaminaRegenBonus(amount); }
    @Override
    public int getVitalityBonus() { return addon.getVitalityBonus(); }
    @Override
    public void addVitalityBonus(int amount) { addon.addVitalityBonus(amount); }
    @Override
    public int getStrengthBonus() { return addon.getStrengthBonus(); }
    @Override
    public void addStrengthBonus(int amount) { addon.addStrengthBonus(amount); }
    @Override
    public Vec3 getMarkerLoc() { return addon.getMarkerLoc(); }
    @Override
    public ResourceLocation getMarkerDimension() { return addon.getMarkerDimension(); }
    @Override
    public void setMarkerLoc(Vec3 loc, ResourceLocation dimension) { addon.setMarkerLoc(loc, dimension); }
    @Override
    public com.sekwah.narutoaddon.menu.SupplyChestContainer getSupplyChest() { return addon.getSupplyChest(); }
    @Override
    public Vec3 getKamuiReturnLoc() { return addon.getKamuiReturnLoc(); }
    @Override
    public ResourceLocation getKamuiReturnDimension() { return addon.getKamuiReturnDimension(); }
    @Override
    public void setKamuiReturnLoc(Vec3 loc, ResourceLocation dimension) { addon.setKamuiReturnLoc(loc, dimension); }
    @Override
    public float getSeninMana() { return addon.getSeninMana(); }
    @Override
    public void setSeninMana(float mana) { addon.setSeninMana(mana); }
    @Override
    public void addSeninMana(float mana) { addon.addSeninMana(mana); }
    @Override
    public float getMaxSeninMana() { return addon.getMaxSeninMana(); }
    @Override
    public boolean isSageMode() { return addon.isSageMode(); }
    @Override
    public void setSageMode(boolean sage) { addon.setSageMode(sage); }
    @Override
    public int getSixPathsOrbs() { return addon.getSixPathsOrbs(); }
    @Override
    public void setSixPathsOrbs(int orbs) { addon.setSixPathsOrbs(orbs); }
    @Override
    public int getTenseiganOrbs() { return addon.getTenseiganOrbs(); }
    @Override
    public void setTenseiganOrbs(int orbs) { addon.setTenseiganOrbs(orbs); }
    @Override
    public int getSealVesselIntegrity() { return addon.getSealVesselIntegrity(); }
    @Override
    public void setSealVesselIntegrity(int integrity) { addon.setSealVesselIntegrity(integrity); }
    @Override
    public float getSensingRadius() { return addon.getSensingRadius(); }
    @Override
    public void setSensingRadius(float radius) { addon.setSensingRadius(radius); }
    @Override
    public int getNightVisionTicks() { return addon.getNightVisionTicks(); }
    @Override
    public void setNightVisionTicks(int ticks) { addon.setNightVisionTicks(ticks); }
    @Override
    public String getHengeTarget() { return addon.getHengeTarget(); }
    @Override
    public void setHengeTarget(String uuid) { addon.setHengeTarget(uuid); }
    @Override
    public String getHengeEntityType() { return addon.getHengeEntityType(); }
    @Override
    public void setHengeEntityType(String entityType) { addon.setHengeEntityType(entityType); }
    @Override
    public String getHengeName() { return addon.getHengeName(); }
    @Override
    public void setHengeName(String name) { addon.setHengeName(name); }
    @Override
    public int getHengeTicks() { return addon.getHengeTicks(); }
    @Override
    public void setHengeTicks(int ticks) { addon.setHengeTicks(ticks); }
    @Override
    public boolean isHenged() { return addon.isHenged(); }
    @Override
    public com.sekwah.narutoaddon.capabilities.absorption.AbsorptionData getAbsorptionData() { return addon.getAbsorptionData(); }
    @Override
    public com.sekwah.narutoaddon.capabilities.disabledabilitydata.DisabledAbilityData getDisabledAbilityData() { return addon.getDisabledAbilityData(); }
    @Override
    public boolean isAbilityDisabled(ResourceLocation abilityId) { return addon.isAbilityDisabled(abilityId); }
    @Override
    public boolean isMenuClickAll() { return addon.isMenuClickAll(); }
    @Override
    public void setMenuClickAll(boolean enabled) { addon.setMenuClickAll(enabled); }
    @Override
    public boolean hasClaimedBossMilestone(String bossId) { return addon.hasClaimedBossMilestone(bossId); }
    @Override
    public void claimBossMilestone(String bossId) { addon.claimBossMilestone(bossId); }
    @Override
    public String getNinjaRank() { return addon.getNinjaRank(); }
    @Override
    public void setNinjaRank(String rank) { addon.setNinjaRank(rank); }
    @Override
    public String getNatureAffinity() { return addon.getNatureAffinity(); }
    @Override
    public void setNatureAffinity(String affinity) { addon.setNatureAffinity(affinity); }
    @Override
    public boolean isHandbookGiven() { return addon.isHandbookGiven(); }
    @Override
    public void setHandbookGiven(boolean given) { addon.setHandbookGiven(given); }
    @Override
    public com.sekwah.narutoaddon.capabilities.mastery.MeditationData getMeditationData() { return addon.getMeditationData(); }
    @Override
    public com.sekwah.narutoaddon.capabilities.mastery.MasteryData getMasteryData() { return addon.getMasteryData(); }
    @Override
    public com.sekwah.narutoaddon.capabilities.skills.SkillData getSkillData() { return addon.getSkillData(); }

    @Override
    public Tag serializeNBT() { return addon.serializeNBT(); }

    public net.minecraft.nbt.CompoundTag serializeSyncNBT() { return addon.serializeSyncNBT(); }

    public void deserializeSyncNBT(net.minecraft.nbt.Tag tag) { addon.deserializeSyncNBT(tag); }

    @Override
    public void deserializeNBT(Tag nbt) { addon.deserializeNBT(nbt); }

    @Override
    public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> cap, net.minecraft.core.Direction side) {
        return net.minecraftforge.common.util.LazyOptional.empty();
    }
}
