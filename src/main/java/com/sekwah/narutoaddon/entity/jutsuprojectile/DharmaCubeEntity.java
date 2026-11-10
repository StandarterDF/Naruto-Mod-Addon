package com.sekwah.narutoaddon.entity.jutsuprojectile;

import com.sekwah.narutoaddon.entity.NarutoEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * A two-by-two-by-two black cube that falls from the sky and crushes everything it lands on.
 */
public class DharmaCubeEntity extends Entity {

    @Nullable
    private UUID owner;

    public DharmaCubeEntity(EntityType<? extends DharmaCubeEntity> entityType, Level level) {
        super(entityType, level);
    }

    public DharmaCubeEntity(Player caster, Level level, double x, double y, double z) {
        this(NarutoEntities.DHARMA_CUBE.get(), level);
        this.owner = caster.getUUID();
        this.setPos(x, y, z);
        this.setDeltaMovement(0.0d, -0.6d, 0.0d);
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.owner = tag.getUUID("Owner");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.owner != null) {
            tag.putUUID("Owner", this.owner);
        }
    }

    @Override
    public void tick() {
        super.tick();
        this.setDeltaMovement(0.0d, -0.6d, 0.0d);
        this.move(net.minecraft.world.entity.MoverType.SELF, this.getDeltaMovement());

        if (this.level().isClientSide) {
            return;
        }
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }

        boolean hit = this.onGround() || this.verticalCollision || this.horizontalCollision;
        if (!hit) {
            AABB box = this.getBoundingBox().inflate(0.1d);
            List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, box);
            for (LivingEntity e : nearby) {
                if (this.owner != null && e.getUUID().equals(this.owner)) {
                    continue;
                }
                hit = true;
                break;
            }
        }
        if (hit) {
            this.impact(level);
        }
    }

    private void impact(ServerLevel level) {
        Player caster = this.owner == null ? null : level.getPlayerByUUID(this.owner);
        DamageSource source = caster != null
                ? caster.damageSources().playerAttack(caster)
                : level.damageSources().magic();
        AABB area = this.getBoundingBox().inflate(3.0d);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area)) {
            if (this.owner != null && target.getUUID().equals(this.owner)) {
                continue;
            }
            target.hurt(source, 18.0f);
        }
        level.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY() + 1.0d, this.getZ(),
                30, 1.5d, 1.5d, 1.5d, 0.1d);
        this.discard();
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }
}