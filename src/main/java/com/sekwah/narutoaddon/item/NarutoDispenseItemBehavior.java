package com.sekwah.narutoaddon.item;

import com.sekwah.narutoaddon.entity.projectile.ExplosiveKunaiEntity;
import com.sekwah.narutoaddon.entity.projectile.KunaiEntity;
import com.sekwah.narutoaddon.entity.projectile.SenbonEntity;
import com.sekwah.narutoaddon.entity.projectile.ShurikenEntity;
import net.minecraft.core.Position;
import net.minecraft.core.dispenser.AbstractProjectileDispenseBehavior;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;

public class NarutoDispenseItemBehavior {

    /**
     * Dispense the specified stack, play the dispense sound and spawn particles. {@link DispenseItemBehavior#bootStrap()}
     */
    public static void register() {
        DispenserBlock.registerBehavior(NarutoItems.KUNAI.get(), new AbstractProjectileDispenseBehavior() {
            protected Projectile getProjectile(Level level, Position pos, ItemStack p_123409_) {
                KunaiEntity arrow = new KunaiEntity(level, pos.x(), pos.y(), pos.z());
                arrow.pickup = AbstractArrow.Pickup.ALLOWED;
                return arrow;
            }
        });

        DispenserBlock.registerBehavior(NarutoItems.SHURIKEN.get(), new AbstractProjectileDispenseBehavior() {
            protected Projectile getProjectile(Level level, Position pos, ItemStack p_123409_) {
                ShurikenEntity arrow = new ShurikenEntity(level, pos.x(), pos.y(), pos.z());
                arrow.pickup = AbstractArrow.Pickup.ALLOWED;
                return arrow;
            }
        });

        DispenserBlock.registerBehavior(NarutoItems.EXPLOSIVE_KUNAI.get(), new AbstractProjectileDispenseBehavior() {
            protected Projectile getProjectile(Level level, Position pos, ItemStack p_123409_) {
                ExplosiveKunaiEntity arrow = new ExplosiveKunaiEntity(level, pos.x(), pos.y(), pos.z());
                arrow.pickup = AbstractArrow.Pickup.ALLOWED;
                return arrow;
            }
        });

        DispenserBlock.registerBehavior(NarutoItems.SENBON.get(), new AbstractProjectileDispenseBehavior() {
            protected Projectile getProjectile(Level level, Position pos, ItemStack p_123409_) {
                SenbonEntity arrow = new SenbonEntity(level, pos.x(), pos.y(), pos.z());
                arrow.pickup = AbstractArrow.Pickup.ALLOWED;
                return arrow;
            }
        });

    }

}
