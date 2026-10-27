package com.inventorywizard.gui;

import net.minecraft.network.chat.Component;

/** "✦ Title ✦" in gold, with lines on both sides that fade out toward the edges of the row. */
public class SectionHeader extends MagicWidget {

    private static final int GOLD = 0xFFD86B;
    private static final int STAR = 0xB98CFF;
    private static final int LINE_GAP = 6;
    private static final int LINE_STEP = 2;

    private final Component label;

    public SectionHeader(int width, Component title) {
        super(width, 20, title);
        label = Component.literal("✦ ").withColor(STAR)
                .append(title.copy().withColor(GOLD))
                .append(Component.literal(" ✦").withColor(STAR));
    }

    @Override
    protected void draw(MagicCanvas canvas) {
        int centerX = getX() + getWidth() / 2;
        int textWidth = canvas.font().width(label);
        int textY = getY() + (getHeight() - 8) / 2;
        canvas.centeredText(label, centerX, textY, 0xFFFFFFFF);

        int lineY = textY + 3;
        int leftEnd = centerX - textWidth / 2 - LINE_GAP;
        int rightStart = centerX + textWidth / 2 + LINE_GAP;
        int length = leftEnd - getX();
        for (int offset = 0; offset < length; offset += LINE_STEP) {
            // Brightest next to the text
            float alpha = 0.7f * (1 - offset / (float) length);
            int color = MagicCanvas.withAlpha(GOLD, alpha);
            canvas.fill(leftEnd - offset - LINE_STEP, lineY, leftEnd - offset, lineY + 1, color);
            canvas.fill(rightStart + offset, lineY, rightStart + offset + LINE_STEP, lineY + 1, color);
        }
    }
}
