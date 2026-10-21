package com.inventorywizard.test.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerScreen.class)
public interface ScreenPositionAccessor {

    @Accessor("leftPos")
    int inventorywizardtest$getLeftPos();

    @Accessor("topPos")
    int inventorywizardtest$getTopPos();
}
