package com.inventorywizard.mixin;

import com.inventorywizard.InventoryWizard;
import com.inventorywizard.SortActions;
import net.minecraft.client.Minecraft;
//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else
//import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
//? if >=26.4 {
/*import net.minecraft.client.gui.GuiPipeline;
*///?} else if >=1.21.6 {
import net.minecraft.client.renderer.RenderPipelines;
//?} else if >=1.21.2 {
/*import net.minecraft.client.renderer.RenderType;
*///?}
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws a lock over locked slots, right after the slot's item so it sits on top of it. */
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {

    @Unique
    private static final Identifier inventorywizard$LOCK_SPRITE = Identifier.fromNamespaceAndPath(InventoryWizard.MOD_ID, "lock");
    @Unique
    private static final int inventorywizard$LOCK_SIZE = 8;

    //? if >=26.4 {
    /*@Inject(method = "extractSlot", at = @At("RETURN"))
    private void inventorywizard$drawLock(GuiGraphicsExtractor graphics, Slot slot, CallbackInfo ci) {
    *///?} else if >=26.1 {
    @Inject(method = "extractSlot", at = @At("RETURN"))
    private void inventorywizard$drawLock(GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
    //?} else if >=1.21.11 {
    /*@Inject(method = "renderSlot", at = @At("RETURN"))
    private void inventorywizard$drawLock(GuiGraphics graphics, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
    *///?} else {
    /*@Inject(method = "renderSlot", at = @At("RETURN"))
    private void inventorywizard$drawLock(GuiGraphics graphics, Slot slot, CallbackInfo ci) {
    *///?}
        // Locks only take effect where the client mod sorts, like the sort buttons
        if (!SortActions.isLocked(slot) || Minecraft.getInstance().getSingleplayerServer() == null
                || !InventoryWizard.handlesScreen((Screen) (Object) this)) {
            return;
        }
        // Slot coordinates are relative to the screen, which vanilla has already translated to
        int x = slot.x - 1;
        int y = slot.y - 1;
        //? if >=26.4 {
        /*graphics.blitSprite(GuiPipeline.TEXTURED, inventorywizard$LOCK_SPRITE, x, y, inventorywizard$LOCK_SIZE, inventorywizard$LOCK_SIZE);
        *///?} else if >=1.21.6 {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, inventorywizard$LOCK_SPRITE, x, y, inventorywizard$LOCK_SIZE, inventorywizard$LOCK_SIZE);
        //?} else if >=1.21.2 {
        /*// Before 1.21.6 the GUI is depth sorted: lift the lock above the item and its count, below the carried item
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 350);
        graphics.blitSprite(RenderType::guiTextured, inventorywizard$LOCK_SPRITE, x, y, inventorywizard$LOCK_SIZE, inventorywizard$LOCK_SIZE);
        graphics.pose().popPose();
        *///?} else {
        /*graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 350);
        graphics.blitSprite(inventorywizard$LOCK_SPRITE, x, y, inventorywizard$LOCK_SIZE, inventorywizard$LOCK_SIZE);
        graphics.pose().popPose();
        *///?}
    }
}
