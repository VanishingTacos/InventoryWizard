package com.inventorywizard;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.*;

public class InventorySorter {

    /** Hotbar category priorities, lowest first (hotbar-priorities in config.yml). */
    public record HotbarPriorities(int weapons, int tools, int food, int buildingBlocks, int utilityItems, int other) {
        public static final HotbarPriorities DEFAULT = new HotbarPriorities(100, 200, 300, 400, 500, 999);
    }

    public static void sortInventory(Inventory inventory) {
        sortInventory(inventory, PlayerDataManager.SortMode.DEFAULT, true);
    }

    public static void sortInventory(Inventory inventory, PlayerDataManager.SortMode mode) {
        sortInventory(inventory, mode, true);
    }

    public static void sortInventory(Inventory inventory, boolean allowPartialStacks) {
        sortInventory(inventory, PlayerDataManager.SortMode.DEFAULT, allowPartialStacks);
    }

    public static void sortInventory(Inventory inventory, PlayerDataManager.SortMode mode, boolean allowPartialStacks) {
        sortRange(inventory, 0, inventory.getSize(), Set.of(), mode, allowPartialStacks, null);
    }

    public static void sortPlayerInventory(Player player) {
        sortPlayerInventory(player, PlayerDataManager.SortMode.DEFAULT, true);
    }

    public static void sortPlayerInventory(Player player, PlayerDataManager.SortMode mode) {
        sortPlayerInventory(player, mode, true);
    }

    public static void sortPlayerInventory(Player player, boolean allowPartialStacks) {
        sortPlayerInventory(player, PlayerDataManager.SortMode.DEFAULT, allowPartialStacks);
    }

    public static void sortPlayerInventory(Player player, PlayerDataManager.SortMode mode, boolean allowPartialStacks) {
        sortPlayerInventory(player, mode, allowPartialStacks, Set.of());
    }

    /** {@code lockedSlots} are player inventory slot numbers (0-35) that keep their item. */
    public static void sortPlayerInventory(Player player, PlayerDataManager.SortMode mode, boolean allowPartialStacks,
                                           Set<Integer> lockedSlots) {
        // Main inventory only (slots 9-35), excluding hotbar and armor
        sortRange(player.getInventory(), 9, 36, lockedSlots, mode, allowPartialStacks, null);
    }

    public static void sortHotbar(Player player) {
        sortHotbar(player, PlayerDataManager.SortMode.DEFAULT);
    }

    public static void sortHotbar(Player player, PlayerDataManager.SortMode mode) {
        sortHotbar(player, mode, HotbarPriorities.DEFAULT);
    }

    public static void sortHotbar(Player player, PlayerDataManager.SortMode mode, HotbarPriorities priorities) {
        sortHotbar(player, mode, priorities, Set.of());
    }

    public static void sortHotbar(Player player, PlayerDataManager.SortMode mode, HotbarPriorities priorities,
                                  Set<Integer> lockedSlots) {
        PlayerInventory inventory = player.getInventory();
        // Always allow partial stacks in the hotbar, and use hotbar priorities even in default mode
        sortRange(inventory, 0, 9, lockedSlots, mode, true, priorities);

        // Update the player's inventory
        player.updateInventory();
    }

    /**
     * Sorts slots [start, end) in place; locked slots keep their item and are skipped over.
     * {@code hotbarPriorities} is null outside the hotbar.
     */
    private static void sortRange(Inventory inventory, int start, int end, Set<Integer> lockedSlots,
                                  PlayerDataManager.SortMode mode, boolean allowPartialStacks, HotbarPriorities hotbarPriorities) {
        List<Integer> slots = new ArrayList<>();
        List<ItemStack> items = new ArrayList<>();
        for (int i = start; i < end; i++) {
            if (lockedSlots.contains(i)) {
                continue;
            }
            slots.add(i);
            ItemStack item = inventory.getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                items.add(item.clone());
            }
        }

        List<ItemStack> sortedItems = sort(items, mode, allowPartialStacks, hotbarPriorities);
        // Merging never adds stacks, but never risk deleting items if it somehow did
        if (sortedItems.size() > slots.size()) {
            return;
        }

        for (int i = 0; i < slots.size(); i++) {
            inventory.setItem(slots.get(i), i < sortedItems.size() ? sortedItems.get(i) : null);
        }
    }

    private static List<ItemStack> sort(List<ItemStack> items, PlayerDataManager.SortMode mode,
                                        boolean allowPartialStacks, HotbarPriorities hotbarPriorities) {
        List<List<ItemStack>> groups = groupItems(items);
        Comparator<List<ItemStack>> byName = Comparator.comparing(group -> group.getFirst().getType().name());

        switch (mode) {
            case ALPHABETICAL:
                groups.sort(byName);
                return stack(groups, allowPartialStacks);
            case STACK_BASED:
                groups.sort(byName);
                return largestStacksFirst(groups);
            default:
                Comparator<List<ItemStack>> byCategory = hotbarPriorities != null
                        ? Comparator.comparingInt(group -> getHotbarPriority(group.getFirst().getType(), hotbarPriorities))
                        : Comparator.comparingInt(group -> getMaterialPriority(group.getFirst().getType()));
                groups.sort(byCategory.thenComparing(byName));
                return stack(groups, allowPartialStacks);
        }
    }

    /** Groups stacks that can merge: same material and same meta (enchantments, names, damage, ...). */
    private static List<List<ItemStack>> groupItems(List<ItemStack> items) {
        List<List<ItemStack>> groups = new ArrayList<>();
        outer:
        for (ItemStack item : items) {
            for (List<ItemStack> group : groups) {
                if (group.getFirst().isSimilar(item)) {
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
            int totalAmount = group.stream().mapToInt(ItemStack::getAmount).sum();
            int maxStackSize = template.getMaxStackSize();

            while (totalAmount >= maxStackSize) {
                ItemStack fullStack = template.clone();
                fullStack.setAmount(maxStackSize);
                result.add(fullStack);
                totalAmount -= maxStackSize;
            }
            if (totalAmount > 0) {
                ItemStack remainingStack = template.clone();
                remainingStack.setAmount(totalAmount);
                (allowPartialStacks ? result : leftovers).add(remainingStack);
            }
        }

        result.addAll(leftovers);
        return result;
    }

    /** Keeps individual stack sizes, largest stacks first within each group. */
    private static List<ItemStack> largestStacksFirst(List<List<ItemStack>> groups) {
        List<ItemStack> result = new ArrayList<>();
        for (List<ItemStack> group : groups) {
            group.stream()
                    .sorted(Comparator.comparingInt(ItemStack::getAmount).reversed())
                    .forEach(result::add);
        }
        return result;
    }

    // Hotbar-specific priority (optimized for PvP/survival)
    private static int getHotbarPriority(Material material, HotbarPriorities priorities) {
        if (isWeaponType(material)) return priorities.weapons();
        if (isToolType(material)) return priorities.tools();
        if (material.isEdible()) return priorities.food();
        if (isQuickBuildBlock(material)) return priorities.buildingBlocks();
        if (isUtilityItem(material)) return priorities.utilityItems();
        return priorities.other();
    }

    private static boolean isQuickBuildBlock(Material material) {
        String name = material.name();
        // Common blocks used for quick building/bridging
        return material.isBlock() && !isStoneLookalike(material) &&
               (name.contains("COBBLESTONE") || name.contains("DIRT") ||
                name.contains("STONE") || name.contains("PLANKS") ||
                name.contains("SAND") || name.contains("GRAVEL") ||
                name.contains("WOOL"));
    }

    private static boolean isUtilityItem(Material material) {
        String name = material.name();
        return name.contains("ENDER_PEARL") || name.contains("BUCKET") ||
               name.contains("TORCH") || name.contains("FLINT_AND_STEEL") ||
               name.contains("COMPASS") || name.contains("MAP");
    }

    // Regular inventory priority
    private static int getMaterialPriority(Material material) {
        // Ores first: deepslate and redstone ores would otherwise match the stone names
        if (isOreType(material)) return 400;
        if (isStoneType(material)) return 100;
        if (isEarthType(material)) return 200;
        if (isWoodType(material)) return 300;
        if (material.isBlock()) return 500;
        if (isToolType(material)) return 600;
        if (isWeaponType(material)) return 700;
        if (isArmorType(material)) return 800;
        if (material.isEdible()) return 900;
        if (isRedstoneType(material)) return 1000;
        if (isTransportationType(material)) return 1100;
        if (isDecorationType(material)) return 1200;
        return 9999;
    }

    /** Names containing "STONE" that aren't stone. */
    private static boolean isStoneLookalike(Material material) {
        String name = material.name();
        return name.contains("REDSTONE") || name.contains("GLOWSTONE") || name.contains("LODESTONE");
    }

    // Blocks only, so stone tools and swords aren't sorted in with stone
    private static boolean isStoneType(Material material) {
        String name = material.name();
        return material.isBlock() && !isStoneLookalike(material) &&
               (name.contains("STONE") || name.contains("GRANITE") || name.contains("DIORITE") ||
                name.contains("ANDESITE") || name.contains("BASALT") || name.contains("TUFF") ||
                name.contains("DEEPSLATE"));
    }

    private static boolean isEarthType(Material material) {
        String name = material.name();
        return material.isBlock() &&
               (name.contains("DIRT") || name.contains("GRASS") || name.contains("SAND") ||
                name.contains("GRAVEL") || name.contains("CLAY") || name.contains("TERRACOTTA") ||
                name.contains("CONCRETE") || name.contains("NETHERRACK") || name.contains("SOUL_SOIL"));
    }

    private static boolean isWoodType(Material material) {
        String name = material.name();
        return (name.contains("LOG") || name.contains("WOOD") || name.contains("PLANKS") ||
                name.contains("LEAVES") || name.contains("SAPLING")) && material.isBlock();
    }

    private static boolean isOreType(Material material) {
        String name = material.name();
        return name.contains("_ORE") || (name.contains("_BLOCK") &&
               (name.contains("IRON") || name.contains("GOLD") || name.contains("DIAMOND") ||
                name.contains("EMERALD") || name.contains("COAL") || name.contains("COPPER") ||
                name.contains("REDSTONE") || name.contains("LAPIS") || name.contains("NETHERITE")));
    }

    private static boolean isToolType(Material material) {
        String name = material.name();
        return name.contains("PICKAXE") || name.contains("AXE") || name.contains("SHOVEL") ||
               name.contains("HOE") || name.contains("SHEARS") || name.contains("FLINT_AND_STEEL") ||
               name.contains("FISHING_ROD") || name.contains("COMPASS") || name.contains("CLOCK");
    }

    // Exact names, since "BOW" alone would also match BOWL
    private static boolean isWeaponType(Material material) {
        String name = material.name();
        return name.endsWith("_SWORD") || name.endsWith("_SPEAR") || name.equals("BOW") || name.equals("CROSSBOW") ||
               name.equals("TRIDENT") || name.equals("MACE") || name.equals("ARROW") || name.endsWith("_ARROW");
    }

    private static boolean isArmorType(Material material) {
        String name = material.name();
        return name.contains("HELMET") || name.contains("CHESTPLATE") || name.contains("LEGGINGS") ||
               name.contains("BOOTS") || name.contains("SHIELD") || name.contains("ELYTRA");
    }

    private static boolean isRedstoneType(Material material) {
        String name = material.name();
        return name.contains("REDSTONE") || name.contains("REPEATER") || name.contains("COMPARATOR") ||
               name.contains("PISTON") || name.contains("DISPENSER") ||
               name.contains("DROPPER") || name.contains("HOPPER") || name.contains("PRESSURE_PLATE") ||
               name.contains("BUTTON") || name.contains("LEVER") || (name.contains("DOOR") && material.isBlock());
    }

    private static boolean isTransportationType(Material material) {
        String name = material.name();
        return name.contains("MINECART") || name.contains("BOAT") || name.contains("SADDLE");
    }

    private static boolean isDecorationType(Material material) {
        String name = material.name();
        return name.contains("CARPET") || name.contains("BANNER") || name.contains("PAINTING") ||
               name.contains("ITEM_FRAME") || name.contains("FLOWER_POT") || name.contains("FLOWER") ||
               name.contains("TULIP") || name.contains("TORCH") || name.contains("LANTERN") ||
               name.contains("CANDLE") || name.contains("HEAD") || name.contains("SKULL");
    }
}
