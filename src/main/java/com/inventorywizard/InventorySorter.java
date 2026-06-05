package com.inventorywizard;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.*;

public class InventorySorter {
    
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
        List<ItemStack> items = new ArrayList<>();
        
        // Collect all items from the inventory
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                items.add(item.clone());
            }
        }
        
        // Clear the inventory
        inventory.clear();
        
        // Sort and stack items based on mode
        List<ItemStack> sortedItems;
        switch (mode) {
            case ALPHABETICAL:
                sortedItems = sortAlphabetically(items, allowPartialStacks);
                break;
            case STACK_BASED:
                sortedItems = sortByStacks(items);
                break;
            default:
                Map<String, List<ItemStack>> groupedItems = groupItems(items);
                sortedItems = stackAndSortByType(groupedItems, false, allowPartialStacks);
                break;
        }
        
        // Put items back into inventory
        for (int i = 0; i < sortedItems.size() && i < inventory.getSize(); i++) {
            inventory.setItem(i, sortedItems.get(i));
        }
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
        PlayerInventory inventory = player.getInventory();
        List<ItemStack> items = new ArrayList<>();
        
        // Collect items from main inventory (slots 9-35, excluding hotbar and armor)
        for (int i = 9; i < 36; i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                items.add(item.clone());
            }
        }
        
        // Clear main inventory slots
        for (int i = 9; i < 36; i++) {
            inventory.setItem(i, null);
        }
        
        // Sort and stack items based on mode
        List<ItemStack> sortedItems;
        switch (mode) {
            case ALPHABETICAL:
                sortedItems = sortAlphabetically(items, allowPartialStacks);
                break;
            case STACK_BASED:
                sortedItems = sortByStacks(items);
                break;
            default:
                Map<String, List<ItemStack>> groupedItems = groupItems(items);
                sortedItems = stackAndSortByType(groupedItems, false, allowPartialStacks);
                break;
        }
        
        // Put items back into main inventory
        int slot = 9;
        for (ItemStack item : sortedItems) {
            if (slot >= 36) break;
            inventory.setItem(slot, item);
            slot++;
        }
    }
    
    public static void sortHotbar(Player player) {
        sortHotbar(player, PlayerDataManager.SortMode.DEFAULT);
    }
    
    public static void sortHotbar(Player player, PlayerDataManager.SortMode mode) {
        PlayerInventory inventory = player.getInventory();
        List<ItemStack> items = new ArrayList<>();
        
        // Collect items from hotbar (slots 0-8)
        for (int i = 0; i < 9; i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                items.add(item.clone());
            }
        }
        
        // Clear hotbar slots
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, null);
        }
        
        // Sort and stack items based on mode
        List<ItemStack> sortedItems;
        switch (mode) {
            case ALPHABETICAL:
                sortedItems = sortAlphabetically(items, true); // always allow partial stacks in hotbar
                break;
            case STACK_BASED:
                sortedItems = sortByStacks(items);
                break;
            default:
                // For hotbar, always use hotbar priority even in default mode
                Map<String, List<ItemStack>> groupedItems = groupItems(items);
                sortedItems = stackAndSortByType(groupedItems, true, true); // always allow partial stacks in hotbar
                break;
        }
        
        // Put items back into hotbar
        for (int i = 0; i < Math.min(sortedItems.size(), 9); i++) {
            inventory.setItem(i, sortedItems.get(i));
        }
        
        // Update the player's inventory
        player.updateInventory();
    }
    
    private static Map<String, List<ItemStack>> groupItems(List<ItemStack> items) {
        Map<String, List<ItemStack>> grouped = new HashMap<>();
        
        for (ItemStack item : items) {
            String key = createItemKey(item);
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(item);
        }
        
        return grouped;
    }
    
    private static String createItemKey(ItemStack item) {
        // Create a unique key based on material, durability, and metadata
        StringBuilder key = new StringBuilder();
        key.append(item.getType().name());
        
        if (item.hasItemMeta()) {
            key.append("|").append(item.getItemMeta().hashCode());
        }
        
        return key.toString();
    }
    
    private static List<ItemStack> stackAndSortByType(Map<String, List<ItemStack>> groupedItems, boolean isHotbar, boolean allowPartialStacks) {
        List<ItemStack> result = new ArrayList<>();
        
        // Create a list of item templates for sorting
        List<ItemStack> templates = new ArrayList<>();
        for (List<ItemStack> itemGroup : groupedItems.values()) {
            if (!itemGroup.isEmpty()) {
                templates.add(itemGroup.get(0));
            }
        }
        
        // Sort templates by material type priority
        templates.sort((item1, item2) -> {
            int priority1 = isHotbar ? getHotbarPriority(item1.getType()) : getMaterialPriority(item1.getType());
            int priority2 = isHotbar ? getHotbarPriority(item2.getType()) : getMaterialPriority(item2.getType());
            
            if (priority1 != priority2) {
                return Integer.compare(priority1, priority2);
            }
            
            // Within same category, sort alphabetically
            return item1.getType().name().compareToIgnoreCase(item2.getType().name());
        });
        
        // Process each sorted template
        for (ItemStack template : templates) {
            String key = createItemKey(template);
            List<ItemStack> itemGroup = groupedItems.get(key);
            
            // Calculate total amount
            int totalAmount = itemGroup.stream().mapToInt(ItemStack::getAmount).sum();
            
            if (totalAmount > 0) {
                int maxStackSize = template.getMaxStackSize();
                
                // Create full stacks
                while (totalAmount > maxStackSize) {
                    ItemStack fullStack = template.clone();
                    fullStack.setAmount(maxStackSize);
                    result.add(fullStack);
                    totalAmount -= maxStackSize;
                }
                
                // Create remaining stack (only if allowPartialStacks is true)
                if (totalAmount > 0 && allowPartialStacks) {
                    ItemStack remainingStack = template.clone();
                    remainingStack.setAmount(totalAmount);
                    result.add(remainingStack);
                }
            }
        }
        
        return result;
    }
    
    // Hotbar-specific priority (optimized for PvP/survival)
    private static int getHotbarPriority(Material material) {
        // 1. Weapons (highest priority)
        if (isWeaponType(material)) return 100;
        
        // 2. Tools
        if (isToolType(material)) return 200;
        
        // 3. Food
        if (material.isEdible()) return 300;
        
        // 4. Blocks (for building/bridging)
        if (isQuickBuildBlock(material)) return 400;
        
        // 5. Utility items
        if (isUtilityItem(material)) return 500;
        
        // 6. Everything else
        return 999;
    }
    
    private static boolean isQuickBuildBlock(Material material) {
        String name = material.name();
        // Common blocks used for quick building/bridging
        return name.contains("COBBLESTONE") || name.contains("DIRT") || 
               name.contains("STONE") || name.contains("PLANKS") ||
               name.contains("SAND") || name.contains("GRAVEL") ||
               name.contains("WOOL");
    }
    
    private static boolean isUtilityItem(Material material) {
        String name = material.name();
        return name.contains("ENDER_PEARL") || name.contains("WATER_BUCKET") || 
               name.contains("LAVA_BUCKET") || name.contains("BUCKET") ||
               name.contains("TORCH") || name.contains("FLINT_AND_STEEL") ||
               name.contains("COMPASS") || name.contains("MAP");
    }
    
    // Regular inventory priority (same as before)
    private static int getMaterialPriority(Material material) {
        String name = material.name();
        
        // 1. Building Blocks - Stone types
        if (isStoneType(material)) return 100;
        
        // 2. Building Blocks - Dirt/Earth types
        if (isEarthType(material)) return 200;
        
        // 3. Building Blocks - Wood types
        if (isWoodType(material)) return 300;
        
        // 4. Building Blocks - Ores and Metals
        if (isOreType(material)) return 400;
        
        // 5. Building Blocks - Other blocks
        if (material.isBlock()) return 500;
        
        // 6. Tools
        if (isToolType(material)) return 600;
        
        // 7. Weapons
        if (isWeaponType(material)) return 700;
        
        // 8. Armor
        if (isArmorType(material)) return 800;
        
        // 9. Food
        if (material.isEdible()) return 900;
        
        // 10. Redstone items
        if (isRedstoneType(material)) return 1000;
        
        // 11. Transportation
        if (isTransportationType(material)) return 1100;
        
        // 12. Decoration
        if (isDecorationType(material)) return 1200;
        
        // 13. Miscellaneous items
        return 9999;
    }
    
    // All helper methods using .name().contains() pattern consistently
    private static boolean isStoneType(Material material) {
        String name = material.name();
        return name.contains("STONE") || name.contains("GRANITE") || name.contains("DIORITE") || 
               name.contains("ANDESITE") || name.contains("COBBLESTONE") || name.contains("BLACKSTONE") ||
               name.contains("BASALT") || name.contains("TUFF") || name.contains("DEEPSLATE");
    }
    
    private static boolean isEarthType(Material material) {
        String name = material.name();
        return name.contains("DIRT") || name.contains("GRASS") || name.contains("SAND") || 
               name.contains("GRAVEL") || name.contains("CLAY") || name.contains("TERRACOTTA") ||
               name.contains("CONCRETE") || name.contains("NETHERRACK") || name.contains("SOUL_SAND") ||
               name.contains("SOUL_SOIL");
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
    
    private static boolean isWeaponType(Material material) {
        String name = material.name();
        return name.contains("SWORD") || name.contains("BOW") || name.contains("CROSSBOW") ||
               name.contains("TRIDENT") || name.contains("ARROW");
    }
    
    private static boolean isArmorType(Material material) {
        String name = material.name();
        return name.contains("HELMET") || name.contains("CHESTPLATE") || name.contains("LEGGINGS") || 
               name.contains("BOOTS") || name.contains("SHIELD") || name.contains("ELYTRA");
    }
    
    private static boolean isRedstoneType(Material material) {
        String name = material.name();
        return name.contains("REDSTONE") || name.contains("REPEATER") || name.contains("COMPARATOR") ||
               name.contains("PISTON") || name.contains("STICKY_PISTON") || name.contains("DISPENSER") ||
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
    
    // Alphabetical sorting method
    private static List<ItemStack> sortAlphabetically(List<ItemStack> items, boolean allowPartialStacks) {
        Map<String, List<ItemStack>> groupedItems = groupItems(items);
        List<ItemStack> result = new ArrayList<>();
        
        // Create a list of item templates for sorting
        List<ItemStack> templates = new ArrayList<>();
        for (List<ItemStack> itemGroup : groupedItems.values()) {
            if (!itemGroup.isEmpty()) {
                templates.add(itemGroup.get(0));
            }
        }
        
        // Sort templates alphabetically by material name
        templates.sort((item1, item2) -> 
            item1.getType().name().compareToIgnoreCase(item2.getType().name()));
        
        // Process each sorted template
        for (ItemStack template : templates) {
            String key = createItemKey(template);
            List<ItemStack> itemGroup = groupedItems.get(key);
            
            // Calculate total amount
            int totalAmount = itemGroup.stream().mapToInt(ItemStack::getAmount).sum();
            
            if (totalAmount > 0) {
                int maxStackSize = template.getMaxStackSize();
                
                // Create full stacks
                while (totalAmount > maxStackSize) {
                    ItemStack fullStack = template.clone();
                    fullStack.setAmount(maxStackSize);
                    result.add(fullStack);
                    totalAmount -= maxStackSize;
                }
                
                // Create remaining stack (only if allowPartialStacks is true)
                if (totalAmount > 0 && allowPartialStacks) {
                    ItemStack remainingStack = template.clone();
                    remainingStack.setAmount(totalAmount);
                    result.add(remainingStack);
                }
            }
        }
        
        return result;
    }
    
    // Stack-based sorting method - groups by item type, then by stack size
    private static List<ItemStack> sortByStacks(List<ItemStack> items) {
        Map<String, List<ItemStack>> groupedItems = groupItems(items);
        List<ItemStack> result = new ArrayList<>();
        
        // Create a list of item templates for sorting
        List<ItemStack> templates = new ArrayList<>();
        for (List<ItemStack> itemGroup : groupedItems.values()) {
            if (!itemGroup.isEmpty()) {
                templates.add(itemGroup.get(0));
            }
        }
        
        // Sort templates alphabetically by material name (to group by item type)
        templates.sort((item1, item2) -> 
            item1.getType().name().compareToIgnoreCase(item2.getType().name()));
        
        // Process each sorted template
        for (ItemStack template : templates) {
            String key = createItemKey(template);
            List<ItemStack> itemGroup = groupedItems.get(key);
            
            // Sort individual stacks by size (largest first)
            List<ItemStack> sortedStacks = new ArrayList<>();
            for (ItemStack item : itemGroup) {
                sortedStacks.add(item.clone());
            }
            
            // Sort stacks by amount (largest to smallest)
            sortedStacks.sort((stack1, stack2) -> Integer.compare(stack2.getAmount(), stack1.getAmount()));
            
            // Add all stacks to result (preserving individual stack sizes)
            result.addAll(sortedStacks);
        }
        
        return result;
    }
    

}