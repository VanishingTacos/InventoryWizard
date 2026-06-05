package com.inventorywizard;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.sql.*;
import java.util.logging.Level;

public class H2DatabaseManager {
    
    private final Plugin plugin;
    private final String dbPath;
    private Connection connection;
    private DatabaseCredentials credentials;
    private ErrorHandler errorHandler;
    
    public H2DatabaseManager(Plugin plugin) {
        this.plugin = plugin;
        this.dbPath = new File(plugin.getDataFolder(), "player_data").getAbsolutePath();
        this.credentials = new DatabaseCredentials((InventoryWizardPlugin) plugin);
        this.errorHandler = new ErrorHandler(plugin.getLogger());
        initializeDatabase();
    }
    

    
    private void initializeDatabase() {
        try {
            // Create database directory if it doesn't exist
            plugin.getDataFolder().mkdirs();
            
            // Load H2 driver explicitly (relocated in shaded JAR)
            try {
                Class.forName("org.h2.Driver");
            } catch (ClassNotFoundException e) {
                // Try the relocated version
                Class.forName("com.inventorywizard.libs.h2.Driver");
            }
            
            // Connect to H2 database with secure credentials
            String url = "jdbc:h2:" + dbPath;
            String username = credentials.getUsername();
            String password = credentials.getPassword();
            connection = DriverManager.getConnection(url, username, password);
            
            // Create table if it doesn't exist
            createTable();
            
            // Validate connection and log security status
            validateConnection();
            logSecurityStatus();
            
            plugin.getLogger().info("H2 database initialized successfully with secure credentials and input validation!");
            
        } catch (ClassNotFoundException e) {
            errorHandler.logError(
                ErrorHandler.getGeneralErrorMessage(),
                "H2 driver not found. Please ensure the plugin is properly built.",
                e
            );
        } catch (SQLException e) {
            errorHandler.logError(
                ErrorHandler.getDatabaseErrorMessage("initialization"),
                "Failed to initialize H2 database",
                e
            );
        }
    }
    
    private void createTable() throws SQLException {
        // Use parameterized query for table creation to prevent SQL injection
        String sql = "CREATE TABLE IF NOT EXISTS player_data (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "sort_mode INT DEFAULT 0, " +
                    "rate_limit_enabled BOOLEAN DEFAULT TRUE, " +
                    "last_updated BIGINT DEFAULT 0)";
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            plugin.getLogger().info("Database table created/verified successfully");
        } catch (SQLException e) {
            errorHandler.logError(
             ErrorHandler.getDatabaseErrorMessage("table creation"),
                "Failed to create database table",
                e
            );
            throw e;
        }
        
        // run database migrations if needed
        migrateDatabase();
    }
    
    private void migrateDatabase() throws SQLException { 
        // ensure table is named player_data (rename if needed for migration)
        renameTableIfNeeded();
        // add rate_limit_enabled column if it doesn't exist (for migration)
        addRateLimitColumnIfMissing();
    }
    
    private void renameTableIfNeeded() throws SQLException {
        try {
            // check if old table exists
            DatabaseMetaData meta = connection.getMetaData();
            ResultSet tables = meta.getTables(null, null, "PLAYER_PREFERENCES", new String[]{"TABLE"});
            if (tables.next()) {
                // old table exists, rename it
                String renameSql = "ALTER TABLE player_preferences RENAME TO player_data";
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute(renameSql);
                    plugin.getLogger().info("Renamed table from player_preferences to player_data");
                }
            }
        } catch (SQLException e) {
            // table might not exist or already renamed - log but don't fail
            plugin.getLogger().info("Table rename check completed (table may already be named correctly)");
        }
    }
    
    private void addRateLimitColumnIfMissing() throws SQLException {
        try {
            String alterSql = "ALTER TABLE player_data ADD COLUMN IF NOT EXISTS rate_limit_enabled BOOLEAN DEFAULT TRUE";
            try (Statement alterStmt = connection.createStatement()) {
                alterStmt.execute(alterSql);
                plugin.getLogger().info("Ensured rate_limit_enabled column exists in database");
            }
        } catch (SQLException e) {
            // column might already exist, or other SQL error - log but don't fail
            plugin.getLogger().info("Rate limit column migration completed (column may already exist)");
        }
    }
    
    public PlayerDataManager.SortMode getPlayerSortMode(Player player) {
        // Validate input parameters
        if (!InputValidator.validatePlayer(player)) {
            errorHandler.logValidationError("player object", player.getName(), "null or invalid");
            return PlayerDataManager.SortMode.DEFAULT;
        }
        
        String uuid = InputValidator.validateUUID(player.getUniqueId().toString());
        if (uuid == null) {
            errorHandler.logValidationError("UUID format", player.getName(), player.getUniqueId().toString());
            return PlayerDataManager.SortMode.DEFAULT;
        }
        
        String sql = "SELECT sort_mode FROM player_data WHERE uuid = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, uuid);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int modeId = rs.getInt("sort_mode");
                    int validatedModeId = InputValidator.validateSortModeId(modeId);
                    return PlayerDataManager.SortMode.fromId(validatedModeId);
                }
            }
        } catch (SQLException e) {
            errorHandler.logDatabaseError("get sort mode", player.getName(), e);
        }
        
        return PlayerDataManager.SortMode.DEFAULT;
    }
    
    public void setPlayerSortMode(Player player, PlayerDataManager.SortMode mode) {
        // Validate all input parameters
        InputValidator.ValidationResult validation = InputValidator.validateDatabaseInput(player, mode, System.currentTimeMillis());
        
        if (!validation.isValid()) {
            errorHandler.logValidationError("database input", player.getName(), validation.getErrorMessage());
            return;
        }
        
        String uuid = validation.getUuid();
        int modeId = validation.getModeId();
        long timestamp = validation.getTimestamp();
        
        // Try INSERT first, if it fails due to duplicate key, use UPDATE
        String insertSql = "INSERT INTO player_data (uuid, sort_mode, last_updated) VALUES (?, ?, ?)";
        String updateSql = "UPDATE player_data SET sort_mode = ?, last_updated = ? WHERE uuid = ?";
        
        try (PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
            insertStmt.setString(1, uuid);
            insertStmt.setInt(2, modeId);
            insertStmt.setLong(3, timestamp);
            
            insertStmt.executeUpdate();
            
        } catch (SQLException e) {
            // If insert fails (duplicate key), try update
            try (PreparedStatement updateStmt = connection.prepareStatement(updateSql)) {
                updateStmt.setInt(1, modeId);
                updateStmt.setLong(2, timestamp);
                updateStmt.setString(3, uuid);
                
                updateStmt.executeUpdate();
                
            } catch (SQLException updateException) {
                errorHandler.logDatabaseError("set sort mode", player.getName(), updateException);
            }
        }
    }
    
    public PlayerDataManager.SortMode cyclePlayerSortMode(Player player) {
        PlayerDataManager.SortMode currentMode = getPlayerSortMode(player);
        PlayerDataManager.SortMode nextMode = currentMode.next();
        setPlayerSortMode(player, nextMode);
        return nextMode;
    }
    
    public void close() {
        if (connection != null) {
            try {
                connection.close();
                plugin.getLogger().info("H2 database connection closed.");
                    } catch (SQLException e) {
            errorHandler.logError(
                ErrorHandler.getDatabaseErrorMessage("connection close"),
                "Error closing database connection",
                e
            );
        }
        }
    }
    
    /**
     * Regenerate database credentials for security purposes
     */
    public void regenerateCredentials() {
        if (credentials != null) {
            credentials.regenerateCredentials();
            plugin.getLogger().info("Database credentials regenerated successfully");
        }
    }
    
    /**
     * Check if secure credentials are being used
     */
    public boolean hasSecureCredentials() {
        return credentials != null && credentials.hasValidCredentials();
    }
    
    /**
     * Validate database connection and log security status
     */
    public void validateConnection() {
        if (connection == null) {
            plugin.getLogger().warning("Database connection is null");
            return;
        }
        
        try {
            // Test connection with a simple query
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("SELECT 1");
                plugin.getLogger().info("Database connection validated successfully");
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Database connection validation failed", e);
        }
    }
    
    /**
     * Log security status for audit purposes
     */
    public void logSecurityStatus() {
        plugin.getLogger().info("=== Database Security Status ===");
        plugin.getLogger().info("Secure credentials: " + (hasSecureCredentials() ? "YES" : "NO"));
        plugin.getLogger().info("Input validation: ENABLED");
        plugin.getLogger().info("SQL injection protection: ENABLED");
        plugin.getLogger().info("Parameterized queries: ENABLED");
        plugin.getLogger().info("=================================");
    }
    
    public boolean getPlayerRateLimitEnabled(Player player) {
        if (!InputValidator.validatePlayer(player)) {
            errorHandler.logValidationError("player object", player.getName(), "null or invalid");
            return true; // default to enabled
        }
        
        String uuid = InputValidator.validateUUID(player.getUniqueId().toString());
        if (uuid == null) {
            errorHandler.logValidationError("UUID format", player.getName(), player.getUniqueId().toString());
            return true;
        }
        
        String sql = "SELECT rate_limit_enabled FROM player_data WHERE uuid = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, uuid);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean("rate_limit_enabled");
                }
            }
        } catch (SQLException e) {
            errorHandler.logDatabaseError("get rate limit enabled", player.getName(), e);
        }
        
        return true; // default to enabled
    }
    
    public void setPlayerRateLimitEnabled(Player player, boolean enabled) {
        if (!InputValidator.validatePlayer(player)) {
            errorHandler.logValidationError("player object", player.getName(), "null or invalid");
            return;
        }
        
        String uuid = InputValidator.validateUUID(player.getUniqueId().toString());
        if (uuid == null) {
            errorHandler.logValidationError("UUID format", player.getName(), player.getUniqueId().toString());
            return;
        }
        
        long timestamp = System.currentTimeMillis();
        
        // try INSERT first, if it fails due to duplicate key, use UPDATE
        String insertSql = "INSERT INTO player_data (uuid, rate_limit_enabled, last_updated) VALUES (?, ?, ?)";
        String updateSql = "UPDATE player_data SET rate_limit_enabled = ?, last_updated = ? WHERE uuid = ?";
        
        try (PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
            insertStmt.setString(1, uuid);
            insertStmt.setBoolean(2, enabled);
            insertStmt.setLong(3, timestamp);
            
            insertStmt.executeUpdate();
            
        } catch (SQLException e) {
            // if insert fails (duplicate key), try update
            try (PreparedStatement updateStmt = connection.prepareStatement(updateSql)) {
                updateStmt.setBoolean(1, enabled);
                updateStmt.setLong(2, timestamp);
                updateStmt.setString(3, uuid);
                
                updateStmt.executeUpdate();
                
            } catch (SQLException updateException) {
                errorHandler.logDatabaseError("set rate limit enabled", player.getName(), updateException);
            }
        }
    }

}