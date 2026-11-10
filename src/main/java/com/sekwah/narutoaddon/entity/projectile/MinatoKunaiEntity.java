package com.sekwah.narutoaddon.entity.projectile;

import com.sekwah.narutoaddon.entity.NarutoEntities;
import com.sekwah.narutoaddon.item.NarutoItems;
import com.sekwah.narutoaddon.mixin.common.AbstractArrowAccessor;
import com.sekwah.narutoaddon.sounds.NarutoSounds;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

public class MinatoKunaiEntity extends NoArrowAbstractArrow {

    public MinatoKunaiEntity(EntityType<? extends MinatoKunaiEntity> type, Level level) {
        super(type, level);
    }

    public MinatoKunaiEntity(Level level, LivingEntity shooter) {
        super(NarutoEntities.MINATO_KUNAI.get(), shooter, level);
    }

    public MinatoKunaiEntity(Level level, double x, double y, double z) {
        super(NarutoEntities.MINATO_KUNAI.get(), x, y, z, level);
    }

    @Override
    protected SoundEvent getDefaultHitGroundSoundEvent() {
        return com.sekwah.narutomod.sounds.NarutoSounds.KUNAI_THUD.get();
    }

    @Override
    public void setSoundEvent(SoundEvent sound) {
        super.setSoundEvent(this.getDefaultHitGroundSoundEvent());
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    protected ItemStack getPickupItem() {
        return new ItemStack(NarutoItems.MINATO_KUNAI.get());
    }

    public boolean isStuckInGround() {
        return ((AbstractArrowAccessor) (Object) this).naruto$isInGround();
    }
}