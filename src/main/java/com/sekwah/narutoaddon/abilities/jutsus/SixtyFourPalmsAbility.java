package com.sekwah.narutoaddon.abilities.jutsus;

import com.sekwah.narutoaddon.abilities.Ability;
import com.sekwah.narutoaddon.abilities.mastery.Mastery;
import com.sekwah.narutoaddon.capabilities.INinjaData;
import com.sekwah.narutoaddon.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutoaddon.capabilities.enemychakra.EnemyChakra;
import com.sekwah.narutoaddon.config.NarutoConfig;
import com.sekwah.narutoaddon.friends.Friends;
import com.sekwah.narutoaddon.util.Targeting;
import com.sekwah.narutoaddon.abilities.jutsus.ByakuganAbility;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = "narutoaddon", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SixtyFourPalmsAbility extends Ability implements Ability.Channeled, Ability.Cooldown, Ability.HandleEnded {

    @Override
    public int noiseRadius() {
        return 10;
    }

    public static final long COMBO = 3112L;

    public static final ResourceLocation ID = new ResourceLocation("narutomod", "sixty_four_palms");

    private static final double RANGE = 3.0d;

    private static final float COST_PER_TICK = 0.25f;

    private static final int COOLDOWN = 240;

    private static final float PALM_DAMAGE = 1.5f;

    private static final float PALM_DRAIN = 4.0f;

    private final Map<UUID, UUID> fixed = new HashMap<>();

    public static final List<PendingStrike> PENDING = new ArrayList<>();

    public record PendingStrike(UUID casterId, UUID targetId, ResourceKey<Level> dimension, long executeAt, int index, int total, float damage) {
    }

    @Override
    public ActivationType activationType() {
        return ActivationType.CHANNELED;
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
    public boolean canActivateBelowMinCharge() {
        return false;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (chargeAmount < 0) {
            return false;
        }
        if (chargeAmount <= 0 && this.checkCooldown(player, ninjaData, this.getTranslationKey(ninjaData))) {
            return false;
        }
        if (!NarutoConfig.byakuganEnabled) {
            player.displayClientMessage(Component.translatable("jutsu.fail.byakugan.disabled")
                    .withStyle(ChatFormatting.RED), true);
            return false;
        }
        if (!ByakuganAbility.isActive(ninjaData)) {
            player.displayClientMessage(Component.translatable("jutsu.fail.byakugan.required",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW))
                    .withStyle(ChatFormatting.RED), true);
            return false;
        }
        if (chargeAmount >= 120) {
            return false;
        }
        LivingEntity target = null;
        if (chargeAmount == 0) {
            Entity looked = Targeting.lookedAtEntity(player, RANGE);
            if (!(looked instanceof LivingEntity l) || !canStrike(player, l)) {
                player.displayClientMessage(Component.translatable("jutsu.fail.sixty_four_palms.notarget",
                        Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW))
                        .withStyle(ChatFormatting.RED), true);
                return false;
            }
            target = l;
        } else if (!this.fixed.containsKey(player.getUUID())) {
            return false;
        }

        float cost = this.masteryCost(ninjaData, COST_PER_TICK);
        if (ninjaData.getChakra() < cost) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        ninjaData.useChakra(cost, 20);
        if (target != null) {
            this.fixed.put(player.getUUID(), target.getUUID());
        }
        return true;
    }

    @Override
    public void handleChannelling(Player player, INinjaData ninjaData, int ticksChanneled) {
        this.recordMasteryCredit(player, ninjaData, Ability.MASTERY_CREDIT_PER_TICK);
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        UUID targetId = this.fixed.remove(player.getUUID());
        int hits = hitsForTicks(ticksActive);
        if (targetId == null || hits <= 0) {
            return;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        long now = level.getGameTime();
        // The palm's weight is read once, at the moment the kata is released: the strikes that follow
        // are already in flight and must not change value if the caster's mastery moves meanwhile.
        float palm = ByakuganAbility.masteredDamage(ninjaData, this, PALM_DAMAGE);
        for (int i = 0; i < hits; i++) {
            PENDING.add(new PendingStrike(player.getUUID(), targetId, level.dimension(), now + i, i, hits, palm));
        }
        this.registerCooldown(player, ninjaData, this.getTranslationKey(ninjaData));
    }

    @Override
    public void recordMastery(Player player, INinjaData ninjaData) {
    }

    private static int hitsForTicks(int ticks) {
        if (ticks >= 120) {
            return 64;
        }
        if (ticks >= 100) {
            return 32;
        }
        if (ticks >= 80) {
            return 16;
        }
        if (ticks >= 60) {
            return 8;
        }
        if (ticks >= 40) {
            return 4;
        }
        if (ticks >= 20) {
            return 2;
        }
        return 0;
    }

    private static boolean canStrike(Player caster, LivingEntity target) {
        if (target == caster || target.isRemoved() || target.isSpectator() || !target.isAlive()) {
            return false;
        }
        if (target instanceof Player other && (other.isCreative() || other.isSpectator())) {
            return false;
        }
        return !target.isAlliedTo(caster) && !Friends.areFriends(caster, target);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftServer server = event.getServer();
        Iterator<PendingStrike> it = PENDING.iterator();
        while (it.hasNext()) {
            PendingStrike ps = it.next();
            ServerLevel level = server.getLevel(ps.dimension());
            if (level == null) {
                it.remove();
                continue;
            }
            if (level.getGameTime() < ps.executeAt()) {
                continue;
            }
            it.remove();
            Player caster = server.getPlayerList().getPlayer(ps.casterId());
            // A dead or gone caster takes the rest of the kata with them.
            if (caster == null || !caster.isAlive() || caster.level().dimension() != ps.dimension()) {
                continue;
            }
            Entity targetEntity = level.getEntity(ps.targetId());
            if (!(targetEntity instanceof LivingEntity target) || !target.isAlive()) {
                continue;
            }
            target.invulnerableTime = 0;
            float damage = ps.damage() + target.getArmorValue() * 0.15f;
            target.hurt(caster != null ? caster.damageSources().playerAttack(caster) : level.damageSources().magic(), damage);
            EnemyChakra.drain(target, PALM_DRAIN);
            level.sendParticles(ParticleTypes.CRIT, target.getX(),
                    target.getY() + target.getBbHeight() * 0.5d, target.getZ(),
                    3, 0.1d, 0.1d, 0.1d, 0.05d);
            if (ps.index() == ps.total() - 1) {
                Vec3 dir = caster != null ? caster.getLookAngle() : new Vec3(0.0d, 0.0d, 1.0d);
                target.setDeltaMovement(dir.x * 1.2d, 0.4d, dir.z * 1.2d);
                target.hurtMarked = true;
            }
        }
    }

    /** A broken hold drops the locked target; strikes already in flight finish on their own. */
    @Override
    public void handleAbilityEnded(Player player, INinjaData ninjaData, int ticksActive) {
        this.fixed.remove(player.getUUID());
    }

    @SubscribeEvent
    public static void onServerStopping(net.minecraftforge.event.server.ServerStoppingEvent event) {
        PENDING.clear();
    }
}
