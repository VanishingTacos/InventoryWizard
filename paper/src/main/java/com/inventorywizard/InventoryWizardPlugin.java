package com.inventorywizard;

import org.bstats.bukkit.Metrics;
import org.bukkit.Sound;
import org.bukkit.block.Container;
import org.bukkit.block.DoubleChest;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.ChestBoat;
import org.bukkit.entity.Player;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.java.JavaPlugin;

public class InventoryWizardPlugin extends JavaPlugin {
    
    private static final int BSTATS_PLUGIN_ID = 34984;
    
    private PlayerDataManager playerDataManager;
    private RateLimiter rateLimiter;
    
    @Override
    public void onEnable() {
        // Initialize player data manager
        playerDataManager = new PlayerDataManager(this);
        
        // Players already online after a /reload never fire a join event
        getServer().getOnlinePlayers().forEach(playerDataManager::preload);
        
        // Initialize rate limiter
        rateLimiter = new RateLimiter(playerDataManager);
        
        // Register event listener
        getServer().getPluginManager().registerEvents(new SortListener(this), this);
        
        // Register command
        SortCommand sortCommand = new SortCommand(this);
        getCommand("iwiz").setExecutor(sortCommand);
        getCommand("iwiz").setTabCompleter(sortCommand);
        
        // Save default config
        saveDefaultConfig();
        
        // Anonymous usage stats on bstats.org; server owners can opt out in plugins/bStats/config.yml
        new Metrics(this, BSTATS_PLUGIN_ID);
        
        getLogger().info("InventoryWizard has awakened! ✨");
        getLogger().info("Commands: /iwiz [hotbar|inventory|all|container|mode]");
        getLogger().info("Hotbar: Shift+Right-click in hotbar OR Double-click an empty hotbar slot");
        getLogger().info("Inventory: Shift+Right-click in main inventory");
        getLogger().info("Both: Shift+Right-click in hotbar (with all permission)");
        getLogger().info("New: Shift+Right-click in hotbar slot 4 to cycle sorting modes!");
        getLogger().info("Storage: H2 database for optimal performance! 🚀");
        getLogger().info("Rate limiting: Enabled to prevent resource exhaustion! 🛡️");
        getLogger().info("Cast your sorting spells wisely! 🧙‍♂️");
    }
    
    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }
    
    public RateLimiter getRateLimiter() {
        return rateLimiter;
    }
    
    /** Whether a features.* switch in config.yml is on, e.g. {@code "hotbar-sorting"}. */
    public boolean isFeatureEnabled(String feature) {
        return getConfig().getBoolean("features." + feature, true);
    }
    
    public boolean allowPartialStacksInChests() {
        return getConfig().getBoolean("features.allow-partial-stacks-chest", true);
    }
    
    public boolean allowPartialStacksInInventory() {
        return getConfig().getBoolean("features.allow-partial-stacks-inventory", true);
    }
    
    public InventorySorter.HotbarPriorities getHotbarPriorities() {
        FileConfiguration config = getConfig();
        InventorySorter.HotbarPriorities defaults = InventorySorter.HotbarPriorities.DEFAULT;
        return new InventorySorter.HotbarPriorities(
                config.getInt("hotbar-priorities.weapons", defaults.weapons()),
                config.getInt("hotbar-priorities.tools", defaults.tools()),
                config.getInt("hotbar-priorities.food", defaults.food()),
                config.getInt("hotbar-priorities.building-blocks", defaults.buildingBlocks()),
                config.getInt("hotbar-priorities.utility-items", defaults.utilityItems()),
                config.getInt("hotbar-priorities.other", defaults.other()));
    }
    
    /** Whether the inventory is a real container of a kind that's turned on and the player may sort. */
    public boolean canSortContainer(Inventory inventory, Player player) {
        if (!isRealContainer(inventory, player)) {
            return false;
        }
        switch (inventory.getType()) {
            case CHEST:
                return isFeatureEnabled("chest-sorting") && player.hasPermission("inventorywizard.chest");
            case SHULKER_BOX:
                return isFeatureEnabled("shulker-sorting") && player.hasPermission("inventorywizard.shulker");
            case BARREL:
                return isFeatureEnabled("barrel-sorting") && player.hasPermission("inventorywizard.barrel");
            case ENDER_CHEST:
                return isFeatureEnabled("enderchest-sorting") && player.hasPermission("inventorywizard.enderchest");
            default:
                return false;
        }
    }

    /**
     * Whether the inventory belongs to an actual chest, barrel, shulker box, chest minecart/boat or the player's
     * ender chest, as opposed to a menu another plugin built with Bukkit.createInventory.
     */
    private static boolean isRealContainer(Inventory inventory, Player player) {
        if (inventory.getType() == InventoryType.ENDER_CHEST) {
            return inventory.equals(player.getEnderChest()) || inventory.getHolder() == player;
        }
        InventoryHolder holder = inventory.getHolder();
        return holder instanceof Container || holder instanceof DoubleChest
                || holder instanceof StorageMinecart || holder instanceof ChestBoat;
    }

    public static String containerDisplayName(InventoryType type) {
        switch (type) {
            case CHEST: return "Chest";
            case SHULKER_BOX: return "Shulker Box";
            case BARREL: return "Barrel";
            case ENDER_CHEST: return "Ender Chest";
            default: return "Container";
        }
    }

    /**
     * Runs a sort unless the player is rate limited, then plays the sound and shows the message.
     * @return false if the player was rate limited
     */
    public boolean sort(Player player, Runnable sort, float pitchOffset, String message) {
        if (!checkRateLimit(player)) {
            return false;
        }
        rateLimiter.recordSort(player);
        timedSort(player, sort, pitchOffset, message);
        return true;
    }
    
    /**
     * Like {@link #sort}, but runs the sort on the next tick. Used from inventory click events, where Bukkit
     * advises against changing inventories; the client is sent the result with {@code updateInventory()}.
     */
    public boolean sortNextTick(Player player, Runnable sort, float pitchOffset, String message) {
        if (!checkRateLimit(player)) {
            return false;
        }
        // Recorded now so a second click in the same tick is already rate limited
        rateLimiter.recordSort(player);
        getServer().getScheduler().runTask(this, () -> {
            if (player.isOnline()) {
                timedSort(player, sort, pitchOffset, message);
                player.updateInventory();
            }
        });
        return true;
    }
    
    private boolean checkRateLimit(Player player) {
        if (rateLimiter.canSort(player)) {
            return true;
        }
        long timeRemaining = rateLimiter.getTimeUntilNextSort(player);
        int sortsUsed = rateLimiter.getCurrentSortCount(player);
        player.sendMessage("§c⏰ " + ErrorHandler.getRateLimitErrorMessage(timeRemaining, sortsUsed));
        return false;
    }
    
    private void timedSort(Player player, Runnable sort, float pitchOffset, String message) {
        long startTime = System.currentTimeMillis();
        sort.run();
        if (rateLimiter.isSortTakingTooLong(startTime)) {
            getLogger().warning("Sort operation took too long for player: " + player.getName());
        }
        feedback(player, pitchOffset, message);
    }
    
    /**
     * Looks up a sound by its constant name, e.g. {@code UI_BUTTON_CLICK}, falling back to that one.
     * Sound.valueOf is marked for removal, but the constants stay, so read them as fields.
     */
    private static Sound configuredSound(String name) {
        try {
            return (Sound) Sound.class.getField(name.trim().toUpperCase(java.util.Locale.ROOT)).get(null);
        } catch (ReflectiveOperationException | ClassCastException | NullPointerException e) {
            return Sound.UI_BUTTON_CLICK;
        }
    }

    /** Plays the configured sound (pitch shifted by {@code pitchOffset}) and sends the message, each if enabled. */
    public void feedback(Player player, float pitchOffset, String message) {
        FileConfiguration config = getConfig();
        if (config.getBoolean("settings.play-sound", true)) {
            Sound sound = configuredSound(config.getString("settings.sound.type", "UI_BUTTON_CLICK"));
            float volume = (float) config.getDouble("settings.sound.volume", 0.5);
            float pitch = (float) config.getDouble("settings.sound.pitch", 1.2) + pitchOffset;
            player.playSound(player.getLocation(), sound, volume, pitch);
        }
        if (config.getBoolean("features.show-messages", true)) {
            player.sendMessage(message);
        }
    }
    
    @Override
    public void onDisable() {
        if (playerDataManager != null) {
            playerDataManager.close();
        }
        getLogger().info("InventoryWizard is resting... The magic will return! ✨");
    }
}