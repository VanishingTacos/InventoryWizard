package com.inventorywizard;

import com.inventorywizard.mixin.AbstractContainerScreenAccessor;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
//? if >=26.1 {
import net.minecraft.world.inventory.ContainerInput;
//?} else
//import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Scrolling while carrying a stack moves single items between the cursor and the slot it was picked up
 * from: up takes one more, down puts one back. Built from vanilla PICKUP clicks, so it also works on
 * servers without the mod.
 */
public final class CursorScroll {

    // Container click buttons, which are not the mouse button numbers (those changed in 26.3)
    private static final int PRIMARY = 0;
    private static final int SECONDARY = 1;

    private static Slot origin;
    private static double pendingScroll;

    private CursorScroll() {}

    /** Called before vanilla handles a press, to remember which slot a stack is picked up from. */
    public static void onMouseClick(AbstractContainerScreen<?> screen, Slot slot, int button, boolean shift) {
        boolean pickup = button == InputConstants.MOUSE_BUTTON_LEFT || button == InputConstants.MOUSE_BUTTON_RIGHT;
        if (pickup && !shift && slot != null && slot.hasItem() && screen.getMenu().getCarried().isEmpty()) {
            origin = slot;
            pendingScroll = 0;
        }
    }

    public static void reset() {
        origin = null;
        pendingScroll = 0;
    }

    /** Returns true when the scroll moved items (or would have) and vanilla scrolling should be cancelled. */
    public static boolean onMouseScroll(Minecraft client, AbstractContainerScreen<?> screen, double amount) {
        AbstractContainerMenu menu = screen.getMenu();
        if (!canAdjust(screen, menu)) {
            pendingScroll = 0;
            return false;
        }
        // Trackpads scroll in fractions of a step
        pendingScroll += amount;
        int steps = (int) pendingScroll;
        pendingScroll -= steps;
        for (int i = 0; i < Math.abs(steps) && canAdjust(screen, menu); i++) {
            if (steps > 0) {
                takeOne(client, menu);
            } else {
                putOneBack(client, menu);
            }
        }
        if (menu.getCarried().isEmpty()) {
            reset();
        }
        return true;
    }

    private static boolean canAdjust(AbstractContainerScreen<?> screen, AbstractContainerMenu menu) {
        ItemStack carried = menu.getCarried();
        if (carried.isEmpty() || origin == null || !menu.slots.contains(origin)
                || ((AbstractContainerScreenAccessor) screen).inventorywizard$isQuickCrafting()) {
            return false;
        }
        // Result slots (crafting, furnace, ...) take nothing back, so there is nothing to adjust against
        ItemStack inSlot = origin.getItem();
        return origin.mayPlace(carried) && (inSlot.isEmpty() || ItemStack.isSameItemSameComponents(inSlot, carried));
    }

    private static void putOneBack(Minecraft client, AbstractContainerMenu menu) {
        if (origin.getItem().getCount() >= origin.getMaxStackSize(menu.getCarried())) {
            return;
        }
        // Right-click with a stack places exactly one
        click(client, menu, SECONDARY);
    }

    /**
     * Vanilla has no "take one" click: drop everything into the slot, pick up all or half of it,
     * then right-click the extra items back one at a time.
     */
    private static void takeOne(Minecraft client, AbstractContainerMenu menu) {
        int carried = menu.getCarried().getCount();
        int inSlot = origin.getItem().getCount();
        int total = carried + inSlot;
        if (inSlot == 0 || total > origin.getMaxStackSize(menu.getCarried())) {
            return;
        }
        int wanted = carried + 1;
        int half = (total + 1) / 2;

        click(client, menu, PRIMARY);
        int pickedUp = wanted <= half ? half : total;
        click(client, menu, wanted <= half ? SECONDARY : PRIMARY);
        for (int i = wanted; i < pickedUp; i++) {
            click(client, menu, SECONDARY);
        }
    }

    private static void click(Minecraft client, AbstractContainerMenu menu, int button) {
        //? if >=26.1 {
        client.gameMode.handleContainerInput(menu.containerId, origin.index, button, ContainerInput.PICKUP, client.player);
        //?} else
        //client.gameMode.handleInventoryMouseClick(menu.containerId, origin.index, button, ClickType.PICKUP, client.player);
    }
}
