package com.inventorywizard;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class PlayerDataManager {
    
    public enum SortMode {
        DEFAULT(0, "Default"),
        ALPHABETICAL(1, "Alphabetical"),
        STACK_BASED(2, "Stack-based");
        
        private final int id;
        private final String displayName;
        
        SortMode(int id, String displayName) {
            this.id = id;
            this.displayName = displayName;
        }
        
        public int getId() {
            return id;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public static SortMode fromId(int id) {
            for (SortMode mode : values()) {
                if (mode.id == id) {
                    return mode;
                }
            }
            return DEFAULT;
        }
        
        public SortMode next() {
            int nextId = (this.id + 1) % values().length;
            return fromId(nextId);
        }
    }
    
    private final InventoryWizardPlugin plugin;
    private H2DatabaseManager database;

    // Preferences of players seen this session. With H2 they are loaded once and written back in the
    // background, so clicks never wait on the database; without H2 they are lost on restart.
    private final Map<UUID, SortMode> sortModes = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> rateLimited = new ConcurrentHashMap<>();

    // Every database call runs on this one thread, so the shared connection is never used concurrently
    private ExecutorService databaseThread;

    public PlayerDataManager(InventoryWizardPlugin plugin) {
        this.plugin = plugin;
        try {
            this.database = new H2DatabaseManager(plugin);
            this.databaseThread = Executors.newSingleThreadExecutor(runnable -> {
                Thread thread = new Thread(runnable, "InventoryWizard-Database");
                thread.setDaemon(true);
                return thread;
            });
            plugin.getLogger().info("Using H2 database for player preferences.");
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to initialize H2 database (" + e.getMessage()
                    + "), keeping player preferences in memory until the next restart.");
            this.database = null;
        }
    }

    /** Loads a player's preferences in the background, e.g. when they join, so the first sort doesn't wait for H2. */
    public void preload(Player player) {
        if (database == null) {
            return;
        }
        databaseThread.execute(() -> {
            // putIfAbsent: a change made while this was loading wins over the stored value
            sortModes.putIfAbsent(player.getUniqueId(), database.getPlayerSortMode(player));
            rateLimited.putIfAbsent(player.getUniqueId(), database.getPlayerRateLimitEnabled(player));
        });
    }

    /** Drops a player's cached preferences, e.g. when they leave; pending writes still go through. */
    public void forget(Player player) {
        sortModes.remove(player.getUniqueId());
        rateLimited.remove(player.getUniqueId());
    }

    public SortMode getPlayerSortMode(Player player) {
        SortMode cached = sortModes.get(player.getUniqueId());
        if (cached != null) {
            return cached;
        }
        if (database == null) {
            return SortMode.DEFAULT;
        }
        // Not preloaded yet (e.g. after a reload): load it once now
        SortMode loaded = onDatabaseThread(() -> database.getPlayerSortMode(player), SortMode.DEFAULT);
        SortMode previous = sortModes.putIfAbsent(player.getUniqueId(), loaded);
        return previous != null ? previous : loaded;
    }

    public void setPlayerSortMode(Player player, SortMode mode) {
        sortModes.put(player.getUniqueId(), mode);
        if (database != null) {
            databaseThread.execute(() -> database.setPlayerSortMode(player, mode));
        }
    }

    public SortMode cyclePlayerSortMode(Player player) {
        SortMode next = getPlayerSortMode(player).next();
        setPlayerSortMode(player, next);
        return next;
    }

    public boolean isPlayerRateLimited(Player player) {
        Boolean cached = rateLimited.get(player.getUniqueId());
        if (cached != null) {
            return cached;
        }
        if (database == null) {
            return true; // default to rate limited
        }
        boolean loaded = onDatabaseThread(() -> database.getPlayerRateLimitEnabled(player), true);
        Boolean previous = rateLimited.putIfAbsent(player.getUniqueId(), loaded);
        return previous != null ? previous : loaded;
    }

    public void setPlayerRateLimited(Player player, boolean enabled) {
        rateLimited.put(player.getUniqueId(), enabled);
        if (database != null) {
            databaseThread.execute(() -> database.setPlayerRateLimitEnabled(player, enabled));
        }
    }

    /** Finishes pending writes, then closes the database. */
    public void close() {
        if (database == null) {
            return;
        }
        databaseThread.shutdown();
        try {
            if (!databaseThread.awaitTermination(10, TimeUnit.SECONDS)) {
                plugin.getLogger().warning("Timed out waiting for player preferences to be saved");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        database.close();
    }

    /**
     * Check if the database is using secure credentials
     */
    public boolean hasSecureCredentials() {
        return database != null && database.hasSecureCredentials();
    }

    /**
     * Regenerate database credentials for security purposes
     */
    public boolean regenerateCredentials() {
        return database != null && onDatabaseThread(database::regenerateCredentials, false);
    }

    /** Runs a database call on the database thread and waits for it, behind any pending writes. */
    private <T> T onDatabaseThread(Callable<T> call, T fallback) {
        try {
            return databaseThread.submit(call).get();
        } catch (ExecutionException e) {
            plugin.getLogger().log(Level.WARNING, "Database call failed", e.getCause());
            return fallback;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return fallback;
        }
    }

}
