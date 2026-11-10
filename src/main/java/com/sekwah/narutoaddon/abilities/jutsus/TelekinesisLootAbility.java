package com.sekwah.narutoaddon.abilities.jutsus;

import com.sekwah.narutoaddon.abilities.Ability;
import com.sekwah.narutoaddon.capabilities.INinjaData;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;


/**
 * Tenseigan branch: channels to pull all nearby loose items to the caster.
 */
public class TelekinesisLootAbility extends Ability implements Ability.Channeled, Ability.HandleEnded {

    public static final long COMBO = 3121L;

    public static final ResourceLocation ID = new ResourceLocation("narutoaddon", "telekinesis_loot");

    private static final float COST_PER_TICK = 0.3f;
    private static final int CHAKRA_REGEN_DELAY = 20;
    private static final double RANGE = 30.0d;

    @Override
    public int noiseRadius() {
        return 10;
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
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (!TenseiganAbility.requireOpen(player, ninjaData, this)) {
            return false;
        }
        float cost = this.masteryCost(ninjaData, COST_PER_TICK);
        if (ninjaData.getChakra() < cost) {
            if (chargeAmount == 0) {
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable("jutsu.fail.notenoughchakra",
                        net.minecraft.network.chat.Component.translatable(this.getTranslationKey(ninjaData)).withStyle(net.minecraft.ChatFormatting.YELLOW)), true);
            }
            return false;
        }
        ninjaData.useChakra(cost, CHAKRA_REGEN_DELAY);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        // Called both when the key is let go and when the pool runs dry: either way nothing may be
        // left hanging in the air.
        releaseItems(player);
    }

    @Override
    public void handleChannelling(Player player, INinjaData ninjaData, int ticksChanneled) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        this.recordMasteryCredit(player, ninjaData, Ability.MASTERY_CREDIT_PER_TICK);
        if (ticksChanneled % 2 != 0) {
            return;
        }
        Vec3 target = player.getEyePosition();
        for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class,
                player.getBoundingBox().inflate(RANGE),
                e -> e.isAlive() && !e.getItem().isEmpty())) {
            if (entity.distanceToSqr(player) > RANGE * RANGE) {
                continue;
            }
            Vec3 dir = target.subtract(entity.position());
            if (dir.lengthSqr() < 0.001d) {
                continue;
            }
            entity.setDeltaMovement(dir.normalize().scale(0.45d));
            entity.hurtMarked = true;
            entity.setNoGravity(true);
            if (entity.distanceTo(player) < 1.5d) {
                entity.setNoGravity(false);
            }
        }
    }

    @Override
    public void handleAbilityEnded(Player player, INinjaData ninjaData, int ticksActive) {
        releaseItems(player);
    }

    /** Hands the pulled items back to gravity. */
    private static void releaseItems(Player player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class,
                player.getBoundingBox().inflate(RANGE))) {
            if (entity.distanceToSqr(player) > RANGE * RANGE) {
                continue;
            }
            entity.setNoGravity(false);
        }
    }

    @Override
    public void recordMastery(Player player, INinjaData ninjaData) {
    }
}