package com.sekwah.narutoaddon.abilities.jutsus;

import com.sekwah.narutoaddon.abilities.Ability;
import com.sekwah.narutoaddon.capabilities.INinjaData;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Tenseigan branch: wraps the caster in a chakra cocoon that absorbs incoming hits.
 */
@Mod.EventBusSubscriber(modid = "narutoaddon", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class OrbCocoonAbility extends Ability implements Ability.Cooldown {

    public static final long COMBO = 3113L;

    public static final ResourceLocation ID = new ResourceLocation("narutoaddon", "orb_cocoon");

    public static final String COCOON_UNTIL_KEY = "narutoaddon:cocoon_until";

    private static final float COST = 60.0f;
    private static final int COOLDOWN = 600;
    private static final int CHAKRA_REGEN_DELAY = 20;
    private static final int DURATION = 100;

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public long defaultCombo() {
        return COMBO;
    }

    @Override
    public int getCooldown() {
        return COOLDOWN;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (!TenseiganAbility.requireOpen(player, ninjaData, this)) {
            return false;
        }
        if (this.checkCooldown(player, ninjaData, this.getTranslationKey(ninjaData))) {
            return false;
        }
        float cost = this.masteryCost(ninjaData, COST);
        if (ninjaData.getChakra() < cost) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        ninjaData.useChakra(cost, CHAKRA_REGEN_DELAY);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        player.getPersistentData().putLong(COCOON_UNTIL_KEY, level.getGameTime() + DURATION);

        this.registerCooldown(player, ninjaData, this.getTranslationKey(ninjaData));
        this.recordMastery(player, ninjaData);
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (player.level().isClientSide()) {
            return;
        }
        // The void and anything else that ignores invulnerability still lands.
        if (event.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }
        long until = player.getPersistentData().getLong(COCOON_UNTIL_KEY);
        if (until <= 0) {
            return;
        }
        if (player.level().getGameTime() >= until) {
            return;
        }
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Player player = event.player;
        if (player.level().isClientSide()) {
            return;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        long until = player.getPersistentData().getLong(COCOON_UNTIL_KEY);
        if (until <= 0 || level.getGameTime() >= until) {
            return;
        }
        if (player.tickCount % 5 != 0) {
            return;
        }
        level.sendParticles(ParticleTypes.END_ROD, player.getX(),
                player.getY() + player.getBbHeight() * 0.5d, player.getZ(),
                8, 0.5d, 0.5d, 0.5d, 0.02d);
    }
}
