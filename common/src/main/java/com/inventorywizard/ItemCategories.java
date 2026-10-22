package com.inventorywizard;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Category priorities used by the default sort mode. Item tags and data components
 * are checked first so modded items are categorised too; registry-path matching
 * covers what tags don't.
 */
final class ItemCategories {

    private ItemCategories() {
    }

    static String path(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
    }

    static String namespace(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace();
    }

    static int hotbarPriority(ItemStack stack, InventoryWizardConfig.HotbarPriorities priorities) {
        if (isWeapon(stack)) return priorities.weapons;
        if (isTool(stack)) return priorities.tools;
        if (isFood(stack)) return priorities.food;
        if (isQuickBuildBlock(stack)) return priorities.buildingBlocks;
        if (isUtilityItem(stack)) return priorities.utilityItems;
        return priorities.other;
    }

    static int inventoryPriority(ItemStack stack) {
        // Ores first: deepslate and redstone ores would otherwise match the stone names
        if (isOre(stack)) return 400;
        if (isStone(stack)) return 100;
        if (isEarth(stack)) return 200;
        if (isWood(stack)) return 300;
        if (isBlock(stack)) return 500;
        if (isTool(stack)) return 600;
        if (isWeapon(stack)) return 700;
        if (isArmor(stack)) return 800;
        if (isFood(stack)) return 900;
        if (isRedstone(stack)) return 1000;
        if (isTransportation(stack)) return 1100;
        if (isDecoration(stack)) return 1200;
        return 9999;
    }

    private static boolean isBlock(ItemStack stack) {
        return stack.getItem() instanceof BlockItem;
    }

    private static boolean pathContains(ItemStack stack, String... parts) {
        String path = path(stack);
        for (String part : parts) {
            if (path.contains(part)) return true;
        }
        return false;
    }

    private static boolean isWeapon(ItemStack stack) {
        return stack.is(ItemTags.SWORDS) || stack.is(ItemTags.SPEARS) || stack.is(ItemTags.ARROWS)
                || stack.is(Items.BOW) || stack.is(Items.CROSSBOW) || stack.is(Items.TRIDENT) || stack.is(Items.MACE);
    }

    private static boolean isTool(ItemStack stack) {
        if (stack.is(ItemTags.PICKAXES) || stack.is(ItemTags.AXES) || stack.is(ItemTags.SHOVELS) || stack.is(ItemTags.HOES)) {
            return true;
        }
        // Swords carry a tool component too (for cobwebs), so exclude weapons here
        return (stack.has(DataComponents.TOOL) && !isWeapon(stack))
                || stack.is(Items.FISHING_ROD) || stack.is(Items.FLINT_AND_STEEL) || stack.is(Items.BRUSH)
                || stack.is(Items.COMPASS) || stack.is(Items.CLOCK);
    }

    private static boolean isArmor(ItemStack stack) {
        return stack.is(ItemTags.HEAD_ARMOR) || stack.is(ItemTags.CHEST_ARMOR) || stack.is(ItemTags.LEG_ARMOR)
                || stack.is(ItemTags.FOOT_ARMOR) || stack.has(DataComponents.BLOCKS_ATTACKS) || stack.has(DataComponents.GLIDER);
    }

    private static boolean isFood(ItemStack stack) {
        return stack.has(DataComponents.FOOD);
    }

    private static boolean isQuickBuildBlock(ItemStack stack) {
        return isBlock(stack) && (stack.is(ItemTags.PLANKS) || stack.is(ItemTags.DIRT) || stack.is(ItemTags.SAND)
                || stack.is(ItemTags.WOOL) || (pathContains(stack, "stone", "gravel") && !isStoneLookalike(stack)));
    }

    // Flint and steel and compasses count as tools, which are checked first
    private static boolean isUtilityItem(ItemStack stack) {
        return stack.is(Items.ENDER_PEARL) || pathContains(stack, "bucket", "torch", "map");
    }

    private static boolean isStone(ItemStack stack) {
        return isBlock(stack) && (stack.is(ItemTags.STONE_CRAFTING_MATERIALS) || stack.is(ItemTags.STONE_BRICKS)
                || (pathContains(stack, "stone", "granite", "diorite", "andesite", "basalt", "tuff", "deepslate")
                        && !isStoneLookalike(stack)));
    }

    /** Names containing "stone" that aren't stone. */
    private static boolean isStoneLookalike(ItemStack stack) {
        return pathContains(stack, "redstone", "glowstone", "lodestone");
    }

    private static boolean isEarth(ItemStack stack) {
        return isBlock(stack) && (stack.is(ItemTags.DIRT) || stack.is(ItemTags.SAND) || stack.is(ItemTags.TERRACOTTA)
                || pathContains(stack, "grass", "gravel", "clay", "concrete", "netherrack", "soul_sand", "soul_soil"));
    }

    private static boolean isWood(ItemStack stack) {
        return stack.is(ItemTags.LOGS) || stack.is(ItemTags.PLANKS) || stack.is(ItemTags.LEAVES) || stack.is(ItemTags.SAPLINGS)
                || (isBlock(stack) && pathContains(stack, "_wood", "_hyphae"));
    }

    private static boolean isOre(ItemStack stack) {
        if (stack.is(ItemTags.COAL_ORES) || stack.is(ItemTags.IRON_ORES) || stack.is(ItemTags.GOLD_ORES)
                || stack.is(ItemTags.DIAMOND_ORES) || pathContains(stack, "_ore")) {
            return true;
        }
        return isBlock(stack) && pathContains(stack, "_block")
                && pathContains(stack, "iron", "gold", "diamond", "emerald", "coal", "copper", "redstone", "lapis", "netherite");
    }

    private static boolean isRedstone(ItemStack stack) {
        return stack.is(ItemTags.RAILS)
                || pathContains(stack, "redstone", "repeater", "comparator", "piston", "dispenser", "dropper",
                        "hopper", "pressure_plate", "lever", "observer", "button", "door");
    }

    private static boolean isTransportation(ItemStack stack) {
        return stack.is(ItemTags.BOATS) || stack.is(ItemTags.CHEST_BOATS) || pathContains(stack, "minecart", "saddle");
    }

    private static boolean isDecoration(ItemStack stack) {
        return stack.is(ItemTags.WOOL_CARPETS) || stack.is(ItemTags.BANNERS) || stack.is(ItemTags.CANDLES) || stack.is(ItemTags.SKULLS)
                || pathContains(stack, "painting", "item_frame", "flower_pot", "flower", "tulip", "torch", "lantern", "head");
    }
}
