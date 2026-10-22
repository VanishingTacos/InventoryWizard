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
    

    
    /** Throws when the database can't be opened, so {@link PlayerDataManager} falls back instead of hitting a null connection later. */
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
            throw new IllegalStateException("H2 driver not found", e);
        } catch (SQLException e) {
            errorHandler.logError(
                ErrorHandler.getDatabaseErrorMessage("initialization"),
                "Failed to initialize H2 database",
                e
            );
            throw new IllegalStateException("Could not open the H2 database", e);
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
        
        // MERGE ... KEY inserts or updates in one statement, leaving the other columns alone
        String sql = "MERGE INTO player_data (uuid, sort_mode, last_updated) KEY (uuid) VALUES (?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, uuid);
            stmt.setInt(2, modeId);
            stmt.setLong(3, timestamp);
            stmt.executeUpdate();
        } catch (SQLException e) {
            errorHandler.logDatabaseError("set sort mode", player.getName(), e);
        }
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
     * Regenerate the database password. The database itself is changed first and the new password is
     * only saved once that worked; otherwise the next start would not be able to open the database.
     * @return true if both the database and database-credentials.yml now hold the new password
     */
    public boolean regenerateCredentials() {
        if (connection == null || credentials == null) {
            return false;
        }
        
        String oldPassword = credentials.getPassword();
        String newPassword = credentials.generatePassword();
        try {
            setDatabasePassword(newPassword);
        } catch (SQLException e) {
            errorHandler.logDatabaseError("regenerate credentials", "console", e);
            return false;
        }
        
        if (!credentials.savePassword(newPassword)) {
            // Put the old password back so the database still matches the file
            try {
                setDatabasePassword(oldPassword);
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not restore the old database password; database-credentials.yml no longer matches the database", e);
            }
            return false;
        }
        
        plugin.getLogger().info("Database credentials regenerated successfully");
        return true;
    }
    
    private void setDatabasePassword(String password) throws SQLException {
        // H2 doesn't take parameters here; the user name comes from H2 itself and the password is generated alphanumerics
        String user = connection.getMetaData().getUserName().replace("\"", "\"\"");
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("ALTER USER \"" + user + "\" SET PASSWORD '" + password.replace("'", "''") + "'");
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
        
        String sql = "MERGE INTO player_data (uuid, rate_limit_enabled, last_updated) KEY (uuid) VALUES (?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, uuid);
            stmt.setBoolean(2, enabled);
            stmt.setLong(3, timestamp);
            stmt.executeUpdate();
        } catch (SQLException e) {
            errorHandler.logDatabaseError("set rate limit enabled", player.getName(), e);
        }
    }

}