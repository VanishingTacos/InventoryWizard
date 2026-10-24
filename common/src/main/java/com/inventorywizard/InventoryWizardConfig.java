package com.inventorywizard;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.inventorywizard.platform.Platform;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;

/**
 * Settings stored in config/inventorywizard.json. Replaces the plugin's config.yml
 * and the per-player H2 database (the per-player values are the sort mode and locked slots).
 */
public class InventoryWizardConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = Platform.INSTANCE.configDir().resolve("inventorywizard.json");

    public SortMode sortMode = SortMode.DEFAULT;

    public boolean playSound = true;
    public Sound sound = new Sound();

    public boolean showMessages = true;
    /** Sort and mode buttons in the inventory, chest and shulker box screens. */
    public boolean showButtons = true;
    public Features features = new Features();

    /** When false, leftover partial stacks are placed after all full stacks instead of being sorted in. */
    public boolean allowPartialStacksChest = true;
    public boolean allowPartialStacksInventory = true;

    public HotbarPriorities hotbarPriorities = new HotbarPriorities();

    /** Player inventory slots (0-8 hotbar, 9-35 main) that sorting never moves. Toggled with Alt+click. */
    public Set<Integer> lockedSlots = new TreeSet<>();

    /** Look up the newest release on Modrinth at startup and mention it in chat. */
    public boolean checkForUpdates = true;

    public static class Sound {
        public String id = "minecraft:ui.button.click";
        public float volume = 0.5f;
        public float pitch = 1.2f;
    }

    public static class Features {
        public boolean chestSorting = true;
        public boolean shulkerSorting = true;
        public boolean barrelSorting = true;
        public boolean enderChestSorting = true;
        public boolean inventorySorting = true;
        public boolean hotbarSorting = true;
    }

    public static class HotbarPriorities {
        public int weapons = 100;
        public int tools = 200;
        public int food = 300;
        public int buildingBlocks = 400;
        public int utilityItems = 500;
        public int other = 999;
    }

    public static InventoryWizardConfig load() {
        InventoryWizardConfig config = null;
        if (Files.exists(PATH)) {
            try (Reader reader = Files.newBufferedReader(PATH)) {
                config = GSON.fromJson(reader, InventoryWizardConfig.class);
            } catch (IOException | JsonParseException e) {
                InventoryWizard.LOGGER.warn("Could not read {}, using defaults", PATH, e);
            }
        }
        if (config == null) {
            config = new InventoryWizardConfig();
        }
        config.fillMissing();
        config.save();
        return config;
    }

    public void save() {
        try {
            Files.createDirectories(PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(PATH)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            InventoryWizard.LOGGER.warn("Could not save {}", PATH, e);
        }
    }

    // Gson leaves fields null when they are missing from an older or hand-edited file
    private void fillMissing() {
        if (sortMode == null) sortMode = SortMode.DEFAULT;
        if (sound == null) sound = new Sound();
        if (sound.id == null) sound.id = new Sound().id;
        if (features == null) features = new Features();
        if (hotbarPriorities == null) hotbarPriorities = new HotbarPriorities();
        if (lockedSlots == null) lockedSlots = new TreeSet<>();
    }
}
