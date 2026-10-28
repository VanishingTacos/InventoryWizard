package com.inventorywizard.gametest;

import com.inventorywizard.InventoryWizard;
import com.inventorywizard.InventoryWizardConfig;
import com.inventorywizard.InventoryWizardConfigScreen;
import com.inventorywizard.SortMode;
import com.mojang.blaze3d.platform.InputConstants;
//? if <1.21.9
//import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.ConfirmScreen;
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
    //? if <1.21.9
    //private static final int CYCLE_MODE_KEY = InputConstants.KEY_M;
    private static final int SORT_WAIT_TICKS = 5;
    private static final String SORT_INVENTORY_BUTTON = "Sort inventory";
    private static final String SORT_CONTAINER_BUTTON = "Sort container";
    private static final String MODE_BUTTON = "Change sort mode";

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
        test("locked slots", () -> testLockedSlots(context));
        test("scroll adjusts held stack", () -> testScrollAdjustsHeldStack(context));
        test("chest", () -> testBlockContainer(context, Blocks.CHEST, new BlockPos(2, 0, 0), ContainerScreen.class, Input.SORT_KEY_NO_HOVER, "chest"));
        test("barrel", () -> testBlockContainer(context, Blocks.BARREL, new BlockPos(0, 0, 2), ContainerScreen.class, Input.SHIFT_RIGHT_CLICK, "barrel"));
        test("shulker box", () -> testBlockContainer(context, Blocks.SHULKER_BOX, new BlockPos(0, 0, -2), ShulkerBoxScreen.class, Input.SORT_KEY_HOVER, "shulker_box"));
        test("ender chest", () -> testEnderChest(context));
        test("disabled feature", () -> testDisabledFeature(context));
        test("inventory sort button", () -> testInventoryButtons(context));
        test("chest sort button", () -> testBlockContainer(context, Blocks.CHEST, new BlockPos(0, 0, 3), ContainerScreen.class, Input.SORT_BUTTON, "chest button"));
        test("buttons turned off", () -> testButtonsOff(context));
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

        context.setScreen(() -> new InventoryWizardConfigScreen(new TitleScreen()));
        context.waitForScreen(InventoryWizardConfigScreen.class);
        context.clickWidget("Reset to Defaults");
        context.waitForScreen(ConfirmScreen.class);
        context.clickWidget(Component.translatable("gui.yes").getString());
        context.waitForScreen(InventoryWizardConfigScreen.class);
        boolean reset = context.computeOnClient(client -> InventoryWizardConfig.load().showMessages);
        assertEquals("show messages saved as the default after resetting", true, reset);
        context.clickWidget(Component.translatable("gui.done").getString());
        context.waitForScreen(TitleScreen.class);

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
        //? if >=1.21.9 {
        shiftRightClick(context);
        //?} else {
        /*// No simulated Shift before 1.21.9 (see shiftRightClick); the cycle mode key does the same
        context.runOnClient(client -> {
            InventoryWizard.CYCLE_MODE_KEY.setKey(InputConstants.Type.KEYSYM.getOrCreate(CYCLE_MODE_KEY));
            KeyMapping.resetMapping();
        });
        context.pressKey(CYCLE_MODE_KEY);
        context.waitTicks(SORT_WAIT_TICKS);
        context.runOnClient(client -> {
            InventoryWizard.CYCLE_MODE_KEY.setKey(InputConstants.UNKNOWN);
            KeyMapping.resetMapping();
        });
        *///?}

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

    private static void testLockedSlots(ClientTestContext context) {
        context.runOnClient(client -> InventoryWizard.config().lockedSlots.clear());
        context.runOnServer(s -> {
            Inventory inv = player(s).getInventory();
            inv.clearContent();
            inv.setItem(0, new ItemStack(Items.BREAD, 3));
            inv.setItem(2, new ItemStack(Items.COBBLESTONE, 10));
            inv.setItem(5, new ItemStack(Items.DIAMOND_PICKAXE));
            inv.setItem(7, new ItemStack(Items.IRON_SWORD));
            inv.setItem(8, new ItemStack(Items.TOTEM_OF_UNDYING));
            inv.setItem(9, new ItemStack(Items.DIAMOND_SWORD));
            inv.setItem(15, new ItemStack(Items.STONE, 10));
            inv.setItem(20, new ItemStack(Items.DIRT, 20));
            inv.setItem(30, new ItemStack(Items.DIRT, 30));
        });
        Map<String, Integer> before = totals(context);

        openInventory(context);
        //? if >=1.21.9 {
        for (int slot : new int[] {5, 8, 9, 20}) {
            context.hoverSlot(inventorySlot(slot));
            context.click(InputConstants.MOUSE_BUTTON_LEFT, InputConstants.MOD_ALT);
            context.waitTicks(2);
        }
        // A second Alt+click unlocks again
        context.hoverSlot(inventorySlot(20));
        context.click(InputConstants.MOUSE_BUTTON_LEFT, InputConstants.MOD_ALT);
        context.waitTicks(2);
        Object locked = context.computeOnClient(client -> List.copyOf(InventoryWizard.config().lockedSlots));
        assertEquals("slots locked by Alt+click", List.of(5, 8, 9), locked);
        assertEquals("Alt+click must not pick items up", before, totals(context));
        //?} else {
        /*// No simulated Alt before 1.21.9 (see shiftRightClick), so lock the slots directly
        context.runOnClient(client -> InventoryWizard.config().lockedSlots.addAll(List.of(5, 8, 9)));
        *///?}

        context.hoverSlot(inventorySlot(1));
        context.pressKey(SORT_KEY);
        context.waitTicks(SORT_WAIT_TICKS);

        assertEquals("hotbar sort skips locked slots",
                List.of("iron_sword x1", "bread x3", "cobblestone x10", "-", "-", "diamond_pickaxe x1", "-", "-", "totem_of_undying x1"),
                inventoryRange(context, 0, 9));
        List<String> main = inventoryRange(context, 9, 36);
        assertPrefix("inventory sort skips locked slots", main, "diamond_sword x1", "stone x10", "dirt x50");
        assertEmptyFrom("inventory sort skips locked slots", main, 3);
        assertEquals("item totals after sorting around locked slots", before, totals(context));

        context.runOnClient(client -> {
            InventoryWizard.config().lockedSlots.clear();
            InventoryWizard.config().save();
        });
        closeScreen(context);
    }

    private static void testScrollAdjustsHeldStack(ClientTestContext context) {
        context.runOnServer(s -> {
            Inventory inv = player(s).getInventory();
            inv.clearContent();
            inv.setItem(9, new ItemStack(Items.DIRT, 10));
        });

        openInventory(context);
        context.hoverSlot(inventorySlot(9));
        context.click(InputConstants.MOUSE_BUTTON_LEFT, 0);
        context.waitTicks(2);
        assertEquals("carried after picking up", "dirt x10", carried(context));

        // Away from the origin slot, so only the mod moves items
        context.hoverSlot(inventorySlot(20));
        context.scroll(-1);
        context.scroll(-1);
        context.waitTicks(SORT_WAIT_TICKS);
        assertEquals("carried after scrolling down twice", "dirt x8", carried(context));
        assertEquals("origin after scrolling down twice", "dirt x2", inventoryRange(context, 9, 10).getFirst());

        for (int i = 0; i < 3; i++) {
            context.scroll(1);
        }
        context.waitTicks(SORT_WAIT_TICKS);
        assertEquals("carried after scrolling up past what the slot had", "dirt x10", carried(context));
        assertEquals("origin emptied by scrolling up", "-", inventoryRange(context, 9, 10).getFirst());
        assertEquals("hovered slot untouched", "-", inventoryRange(context, 20, 21).getFirst());

        context.hoverSlot(inventorySlot(9));
        context.click(InputConstants.MOUSE_BUTTON_LEFT, 0);
        context.waitTicks(2);
        assertEquals("stack put back", "dirt x10", inventoryRange(context, 9, 10).getFirst());
        closeScreen(context);
    }

    /** The server's view of the cursor stack, so it also checks the clicks reached the server. */
    private static String carried(ClientTestContext context) {
        return context.computeOnServer(s -> describe(player(s).containerMenu.getCarried()));
    }

    private enum Input {
        SHIFT_RIGHT_CLICK,
        SORT_KEY_HOVER,
        SORT_KEY_NO_HOVER,
        SORT_BUTTON
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

    private static void testInventoryButtons(ClientTestContext context) {
        context.runOnServer(s -> {
            Inventory inv = player(s).getInventory();
            inv.clearContent();
            inv.setItem(9, new ItemStack(Items.DIRT, 20));
            inv.setItem(15, new ItemStack(Items.STONE, 10));
            inv.setItem(20, new ItemStack(Items.DIRT, 30));
            inv.setItem(25, new ItemStack(Items.BREAD, 5));
            inv.setItem(0, new ItemStack(Items.BREAD, 3));
            inv.setItem(7, new ItemStack(Items.IRON_SWORD));
        });
        Map<String, Integer> before = totals(context);

        openInventory(context);
        context.clickWidget(SORT_INVENTORY_BUTTON);
        context.waitTicks(SORT_WAIT_TICKS);

        List<String> main = inventoryRange(context, 9, 36);
        assertPrefix("inventory sort button", main, "stone x10", "dirt x50", "bread x5");
        assertEmptyFrom("inventory sort button", main, 3);
        assertEquals("hotbar untouched by the inventory sort button",
                List.of("bread x3", "-", "-", "-", "-", "-", "-", "iron_sword x1", "-"),
                inventoryRange(context, 0, 9));
        assertEquals("item totals after the inventory sort button", before, totals(context));
        boolean focused = context.computeOnClient(client -> ClientTestContext.screen(client).getFocused() instanceof AbstractWidget w
                && w.getMessage().getString().startsWith(SORT_INVENTORY_BUTTON));
        assertEquals("sort button not left highlighted after a click", false, focused);

        context.clickWidget(MODE_BUTTON);
        SortMode mode = context.computeOnClient(client -> InventoryWizard.config().sortMode);
        assertEquals("mode after clicking the mode button", SortMode.ALPHABETICAL, mode);
        assertEquals("the mode button must not move items", main, inventoryRange(context, 9, 36));

        context.runOnClient(client -> InventoryWizard.config().sortMode = SortMode.DEFAULT);
        closeScreen(context);
    }

    private static void testButtonsOff(ClientTestContext context) {
        context.runOnClient(client -> InventoryWizard.config().showButtons = false);
        openInventory(context);
        assertEquals("sort button hidden when turned off", false, context.hasWidget(SORT_INVENTORY_BUTTON));
        assertEquals("mode button hidden when turned off", false, context.hasWidget(MODE_BUTTON));
        context.runOnClient(client -> InventoryWizard.config().showButtons = true);
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

    /**
     * Before 1.21.9 Minecraft reads Shift from the real keyboard rather than the click, so a simulated
     * Shift+Right-click is impossible there; the sort key over the same slot runs the same sort.
     */
    private static void shiftRightClick(ClientTestContext context) {
        //? if >=1.21.9 {
        context.click(InputConstants.MOUSE_BUTTON_RIGHT, InputConstants.MOD_SHIFT);
        //?} else
        //context.pressKey(SORT_KEY);
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
            case SORT_BUTTON -> {
                context.clickWidget(SORT_CONTAINER_BUTTON);
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
