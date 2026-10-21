package com.inventorywizard.gametest;

import com.inventorywizard.InventoryWizard;
import com.inventorywizard.InventoryWizardConfig;
import com.inventorywizard.InventoryWizardConfigScreen;
import com.inventorywizard.SortMode;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Predicate;

/**
 * The same scenarios as the Fabric client gametest (fabric/src/gametest), driven through
 * {@link ClientTestContext} so they run on NeoForge and Forge. Each loader's own screen events
 * deliver the simulated clicks and key presses, so this exercises its entrypoint wiring too.
 */
final class InventoryWizardClientGameTests {

    private static final int SORT_KEY = InputConstants.KEY_R;
    private static final int SORT_WAIT_TICKS = 5;

    private InventoryWizardClientGameTests() {}

    static void run(ClientTestContext context) {
        context.runOnClient(client -> {
            InventoryWizard.config().sortMode = SortMode.DEFAULT;
            InventoryWizard.config().save();
        });

        test("config screen", () -> testConfigScreen(context));

        context.createWorld("inventorywizard-gametest");
        test("inventory Shift+Right-click", () -> testInventoryShiftRightClick(context));
        test("sort key on hotbar", () -> testSortKeyOnHotbar(context));
        test("cycle mode slot", () -> testCycleModeSlot(context));
        test("partial stacks off", () -> testPartialStacksOff(context));
        test("chest", () -> testBlockContainer(context, Blocks.CHEST, new BlockPos(2, 0, 0), ContainerScreen.class, Input.SORT_KEY_NO_HOVER, "chest"));
        test("barrel", () -> testBlockContainer(context, Blocks.BARREL, new BlockPos(0, 0, 2), ContainerScreen.class, Input.SHIFT_RIGHT_CLICK, "barrel"));
        test("shulker box", () -> testBlockContainer(context, Blocks.SHULKER_BOX, new BlockPos(0, 0, -2), ShulkerBoxScreen.class, Input.SORT_KEY_HOVER, "shulker_box"));
        test("ender chest", () -> testEnderChest(context));
        test("disabled feature", () -> testDisabledFeature(context));
        context.leaveWorld();
    }

    private static void test(String name, Runnable body) {
        InventoryWizard.LOGGER.info("[gametest] Running: {}", name);
        body.run();
        InventoryWizard.LOGGER.info("[gametest] Passed: {}", name);
    }

    // ---- settings screen -----------------------------------------------------

    private static void testConfigScreen(ClientTestContext context) {
        context.runOnClient(client -> InventoryWizard.config().showMessages = true);

        context.setScreen(() -> new InventoryWizardConfigScreen(new TitleScreen()));
        context.waitForScreen(InventoryWizardConfigScreen.class);

        context.clickWidget("Chat Messages");
        boolean toggled = context.computeOnClient(client -> InventoryWizard.config().showMessages);
        assertEquals("show messages after clicking its button", false, toggled);

        context.clickWidget(Component.translatable("gui.done").getString());
        context.waitForScreen(TitleScreen.class);
        boolean reloaded = context.computeOnClient(client -> InventoryWizardConfig.load().showMessages);
        assertEquals("show messages saved when the screen closes", false, reloaded);

        context.runOnClient(client -> {
            InventoryWizard.config().showMessages = true;
            InventoryWizard.config().save();
        });
    }

    // ---- singleplayer tests -------------------------------------------------

    private static void testInventoryShiftRightClick(ClientTestContext context) {
        context.runOnServer(s -> {
            Inventory inv = player(s).getInventory();
            inv.clearContent();
            inv.setItem(9, new ItemStack(Items.DIRT, 20));
            inv.setItem(12, new ItemStack(Items.DIAMOND_SWORD));
            inv.setItem(15, new ItemStack(Items.STONE, 10));
            inv.setItem(20, new ItemStack(Items.DIRT, 30));
            inv.setItem(22, named(Items.DIAMOND_SWORD, "Excalibur"));
            inv.setItem(25, new ItemStack(Items.BREAD, 5));
            inv.setItem(30, new ItemStack(Items.DIRT, 50));
            inv.setItem(0, new ItemStack(Items.BREAD, 3));
            inv.setItem(2, new ItemStack(Items.COBBLESTONE, 10));
            inv.setItem(5, new ItemStack(Items.DIAMOND_PICKAXE));
            inv.setItem(7, new ItemStack(Items.IRON_SWORD));
        });
        Map<String, Integer> before = totals(context);

        openInventory(context);
        context.hoverSlot(inventorySlot(15));
        shiftRightClick(context);

        List<String> main = inventoryRange(context, 9, 36);
        assertPrefix("inventory shift+right-click", main,
                "stone x10", "dirt x64", "dirt x36", "diamond_sword x1", "diamond_sword* x1", "bread x5");
        assertEmptyFrom("inventory shift+right-click", main, 6);
        assertEquals("hotbar untouched by inventory sort",
                List.of("bread x3", "-", "cobblestone x10", "-", "-", "diamond_pickaxe x1", "-", "iron_sword x1", "-"),
                inventoryRange(context, 0, 9));
        assertEquals("item totals after inventory sort", before, totals(context));
    }

    private static void testSortKeyOnHotbar(ClientTestContext context) {
        Map<String, Integer> before = totals(context);

        context.hoverSlot(inventorySlot(1));
        context.pressKey(SORT_KEY);
        context.waitTicks(SORT_WAIT_TICKS);

        List<String> hotbar = inventoryRange(context, 0, 9);
        assertPrefix("sort key on hotbar", hotbar, "iron_sword x1", "diamond_pickaxe x1", "bread x3", "cobblestone x10");
        assertEmptyFrom("sort key on hotbar", hotbar, 4);
        assertEquals("item totals after hotbar sort", before, totals(context));
    }

    private static void testCycleModeSlot(ClientTestContext context) {
        List<String> before = inventoryRange(context, 0, 36);

        context.hoverSlot(inventorySlot(4));
        shiftRightClick(context);

        SortMode mode = context.computeOnClient(client -> InventoryWizard.config().sortMode);
        assertEquals("mode after Shift+Right-click on hotbar slot 4", SortMode.ALPHABETICAL, mode);
        assertEquals("cycling the mode must not move items", before, inventoryRange(context, 0, 36));

        // Alphabetical mode: sort the main inventory by item id
        context.hoverSlot(inventorySlot(20));
        shiftRightClick(context);
        List<String> main = inventoryRange(context, 9, 36);
        assertPrefix("alphabetical sort", main,
                "bread x5", "diamond_sword x1", "diamond_sword* x1", "dirt x64", "dirt x36", "stone x10");

        context.runOnClient(client -> InventoryWizard.config().sortMode = SortMode.DEFAULT);
        closeScreen(context);
    }

    private static void testPartialStacksOff(ClientTestContext context) {
        context.runOnClient(client -> InventoryWizard.config().allowPartialStacksInventory = false);
        context.runOnServer(s -> {
            Inventory inv = player(s).getInventory();
            inv.clearContent();
            inv.setItem(10, new ItemStack(Items.DIRT, 40));
            inv.setItem(11, new ItemStack(Items.STONE, 5));
            inv.setItem(30, new ItemStack(Items.DIRT, 30));
        });
        Map<String, Integer> before = totals(context);

        openInventory(context);
        context.hoverSlot(inventorySlot(10));
        shiftRightClick(context);

        List<String> main = inventoryRange(context, 9, 36);
        assertPrefix("partial stacks off", main, "dirt x64", "stone x5", "dirt x6");
        assertEmptyFrom("partial stacks off", main, 3);
        assertEquals("partial stacks off keeps every item", before, totals(context));

        context.runOnClient(client -> InventoryWizard.config().allowPartialStacksInventory = true);
        closeScreen(context);
    }

    private enum Input {
        SHIFT_RIGHT_CLICK,
        SORT_KEY_HOVER,
        SORT_KEY_NO_HOVER
    }

    private static void testBlockContainer(ClientTestContext context, Block block, BlockPos offset,
                                           Class<? extends AbstractContainerScreen<?>> screenClass, Input input, String name) {
        BlockPos pos = context.computeOnServer(s -> {
            ServerPlayer player = player(s);
            BlockPos p = player.blockPosition().offset(offset);
            ServerLevel level = (ServerLevel) player.level();
            level.setBlockAndUpdate(p, block.defaultBlockState());
            Container container = (Container) level.getBlockEntity(p);
            fillScrambled(container);
            player.openMenu((MenuProvider) container);
            return p;
        });
        context.waitForScreen(screenClass);

        sortOpenContainer(context, input);

        List<String> items = context.computeOnServer(s -> describe((Container) s.overworld().getBlockEntity(pos)));
        assertSortedScrambled(name, items);
        closeScreen(context);
    }

    private static void testEnderChest(ClientTestContext context) {
        context.runOnServer(s -> {
            ServerPlayer player = player(s);
            fillScrambled(player.getEnderChestInventory());
            player.openMenu(new SimpleMenuProvider(
                    (id, inventory, p) -> ChestMenu.threeRows(id, inventory, p.getEnderChestInventory()),
                    Component.literal("Ender Chest")));
        });
        context.waitForScreen(ContainerScreen.class);

        sortOpenContainer(context, Input.SORT_KEY_NO_HOVER);

        List<String> items = context.computeOnServer(s -> describe(player(s).getEnderChestInventory()));
        assertSortedScrambled("ender_chest", items);
        closeScreen(context);
    }

    private static void testDisabledFeature(ClientTestContext context) {
        context.runOnClient(client -> InventoryWizard.config().features.barrelSorting = false);
        BlockPos pos = context.computeOnServer(s -> {
            ServerPlayer player = player(s);
            BlockPos p = player.blockPosition().offset(-2, 0, 0);
            ServerLevel level = (ServerLevel) player.level();
            level.setBlockAndUpdate(p, Blocks.BARREL.defaultBlockState());
            Container container = (Container) level.getBlockEntity(p);
            fillScrambled(container);
            player.openMenu((MenuProvider) container);
            return p;
        });
        context.waitForScreen(ContainerScreen.class);
        List<String> before = context.computeOnServer(s -> describe((Container) s.overworld().getBlockEntity(pos)));

        sortOpenContainer(context, Input.SORT_KEY_NO_HOVER);

        List<String> after = context.computeOnServer(s -> describe((Container) s.overworld().getBlockEntity(pos)));
        assertEquals("disabled barrel sorting leaves the barrel alone", before, after);

        context.runOnClient(client -> InventoryWizard.config().features.barrelSorting = true);
        closeScreen(context);
    }

    // ---- input helpers -------------------------------------------------------

    private static void openInventory(ClientTestContext context) {
        int key = context.computeOnClient(client -> client.options.keyInventory.getDefaultKey().getValue());
        context.pressKey(key);
        context.waitForScreen(InventoryScreen.class);
    }

    private static void closeScreen(ClientTestContext context) {
        context.runOnClient(client -> client.player.closeContainer());
        context.waitTicks(3);
    }

    private static void shiftRightClick(ClientTestContext context) {
        context.click(InputConstants.MOUSE_BUTTON_RIGHT, InputConstants.MOD_SHIFT);
        context.waitTicks(SORT_WAIT_TICKS);
    }

    private static void sortOpenContainer(ClientTestContext context, Input input) {
        switch (input) {
            case SHIFT_RIGHT_CLICK -> {
                context.hoverSlot(containerSlot(13));
                shiftRightClick(context);
            }
            case SORT_KEY_HOVER -> {
                context.hoverSlot(containerSlot(13));
                context.pressKey(SORT_KEY);
                context.waitTicks(SORT_WAIT_TICKS);
            }
            case SORT_KEY_NO_HOVER -> {
                context.setCursorGui(1, 1);
                context.waitTicks(2);
                context.pressKey(SORT_KEY);
                context.waitTicks(SORT_WAIT_TICKS);
            }
        }
    }

    private static Predicate<Slot> inventorySlot(int index) {
        return slot -> slot.container instanceof Inventory && slot.getContainerSlot() == index;
    }

    private static Predicate<Slot> containerSlot(int index) {
        return slot -> !(slot.container instanceof Inventory) && slot.getContainerSlot() == index;
    }

    // ---- item helpers ----------------------------------------------------------

    private static ServerPlayer player(MinecraftServer server) {
        return server.getPlayerList().getPlayers().getFirst();
    }

    private static ItemStack named(Item item, String name) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        return stack;
    }

    private static void fillScrambled(Container container) {
        container.clearContent();
        container.setItem(0, new ItemStack(Items.DIRT, 5));
        container.setItem(3, new ItemStack(Items.DIAMOND_SWORD));
        container.setItem(8, new ItemStack(Items.STONE, 7));
        container.setItem(14, new ItemStack(Items.DIRT, 5));
        container.setItem(20, new ItemStack(Items.BREAD, 2));
        container.setItem(26, new ItemStack(Items.OAK_LOG, 3));
    }

    private static void assertSortedScrambled(String name, List<String> items) {
        assertPrefix(name, items, "stone x7", "dirt x10", "oak_log x3", "diamond_sword x1", "bread x2");
        assertEmptyFrom(name, items, 5);
    }

    private static String describe(ItemStack stack) {
        if (stack.isEmpty()) return "-";
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        return path + (stack.has(DataComponents.CUSTOM_NAME) ? "*" : "") + " x" + stack.getCount();
    }

    private static List<String> describe(Container container) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < container.getContainerSize(); i++) {
            result.add(describe(container.getItem(i)));
        }
        return result;
    }

    private static List<String> inventoryRange(ClientTestContext context, int start, int end) {
        return context.computeOnServer(s -> describe(player(s).getInventory()).subList(start, end));
    }

    private static Map<String, Integer> totals(ClientTestContext context) {
        return context.computeOnServer(s -> {
            Map<String, Integer> totals = new TreeMap<>();
            Inventory inv = player(s).getInventory();
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack stack = inv.getItem(i);
                if (!stack.isEmpty()) {
                    totals.merge(describe(stack.copyWithCount(1)), stack.getCount(), Integer::sum);
                }
            }
            return totals;
        });
    }

    // ---- assertions --------------------------------------------------------------

    private static void assertEquals(String what, Object expected, Object actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError(what + ": expected " + expected + " but was " + actual);
        }
    }

    private static void assertPrefix(String what, List<String> actual, String... expected) {
        assertEquals(what, List.of(expected), actual.subList(0, expected.length));
    }

    private static void assertEmptyFrom(String what, List<String> actual, int from) {
        List<String> rest = actual.subList(from, actual.size());
        if (rest.stream().anyMatch(slot -> !slot.equals("-"))) {
            throw new AssertionError(what + ": expected only empty slots after " + from + " but was " + actual);
        }
    }
}
