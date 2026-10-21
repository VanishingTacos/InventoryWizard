package com.inventorywizard;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Input validation and sanitization for database operations
 * Prevents SQL injection and ensures data integrity
 */
public class InputValidator {
    
    // UUID pattern for validation
    private static final Pattern UUID_PATTERN = Pattern.compile(
        "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
    );
    
    // Maximum values for validation
    private static final int MAX_SORT_MODE_ID = 10; // Reasonable upper limit
    private static final long MAX_TIMESTAMP = System.currentTimeMillis() + (365L * 24 * 60 * 60 * 1000); // 1 year in future
    private static final long MIN_TIMESTAMP = 0L;
    
    /**
     * Validate and sanitize UUID string
     * @param uuidString The UUID string to validate
     * @return Validated UUID string or null if invalid
     */
    public static String validateUUID(String uuidString) {
        if (uuidString == null || uuidString.trim().isEmpty()) {
            return null;
        }
        
        String trimmed = uuidString.trim();
        
        // Check if it matches UUID pattern
        if (!UUID_PATTERN.matcher(trimmed).matches()) {
            return null;
        }
        
        // Try to parse as UUID to ensure it's valid
        try {
            UUID.fromString(trimmed);
            return trimmed;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    
    /**
     * Validate sort mode ID
     * @param modeId The sort mode ID to validate
     * @return Validated mode ID or default value if invalid
     */
    public static int validateSortModeId(int modeId) {
        if (modeId < 0 || modeId > MAX_SORT_MODE_ID) {
            return 0; // Default mode
        }
        return modeId;
    }
    
    /**
     * Validate timestamp
     * @param timestamp The timestamp to validate
     * @return Validated timestamp or current time if invalid
     */
    public static long validateTimestamp(long timestamp) {
        if (timestamp < MIN_TIMESTAMP || timestamp > MAX_TIMESTAMP) {
            return System.currentTimeMillis();
        }
        return timestamp;
    }
    
    /**
     * Validate player object
     * @param player The player object to validate
     * @return true if player is valid, false otherwise
     */
    public static boolean validatePlayer(org.bukkit.entity.Player player) {
        return player != null && 
               player.getUniqueId() != null && 
               player.getName() != null && 
               !player.getName().trim().isEmpty();
    }
    
    /**
     * Sanitize string input for database operations
     * @param input The input string to sanitize
     * @return Sanitized string or null if invalid
     */
    public static String sanitizeString(String input) {
        if (input == null) {
            return null;
        }
        
        String sanitized = input.trim();
        
        // Remove any potential SQL injection characters
        sanitized = sanitized.replaceAll("['\"`;]", "");
        
        // Limit length to prevent buffer overflow
        if (sanitized.length() > 255) {
            sanitized = sanitized.substring(0, 255);
        }
        
        return sanitized.isEmpty() ? null : sanitized;
    }
    
    /**
     * Validate and sanitize all database input parameters
     * @param player The player object
     * @param mode The sort mode
     * @param timestamp The timestamp
     * @return ValidationResult containing validated parameters or error message
     */
    public static ValidationResult validateDatabaseInput(org.bukkit.entity.Player player, 
                                                       PlayerDataManager.SortMode mode, 
                                                       long timestamp) {
        // Validate player
        if (!validatePlayer(player)) {
            return new ValidationResult(false, "Invalid player object");
        }
        
        // Validate UUID
        String uuid = validateUUID(player.getUniqueId().toString());
        if (uuid == null) {
            return new ValidationResult(false, "Invalid UUID format");
        }
        
        // Validate sort mode
        if (mode == null) {
            return new ValidationResult(false, "Invalid sort mode");
        }
        
        int modeId = validateSortModeId(mode.getId());
        
        // Validate timestamp
        long validTimestamp = validateTimestamp(timestamp);
        
        return new ValidationResult(true, null, uuid, modeId, validTimestamp);
    }
    
    /**
     * Result of input validation
     */
    public static class ValidationResult {
        private final boolean isValid;
        private final String errorMessage;
        private final String uuid;
        private final int modeId;
        private final long timestamp;
        
        public ValidationResult(boolean isValid, String errorMessage) {
            this.isValid = isValid;
            this.errorMessage = errorMessage;
            this.uuid = null;
            this.modeId = 0;
            this.timestamp = 0;
        }
        
        public ValidationResult(boolean isValid, String errorMessage, String uuid, int modeId, long timestamp) {
            this.isValid = isValid;
            this.errorMessage = errorMessage;
            this.uuid = uuid;
            this.modeId = modeId;
            this.timestamp = timestamp;
        }
        
        public boolean isValid() {
            return isValid;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
        
        public String getUuid() {
            return uuid;
        }
        
        public int getModeId() {
            return modeId;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
    }
} 