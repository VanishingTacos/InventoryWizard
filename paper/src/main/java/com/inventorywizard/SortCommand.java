package com.inventorywizard;

import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SortCommand implements CommandExecutor, TabCompleter {
    
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
                
                // Check rate limiting
                if (!plugin.getRateLimiter().canSort(player)) {
                    long timeRemaining = plugin.getRateLimiter().getTimeUntilNextSort(player);
                    int sortsUsed = plugin.getRateLimiter().getCurrentSortCount(player);
                    String rateLimitMessage = ErrorHandler.getRateLimitErrorMessage(timeRemaining, sortsUsed);
                    player.sendMessage("§c⏰ " + rateLimitMessage);
                    return true;
                }
                
                long startTime = System.currentTimeMillis();
                PlayerDataManager.SortMode mode = plugin.getPlayerDataManager().getPlayerSortMode(player);
                InventorySorter.sortHotbar(player, mode);
                
                // Record the sort operation
                plugin.getRateLimiter().recordSort(player);
                
                // Check if sort took too long
                if (plugin.getRateLimiter().isSortTakingTooLong(startTime)) {
                    plugin.getLogger().warning("Sort operation took too long for player: " + player.getName());
                }
                
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.4f);
                player.sendMessage("§6✨ Hotbar enchanted by the InventoryWizard! (" + mode.getDisplayName() + ")");
                break;
                
            case "inventory":
            case "inv":
                if (!player.hasPermission("inventorywizard.inventory")) {
                    player.sendMessage("§c🧙✨ " + ErrorHandler.getPermissionErrorMessage());
                    return true;
                }
                
                // Check rate limiting
                if (!plugin.getRateLimiter().canSort(player)) {
                    long timeRemaining = plugin.getRateLimiter().getTimeUntilNextSort(player);
                    int sortsUsed = plugin.getRateLimiter().getCurrentSortCount(player);
                    String rateLimitMessage = ErrorHandler.getRateLimitErrorMessage(timeRemaining, sortsUsed);
                    player.sendMessage("§c⏰ " + rateLimitMessage);
                    return true;
                }
                
                long invStartTime = System.currentTimeMillis();
                PlayerDataManager.SortMode invMode = plugin.getPlayerDataManager().getPlayerSortMode(player);
                InventorySorter.sortPlayerInventory(player, invMode);
                
                // Record the sort operation
                plugin.getRateLimiter().recordSort(player);
                
                // Check if sort took too long
                if (plugin.getRateLimiter().isSortTakingTooLong(invStartTime)) {
                    plugin.getLogger().warning("Sort operation took too long for player: " + player.getName());
                }
                
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
                player.sendMessage("§a✨ Inventory magically organized! (" + invMode.getDisplayName() + ")");
                break;
                
            case "all":
            case "both":
                if (!player.hasPermission("inventorywizard.all")) {
                    player.sendMessage("§c🧙✨ " + ErrorHandler.getPermissionErrorMessage());
                    return true;
                }
                
                // Check rate limiting
                if (!plugin.getRateLimiter().canSort(player)) {
                    long timeRemaining = plugin.getRateLimiter().getTimeUntilNextSort(player);
                    int sortsUsed = plugin.getRateLimiter().getCurrentSortCount(player);
                    String rateLimitMessage = ErrorHandler.getRateLimitErrorMessage(timeRemaining, sortsUsed);
                    player.sendMessage("§c⏰ " + rateLimitMessage);
                    return true;
                }
                
                long allStartTime = System.currentTimeMillis();
                PlayerDataManager.SortMode allMode = plugin.getPlayerDataManager().getPlayerSortMode(player);
                InventorySorter.sortPlayerInventory(player, allMode);
                InventorySorter.sortHotbar(player, allMode);
                
                // Record the sort operation
                plugin.getRateLimiter().recordSort(player);
                
                // Check if sort took too long
                if (plugin.getRateLimiter().isSortTakingTooLong(allStartTime)) {
                    plugin.getLogger().warning("Sort operation took too long for player: " + player.getName());
                }
                
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                player.sendMessage("§b🧙✨ Complete inventory transformation complete! (" + allMode.getDisplayName() + ")");
                break;
                
            case "regen-credentials":
                if (!player.hasPermission("inventorywizard.admin")) {
                    player.sendMessage("§c🧙✨ " + ErrorHandler.getPermissionErrorMessage());
                    return true;
                }
                plugin.getPlayerDataManager().regenerateCredentials();
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
                player.sendMessage("§e🧙✨ InventoryWizard Usage: §f/iwiz [hotbar|inventory|all]");
                player.sendMessage("§7Cast your sorting spells with: hotbar, inventory, or all");
                return true;
        }
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = Arrays.asList("hotbar", "inventory", "all");
            
            // Add admin commands for admins
            if (sender.hasPermission("inventorywizard.admin")) {
                options = Arrays.asList("hotbar", "inventory", "all", "regen-credentials", "rate-limit", "reset-rate-limit", "rate-limit-enabled");
            }
            
            return options.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
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