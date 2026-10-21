package com.inventorywizard;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Secure error handling to prevent information disclosure
 * Sanitizes error messages while maintaining useful logging
 */
public class ErrorHandler {
    
    private final Logger logger;
    
    public ErrorHandler(Logger logger) {
        this.logger = logger;
    }
    
    /**
     * Log a secure error message for users (no sensitive information)
     * @param userMessage User-friendly error message
     * @param detailedMessage Detailed message for server logs only
     * @param exception The exception that occurred (optional)
     */
    public void logError(String userMessage, String detailedMessage, Exception exception) {
        // Log detailed information for administrators
        if (exception != null) {
            logger.log(Level.WARNING, detailedMessage, exception);
        } else {
            logger.warning(detailedMessage);
        }
    }
    
    /**
     * Log a secure error message for users (no sensitive information)
     * @param userMessage User-friendly error message
     * @param detailedMessage Detailed message for server logs only
     */
    public void logSecureError(String userMessage, String detailedMessage) {
        logError(userMessage, detailedMessage, null);
    }
    
    /**
     * Get a user-friendly error message for database operations
     * @param operation The operation that failed
     * @return User-friendly error message
     */
    public static String getDatabaseErrorMessage(String operation) {
        return "Database operation failed: " + operation + ". Please contact an administrator.";
    }
    
    /**
     * Get a user-friendly error message for permission issues
     * @return User-friendly error message
     */
    public static String getPermissionErrorMessage() {
        return "You don't have permission to perform this action.";
    }
    
    /**
     * Get a user-friendly error message for validation issues
     * @param field The field that failed validation
     * @return User-friendly error message
     */
    public static String getValidationErrorMessage(String field) {
        return "Invalid " + field + " provided. Please try again.";
    }
    
    /**
     * Get a user-friendly error message for rate limiting
     * @param timeRemaining Time remaining in milliseconds
     * @param sortsUsed Sorts used this minute
     * @return User-friendly error message
     */
    public static String getRateLimitErrorMessage(long timeRemaining, int sortsUsed) {
        if (sortsUsed >= 10) {
            return "Rate limited! You've used all " + sortsUsed + " sorts this minute.";
        } else if (timeRemaining > 0) {
            return "Rate limited! Please wait " + (timeRemaining / 1000) + " seconds before trying again.";
        } else {
            return "Rate limited! Please wait a moment before trying again.";
        }
    }
    
    /**
     * Get a user-friendly error message for general errors
     * @return User-friendly error message
     */
    public static String getGeneralErrorMessage() {
        return "An error occurred. Please try again later.";
    }
    
    /**
     * Sanitize an exception message for logging (remove sensitive info)
     * @param exception The exception to sanitize
     * @return Sanitized message
     */
    public static String sanitizeExceptionMessage(Exception exception) {
        if (exception == null) {
            return "Unknown error";
        }
        
        String message = exception.getMessage();
        if (message == null) {
            return exception.getClass().getSimpleName();
        }
        
        // Remove potential sensitive information
        message = message.replaceAll("(?i)password", "[PASSWORD]");
        message = message.replaceAll("(?i)secret", "[SECRET]");
        message = message.replaceAll("(?i)key", "[KEY]");
        message = message.replaceAll("(?i)token", "[TOKEN]");
        message = message.replaceAll("(?i)credential", "[CREDENTIAL]");
        
        // Remove file paths that might reveal system structure
        message = message.replaceAll("[/\\\\][^/\\\\]*\\.(db|sql|yml|yaml|properties)", "[CONFIG_FILE]");
        
        // Remove IP addresses
        message = message.replaceAll("\\b(?:[0-9]{1,3}\\.){3}[0-9]{1,3}\\b", "[IP_ADDRESS]");
        
        // Limit message length
        if (message.length() > 200) {
            message = message.substring(0, 197) + "...";
        }
        
        return message;
    }
    
    /**
     * Log a database error securely
     * @param operation The database operation that failed
     * @param playerName The player name (for context)
     * @param exception The database exception
     */
    public void logDatabaseError(String operation, String playerName, Exception exception) {
        String detailedMessage = "Database error during " + operation + " for player: " + playerName;
        String sanitizedMessage = sanitizeExceptionMessage(exception);
        
        logger.log(Level.WARNING, detailedMessage + " - " + sanitizedMessage, exception);
    }
    
    /**
     * Log a validation error securely
     * @param field The field that failed validation
     * @param playerName The player name (for context)
     * @param value The invalid value (sanitized)
     */
    public void logValidationError(String field, String playerName, String value) {
        String message = "Validation error for " + field + " from player: " + playerName + " (value: " + value + ")";
        logger.warning(message);
    }
    
    /**
     * Log a rate limiting event
     * @param playerName The player name
     * @param reason The reason for rate limiting
     */
    public void logRateLimitEvent(String playerName, String reason) {
        String message = "Rate limiting applied to player: " + playerName + " - " + reason;
        logger.info(message);
    }
} 