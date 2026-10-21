package com.inventorywizard;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.logging.Level;

/**
 * Secure credential management for H2 database
 * Generates random credentials and stores them securely
 */
public class DatabaseCredentials {
    
    private final File credentialsFile;
    private final FileConfiguration credentialsConfig;
    private final InventoryWizardPlugin plugin;
    
    private static final String USERNAME_KEY = "database.username";
    private static final String PASSWORD_KEY = "database.password";
    private static final String SALT_KEY = "database.salt";
    
    public DatabaseCredentials(InventoryWizardPlugin plugin) {
        this.plugin = plugin;
        this.credentialsFile = new File(plugin.getDataFolder(), "database-credentials.yml");
        this.credentialsConfig = YamlConfiguration.loadConfiguration(credentialsFile);
        initializeCredentials();
    }
    
    /**
     * Initialize or load database credentials
     */
    private void initializeCredentials() {
        if (!credentialsFile.exists()) {
            generateNewCredentials();
        } else {
            validateExistingCredentials();
        }
    }
    
    /**
     * Generate new secure credentials
     */
    private void generateNewCredentials() {
        try {
            // Generate random username (8-12 characters)
            String username = generateRandomString(8, 12);
            
            // Generate random password (16-24 characters)
            String password = generateRandomString(16, 24);
            
            // Generate salt for additional security
            String salt = generateRandomString(16, 16);
            
            // Store credentials
            credentialsConfig.set(USERNAME_KEY, username);
            credentialsConfig.set(PASSWORD_KEY, password);
            credentialsConfig.set(SALT_KEY, salt);
            
            credentialsConfig.save(credentialsFile);
            
            plugin.getLogger().info("Generated new secure database credentials");
            plugin.getLogger().info("Database credentials stored in: " + credentialsFile.getAbsolutePath());
            
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save database credentials", e);
            // Fallback to default credentials if file operations fail
            plugin.getLogger().warning("Using fallback credentials - database security reduced");
        }
    }
    
    /**
     * Validate existing credentials
     */
    private void validateExistingCredentials() {
        String username = credentialsConfig.getString(USERNAME_KEY);
        String password = credentialsConfig.getString(PASSWORD_KEY);
        String salt = credentialsConfig.getString(SALT_KEY);
        
        if (username == null || password == null || salt == null || 
            username.isEmpty() || password.isEmpty() || salt.isEmpty()) {
            plugin.getLogger().warning("Invalid credentials found, generating new ones");
            generateNewCredentials();
        } else {
            plugin.getLogger().info("Loaded existing database credentials");
        }
    }
    
    /**
     * Generate a random string of specified length
     */
    private String generateRandomString(int minLength, int maxLength) {
        SecureRandom random = new SecureRandom();
        int length = random.nextInt(maxLength - minLength + 1) + minLength;
        
        // Use alphanumeric characters for better compatibility
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder(length);
        
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        
        return sb.toString();
    }
    
    /**
     * Get the database username
     */
    public String getUsername() {
        return credentialsConfig.getString(USERNAME_KEY, "inventorywizard");
    }
    
    /**
     * Get the database password
     */
    public String getPassword() {
        return credentialsConfig.getString(PASSWORD_KEY, "");
    }
    
    /**
     * Get the salt for additional security
     */
    public String getSalt() {
        return credentialsConfig.getString(SALT_KEY, "");
    }
    
    /**
     * Regenerate credentials (for security purposes)
     */
    public void regenerateCredentials() {
        plugin.getLogger().info("Regenerating database credentials...");
        generateNewCredentials();
    }
    
    /**
     * Check if credentials file exists and is valid
     */
    public boolean hasValidCredentials() {
        return credentialsFile.exists() && 
               credentialsConfig.getString(USERNAME_KEY) != null &&
               credentialsConfig.getString(PASSWORD_KEY) != null &&
               !credentialsConfig.getString(USERNAME_KEY).isEmpty() &&
               !credentialsConfig.getString(PASSWORD_KEY).isEmpty();
    }
} 