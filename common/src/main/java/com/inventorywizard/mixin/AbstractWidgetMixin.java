package com.inventorywizard.mixin;

import com.inventorywizard.gui.MagicCanvas;
import com.inventorywizard.gui.WidgetIcon;
//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else
//import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws a {@link WidgetIcon} right after the widget itself; only the settings screen's buttons have one. */
@Mixin(AbstractWidget.class)
public abstract class AbstractWidgetMixin implements WidgetIcon {

    @Unique
    private ItemStack inventorywizard$icon;

    @Override
    public void inventorywizard$setIcon(ItemStack icon) {
        inventorywizard$icon = icon;
    }

    //? if >=26.4 {
    /*@Inject(method = "extractRenderState", at = @At("RETURN"))
    private void inventorywizard$drawIcon(GuiGraphicsExtractor graphics, CallbackInfo ci) {
    *///?} else if >=26.1 {
    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void inventorywizard$drawIcon(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
    //?} else {
    /*@Inject(method = "render", at = @At("RETURN"))
    private void inventorywizard$drawIcon(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
    *///?}
        AbstractWidget widget = (AbstractWidget) (Object) this;
        // visible became protected in 26.4
        //? if >=26.4 {
        /*boolean visible = widget.isVisible();
        *///?} else
        boolean visible = widget.visible;
        if (inventorywizard$icon != null && visible) {
            WidgetIcon.draw(widget, inventorywizard$icon, new MagicCanvas(graphics));
        }
    }
}
