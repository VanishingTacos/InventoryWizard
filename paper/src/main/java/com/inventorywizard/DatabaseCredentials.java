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
        
        // The salt isn't used to open the database, so a missing one is no reason to replace the
        // username and password (which would lock the plugin out of an existing database)
        if (salt == null || salt.isEmpty()) {
            credentialsConfig.set(SALT_KEY, generateRandomString(16, 16));
            save();
        }
        
        if (username == null || password == null || 
            username.isEmpty() || password.isEmpty()) {
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
     * Generate a new random password without storing it
     */
    public String generatePassword() {
        return generateRandomString(16, 24);
    }
    
    /**
     * Store a new password, after it has been set on the database
     * @return true if it was written to disk
     */
    public boolean savePassword(String password) {
        String oldPassword = getPassword();
        credentialsConfig.set(PASSWORD_KEY, password);
        if (save()) {
            return true;
        }
        credentialsConfig.set(PASSWORD_KEY, oldPassword);
        return false;
    }
    
    private boolean save() {
        try {
            credentialsConfig.save(credentialsFile);
            return true;
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save database credentials", e);
            return false;
        }
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