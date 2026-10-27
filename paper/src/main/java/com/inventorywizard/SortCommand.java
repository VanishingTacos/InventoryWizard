package com.inventorywizard;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SortCommand implements CommandExecutor, TabCompleter {
    
    private static final String DISABLED_MESSAGE = "§c🧙✨ That kind of sorting is turned off in the InventoryWizard config.";
    
    private final InventoryWizardPlugin plugin;
    
    public SortCommand(InventoryWizardPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c🧙✨ The InventoryWizard's magic only works for players!");
            return true;
        }
        
        Player player = (Player) sender;
        
        // Default to inventory if no args
        String sortType = args.length > 0 ? args[0].toLowerCase() : "inventory";
        
        switch (sortType) {
            case "hotbar":
            case "hb":
                if (!player.hasPermission("inventorywizard.hotbar")) {
                    player.sendMessage("§c🧙✨" + ErrorHandler.getPermissionErrorMessage());
                    return true;
                }
                if (!plugin.isFeatureEnabled("hotbar-sorting")) {
                    player.sendMessage(DISABLED_MESSAGE);
                    return true;
                }
                
                PlayerDataManager.SortMode mode = plugin.getPlayerDataManager().getPlayerSortMode(player);
                plugin.sort(player, () -> InventorySorter.sortHotbar(player, mode, plugin.getHotbarPriorities(), lockedSlots(player)),
                        0.2f, "§6✨ Hotbar enchanted by the InventoryWizard! (" + mode.getDisplayName() + ")");
                break;
                
            case "inventory":
            case "inv":
                if (!player.hasPermission("inventorywizard.inventory")) {
                    player.sendMessage("§c🧙✨ " + ErrorHandler.getPermissionErrorMessage());
                    return true;
                }
                if (!plugin.isFeatureEnabled("inventory-sorting")) {
                    player.sendMessage(DISABLED_MESSAGE);
                    return true;
                }
                
                PlayerDataManager.SortMode invMode = plugin.getPlayerDataManager().getPlayerSortMode(player);
                plugin.sort(player, () -> InventorySorter.sortPlayerInventory(player, invMode, plugin.allowPartialStacksInInventory(), lockedSlots(player)),
                        0f, "§a✨ Inventory magically organized! (" + invMode.getDisplayName() + ")");
                break;
                
            case "all":
            case "both":
                if (!player.hasPermission("inventorywizard.all")) {
                    player.sendMessage("§c🧙✨ " + ErrorHandler.getPermissionErrorMessage());
                    return true;
                }
                boolean inventory = plugin.isFeatureEnabled("inventory-sorting");
                boolean hotbar = plugin.isFeatureEnabled("hotbar-sorting");
                if (!inventory && !hotbar) {
                    player.sendMessage(DISABLED_MESSAGE);
                    return true;
                }
                
                PlayerDataManager.SortMode allMode = plugin.getPlayerDataManager().getPlayerSortMode(player);
                Set<Integer> allLocked = lockedSlots(player);
                plugin.sort(player, () -> {
                    if (inventory) {
                        InventorySorter.sortPlayerInventory(player, allMode, plugin.allowPartialStacksInInventory(), allLocked);
                    }
                    if (hotbar) {
                        InventorySorter.sortHotbar(player, allMode, plugin.getHotbarPriorities(), allLocked);
                    }
                }, -0.2f, "§b🧙✨ Complete inventory transformation complete! (" + allMode.getDisplayName() + ")");
                break;
                
            case "container":
                Inventory top = player.getOpenInventory().getTopInventory();
                if (!plugin.canSortContainer(top, player)) {
                    player.sendMessage("§c🧙✨ Open a chest, barrel, shulker box or ender chest you're allowed to sort first.");
                    return true;
                }
                PlayerDataManager.SortMode containerMode = plugin.getPlayerDataManager().getPlayerSortMode(player);
                String containerType = InventoryWizardPlugin.containerDisplayName(top.getType());
                plugin.sort(player, () -> InventorySorter.sortInventory(top, containerMode, plugin.allowPartialStacksInChests()),
                        0f, "§a✨ " + containerType + " magically sorted! (" + containerMode.getDisplayName() + ")");
                break;

            case "mode":
                mode(player, args);
                break;

            case "lock":
                lock(player, args);
                break;

            case "regen-credentials":
                if (!player.hasPermission("inventorywizard.admin")) {
                    player.sendMessage("§c🧙✨ " + ErrorHandler.getPermissionErrorMessage());
                    return true;
                }
                if (!plugin.getPlayerDataManager().regenerateCredentials()) {
                    player.sendMessage("§c🔐 Could not regenerate the database credentials; the old ones are still in use. See the server log.");
                    return true;
                }
                player.sendMessage("§a🔐 Database credentials regenerated successfully!");
                player.sendMessage("§7New credentials have been saved to the configuration file.");
                break;
                
            case "rate-limit":
                if (!player.hasPermission("inventorywizard.admin")) {
                    player.sendMessage("§c🧙✨ " + ErrorHandler.getPermissionErrorMessage());
                    return true;
                }
                String stats = plugin.getRateLimiter().getPlayerStats(player);
                player.sendMessage("§6⏰ Rate Limiting Information:");
                for (String line : stats.split("\n")) {
                    player.sendMessage("§7" + line);
                }
                break;
                
            case "reset-rate-limit":
                if (!player.hasPermission("inventorywizard.admin")) {
                    player.sendMessage("§c🧙✨ " + ErrorHandler.getPermissionErrorMessage());
                    return true;
                }
                plugin.getRateLimiter().resetPlayer(player);
                player.sendMessage("§a⏰ Rate limiting reset for your account!");
                break;
                
            case "rate-limit-enabled":
                if (!player.hasPermission("inventorywizard.admin")) {
                    player.sendMessage("§c🧙✨ " + ErrorHandler.getPermissionErrorMessage());
                    return true;
                }
                if (args.length < 3) {
                    player.sendMessage("§e🧙✨ Usage: §f/iwiz rate-limit-enabled <playername> <on|off>");
                    return true;
                }
                String targetPlayerName = args[1];
                String state = args[2].toLowerCase();
                boolean enableRateLimit = state.equals("on");
                if (!state.equals("on") && !state.equals("off")) {
                    player.sendMessage("§c🧙✨ Invalid state. Use 'on' or 'off'.");
                    return true;
                }
                
                // find the target player
                Player targetPlayer = plugin.getServer().getPlayer(targetPlayerName);
                if (targetPlayer == null) {
                    player.sendMessage("§c🧙✨ Player '" + targetPlayerName + "' is not online.");
                    return true;
                }
                
                // set the rate limit preference
                plugin.getPlayerDataManager().setPlayerRateLimited(targetPlayer, enableRateLimit);
                player.sendMessage("§a🛡️ Rate limiting " + (enableRateLimit ? "enabled" : "disabled") + " for " + targetPlayer.getName() + ".");
                targetPlayer.sendMessage("§6🛡️ Rate limiting for InventoryWizard sorting has been " + (enableRateLimit ? "enabled" : "disabled") + " by an admin.");
                break;
                

                
            default:
                player.sendMessage("§e🧙✨ InventoryWizard Usage: §f/iwiz [hotbar|inventory|all|container|mode|lock]");
                player.sendMessage("§7Cast your sorting spells with: hotbar, inventory, all, or container (the one you have open)");
                player.sendMessage("§7Change the sorting mode with: mode [default|alphabetical|stack-based]");
                player.sendMessage("§7Keep a slot in place with: lock [slot|list|clear]");
                return true;
        }
        
        return true;
    }
    
    private Set<Integer> lockedSlots(Player player) {
        return plugin.getPlayerDataManager().getLockedSlots(player);
    }

    /**
     * {@code /iwiz mode} cycles the sort mode, {@code /iwiz mode <mode>} sets it. The client mod's mode button
     * uses the second form, so its own mode and the server's stay the same.
     */
    private void mode(Player player, String[] args) {
        PlayerDataManager preferences = plugin.getPlayerDataManager();
        PlayerDataManager.SortMode mode;
        if (args.length > 1) {
            mode = PlayerDataManager.SortMode.fromName(args[1]);
            if (mode == null) {
                player.sendMessage("§c🧙✨ Usage: §f/iwiz mode [default|alphabetical|stack-based]");
                return;
            }
            preferences.setPlayerSortMode(player, mode);
        } else {
            mode = preferences.cyclePlayerSortMode(player);
        }
        plugin.feedback(player, 0.4f, "§e🔄 Sorting mode changed to: §6" + mode.getDisplayName());
    }

    /**
     * {@code /iwiz lock} toggles the held hotbar slot, {@code /iwiz lock <slot>} any inventory slot
     * (0-8 hotbar, 9-35 main inventory, the numbering Bukkit uses). A server can't see Alt+click.
     */
    private void lock(Player player, String[] args) {
        PlayerDataManager preferences = plugin.getPlayerDataManager();
        String arg = args.length > 1 ? args[1].toLowerCase() : null;

        if ("list".equals(arg)) {
            Set<Integer> locked = preferences.getLockedSlots(player);
            player.sendMessage(locked.isEmpty()
                    ? "§e🔓 No locked slots."
                    : "§e🔒 Locked slots: §f" + locked.stream().map(String::valueOf).collect(Collectors.joining(", ")));
            return;
        }
        if ("clear".equals(arg)) {
            preferences.setLockedSlots(player, Set.of());
            player.sendMessage("§a🔓 All slots unlocked.");
            return;
        }

        int slot;
        if (arg == null) {
            slot = player.getInventory().getHeldItemSlot();
        } else {
            try {
                slot = Integer.parseInt(arg);
            } catch (NumberFormatException e) {
                slot = -1;
            }
            if (!InputValidator.isValidLockedSlot(slot)) {
                player.sendMessage("§c🧙✨ Usage: §f/iwiz lock [slot|list|clear]§c, slot 0-8 is the hotbar and 9-35 the inventory.");
                return;
            }
        }

        boolean locked = preferences.toggleLockedSlot(player, slot);
        player.sendMessage(locked
                ? "§a🔒 Slot " + slot + " locked: sorting will leave it alone."
                : "§e🔓 Slot " + slot + " unlocked.");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = Arrays.asList("hotbar", "inventory", "all", "container", "mode", "lock");

            // Add admin commands for admins
            if (sender.hasPermission("inventorywizard.admin")) {
                options = Arrays.asList("hotbar", "inventory", "all", "container", "mode", "lock",
                        "regen-credentials", "rate-limit", "reset-rate-limit", "rate-limit-enabled");
            }
            
            return options.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        
        if (args.length == 2 && args[0].equalsIgnoreCase("mode")) {
            return Arrays.asList("default", "alphabetical", "stack-based").stream()
                    .filter(s -> s.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("lock")) {
            return Arrays.asList("list", "clear").stream()
                    .filter(s -> s.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        // tab completion for rate-limit-enabled subcommand
        if (args.length == 2 && args[0].equalsIgnoreCase("rate-limit-enabled") && sender.hasPermission("inventorywizard.admin")) {
            // suggest online player names
            return plugin.getServer().getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        
        // tab completion for rate-limit-enabled state
        if (args.length == 3 && args[0].equalsIgnoreCase("rate-limit-enabled") && sender.hasPermission("inventorywizard.admin")) {
            return Arrays.asList("on", "off").stream()
                    .filter(s -> s.startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }
        
        return null;
    }
}