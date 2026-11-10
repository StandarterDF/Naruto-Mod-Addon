package com.sekwah.narutoaddon.mixin.client;

import com.sekwah.narutoaddon.client.ClientSenriganFreecam;

import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Slides the camera out of the body while the Senrigan freecam stance is on.
 */
@Mixin(Camera.class)
public abstract class FreecamCameraMixin {

    @Accessor("position")
    public abstract void narutoaddon$setPosition(net.minecraft.world.phys.Vec3 pos);

    @Inject(method = "setup", at = @At("RETURN"))
    private void narutoaddon$freecamOffset(BlockGetter level, Entity entity, boolean detached,
                                            boolean thirdPersonReverse, float partialTick, CallbackInfo ci) {
        if (!ClientSenriganFreecam.isActive()) {
            return;
        }
        Camera self = (Camera) (Object) this;
        this.narutoaddon$setPosition(self.getPosition().add(ClientSenriganFreecam.interpolatedOffset(partialTick)));
    }
}