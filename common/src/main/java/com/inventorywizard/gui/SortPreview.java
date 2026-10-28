package com.inventorywizard.gui;

import com.inventorywizard.InventorySorter;
import com.inventorywizard.InventoryWizardConfig;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * A row of inventory slots showing sample items sorted with the current settings, through the real sorter.
 * When the settings change, the items slide over to their new slots.
 */
public class SortPreview extends MagicWidget {

    private static final int SLOTS = 9;
    private static final int SLOT_SIZE = 18;
    private static final long SLIDE_TIME = 350;
    private static final int HOP_HEIGHT = 5;

    private static final int SLOT_BACKGROUND = 0xFF8B8B8B;
    private static final int SLOT_SHADOW = 0xFF373737;
    private static final int SLOT_HIGHLIGHT = 0xFFFFFFFF;
    private static final int HINT_COLOR = 0xFFA898C8;

    /** Empty while there are no items to draw, see MagicCanvas.itemsReady. */
    private final List<ItemStack> sample;
    private List<ItemStack> shown = List.of();
    /** For each shown stack, the slot it slides in from, or -1 to drop in from above. */
    private int[] from = new int[0];
    private long slideStart;

    public SortPreview(int width) {
        super(width, 20, Component.translatable("inventorywizard.config.preview"));
        setTooltip(Tooltip.create(Component.translatable("inventorywizard.config.preview.tooltip")));
        // Item tags aren't loaded on the title screen, so these are items the sorter places by name or by
        // their own components, and the three modes order them differently
        sample = !MagicCanvas.itemsReady() ? List.of() : List.of(
                new ItemStack(Items.BREAD, 5),
                new ItemStack(Items.COBBLESTONE, 40),
                new ItemStack(Items.BOW),
                new ItemStack(Items.GRAVEL, 12),
                new ItemStack(Items.APPLE, 3),
                new ItemStack(Items.COBBLESTONE, 30),
                new ItemStack(Items.COMPASS),
                new ItemStack(Items.GRAVEL, 20),
                new ItemStack(Items.BREAD, 7));
    }

    /** Sorts the sample again; when the result changed, the items slide to their new slots. */
    public void refresh(InventoryWizardConfig config) {
        if (sample.isEmpty()) {
            return;
        }
        List<ItemStack> sorted = InventorySorter.sort(sample, config.sortMode, InventorySorter.Region.INVENTORY, config);
        if (sameStacks(sorted, shown)) {
            return;
        }
        boolean first = shown.isEmpty();
        int[] newFrom = new int[sorted.size()];
        boolean[] taken = new boolean[shown.size()];
        for (int i = 0; i < sorted.size(); i++) {
            newFrom[i] = -1;
            for (int j = 0; j < shown.size(); j++) {
                if (!taken[j] && ItemStack.isSameItem(shown.get(j), sorted.get(i))) {
                    taken[j] = true;
                    newFrom[i] = j;
                    break;
                }
            }
        }
        shown = sorted;
        from = newFrom;
        // The first sort shows up already in place
        slideStart = first ? 0 : now();
    }

    private static boolean sameStacks(List<ItemStack> a, List<ItemStack> b) {
        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            if (!ItemStack.isSameItem(a.get(i), b.get(i)) || a.get(i).getCount() != b.get(i).getCount()) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void draw(MagicCanvas canvas) {
        if (sample.isEmpty()) {
            canvas.centeredText(Component.translatable("inventorywizard.config.preview.unavailable"),
                    getX() + getWidth() / 2, getY() + (getHeight() - 8) / 2, HINT_COLOR);
            return;
        }
        int left = getX() + (getWidth() - SLOTS * SLOT_SIZE) / 2;
        int top = getY() + 1;
        for (int i = 0; i < SLOTS; i++) {
            drawSlot(canvas, left + i * SLOT_SIZE, top);
        }

        float t = Math.min(1f, (now() - slideStart) / (float) SLIDE_TIME);
        float eased = 1 - (1 - t) * (1 - t) * (1 - t);
        for (int i = 0; i < shown.size() && i < SLOTS; i++) {
            ItemStack stack = shown.get(i);
            int targetX = left + i * SLOT_SIZE + 1;
            int x;
            int y;
            if (from[i] >= 0) {
                int startX = left + from[i] * SLOT_SIZE + 1;
                x = Math.round(startX + (targetX - startX) * eased);
                // Moving items hop over the others
                y = top + 1 - (startX == targetX ? 0 : Math.round((float) Math.sin(Math.PI * eased) * HOP_HEIGHT));
            } else {
                x = targetX;
                y = top + 1 - Math.round((1 - eased) * 8);
            }
            canvas.item(stack, x, y);
            canvas.itemDecorations(stack, x, y);
        }
    }

    /** The bevelled slot from the inventory screen: dark top and left edges, light bottom and right. */
    private static void drawSlot(MagicCanvas canvas, int x, int y) {
        canvas.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, SLOT_BACKGROUND);
        canvas.fill(x, y, x + SLOT_SIZE - 1, y + 1, SLOT_SHADOW);
        canvas.fill(x, y, x + 1, y + SLOT_SIZE - 1, SLOT_SHADOW);
        canvas.fill(x + 1, y + SLOT_SIZE - 1, x + SLOT_SIZE, y + SLOT_SIZE, SLOT_HIGHLIGHT);
        canvas.fill(x + SLOT_SIZE - 1, y + 1, x + SLOT_SIZE, y + SLOT_SIZE, SLOT_HIGHLIGHT);
    }
}
