package com.inventorywizard;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Set;

public class SortListener implements Listener {

    private final PlayerDataManager preferences;
    private final InventoryWizardPlugin plugin;

    public SortListener(InventoryWizardPlugin plugin) {
        this.preferences = plugin.getPlayerDataManager();
        this.plugin = plugin;
    }

    // ignoreCancelled: leave clicks alone that another plugin (e.g. a GUI menu) has already handled.
    // Clicks are only cancelled when we actually use them, so vanilla shift-clicking keeps working elsewhere.
    // The sorts themselves run on the next tick (sortNextTick), outside the click event.
    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        Inventory clickedInventory = event.getClickedInventory();
        if (clickedInventory == null) {
            return;
        }

        boolean playerInventory = clickedInventory.getType() == InventoryType.PLAYER;
        boolean hotbarSlot = playerInventory && event.getSlot() >= 0 && event.getSlot() <= 8;

        // Handle Shift+Right Click for hotbar/inventory/container sorting
        if (event.getClick() == ClickType.SHIFT_RIGHT) {
            if (!player.hasPermission("inventorywizard.use")) {
                return;
            }

            if (hotbarSlot) {
                onHotbarShiftRightClick(event, player);
            } else if (plugin.canSortContainer(clickedInventory, player)) {
                event.setCancelled(true);
                PlayerDataManager.SortMode mode = preferences.getPlayerSortMode(player);
                String containerType = InventoryWizardPlugin.containerDisplayName(clickedInventory.getType());
                plugin.sortNextTick(player, () -> InventorySorter.sortInventory(clickedInventory, mode, plugin.allowPartialStacksInChests()),
                        0f, "§a✨ " + containerType + " magically sorted! (" + mode.getDisplayName() + ")");
            } else if (playerInventory && event.getSlot() > 8 && event.getSlot() < 36
                    && player.hasPermission("inventorywizard.inventory") && plugin.isFeatureEnabled("inventory-sorting")) {
                event.setCancelled(true);
                PlayerDataManager.SortMode mode = preferences.getPlayerSortMode(player);
                plugin.sortNextTick(player, () -> InventorySorter.sortPlayerInventory(player, mode, plugin.allowPartialStacksInInventory(),
                        preferences.getLockedSlots(player)),
                        0f, "§a✨ Inventory organized with wizard magic! (" + mode.getDisplayName() + ")");
            }
        }

        // Handle Double Click for hotbar sorting (alternative method). Only with an empty cursor: with an item
        // on the cursor, a double click is vanilla's "collect matching items", which we leave alone.
        else if (event.getClick() == ClickType.DOUBLE_CLICK && hotbarSlot && isEmpty(event.getCursor())
                && player.hasPermission("inventorywizard.hotbar") && plugin.isFeatureEnabled("hotbar-sorting")) {
            event.setCancelled(true);
            PlayerDataManager.SortMode mode = preferences.getPlayerSortMode(player);
            plugin.sortNextTick(player, () -> InventorySorter.sortHotbar(player, mode, plugin.getHotbarPriorities(), preferences.getLockedSlots(player)),
                    0.2f, "§6✨ Hotbar arranged by wizardry! (" + mode.getDisplayName() + ")");
        }
    }

    /** Slot 4 cycles the sort mode; other hotbar slots sort the hotbar, plus the inventory with the "all" permission. */
    private void onHotbarShiftRightClick(InventoryClickEvent event, Player player) {
        // Special case: Slot 4 (middle hotbar slot) cycles sorting modes
        if (event.getSlot() == 4) {
            event.setCancelled(true);
            PlayerDataManager.SortMode newMode = preferences.cyclePlayerSortMode(player);
            plugin.feedback(player, 0.4f, "§e🔄 Sorting mode changed to: §6" + newMode.getDisplayName());
            return;
        }

        boolean hotbarEnabled = plugin.isFeatureEnabled("hotbar-sorting");
        boolean inventory = player.hasPermission("inventorywizard.all") && plugin.isFeatureEnabled("inventory-sorting");
        boolean hotbar = (player.hasPermission("inventorywizard.all") || player.hasPermission("inventorywizard.hotbar")) && hotbarEnabled;
        if (!inventory && !hotbar) {
            return;
        }

        event.setCancelled(true);
        PlayerDataManager.SortMode mode = preferences.getPlayerSortMode(player);
        String message;
        float pitchOffset;
        if (inventory && hotbar) {
            message = "§b🧙✨ Complete inventory enchanted by the InventoryWizard! (" + mode.getDisplayName() + ")";
            pitchOffset = -0.2f;
        } else if (hotbar) {
            message = "§6✨ Hotbar organized by the InventoryWizard! (" + mode.getDisplayName() + ")";
            pitchOffset = 0.2f;
        } else {
            message = "§a✨ Inventory organized with wizard magic! (" + mode.getDisplayName() + ")";
            pitchOffset = 0f;
        }

        Set<Integer> lockedSlots = preferences.getLockedSlots(player);
        plugin.sortNextTick(player, () -> {
            if (inventory) {
                InventorySorter.sortPlayerInventory(player, mode, plugin.allowPartialStacksInInventory(), lockedSlots);
            }
            if (hotbar) {
                InventorySorter.sortHotbar(player, mode, plugin.getHotbarPriorities(), lockedSlots);
            }
        }, pitchOffset, message);
    }

    // Preferences are cached while a player is online, so sorting never waits on the database
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        preferences.preload(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        preferences.forget(event.getPlayer());
    }

    private static boolean isEmpty(ItemStack item) {
        return item == null || item.getType().isAir();
    }
}
