package com.sekwah.narutoaddon.client;

import com.sekwah.narutoaddon.abilities.jutsus.ByakuganAbility;
import com.sekwah.narutoaddon.capabilities.INinjaData;
import com.sekwah.narutoaddon.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutoaddon.client.keybinds.NarutoKeyHandler;
import com.sekwah.narutoaddon.config.NarutoConfig;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * The Byakugan's far sight: while the eye is open and the last key of its own combo is held down,
 * the mouse wheel draws the world closer.
 *
 * <p>The far sight is not part of the open eye by itself. An eye that always owned the wheel stole
 * it from a player who only wanted another hotbar slot, so the zoom has to be asked for: the eye
 * watches on its own, the lens is held in the hand and let go the moment the key comes up.</p>
 *
 * <p>The lens moves the way the vanilla spyglass does - half of the remaining distance every tick,
 * interpolated by the render's partial tick - so the picture glides instead of stepping at 20 Hz.</p>
 */
@Mod.EventBusSubscriber(modid = "narutoaddon", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientFarsightHandler {

    private static final float MIN_ZOOM = 1.0f;

    /** The hardest the eye can narrow the view. */
    private static final float MAX_ZOOM = 10.0f;

    /** One wheel notch changes the zoom by this factor, so a notch feels alike at 1x and at 9x. */
    private static final float ZOOM_STEP = 1.2f;

    /** Share of the remaining distance covered per tick - the spyglass' own easing. */
    private static final float EASE = 0.5f;

    /** The lens this tick, the lens last tick, and where the wheel has pointed it. */
    private static float zoom = MIN_ZOOM;
    private static float oldZoom = MIN_ZOOM;
    private static float targetZoom = MIN_ZOOM;

    private ClientFarsightHandler() {
    }

    /** Moves the lens once a tick and keeps the previous value for the partial-tick blend. */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        oldZoom = zoom;
        // Letting go of the key drops the lens back to the naked eye; the wheel has to be held
        // again next time, which is the whole point of holding the combo.
        if (!isFocused(Minecraft.getInstance())) {
            targetZoom = MIN_ZOOM;
        }
        zoom += (targetZoom - zoom) * EASE;
        if (Math.abs(targetZoom - zoom) < 0.01f) {
            zoom = targetZoom;
        }
    }

    /** Zooms with the wheel while the eye's combo is held. */
    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (!isFocused(Minecraft.getInstance())) {
            return;
        }
        double delta = event.getScrollDelta();
        if (delta > 0.0d) {
            targetZoom = Math.min(MAX_ZOOM, targetZoom * ZOOM_STEP);
        } else if (delta < 0.0d) {
            targetZoom = Math.max(MIN_ZOOM, targetZoom / ZOOM_STEP);
        }
        // The notch belonged to the eye, not to the hotbar.
        event.setCanceled(true);
    }

    /** Narrows the field of view while the lens is held open. */
    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        if (minecraft.options.getCameraType() != CameraType.FIRST_PERSON) {
            return;
        }
        // The spyglass owns the view while it is up; two lenses at once would fight over it.
        if (minecraft.player.isScoping()) {
            return;
        }
        float lens = (float) Mth.lerp(event.getPartialTick(), oldZoom, zoom);
        if (lens <= MIN_ZOOM) {
            return;
        }
        event.setFOV(event.getFOV() / lens);
    }

    /** @return whether the far sight is being held on the local player right now. */
    private static boolean isFocused(Minecraft minecraft) {
        Player player = minecraft.player;
        if (player == null || !NarutoConfig.byakuganEnabled) {
            return false;
        }
        if (!comboHeld()) {
            return false;
        }
        INinjaData data = player.getCapability(NinjaCapabilityHandler.NINJA_DATA).resolve().orElse(null);
        if (data == null) {
            return false;
        }
        return ByakuganAbility.isActive(data);
    }

    /**
     * @return whether the last key of the Byakugan combo is down. That is the key the base handler
     *         itself reads to decide a combo is being held, so the gesture matches every other
     *         channeled technique.
     */
    private static boolean comboHeld() {
        KeyMapping key = NarutoKeyHandler.getJutsuKey((int) (ByakuganAbility.COMBO % 10L));
        return key != null && key.isDown();
    }
}
