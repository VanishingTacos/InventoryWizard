package com.inventorywizard.gui;

//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else
//import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/** A decoration on the settings screen: drawn with a {@link MagicCanvas}, never focused or clicked. */
public abstract class MagicWidget extends AbstractWidget {

    protected MagicWidget(int width, int height, Component message) {
        super(0, 0, width, height, message);
        active = false;
    }

    protected abstract void draw(MagicCanvas canvas);

    //? if >=26.4 {
    /*@Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics) {
        draw(new MagicCanvas(graphics));
    }
    *///?} else if >=26.1 {
    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        draw(new MagicCanvas(graphics));
    }
    //?} else {
    /*@Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        draw(new MagicCanvas(graphics));
    }
    *///?}

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, getMessage());
    }

    /** Wall-clock time drives the animations, so they run at the same speed at any frame rate. */
    protected static long now() {
        return System.currentTimeMillis();
    }
}
