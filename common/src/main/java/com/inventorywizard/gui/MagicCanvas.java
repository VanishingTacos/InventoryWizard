package com.inventorywizard.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else
//import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The few drawing calls the settings screen's decorations use. Between 1.21.1 and 26.x they only changed
 * names (GuiGraphics became GuiGraphicsExtractor in 26.1), so the version differences all live here.
 * Colours are ARGB with alpha, which every version accepts.
 */
public final class MagicCanvas {

    //? if >=26.1 {
    private final GuiGraphicsExtractor graphics;

    public MagicCanvas(GuiGraphicsExtractor graphics) {
        this.graphics = graphics;
    }
    //?} else {
    /*private final GuiGraphics graphics;

    public MagicCanvas(GuiGraphics graphics) {
        this.graphics = graphics;
    }
    *///?}

    public Font font() {
        return Minecraft.getInstance().font;
    }

    public void fill(int x1, int y1, int x2, int y2, int argb) {
        graphics.fill(x1, y1, x2, y2, argb);
    }

    /** A fill that covers items drawn earlier. Before 1.21.6 the GUI is depth sorted, so it's lifted above them. */
    public void fillOverItems(int x1, int y1, int x2, int y2, int argb) {
        //? if >=1.21.6 {
        graphics.fill(x1, y1, x2, y2, argb);
        //?} else {
        /*graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 200);
        graphics.fill(x1, y1, x2, y2, argb);
        graphics.pose().popPose();
        *///?}
    }

    public void text(Component text, int x, int y, int argb, boolean shadow) {
        //? if >=26.1 {
        graphics.text(font(), text, x, y, argb, shadow);
        //?} else
        //graphics.drawString(font(), text, x, y, argb, shadow);
    }

    public void centeredText(Component text, int centerX, int y, int argb) {
        text(text, centerX - font().width(text) / 2, y, argb, true);
    }

    public void item(ItemStack stack, int x, int y) {
        //? if >=26.1 {
        graphics.fakeItem(stack, x, y);
        //?} else
        //graphics.renderFakeItem(stack, x, y);
    }

    /** The stack count (and durability bar) drawn over an item. */
    public void itemDecorations(ItemStack stack, int x, int y) {
        //? if >=26.1 {
        graphics.itemDecorations(font(), stack, x, y);
        //?} else
        //graphics.renderItemDecorations(font(), stack, x, y);
    }

    /** Draws everything until {@link #pop()} moved to (x, y) and scaled up. */
    public void pushScaled(float x, float y, float scale) {
        //? if >=1.21.6 {
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);
        //?} else {
        /*graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(scale, scale, 1);
        *///?}
    }

    public void pop() {
        //? if >=1.21.6 {
        graphics.pose().popMatrix();
        //?} else
        //graphics.pose().popPose();
    }

    /**
     * Whether item stacks can be made right now. From 26.1 items get their components from the joined
     * world's data, so on the title screen there are no stacks to draw.
     */
    public static boolean itemsReady() {
        //? if >=26.1 {
        return Items.STONE.builtInRegistryHolder().areComponentsBound();
        //?} else
        //return true;
    }

    /**
     * An ARGB colour at a fraction of full opacity. Text drawn with an alpha below 4 counts as opaque
     * before 1.21.6, so this never goes that low.
     */
    public static int withAlpha(int rgb, float alpha) {
        int a = Math.max(5, Math.min(255, Math.round(alpha * 255)));
        return a << 24 | rgb & 0xFFFFFF;
    }

    /** Blends two RGB colours; t = 0 gives a, t = 1 gives b. */
    public static int mix(int a, int b, float t) {
        int r = Math.round((a >> 16 & 0xFF) + ((b >> 16 & 0xFF) - (a >> 16 & 0xFF)) * t);
        int g = Math.round((a >> 8 & 0xFF) + ((b >> 8 & 0xFF) - (a >> 8 & 0xFF)) * t);
        int bl = Math.round((a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * t);
        return r << 16 | g << 8 | bl;
    }
}
