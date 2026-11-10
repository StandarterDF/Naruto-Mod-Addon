package com.sekwah.narutoaddon.client.keybinds;

import net.minecraft.client.KeyMapping;

import java.lang.reflect.Field;

public class KeyBindingTickHeld extends KeyMapping {

    private static final Field CLICK_COUNT_FIELD;
    static {
        try {
            CLICK_COUNT_FIELD = KeyMapping.class.getDeclaredField("clickCount");
            CLICK_COUNT_FIELD.setAccessible(true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private int getClickCount() {
        try { return CLICK_COUNT_FIELD.getInt(this); } catch (Exception e) { return 0; }
    }
    private void setClickCount(int v) {
        try { CLICK_COUNT_FIELD.setInt(this, v); } catch (Exception e) {}
    }

    boolean hasConsumedClickState = false;

    int heldTicks = 0;
    private Runnable onClick = null;

    public KeyBindingTickHeld(String name, int keyCode, String category) {
        super(name, keyCode, category);
    }

    public void registerClickConsumer(Runnable runnable) {
        this.onClick = runnable;
    }

    public enum KeyState {
        CLICK,
        HELD,
        NOT_PRESSED
    }

    public void update() {
        if (this.isDown()) {
            heldTicks++;
        }
        if (this.onClick != null) {
            KeyState clickState = this.consumeClickState();
            if(clickState == KeyState.CLICK) {
                onClick.run();
            }
        }
    }

    public KeyState consumeClickState() {
        if (this.isDown()) {
            if(hasConsumedClickState) {
                return KeyState.HELD;
            }
            hasConsumedClickState = true;
            return KeyState.CLICK;
        } else {
            if(getClickCount() == 0) {
                return KeyState.NOT_PRESSED;
            } else {
                setClickCount(getClickCount() - 1);
                return KeyState.CLICK;
            }
        }
    }

    public int consumeReleaseDuration() {
        if (this.isDown()) {
            return 0;
        }

        int returnValue = heldTicks;
        heldTicks = 0;
        return returnValue;
    }

    public int currentHeldValue() {
        return heldTicks;
    }

    public boolean isCurrentlyHeld() {
        return heldTicks > 20 && this.isDown();
    }

    @Override
    public void setDown(boolean down) {
        if(this.isDown() && !down) {
            if(hasConsumedClickState && getClickCount() > 0) {
                setClickCount(0);
            }
            hasConsumedClickState = false;
        }
        else if(!this.isDown() && down) {
            this.heldTicks = 0;
        }

        super.setDown(down);
    }
}
