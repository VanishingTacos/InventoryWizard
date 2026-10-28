package com.inventorywizard.test;

import com.inventorywizard.InventorySorter;
import com.inventorywizard.InventoryWizardPlugin;
import com.inventorywizard.PlayerDataManager;
import com.inventorywizard.PlayerDataManager.SortMode;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * Runs once the server has loaded, logs "[iwiz-test] PASS/FAIL name" per test and a summary, then stops the server.
 * Player-driven paths (clicks, /iwiz as a player) need a real client and are not covered here.
 */
public class InventoryWizardTestPlugin extends JavaPlugin implements Listener {

    private static final String TAG = "[iwiz-test] ";

    private int passed;
    private int failed;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onServerLoad(ServerLoadEvent event) {
        getLogger().info(TAG + "Running");
        try {
            runAll();
        } catch (Throwable t) {
            failed++;
            getLogger().severe(TAG + "FAIL harness: " + t);
            t.printStackTrace();
        }
        String summary = passed + " passed, " + failed + " failed";
        getLogger().info(TAG + summary);
        try {
            // Read by the runPaperTest task, since the server exits cleanly either way
            Files.writeString(Path.of("iwiz-test-result.txt"), summary);
        } catch (IOException e) {
            getLogger().severe(TAG + "could not write result file: " + e);
        }
        Bukkit.shutdown();
    }

    private void runAll() {
        InventoryWizardPlugin wizard = (InventoryWizardPlugin) Bukkit.getPluginManager().getPlugin("InventoryWizard");

        test("plugin enabled", () -> wizard != null && wizard.isEnabled());
        // getDescription() rather than Paper's getPluginMeta() so the tests also run on Spigot
        test("plugin version comes from gradle.properties", () -> !wizard.getDescription().getVersion().contains("$"));

        test("iwiz command registered with aliases", () -> {
            PluginCommand command = Bukkit.getPluginCommand("iwiz");
            return command != null && command.getPlugin() == wizard
                    && command.getAliases().containsAll(List.of("inventorywizard", "sort"));
        });
        // The client mod looks for this name in the command list to show its buttons, then sends sorts with it
        test("namespaced inventorywizard:iwiz registered for the client mod",
                () -> Bukkit.getPluginCommand("inventorywizard:iwiz") == Bukkit.getPluginCommand("iwiz"));
        test("permissions registered", () -> Bukkit.getPluginManager().getPermission("inventorywizard.use") != null
                && Bukkit.getPluginManager().getPermission("inventorywizard.admin") != null);
        test("console /iwiz is rejected without errors", () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "iwiz"));

        File data = wizard.getDataFolder();
        test("config.yml written", () -> new File(data, "config.yml").isFile());
        test("database credentials written", () -> new File(data, "database-credentials.yml").isFile());
        test("H2 database created", () -> new File(data, "player_data.mv.db").isFile());
        test("H2 driver is relocated", () -> classExists("com.inventorywizard.libs.h2.Driver", wizard)
                && !classExists("org.h2.Driver", wizard));
        test("player data manager uses H2", () -> wizard.getPlayerDataManager().hasSecureCredentials());

        test("regenerated credentials still open the database", () -> {
            File credentialsFile = new File(data, "database-credentials.yml");
            String oldPassword = YamlConfiguration.loadConfiguration(credentialsFile).getString("database.password");
            if (!wizard.getPlayerDataManager().regenerateCredentials()) {
                return false;
            }
            YamlConfiguration updated = YamlConfiguration.loadConfiguration(credentialsFile);
            String user = updated.getString("database.username");
            String newPassword = updated.getString("database.password");
            return !newPassword.equals(oldPassword)
                    && canOpenDatabase(wizard, user, newPassword)
                    && !canOpenDatabase(wizard, user, oldPassword);
        });

        test("sort modes cycle", () -> SortMode.DEFAULT.next() == SortMode.ALPHABETICAL
                && SortMode.ALPHABETICAL.next() == SortMode.STACK_BASED
                && SortMode.STACK_BASED.next() == SortMode.DEFAULT);
        // The client mod's mode button sends its enum names, e.g. "stack_based"
        test("sort modes parse by name", () -> SortMode.fromName("alphabetical") == SortMode.ALPHABETICAL
                && SortMode.fromName("stack_based") == SortMode.STACK_BASED
                && SortMode.fromName("Stack-Based") == SortMode.STACK_BASED
                && SortMode.fromName("default") == SortMode.DEFAULT
                && SortMode.fromName("bogus") == null);

        World world = Bukkit.getWorlds().getFirst();
        Map<String, Supplier<Inventory>> containers = new LinkedHashMap<>();
        containers.put("chest", () -> placeContainer(world, 0, Material.CHEST));
        containers.put("barrel", () -> placeContainer(world, 2, Material.BARREL));
        containers.put("shulker box", () -> placeContainer(world, 4, Material.SHULKER_BOX));
        containers.put("ender chest", () -> Bukkit.createInventory(null, org.bukkit.event.inventory.InventoryType.ENDER_CHEST));

        for (Map.Entry<String, Supplier<Inventory>> container : containers.entrySet()) {
            for (SortMode mode : SortMode.values()) {
                String name = container.getKey() + " / " + mode.getDisplayName();
                test(name + " keeps every item", () -> {
                    Inventory inventory = container.getValue().get();
                    fill(inventory);
                    Map<Material, Integer> before = count(inventory);
                    InventorySorter.sortInventory(inventory, mode);
                    return before.equals(count(inventory));
                });
            }
        }

        test("default mode merges and orders stacks", () -> {
            Inventory inventory = placeContainer(world, 6, Material.CHEST);
            fill(inventory);
            InventorySorter.sortInventory(inventory, SortMode.DEFAULT);
            List<ItemStack> items = contents(inventory);
            // Stone blocks sort first; 40 + 40 cobblestone merge into 64 + 16
            return items.getFirst().getType() == Material.COBBLESTONE && items.getFirst().getAmount() == 64
                    && items.get(1).getType() == Material.COBBLESTONE && items.get(1).getAmount() == 16
                    && noGaps(inventory, items.size());
        });

        test("alphabetical mode orders by name", () -> {
            Inventory inventory = placeContainer(world, 8, Material.BARREL);
            fill(inventory);
            InventorySorter.sortInventory(inventory, SortMode.ALPHABETICAL);
            List<ItemStack> items = contents(inventory);
            for (int i = 1; i < items.size(); i++) {
                if (items.get(i - 1).getType().name().compareToIgnoreCase(items.get(i).getType().name()) > 0) {
                    return false;
                }
            }
            return noGaps(inventory, items.size());
        });

        test("stack-based mode keeps stack sizes, largest first", () -> {
            Inventory inventory = placeContainer(world, 10, Material.CHEST);
            fill(inventory);
            InventorySorter.sortInventory(inventory, SortMode.STACK_BASED);
            List<ItemStack> cobble = contents(inventory).stream().filter(i -> i.getType() == Material.COBBLESTONE).toList();
            return cobble.size() == 2 && cobble.get(0).getAmount() == 40 && cobble.get(1).getAmount() == 40;
        });

        test("partial stacks off keeps every item, leftovers last", () -> {
            Inventory inventory = placeContainer(world, 14, Material.CHEST);
            fill(inventory);
            inventory.setItem(22, new ItemStack(Material.STONE, 64));
            Map<Material, Integer> before = count(inventory);
            InventorySorter.sortInventory(inventory, SortMode.DEFAULT, false);
            List<ItemStack> items = contents(inventory);
            // Full stacks (64 cobblestone, 64 stone, the sword) come before any partial stack
            int firstPartial = 0;
            while (firstPartial < items.size() && items.get(firstPartial).getAmount() == items.get(firstPartial).getMaxStackSize()) {
                firstPartial++;
            }
            boolean partialsLast = items.subList(firstPartial, items.size()).stream()
                    .allMatch(i -> i.getAmount() < i.getMaxStackSize());
            return before.equals(count(inventory)) && partialsLast && items.stream().anyMatch(i -> i.getType() == Material.DIAMOND_SWORD);
        });

        test("items with different meta are not merged", () -> {
            Inventory inventory = placeContainer(world, 16, Material.CHEST);
            ItemStack named = new ItemStack(Material.DIAMOND, 10);
            ItemMeta meta = named.getItemMeta();
            meta.setDisplayName("Shiny");
            named.setItemMeta(meta);
            inventory.setItem(0, named);
            inventory.setItem(5, new ItemStack(Material.DIAMOND, 10));
            InventorySorter.sortInventory(inventory, SortMode.DEFAULT);
            List<ItemStack> items = contents(inventory);
            return items.size() == 2 && items.stream().filter(ItemStack::hasItemMeta)
                    .filter(i -> i.getItemMeta().hasDisplayName()).count() == 1;
        });

        test("ores sort apart from stone, bowls not with weapons", () -> {
            Inventory inventory = placeContainer(world, 18, Material.CHEST);
            inventory.setItem(0, new ItemStack(Material.DEEPSLATE_IRON_ORE, 3));
            inventory.setItem(1, new ItemStack(Material.STONE_PICKAXE));
            inventory.setItem(2, new ItemStack(Material.COBBLESTONE, 3));
            inventory.setItem(3, new ItemStack(Material.BOWL, 3));
            inventory.setItem(4, new ItemStack(Material.IRON_SWORD));
            InventorySorter.sortInventory(inventory, SortMode.DEFAULT);
            List<Material> order = contents(inventory).stream().map(ItemStack::getType).toList();
            // stone 100, ore 400, tools 600, weapons 700, misc 9999
            return order.equals(List.of(Material.COBBLESTONE, Material.DEEPSLATE_IRON_ORE, Material.STONE_PICKAXE,
                    Material.IRON_SWORD, Material.BOWL));
        });

        test("sorted chest persists in the world", () -> {
            placeContainer(world, 12, Material.CHEST);
            Inventory inventory = ((Container) world.getBlockAt(12, 100, 0).getState()).getInventory();
            fill(inventory);
            Map<Material, Integer> before = count(inventory);
            InventorySorter.sortInventory(inventory, SortMode.DEFAULT);
            Inventory reread = ((Container) world.getBlockAt(12, 100, 0).getState()).getInventory();
            return before.equals(count(reread)) && reread.getItem(0) != null;
        });

        test("sorting leaves locked slots alone", () -> {
            ItemStack[] slots = new ItemStack[36];
            slots[0] = new ItemStack(Material.BREAD, 3);
            slots[2] = new ItemStack(Material.COBBLESTONE, 10);
            slots[5] = new ItemStack(Material.DIAMOND_PICKAXE);
            slots[7] = new ItemStack(Material.IRON_SWORD);
            slots[8] = new ItemStack(Material.TOTEM_OF_UNDYING);
            slots[9] = new ItemStack(Material.DIAMOND_SWORD);
            slots[15] = new ItemStack(Material.STONE, 10);
            slots[20] = new ItemStack(Material.DIRT, 20);
            slots[30] = new ItemStack(Material.DIRT, 30);
            Player player = fakePlayer(UUID.randomUUID(), slots);
            Set<Integer> locked = Set.of(5, 8, 9);

            InventorySorter.sortPlayerInventory(player, SortMode.DEFAULT, true, locked);
            InventorySorter.sortHotbar(player, SortMode.DEFAULT, InventorySorter.HotbarPriorities.DEFAULT, locked);

            List<String> hotbar = new ArrayList<>();
            for (int i = 0; i < 9; i++) {
                hotbar.add(slots[i] == null ? "-" : slots[i].getType() + " x" + slots[i].getAmount());
            }
            boolean hotbarOk = hotbar.equals(List.of("IRON_SWORD x1", "BREAD x3", "COBBLESTONE x10", "-", "-",
                    "DIAMOND_PICKAXE x1", "-", "-", "TOTEM_OF_UNDYING x1"));
            boolean inventoryOk = slots[9].getType() == Material.DIAMOND_SWORD
                    && slots[10].getType() == Material.STONE && slots[11].getType() == Material.DIRT && slots[11].getAmount() == 50
                    && Arrays.stream(slots, 12, 36).allMatch(item -> item == null);
            return hotbarOk && inventoryOk;
        });

        test("locked slots are saved in H2", () -> {
            PlayerDataManager preferences = wizard.getPlayerDataManager();
            Player player = fakePlayer(UUID.randomUUID(), new ItemStack[36]);
            boolean locked = preferences.toggleLockedSlot(player, 8) && preferences.toggleLockedSlot(player, 35);
            boolean unlocked = !preferences.toggleLockedSlot(player, 35);
            // Dropping the cache makes the next read go to the database, behind the pending writes
            preferences.forget(player);
            return locked && unlocked && preferences.getLockedSlots(player).equals(Set.of(8));
        });
    }

    /** Just enough of a Player for code that reads its id, name and inventory slots. */
    private static Player fakePlayer(UUID id, ItemStack[] slots) {
        PlayerInventory inventory = fake(PlayerInventory.class, (name, args) -> switch (name) {
            case "getItem" -> slots[(int) args[0]];
            case "setItem" -> {
                slots[(int) args[0]] = (ItemStack) args[1];
                yield null;
            }
            case "getSize" -> slots.length;
            default -> throw new UnsupportedOperationException(name);
        });
        return fake(Player.class, (name, args) -> switch (name) {
            case "getUniqueId" -> id;
            case "getName", "toString" -> "iwiz-test-player";
            case "getInventory" -> inventory;
            case "updateInventory" -> null;
            default -> throw new UnsupportedOperationException(name);
        });
    }

    private static <T> T fake(Class<T> type, BiFunction<String, Object[], Object> methods) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type},
                (proxy, method, args) -> methods.apply(method.getName(), args)));
    }

    private void test(String name, Supplier<Boolean> body) {
        boolean ok;
        String detail = "";
        try {
            ok = Boolean.TRUE.equals(body.get());
        } catch (Throwable t) {
            ok = false;
            detail = " (" + t + ")";
        }
        if (ok) {
            passed++;
            getLogger().info(TAG + "PASS " + name);
        } else {
            failed++;
            getLogger().severe(TAG + "FAIL " + name + detail);
        }
    }

    private static Inventory placeContainer(World world, int x, Material type) {
        Block block = world.getBlockAt(x, 100, 0);
        block.setType(type);
        Inventory inventory = ((Container) block.getState()).getInventory();
        inventory.clear();
        return inventory;
    }

    // Scattered, split and mixed stacks so every mode has something to merge and reorder
    private static void fill(Inventory inventory) {
        inventory.clear();
        inventory.setItem(1, new ItemStack(Material.DIAMOND_SWORD));
        inventory.setItem(3, new ItemStack(Material.COBBLESTONE, 40));
        inventory.setItem(5, new ItemStack(Material.BREAD, 7));
        inventory.setItem(8, new ItemStack(Material.OAK_PLANKS, 20));
        inventory.setItem(11, new ItemStack(Material.COBBLESTONE, 40));
        inventory.setItem(14, new ItemStack(Material.ENDER_PEARL, 3));
        inventory.setItem(17, new ItemStack(Material.BREAD, 9));
        inventory.setItem(20, new ItemStack(Material.IRON_INGOT, 5));
        inventory.setItem(26, new ItemStack(Material.ENDER_PEARL, 10));
    }

    private static Map<Material, Integer> count(Inventory inventory) {
        Map<Material, Integer> counts = new LinkedHashMap<>();
        for (ItemStack item : contents(inventory)) {
            counts.merge(item.getType(), item.getAmount(), Integer::sum);
        }
        return counts;
    }

    private static List<ItemStack> contents(Inventory inventory) {
        List<ItemStack> items = new ArrayList<>();
        for (ItemStack item : inventory.getContents()) {
            if (item != null && !item.getType().isAir()) {
                items.add(item);
            }
        }
        return items;
    }

    private static boolean noGaps(Inventory inventory, int filled) {
        for (int i = 0; i < filled; i++) {
            ItemStack item = inventory.getItem(i);
            if (item == null || item.getType().isAir()) {
                return false;
            }
        }
        return true;
    }

    /** Opens a second connection to the plugin's H2 file, the way the next server start would. */
    private static boolean canOpenDatabase(JavaPlugin plugin, String user, String password) {
        try {
            Driver driver = (Driver) Class.forName("com.inventorywizard.libs.h2.Driver", true, plugin.getClass().getClassLoader())
                    .getDeclaredConstructor().newInstance();
            Properties properties = new Properties();
            properties.setProperty("user", user);
            properties.setProperty("password", password);
            String url = "jdbc:h2:" + new File(plugin.getDataFolder(), "player_data").getAbsolutePath();
            try (Connection connection = driver.connect(url, properties)) {
                return connection != null;
            }
        } catch (SQLException e) {
            return false;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static boolean classExists(String name, JavaPlugin plugin) {
        try {
            Class.forName(name, false, plugin.getClass().getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
