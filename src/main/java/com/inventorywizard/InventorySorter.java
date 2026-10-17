package com.inventorywizard;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Pure sorting logic: takes the items currently in a region and returns them in
 * their new order. Never drops items - the result always holds the same total count.
 */
public final class InventorySorter {

    private InventorySorter() {
    }

    public enum Region {
        CONTAINER,
        INVENTORY,
        HOTBAR
    }

    public static List<ItemStack> sort(List<ItemStack> items, SortMode mode, Region region, InventoryWizardConfig config) {
        List<List<ItemStack>> groups = groupItems(items);

        return switch (mode) {
            case ALPHABETICAL -> {
                groups.sort(Comparator.comparing(group -> ItemCategories.path(group.getFirst())));
                yield stack(groups, allowPartialStacks(region, config));
            }
            case STACK_BASED -> sortByStacks(groups);
            case DEFAULT -> {
                Comparator<List<ItemStack>> byCategory = region == Region.HOTBAR
                        ? Comparator.comparingInt(group -> ItemCategories.hotbarPriority(group.getFirst(), config.hotbarPriorities))
                        : Comparator.comparingInt(group -> ItemCategories.inventoryPriority(group.getFirst()));
                groups.sort(byCategory.thenComparing(group -> ItemCategories.path(group.getFirst())));
                yield stack(groups, allowPartialStacks(region, config));
            }
        };
    }

    private static boolean allowPartialStacks(Region region, InventoryWizardConfig config) {
        return switch (region) {
            case CONTAINER -> config.allowPartialStacksChest;
            case INVENTORY -> config.allowPartialStacksInventory;
            case HOTBAR -> true;
        };
    }

    /** Groups stacks that can merge: same item and same components (enchantments, names, ...). */
    private static List<List<ItemStack>> groupItems(List<ItemStack> items) {
        List<List<ItemStack>> groups = new ArrayList<>();
        outer:
        for (ItemStack item : items) {
            if (item.isEmpty()) continue;
            for (List<ItemStack> group : groups) {
                if (ItemStack.isSameItemSameComponents(group.getFirst(), item)) {
                    group.add(item);
                    continue outer;
                }
            }
            List<ItemStack> group = new ArrayList<>();
            group.add(item);
            groups.add(group);
        }
        return groups;
    }

    /**
     * Merges each group into full stacks. Leftover partial stacks follow their group,
     * or, when partial stacks aren't allowed, go after every full stack.
     */
    private static List<ItemStack> stack(List<List<ItemStack>> groups, boolean allowPartialStacks) {
        List<ItemStack> result = new ArrayList<>();
        List<ItemStack> leftovers = new ArrayList<>();

        for (List<ItemStack> group : groups) {
            ItemStack template = group.getFirst();
            int total = group.stream().mapToInt(ItemStack::getCount).sum();
            int maxStackSize = template.getMaxStackSize();

            while (total >= maxStackSize) {
                result.add(template.copyWithCount(maxStackSize));
                total -= maxStackSize;
            }
            if (total > 0) {
                (allowPartialStacks ? result : leftovers).add(template.copyWithCount(total));
            }
        }

        result.addAll(leftovers);
        return result;
    }

    /** Groups by item name, keeps individual stack sizes, largest stacks first. */
    private static List<ItemStack> sortByStacks(List<List<ItemStack>> groups) {
        groups.sort(Comparator.comparing(group -> ItemCategories.path(group.getFirst())));
        List<ItemStack> result = new ArrayList<>();
        for (List<ItemStack> group : groups) {
            group.stream()
                    .sorted(Comparator.comparingInt(ItemStack::getCount).reversed())
                    .map(ItemStack::copy)
                    .forEach(result::add);
        }
        return result;
    }
}
