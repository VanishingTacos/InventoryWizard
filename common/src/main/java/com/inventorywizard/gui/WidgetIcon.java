package com.inventorywizard.gui;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * An item drawn at the left edge of a button. Every widget gets this through AbstractWidgetMixin, which draws
 * the icon as part of the widget, so it scrolls and clips with the options list like the button does.
 */
public interface WidgetIcon {

    void inventorywizard$setIcon(ItemStack icon);

    /** Gives the widget an icon, or leaves it plain while there are no items to draw (see MagicCanvas.itemsReady). */
    static void set(AbstractWidget widget, Item icon) {
        if (widget != null && MagicCanvas.itemsReady()) {
            ((WidgetIcon) widget).inventorywizard$setIcon(new ItemStack(icon));
        }
    }

    static void draw(AbstractWidget widget, ItemStack icon, MagicCanvas canvas) {
        int x = widget.getX() + 3;
        // Hop up a pixel under the mouse
        int y = widget.getY() + (widget.getHeight() - 16) / 2 - (widget.active && widget.isHovered() ? 1 : 0);
        canvas.item(icon, x, y);
        if (!widget.active) {
            // Fade into the greyed-out button
            canvas.fillOverItems(x, y, x + 16, y + 16, 0xA0303030);
        }
    }
}
