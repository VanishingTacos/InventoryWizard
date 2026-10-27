package com.inventorywizard.gui;

import com.inventorywizard.InventoryWizard;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The settings screen's header: "InventoryWizard" in the logo's colours with a shimmer sweeping across it,
 * twinkling sparkles on both sides, and a tip underneath that changes every few seconds.
 */
public class TitleBanner extends MagicWidget {

    private static final String FIRST = "Inventory";
    private static final String SECOND = "Wizard";
    // From icon.png
    private static final int MINT = 0x7FE8A0;
    private static final int CORAL = 0xF2704E;
    private static final int TIP_COLOR = 0xC8B8F0;
    private static final int[] SPARKLE_COLORS = {0xFFD54F, 0x6FE3F0, 0xE48CFF, 0x8CF07A};

    private static final float TITLE_SCALE = 2f;
    private static final int TITLE_Y = 5;
    private static final int TIP_Y = 29;

    private static final long SHIMMER_PERIOD = 4000;
    private static final long SHIMMER_SWEEP = 1200;
    private static final long TIP_PERIOD = 6000;
    private static final long TIP_FADE = 400;

    /** Sparkle positions are fixed; only their brightness changes. */
    private record Sparkle(float side, float dx, float dy, int arm, int color, float phase, long period) {
    }

    private final List<Sparkle> sparkles = new ArrayList<>();
    private final List<Component> tips = List.of(
            Component.translatable("inventorywizard.config.tip.sort_key", InventoryWizard.SORT_KEY.getTranslatedKeyMessage()),
            Component.translatable("inventorywizard.config.tip.lock"),
            Component.translatable("inventorywizard.config.tip.shift_click"),
            Component.translatable("inventorywizard.config.tip.scroll"),
            Component.translatable("inventorywizard.config.tip.command"));

    public TitleBanner(int width, int height, Component title) {
        super(width, height, title);
        Random random = new Random(7);
        for (int i = 0; i < 14; i++) {
            sparkles.add(new Sparkle(
                    i % 2 == 0 ? -1 : 1,
                    6 + random.nextFloat() * 44,
                    random.nextFloat() * 26,
                    random.nextInt(3) == 0 ? 2 : 1,
                    SPARKLE_COLORS[random.nextInt(SPARKLE_COLORS.length)],
                    random.nextFloat(),
                    1400 + random.nextInt(1600)));
        }
    }

    @Override
    protected void draw(MagicCanvas canvas) {
        long now = now();
        Component title = shimmeringTitle(now);
        float titleWidth = canvas.font().width(title) * TITLE_SCALE;
        int centerX = getX() + getWidth() / 2;
        float left = centerX - titleWidth / 2;

        for (Sparkle sparkle : sparkles) {
            drawSparkle(canvas, sparkle, centerX, titleWidth, now);
        }

        canvas.pushScaled(left, getY() + TITLE_Y, TITLE_SCALE);
        canvas.text(title, 0, 0, 0xFFFFFFFF, true);
        canvas.pop();

        long tipTime = now % TIP_PERIOD;
        float tipAlpha = Math.min(1f, Math.min(tipTime, TIP_PERIOD - tipTime) / (float) TIP_FADE);
        Component tip = tips.get((int) (now / TIP_PERIOD % tips.size()));
        canvas.centeredText(tip, centerX, getY() + TIP_Y, MagicCanvas.withAlpha(TIP_COLOR, tipAlpha));
    }

    /** Each letter in its half's colour, brightened while the shimmer passes over it. */
    private static Component shimmeringTitle(long now) {
        long sweepTime = now % SHIMMER_PERIOD;
        int length = FIRST.length() + SECOND.length();
        // Starts and ends a little off the word so the highlight fades in and out
        float highlight = sweepTime < SHIMMER_SWEEP ? -3 + (length + 6) * sweepTime / (float) SHIMMER_SWEEP : -100;

        MutableComponent title = Component.empty();
        for (int i = 0; i < length; i++) {
            boolean first = i < FIRST.length();
            char c = first ? FIRST.charAt(i) : SECOND.charAt(i - FIRST.length());
            float glow = Math.max(0, 1 - Math.abs(i - highlight) / 2.5f);
            int color = MagicCanvas.mix(first ? MINT : CORAL, 0xFFFFFF, glow * 0.8f);
            title.append(Component.literal(String.valueOf(c)).withStyle(style -> style.withColor(color).withBold(true)));
        }
        return title;
    }

    /** A four-point star that brightens and fades in a loop, on either side of the title. */
    private void drawSparkle(MagicCanvas canvas, Sparkle sparkle, int centerX, float titleWidth, long now) {
        double wave = Math.sin(2 * Math.PI * ((now % sparkle.period()) / (double) sparkle.period() + sparkle.phase()));
        if (wave <= 0) {
            return;
        }
        float alpha = (float) (wave * wave);
        int x = Math.round(centerX + sparkle.side() * (titleWidth / 2 + sparkle.dx()));
        int y = Math.round(getY() + 2 + sparkle.dy());
        int arm = sparkle.arm();

        canvas.fill(x, y, x + 1, y + 1, MagicCanvas.withAlpha(0xFFFFFF, alpha));
        int armColor = MagicCanvas.withAlpha(sparkle.color(), alpha * 0.85f);
        canvas.fill(x - arm, y, x, y + 1, armColor);
        canvas.fill(x + 1, y, x + 1 + arm, y + 1, armColor);
        canvas.fill(x, y - arm, x + 1, y, armColor);
        canvas.fill(x, y + 1, x + 1, y + 1 + arm, armColor);
    }
}
