package com.sekwah.narutoaddon.entity.jutsuprojectile;

import com.sekwah.narutoaddon.damagetypes.NarutoDamageTypes;
import com.sekwah.narutoaddon.entity.NarutoEntities;
import com.sekwah.narutoaddon.entity.projectile.AbstractNonGlowingHurtingProjectile;
import com.sekwah.narutoaddon.friends.Friends;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A real, visible chakra sphere in flight: either a Truth-Seeking Orb (style 0, black) or a
 * Tenseigan sphere (style 1, glowing turquoise).
 *
 * <p>Both techniques that throw a sphere - the Six Paths orb launch and the Tenseigan orb shot -
 * use this one entity, so the thing in flight is the same geometry as the halo behind a caster
 * (see {@code TruthSeekerOrbModel}). Only the material differs, and that is the synced style.</p>
 *
 * <p>The blast is a genuine explosion, and how many of them there are along with their radius and
 * the damage each body takes are handed in by the technique. The Tenseigan scales all three with
 * mastery, the Six Paths orb throws its fixed one.</p>
 */
public class ChakraOrbEntity extends AbstractNonGlowingHurtingProjectile {

    /** How long the orb may fly before it goes off where it is. */
    private static final int MAX_LIFE = 60;

    /** Sphere radius used by the renderer, synced so both sides draw the same size. */
    private static final EntityDataAccessor<Float> DATA_RADIUS =
            SynchedEntityData.defineId(ChakraOrbEntity.class, EntityDataSerializers.FLOAT);

    /** 0 = Truth-Seeking Orb (black), 1 = Tenseigan sphere (turquoise). */
    private static final EntityDataAccessor<Byte> DATA_STYLE =
            SynchedEntityData.defineId(ChakraOrbEntity.class, EntityDataSerializers.BYTE);

    /** Server-only payload of the throw; persisted so a reload does not turn it into a dud. */
    private float damage = 0.0f;
    private float blastRadius = 3.0f;
    private int blasts = 1;
    private boolean tagOrbLaunch = false;
    private float mastery = 0.0f;
    private int life = MAX_LIFE;

    public ChakraOrbEntity(EntityType<ChakraOrbEntity> type, Level level) {
        super(type, level);
    }

    /**
     * Builds a sphere leaving the caster's eyes along their look.
     *
     * @param caster      the ninja throwing it
     * @param speed       flight speed in blocks per tick
     * @param renderRadius radius of the drawn sphere
     * @param style       0 for black, 1 for turquoise
     * @param damage      damage dealt to every body caught in the blast
     * @param blastRadius radius of the first explosion and of the damage sphere
     * @param blasts      how many explosions the impact is made of
     * @param tagOrbLaunch whether to mark victims for the Six Paths orb-kill bookkeeping
     * @param mastery     mastery of the throwing technique, used by that bookkeeping
     */
    public static ChakraOrbEntity launch(LivingEntity caster, double speed, float renderRadius, byte style,
                                         float damage, float blastRadius, int blasts, boolean tagOrbLaunch,
                                         float mastery) {
        ChakraOrbEntity orb = new ChakraOrbEntity(NarutoEntities.CHAKRA_ORB.get(), caster.level());
        Vec3 spawnPos = caster.getEyePosition().subtract(0.0d, 0.2d, 0.0d);
        orb.moveTo(spawnPos.x, spawnPos.y, spawnPos.z);
        orb.setOwner(caster);
        Vec3 look = caster.getLookAngle().scale(speed);
        orb.xPower = look.x;
        orb.yPower = look.y;
        orb.zPower = look.z;
        orb.setRot(caster.getYRot(), caster.getXRot());
        orb.damage = damage;
        orb.blastRadius = blastRadius;
        orb.blasts = blasts;
        orb.tagOrbLaunch = tagOrbLaunch;
        orb.mastery = mastery;
        orb.life = MAX_LIFE;
        orb.entityData.set(DATA_RADIUS, renderRadius);
        orb.entityData.set(DATA_STYLE, style);
        return orb;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_RADIUS, 0.5f);
        this.entityData.define(DATA_STYLE, (byte) 0);
    }

    /** @return the synced sphere radius used for rendering. */
    public float getRenderRadius() {
        return this.entityData.get(DATA_RADIUS);
    }

    /** @return the visual style: 0 = Truth-Seeking Orb, 1 = Tenseigan sphere. */
    public byte getStyle() {
        return this.entityData.get(DATA_STYLE);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.isRemoved() || !(this.level() instanceof ServerLevel level)) {
            return;
        }
        if (this.tickCount % 2 == 0) {
            ParticleOptions trail = this.getStyle() == 0 ? ParticleTypes.SQUID_INK : ParticleTypes.END_ROD;
            level.sendParticles(trail, this.getX(), this.getY(), this.getZ(),
                    1, 0.05d, 0.05d, 0.05d, 0.01d);
        }
        if (--this.life <= 0) {
            this.explode(level);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult hitResult) {
        super.onHitEntity(hitResult);
        if (!(this.level() instanceof ServerLevel level) || this.isRemoved()) {
            return;
        }
        Entity target = hitResult.getEntity();
        Entity owner = this.getOwner();
        boolean valid = target != owner
                && target.isAlive()
                && !(target instanceof Player player && player.isCreative())
                && (owner == null || !Friends.isOwnSide(owner, target));
        if (valid) {
            DamageSource source = NarutoDamageTypes.getDamageSource(level, NarutoDamageTypes.TAILED_BALL, this, owner);
            target.hurt(source, this.damage);
        }
        this.explode(level);
    }

    @Override
    protected void onHit(HitResult hitResult) {
        super.onHit(hitResult);
        if (hitResult.getType() == HitResult.Type.BLOCK && this.level() instanceof ServerLevel level) {
            this.explode(level);
        }
    }

    /** Detonates the orb into its explosions, particles, sound and area damage. */
    public void explode(ServerLevel level) {
        if (this.isRemoved()) {
            return;
        }
        double x = this.getX();
        double y = this.getY();
        double z = this.getZ();
        byte style = this.getStyle();

        for (int i = 0; i < this.blasts; i++) {
            double spread = this.blastRadius * 0.35d;
            double dx = (this.random.nextDouble() - 0.5d) * 2.0d * spread;
            double dy = (this.random.nextDouble() - 0.5d) * 2.0d * spread;
            double dz = (this.random.nextDouble() - 0.5d) * 2.0d * spread;
            float radius = this.blastRadius * (i == 0 ? 1.0f : 0.6f);
            level.explode(this, x + dx, y + dy, z + dz, radius, Level.ExplosionInteraction.NONE);
        }

        int particleCount = Math.max(10, (int) (this.blastRadius * 20.0f));
        ParticleOptions main = style == 0 ? ParticleTypes.SQUID_INK : ParticleTypes.END_ROD;
        double scatter = this.blastRadius * 0.4d;
        level.sendParticles(main, x, y, z, particleCount, scatter, scatter, scatter, 0.06d);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, particleCount / 2,
                scatter, scatter, scatter, 0.04d);
        level.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 4.0f, 0.7f);

        Entity owner = this.getOwner();
        float reach = this.blastRadius * 1.4f;
        AABB box = new AABB(x - reach, y - reach, z - reach, x + reach, y + reach, z + reach);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box)) {
            if (target == owner || !target.isAlive()) {
                continue;
            }
            if (owner != null && Friends.isOwnSide(owner, target)) {
                continue;
            }
            if (target instanceof Player player && (player.isCreative() || player.isSpectator())) {
                continue;
            }
            if (target.distanceToSqr(x, y, z) > reach * reach) {
                continue;
            }
            target.hurt(level.damageSources().indirectMagic(owner, owner), this.damage);
            if (this.tagOrbLaunch) {
                target.getPersistentData().putBoolean("narutomod:orb_launch_kill", true);
                target.getPersistentData().putFloat("narutomod:orb_launch_mastery", this.mastery);
            }
        }

        this.discard();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("damage", this.damage);
        tag.putFloat("blastRadius", this.blastRadius);
        tag.putInt("blasts", this.blasts);
        tag.putBoolean("tagOrbLaunch", this.tagOrbLaunch);
        tag.putFloat("mastery", this.mastery);
        tag.putInt("life", this.life);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("damage")) {
            this.damage = tag.getFloat("damage");
        }
        if (tag.contains("blastRadius")) {
            this.blastRadius = tag.getFloat("blastRadius");
        }
        if (tag.contains("blasts")) {
            this.blasts = tag.getInt("blasts");
        }
        if (tag.contains("tagOrbLaunch")) {
            this.tagOrbLaunch = tag.getBoolean("tagOrbLaunch");
        }
        if (tag.contains("mastery")) {
            this.mastery = tag.getFloat("mastery");
        }
        if (tag.contains("life")) {
            this.life = tag.getInt("life");
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    protected ParticleOptions getTrailParticle() {
        // The base class drops one of these every tick; match the sphere so the trail never lies
        // about which technique threw it.
        return this.getStyle() == 0 ? ParticleTypes.SQUID_INK : ParticleTypes.END_ROD;
    }

    @Override
    protected boolean shouldBurn() {
        return false;
    }
}
