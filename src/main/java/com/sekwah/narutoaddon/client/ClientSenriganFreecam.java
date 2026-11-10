package com.sekwah.narutoaddon.client;

import com.sekwah.narutoaddon.abilities.jutsus.SenriganFreecamAbility;
import com.sekwah.narutoaddon.abilities.ToggleState;
import com.sekwah.narutoaddon.capabilities.INinjaData;
import com.sekwah.narutoaddon.capabilities.NinjaCapabilityHandler;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-side state for the Senrigan freecam: a freely flying camera offset from the caster's body.
 *
 * <p>The stance itself is a server-side toggle; this class only reads it (through {@link ToggleState})
 * and accumulates the camera offset while it is on. Damage taken while the eye is out resets the
 * offset and closes the stance on the next tick.</p>
 */
@Mod.EventBusSubscriber(modid = "narutoaddon", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientSenriganFreecam {

    private ClientSenriganFreecam() {
    }

    private static boolean active = false;
    private static Vec3 offset = Vec3.ZERO;
    private static Vec3 prevOffset = Vec3.ZERO;
    private static float lastHealth = -1f;
    private static CameraType savedCameraType = null;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getInstance();

        // Movement input is consumed by the body's tick loop; zero it out while the eye is out so the
        // real body stands still. The camera itself reads the keys directly, so flight still works.
        if (event.phase == TickEvent.Phase.START) {
            if (active && mc.player != null) {
                mc.player.input.forwardImpulse = 0.0F;
                mc.player.input.leftImpulse = 0.0F;
                mc.player.input.jumping = false;
            }
            return;
        }

        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (mc.player == null || mc.level == null) {
            return;
        }

        INinjaData ninjaData = mc.player.getCapability(NinjaCapabilityHandler.NINJA_DATA).resolve().orElse(null);
        boolean on = ninjaData != null && ToggleState.isActive(ninjaData, SenriganFreecamAbility.ID);

        if (!on && active) {
            active = false;
            offset = Vec3.ZERO;
            prevOffset = Vec3.ZERO;
            if (savedCameraType != null) {
                mc.options.setCameraType(savedCameraType);
                savedCameraType = null;
            }
            return;
        }
        if (!on) {
            prevOffset = offset;
            return;
        }
        if (!active) {
            active = true;
            offset = Vec3.ZERO;
            prevOffset = Vec3.ZERO;
            lastHealth = mc.player.getHealth();
            // Switch to third person so the vanilla renderer keeps drawing the caster's own model.
            if (savedCameraType == null) {
                savedCameraType = mc.options.getCameraType();
            }
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }

        // Damage taken while the eye is out brings it straight back.
        if (mc.player.getHealth() < lastHealth) {
            active = false;
            offset = Vec3.ZERO;
            prevOffset = Vec3.ZERO;
            lastHealth = mc.player.getHealth();
            if (savedCameraType != null) {
                mc.options.setCameraType(savedCameraType);
                savedCameraType = null;
            }
            return;
        }
        lastHealth = mc.player.getHealth();

        // While a screen is open the offset is frozen: the player is not steering.
        if (mc.screen != null) {
            prevOffset = offset;
            return;
        }

        // Remember the previous offset before advancing it, for smooth interpolation on render.
        prevOffset = offset;

        double speed = 0.6d;
        if (mc.options.keySprint.isDown()) {
            speed *= 3.0d;
        }

        double yaw = Math.toRadians(mc.player.getViewYRot(1.0f));
        // Forward and right in the horizontal plane, derived from the yaw only so vertical look
        // does not tilt the flight direction.
        Vec3 forward = new Vec3(-Math.sin(yaw), 0.0d, Math.cos(yaw));
        // Facing south (yaw 0) the right hand points west, i.e. -X, hence the negated vector.
        Vec3 right = new Vec3(-Math.cos(yaw), 0.0d, -Math.sin(yaw));

        Vec3 move = Vec3.ZERO;
        if (mc.options.keyUp.isDown()) {
            move = move.add(forward);
        }
        if (mc.options.keyDown.isDown()) {
            move = move.subtract(forward);
        }
        if (mc.options.keyLeft.isDown()) {
            move = move.subtract(right);
        }
        if (mc.options.keyRight.isDown()) {
            move = move.add(right);
        }
        if (mc.options.keyJump.isDown()) {
            move = move.add(0.0d, 1.0d, 0.0d);
        }
        if (mc.options.keyShift.isDown()) {
            move = move.subtract(0.0d, 1.0d, 0.0d);
        }

        if (move.lengthSqr() > 0.0d) {
            move = move.normalize().scale(speed);
            offset = offset.add(move);
        }

        if (offset.length() > 50.0d) {
            offset = offset.normalize().scale(50.0d);
        }
    }

    public static boolean isActive() {
        return active;
    }

    public static Vec3 offset() {
        return offset;
    }

    /**
     * Smoothly interpolated offset for a given partial tick, used by the camera mixin so the camera
     * does not jump in whole-tick steps.
     */
    public static Vec3 interpolatedOffset(float partialTick) {
        return new Vec3(
                Mth.lerp((double) partialTick, prevOffset.x, offset.x),
                Mth.lerp((double) partialTick, prevOffset.y, offset.y),
                Mth.lerp((double) partialTick, prevOffset.z, offset.z));
    }
}