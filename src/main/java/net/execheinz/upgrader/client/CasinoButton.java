package net.execheinz.upgrader.client;

import net.execheinz.upgrader.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;

final class CasinoButton extends Button {
    enum Style { PRIMARY, CHIP }

    private final Style style;

    CasinoButton(int x, int y, int width, int height, Component message, Style style, OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        this.style = style;
    }

    @Override
    public void onPress() {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(
            style == Style.PRIMARY ? ModSounds.BET_CONFIRM.get() : ModSounds.UI_CLICK.get(), 1.0F,
            style == Style.PRIMARY ? 0.40F : 0.34F));
        super.onPress();
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean hot = active && isHoveredOrFocused();
        if (style == Style.PRIMARY) {
            if (active) {
                CasinoRenderer.glow(graphics, getX(), getY(), width, height, 6, hot ? 0x90FFD44F : 0x48FFD44F);
                // Deep lower edge plus a pale top bevel gives the button the
                // bright, weighty Upgrader-casino appearance at every GUI scale.
                CasinoRenderer.roundedRect(graphics, getX(), getY() + 2, width, height, 6, 0xFF9E6800);
                CasinoRenderer.card(graphics, getX(), getY(), width, height - 2, 6,
                    hot ? 0xFFFFFFC1 : 0xFFFFE37A,
                    hot ? 0xFFFFF19A : 0xFFFFDE68,
                    hot ? 0xFFFFB817 : 0xFFF0A808);
                graphics.fill(getX() + 7, getY() + 2, getX() + width - 7, getY() + 3, hot ? 0xBFFFFFFF : 0x80FFFFFF);
                graphics.fill(getX() + 6, getY() + height - 4, getX() + width - 6, getY() + height - 3, 0x50906000);
                int textY = getY() + (height - 10) / 2;
                graphics.drawCenteredString(Minecraft.getInstance().font, getMessage(), getX() + width / 2 + 1, textY + 1, 0x703B2500);
                graphics.drawCenteredString(Minecraft.getInstance().font, getMessage(), getX() + width / 2, textY, 0xFF17130A);
            } else {
                CasinoRenderer.card(graphics, getX(), getY(), width, height, 6, 0xFF454D5D, 0xFF343B48, 0xFF202631);
                graphics.fill(getX() + 7, getY() + 2, getX() + width - 7, getY() + 3, 0x304F5A6B);
                graphics.drawCenteredString(Minecraft.getInstance().font, getMessage(), getX() + width / 2, getY() + (height - 8) / 2, 0xFF858E9E);
            }
        } else {
            int border = active ? (hot ? 0xFFFFD34E : 0xFF49566C) : 0xFF303743;
            int top = active ? (hot ? 0xFF363F51 : 0xFF252D3B) : 0xFF1B2029;
            int bottom = active ? 0xFF171D27 : 0xFF161A21;
            CasinoRenderer.card(graphics, getX(), getY(), width, height, 4, border, top, bottom);
            graphics.drawCenteredString(Minecraft.getInstance().font, getMessage(), getX() + width / 2, getY() + (height - 8) / 2, active ? (hot ? 0xFFFFD34E : 0xFFDDE4F0) : 0xFF697383);
        }
    }
}
