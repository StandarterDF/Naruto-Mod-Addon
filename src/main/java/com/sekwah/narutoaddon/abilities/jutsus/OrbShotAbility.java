package com.sekwah.narutoaddon.abilities.jutsus;

import com.sekwah.narutoaddon.abilities.Ability;
import com.sekwah.narutoaddon.abilities.mastery.Mastery;
import com.sekwah.narutoaddon.capabilities.INinjaData;
import com.sekwah.narutoaddon.entity.jutsuprojectile.ChakraOrbEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tenseigan branch: the Truth-Seeking Orbs of the halo behind the caster, thrown as one sphere.
 *
 * <p>A tap throws a single orb. Holding the same keys gathers: every second one more orb is pulled
 * out of the ring into the sphere forming in front of the caster, up to all six, and letting go
 * throws everything at once. The ring is therefore a real resource - the halo behind the back
 * visibly empties as the sphere is fed, and regrows one orb every five seconds while the eye is
 * open.</p>
 *
 * <p>The shot's damage scales with the number of orbs inside it, so a full six-orb sphere hits six
 * times as hard as a single orb, while its blast grows more gently so it stays a strike rather than
 * a crater.</p>
 */
public class OrbShotAbility extends Ability implements Ability.Channeled, Ability.Cooldown {

    public static final long COMBO = 3113L;

    public static final ResourceLocation ID = new ResourceLocation("narutoaddon", "orb_shot");

    /** Chakra paid to open the sphere, and every tick it is gathered afterwards. */
    private static final float OPEN_COST = 30.0f;
    private static final float UPKEEP = 0.35f;

    private static final int COOLDOWN = 100;
    private static final int CHAKRA_REGEN_DELAY = 20;

    /** One more orb is pulled into the sphere every second of holding. */
    public static final int ORB_INTERVAL = 20;

    /** Orbs gathered so far, per caster, while the technique is held. */
    private static final Map<UUID, Integer> CHARGED = new HashMap<>();

    @Override
    public ActivationType activationType() {
        return ActivationType.CHANNELED;
    }

    /** A tap is a single-orb shot; it never fizzles for being too short. */
    @Override
    public boolean canActivateBelowMinCharge() {
        return true;
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
        // 0 is the first tick of a held cast, -1 is a tap of a channeled technique. Both open the
        // sphere and take the first orb out of the ring.
        boolean opening = chargeAmount <= 0;
        if (opening) {
            if (this.checkCooldown(player, ninjaData, this.getTranslationKey(ninjaData))) {
                return false;
            }
            if (TenseiganAbility.orbs(ninjaData) <= 0) {
                player.displayClientMessage(Component.translatable("jutsu.fail.tenseigan.no_orbs",
                                Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW))
                        .withStyle(ChatFormatting.RED), true);
                return false;
            }
        }
        float cost = this.masteryCost(ninjaData, opening ? OPEN_COST : UPKEEP);
        if (ninjaData.getChakra() < cost) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        ninjaData.useChakra(cost, CHAKRA_REGEN_DELAY);
        if (opening) {
            // The first orb leaves the halo the moment the sphere opens.
            TenseiganAbility.spendOrb(ninjaData);
            CHARGED.put(player.getUUID(), 1);
        }
        return true;
    }

    /**
     * The gathering: one more orb is pulled out of the halo every second and merged into the sphere
     * ahead of the caster, up to the six the ring holds.
     */
    @Override
    public void handleChannelling(Player player, INinjaData ninjaData, int ticksChanneled) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        this.recordMasteryCredit(player, ninjaData, MASTERY_CREDIT_PER_TICK);

        if (ticksChanneled > 0 && ticksChanneled % ORB_INTERVAL == 0) {
            int gathered = CHARGED.getOrDefault(player.getUUID(), 1);
            if (gathered < TenseiganAbility.MAX_ORBS && TenseiganAbility.spendOrb(ninjaData)) {
                gathered++;
                CHARGED.put(player.getUUID(), gathered);
                level.playSound(null, player, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS,
                        0.5f, 0.8f + 0.1f * gathered);
            }
        }

        drawGathering(level, player, CHARGED.getOrDefault(player.getUUID(), 1), ticksChanneled);
    }

    /** Where the sphere forms: a little ahead of the eyes, growing with the orbs inside it. */
    private static Vec3 gatherCenter(Player player, int gathered) {
        Vec3 look = player.getLookAngle();
        double ahead = 0.7d + 0.08d * (gathered - 1);
        return player.getEyePosition().add(look.scale(ahead)).subtract(0.0d, 0.15d, 0.0d);
    }

    /** Turquoise motes closing into one growing sphere. */
    private static void drawGathering(ServerLevel level, Player player, int gathered, int ticksChanneled) {
        Vec3 center = gatherCenter(player, gathered);
        double radius = 0.18d + 0.10d * (gathered - 1);
        double spin = ticksChanneled * 0.35d;
        int points = 8 + gathered * 4;

        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2.0d * i / points + spin;
            double r = radius * (0.7d + 0.3d * Math.sin(ticksChanneled * 0.2d + i));
            level.sendParticles(ParticleTypes.END_ROD,
                    center.x + Math.cos(angle) * r,
                    center.y + Math.sin(angle * 1.7d) * r * 0.6d,
                    center.z + Math.sin(angle) * r,
                    1, 0.0d, 0.0d, 0.0d, 0.0d);
        }
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, center.x, center.y, center.z,
                2 + gathered, radius * 0.5d, radius * 0.5d, radius * 0.5d, 0.01d);
    }

    /** Letting go throws whatever was gathered; a tap throws the single orb it took. */
    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        if (!CHARGED.containsKey(player.getUUID())) {
            return;
        }
        this.fire(player, ninjaData, level, CHARGED.getOrDefault(player.getUUID(), 1));
    }

    /**
     * Throws the sphere. Damage grows with the orbs inside it - six orbs hit six times as hard -
     * while the radius and the number of blasts grow more gently.
     */
    private void fire(Player player, INinjaData ninjaData, ServerLevel level, int gathered) {
        int power = Mth.clamp(gathered, 1, TenseiganAbility.MAX_ORBS);
        CHARGED.remove(player.getUUID());

        float mastery = Mastery.level(ninjaData, this);
        float damage = (10.0f + 15.0f * mastery) * power;
        float blastRadius = (3.0f + 2.0f * mastery) * (1.0f + 0.10f * (power - 1));
        int blasts = 1 + (int) Math.floor(mastery * 3.0f) + (power - 1);
        float renderRadius = Mth.lerp(mastery, 0.30f, 0.55f) * (1.0f + 0.15f * (power - 1));

        ChakraOrbEntity orb = ChakraOrbEntity.launch(player, 1.6d, renderRadius, (byte) 1,
                damage, blastRadius, blasts, false, mastery);
        level.addFreshEntity(orb);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8f, 0.6f + 0.1f * power);

        this.registerCooldown(player, ninjaData, this.getTranslationKey(ninjaData));
    }

    /** A held sphere is credited per tick while it gathers, not on the throw. */
    @Override
    public void recordMastery(Player player, INinjaData ninjaData) {
    }
}
