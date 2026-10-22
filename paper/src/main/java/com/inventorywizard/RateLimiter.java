package com.inventorywizard;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiter to prevent resource exhaustion attacks
 * Limits sorting operations per player to prevent spam
 */
public class RateLimiter {
    
    private final Map<UUID, Long> lastSortTime = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> sortCount = new ConcurrentHashMap<>();
    private final Map<UUID, Long> windowStart = new ConcurrentHashMap<>();
    
    private final PlayerDataManager playerDataManager;
    
    // Configuration
    // Short enough that MAX_SORTS_PER_MINUTE is reachable; at 10 seconds it could never trigger
    private static final long MIN_SORT_INTERVAL_MS = 1000; // 1 second between sorts
    private static final int MAX_SORTS_PER_MINUTE = 10; // Max 10 sorts per minute
    private static final long RESET_INTERVAL_MS = 60000; // Reset counter every minute
    private static final long MAX_SORT_DURATION_MS = 5000; // Max 5 seconds per sort operation
    
    public RateLimiter(PlayerDataManager playerDataManager) {
        this.playerDataManager = playerDataManager;
    }
    
    /**
     * Check if a player can perform a sort operation
     * @param player The player attempting to sort
     * @return true if allowed, false if rate limited
     */
    public boolean canSort(Player player) {
        if (player == null) {
            return false;
        }
        
        if (!playerDataManager.isPlayerRateLimited(player)) {
            return true; // no rate limiting for this player
        }
        
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        
        // Check minimum interval between sorts
        Long lastSort = lastSortTime.get(playerId);
        if (lastSort != null) {
            long timeSinceLastSort = currentTime - lastSort;
            if (timeSinceLastSort < MIN_SORT_INTERVAL_MS) {
                return false;
            }
        }
        
        // Check sort count per minute
        resetWindowIfExpired(playerId, currentTime);
        Integer count = sortCount.get(playerId);
        return count == null || count < MAX_SORTS_PER_MINUTE;
    }
    
    /**
     * Record a sort operation for rate limiting
     * @param player The player who performed the sort
     */
    public void recordSort(Player player) {
        if (player == null) {
            return;
        }
        
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        
        // Update last sort time
        lastSortTime.put(playerId, currentTime);
        
        // Update sort count for the current minute
        resetWindowIfExpired(playerId, currentTime);
        windowStart.putIfAbsent(playerId, currentTime);
        sortCount.merge(playerId, 1, Integer::sum);
        
        // Clean up old entries to prevent memory leaks
        cleanupOldEntries(currentTime);
    }
    
    /** Starts a fresh minute once the current one is over, so the count doesn't build up across minutes. */
    private void resetWindowIfExpired(UUID playerId, long currentTime) {
        Long start = windowStart.get(playerId);
        if (start != null && currentTime - start >= RESET_INTERVAL_MS) {
            windowStart.remove(playerId);
            sortCount.remove(playerId);
        }
    }
    
    /**
     * Clean up old rate limiting entries to prevent memory leaks
     * @param currentTime Current timestamp
     */
    private void cleanupOldEntries(long currentTime) {
        // Remove entries older than 5 minutes
        long cutoffTime = currentTime - (5 * 60000);
        
        lastSortTime.entrySet().removeIf(entry -> entry.getValue() < cutoffTime);
        sortCount.entrySet().removeIf(entry -> {
            UUID playerId = entry.getKey();
            Long lastSort = lastSortTime.get(playerId);
            return lastSort == null || lastSort < cutoffTime;
        });
        windowStart.entrySet().removeIf(entry -> !sortCount.containsKey(entry.getKey()));
    }
    
    /**
     * Get the time remaining before a player can sort again
     * @param player The player to check
     * @return Time in milliseconds, or 0 if they can sort now
     */
    public long getTimeUntilNextSort(Player player) {
        if (player == null) {
            return 0;
        }
        
        UUID playerId = player.getUniqueId();
        Long lastSort = lastSortTime.get(playerId);
        
        if (lastSort == null) {
            return 0;
        }
        
        long timeSinceLastSort = System.currentTimeMillis() - lastSort;
        long remaining = MIN_SORT_INTERVAL_MS - timeSinceLastSort;
        
        return Math.max(0, remaining);
    }
    
    /**
     * Get the number of sorts remaining for a player this minute
     * @param player The player to check
     * @return Number of sorts remaining, or MAX_SORTS_PER_MINUTE if unlimited
     */
    public int getSortsRemaining(Player player) {
        if (player == null) {
            return 0;
        }
        
        UUID playerId = player.getUniqueId();
        Integer count = sortCount.get(playerId);
        
        if (count == null) {
            return MAX_SORTS_PER_MINUTE;
        }
        
        return Math.max(0, MAX_SORTS_PER_MINUTE - count);
    }
    
    /**
     * Get the current sort count for a player this minute
     * @param player The player to check
     * @return Number of sorts used this minute
     */
    public int getCurrentSortCount(Player player) {
        if (player == null) {
            return 0;
        }
        
        UUID playerId = player.getUniqueId();
        Integer count = sortCount.get(playerId);
        
        return count != null ? count : 0;
    }
    
    /**
     * Reset rate limiting for a specific player (admin function)
     * @param player The player to reset
     */
    public void resetPlayer(Player player) {
        if (player == null) {
            return;
        }
        
        UUID playerId = player.getUniqueId();
        lastSortTime.remove(playerId);
        sortCount.remove(playerId);
        windowStart.remove(playerId);
    }
    
    /**
     * Get rate limiting statistics for a player
     * @param player The player to get stats for
     * @return Rate limiting info string
     */
    public String getPlayerStats(Player player) {
        if (player == null) {
            return "Invalid player";
        }
        
        UUID playerId = player.getUniqueId();
        Integer count = sortCount.get(playerId);
        
        long timeUntilNext = getTimeUntilNextSort(player);
        int sortsRemaining = getSortsRemaining(player);
        
        StringBuilder stats = new StringBuilder();
        stats.append("Rate Limiting Stats for ").append(player.getName()).append(":\n");
        stats.append("Time until next sort: ").append(timeUntilNext).append("ms\n");
        stats.append("Sorts remaining this minute: ").append(sortsRemaining).append("\n");
        stats.append("Total sorts this minute: ").append(count != null ? count : 0);
        
        return stats.toString();
    }
    
    /**
     * Check if a sort operation is taking too long (potential DoS)
     * @param startTime The time when the sort operation started
     * @return true if the operation is taking too long
     */
    public boolean isSortTakingTooLong(long startTime) {
        long currentTime = System.currentTimeMillis();
        return (currentTime - startTime) > MAX_SORT_DURATION_MS;
    }
} 