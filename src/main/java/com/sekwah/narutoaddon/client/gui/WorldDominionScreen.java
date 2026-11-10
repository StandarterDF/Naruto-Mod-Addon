package com.sekwah.narutoaddon.client.gui;

import com.sekwah.narutoaddon.network.PacketHandler;
import com.sekwah.narutoaddon.network.c2s.ServerWorldDominionPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * Client screen opened by the Dominion of the World technique: a slider for the time of day and
 * buttons for the three weather states, each sent to the server on interaction.
 */
public class WorldDominionScreen extends Screen {

    private TimeSlider timeSlider;

    public WorldDominionScreen() {
        super(Component.translatable("naruto.gui.senrigan.world"));
    }

    @Override
    protected void init() {
        int buttonWidth = 120;
        int buttonHeight = 20;
        int gap = 10;
        int rowWidth = 4 * buttonWidth + 3 * gap;
        int startX = (this.width - rowWidth) / 2;
        int firstY = this.height / 2 - 30;
        int secondY = firstY + buttonHeight + 8;

        int initialTime = 6000;
        if (Minecraft.getInstance().level != null) {
            initialTime = (int) (Minecraft.getInstance().level.getDayTime() % 24000L);
        }

        this.timeSlider = new TimeSlider(this, startX, firstY, rowWidth, buttonHeight, initialTime);
        this.addRenderableWidget(this.timeSlider);

        int weatherRowWidth = 3 * buttonWidth + 2 * gap;
        int weatherStartX = (this.width - weatherRowWidth) / 2;
        this.addRenderableWidget(Button.builder(Component.translatable("naruto.gui.senrigan.world.clear"),
                        b -> this.send((byte) 4, 0))
                .bounds(weatherStartX, secondY, buttonWidth, buttonHeight).build());
        this.addRenderableWidget(Button.builder(Component.translatable("naruto.gui.senrigan.world.rain"),
                        b -> this.send((byte) 5, 0))
                .bounds(weatherStartX + buttonWidth + gap, secondY, buttonWidth, buttonHeight).build());
        this.addRenderableWidget(Button.builder(Component.translatable("naruto.gui.senrigan.world.thunder"),
                        b -> this.send((byte) 6, 0))
                .bounds(weatherStartX + 2 * (buttonWidth + gap), secondY, buttonWidth, buttonHeight).build());

        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"),
                        b -> this.onClose())
                .bounds((this.width - 120) / 2, secondY + buttonHeight + 12, 120, buttonHeight).build());
    }

    private void send(byte action, int value) {
        PacketHandler.sendToServer(new ServerWorldDominionPacket(action, value));
        this.onClose();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(null);
        }
    }

    private static int quantize(double value) {
        int ticks = (int) Math.round(value * 23999.0);
        ticks = (ticks / 1000) * 1000;
        return Mth.clamp(ticks, 0, 23000);
    }

    private static class TimeSlider extends AbstractSliderButton {
        private final WorldDominionScreen screen;
        private int timeTicks;

        TimeSlider(WorldDominionScreen screen, int x, int y, int width, int height, int initialTicks) {
            super(x, y, width, height, Component.empty(), initialTicks / 23999.0);
            this.screen = screen;
            this.timeTicks = quantize(initialTicks / 23999.0);
            this.value = this.timeTicks / 23999.0;
            this.updateMessage();
        }

        @Override
        protected void updateMessage() {
            int tick = this.timeTicks;
            int hour = ((tick / 1000) + 6) % 24;
            int minute = ((tick % 1000) * 60) / 1000;
            String timeStr = String.format(java.util.Locale.ROOT, "%02d:%02d", hour, minute);
            String phaseKey;
            if (tick >= 23000 || tick < 1000) {
                phaseKey = "naruto.gui.senrigan.world.phase.dawn";
            } else if (tick < 12000) {
                phaseKey = "naruto.gui.senrigan.world.phase.day";
            } else if (tick < 13000) {
                phaseKey = "naruto.gui.senrigan.world.phase.dusk";
            } else {
                phaseKey = "naruto.gui.senrigan.world.phase.night";
            }
            this.setMessage(Component.translatable("naruto.gui.senrigan.world.time")
                    .append(": ")
                    .append(Component.translatable("naruto.gui.senrigan.world.timevalue", timeStr,
                            Component.translatable(phaseKey))));
        }

        @Override
        protected void applyValue() {
            this.timeTicks = quantize(this.value);
            this.value = this.timeTicks / 23999.0;
        }

        @Override
        public void onRelease(double mouseX, double mouseY) {
            super.onRelease(mouseX, mouseY);
            this.screen.send((byte) 0, this.timeTicks);
        }
    }
}
