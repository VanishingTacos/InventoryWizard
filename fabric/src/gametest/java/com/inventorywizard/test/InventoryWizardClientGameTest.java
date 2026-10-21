package com.inventorywizard.test;

import com.inventorywizard.InventoryWizard;
import com.inventorywizard.InventoryWizardConfig;
import com.inventorywizard.InventoryWizardConfigScreen;
import com.inventorywizard.SortMode;
import com.inventorywizard.mixin.AbstractContainerScreenAccessor;
import com.inventorywizard.test.mixin.ScreenPositionAccessor;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.client.input.MouseButtonInfo;
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
 * Drives the real game: creates a singleplayer world, sorts with simulated
 * Shift+Right-clicks and the sort key, and checks the server-side result.
 * Then joins a dedicated server to check the mod stays inactive there.
 */
@SuppressWarnings("UnstableApiUsage")
public class InventoryWizardClientGameTest implements FabricClientGameTest {

    private static final int SORT_KEY = InputConstants.KEY_R;
    private static final int SORT_WAIT_TICKS = 5;
    /** Extra ticks to linger after each step so a person can follow along: ./gradlew runClientGameTest -PtestPace=20 */
    private static final int PACE_TICKS = Integer.getInteger("inventorywizard.test.pace", 0);

    @Override
    public void runTest(ClientGameTestContext context) {
        context.runOnClient(client -> {
            InventoryWizard.config().sortMode = SortMode.DEFAULT;
            InventoryWizard.config().save();
        });

        testConfigScreen(context);

        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            singleplayer.getConnection().waitForChunksRender();
            TestServerContext server = singleplayer.getServer();
            server.runCommand("gamemode survival @a");
            server.runCommand("time set day");
            context.waitTicks(5);
            pace(context);

            testInventoryShiftRightClick(context, server);
            testSortKeyOnHotbar(context, server);
            testCycleModeSlot(context, server);
            testPartialStacksOff(context, server);
            testBlockContainer(context, server, Blocks.CHEST, new BlockPos(2, 0, 0), ContainerScreen.class, Input.SORT_KEY_NO_HOVER, "chest");
            testBlockContainer(context, server, Blocks.BARREL, new BlockPos(0, 0, 2), ContainerScreen.class, Input.SHIFT_RIGHT_CLICK, "barrel");
            testBlockContainer(context, server, Blocks.SHULKER_BOX, new BlockPos(0, 0, -2), ShulkerBoxScreen.class, Input.SORT_KEY_HOVER, "shulker_box");
            testEnderChest(context, server);
            testDisabledFeature(context, server);
        }

        testDedicatedServerDoesNothing(context);
    }

    // ---- settings screen -----------------------------------------------------

    private static void testConfigScreen(ClientGameTestContext context) {
        context.runOnClient(client -> InventoryWizard.config().showMessages = true);

        context.setScreen(() -> new InventoryWizardConfigScreen(new TitleScreen()));
        context.waitForScreen(InventoryWizardConfigScreen.class);
        context.takeScreenshot("inventorywizard-config-screen");
        pace(context);

        clickWidget(context, "Chat Messages");
        boolean toggled = context.computeOnClient(client -> InventoryWizard.config().showMessages);
        assertEquals("show messages after clicking its button", false, toggled);

        context.clickScreenButton("gui.done");
        context.waitForScreen(TitleScreen.class);
        boolean reloaded = context.computeOnClient(client -> InventoryWizardConfig.load().showMessages);
        assertEquals("show messages saved when the screen closes", false, reloaded);

        context.runOnClient(client -> {
            InventoryWizard.config().showMessages = true;
            InventoryWizard.config().save();
        });
    }

    // ---- singleplayer tests -------------------------------------------------

    private static void testInventoryShiftRightClick(ClientGameTestContext context, TestServerContext server) {
        server.runOnServer(s -> {
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
        Map<String, Integer> before = totals(server);

        openInventory(context);
        hoverSlot(context, inventorySlot(15));
        shiftRightClick(context);
        context.takeScreenshot("inventorywizard-inventory-sorted");
        pace(context);

        List<String> main = inventoryRange(server, 9, 36);
        assertPrefix("inventory shift+right-click", main,
                "stone x10", "dirt x64", "dirt x36", "diamond_sword x1", "diamond_sword* x1", "bread x5");
        assertEmptyFrom("inventory shift+right-click", main, 6);
        assertEquals("hotbar untouched by inventory sort",
                List.of("bread x3", "-", "cobblestone x10", "-", "-", "diamond_pickaxe x1", "-", "iron_sword x1", "-"),
                inventoryRange(server, 0, 9));
        assertEquals("item totals after inventory sort", before, totals(server));
    }

    private static void testSortKeyOnHotbar(ClientGameTestContext context, TestServerContext server) {
        Map<String, Integer> before = totals(server);

        hoverSlot(context, inventorySlot(1));
        context.getInput().pressKey(SORT_KEY);
        context.waitTicks(SORT_WAIT_TICKS);
        context.takeScreenshot("inventorywizard-hotbar-sorted");
        pace(context);

        List<String> hotbar = inventoryRange(server, 0, 9);
        assertPrefix("sort key on hotbar", hotbar, "iron_sword x1", "diamond_pickaxe x1", "bread x3", "cobblestone x10");
        assertEmptyFrom("sort key on hotbar", hotbar, 4);
        assertEquals("item totals after hotbar sort", before, totals(server));
    }

    private static void testCycleModeSlot(ClientGameTestContext context, TestServerContext server) {
        List<String> before = inventoryRange(server, 0, 36);

        hoverSlot(context, inventorySlot(4));
        shiftRightClick(context);
        context.takeScreenshot("inventorywizard-mode-cycled");
        pace(context);

        SortMode mode = context.computeOnClient(client -> InventoryWizard.config().sortMode);
        assertEquals("mode after Shift+Right-click on hotbar slot 4", SortMode.ALPHABETICAL, mode);
        assertEquals("cycling the mode must not move items", before, inventoryRange(server, 0, 36));

        // Alphabetical mode: sort the main inventory by item id
        hoverSlot(context, inventorySlot(20));
        shiftRightClick(context);
        List<String> main = inventoryRange(server, 9, 36);
        assertPrefix("alphabetical sort", main,
                "bread x5", "diamond_sword x1", "diamond_sword* x1", "dirt x64", "dirt x36", "stone x10");

        context.runOnClient(client -> InventoryWizard.config().sortMode = SortMode.DEFAULT);
        closeScreen(context);
    }

    private static void testPartialStacksOff(ClientGameTestContext context, TestServerContext server) {
        context.runOnClient(client -> InventoryWizard.config().allowPartialStacksInventory = false);
        server.runOnServer(s -> {
            Inventory inv = player(s).getInventory();
            inv.clearContent();
            inv.setItem(10, new ItemStack(Items.DIRT, 40));
            inv.setItem(11, new ItemStack(Items.STONE, 5));
            inv.setItem(30, new ItemStack(Items.DIRT, 30));
        });
        Map<String, Integer> before = totals(server);

        openInventory(context);
        hoverSlot(context, inventorySlot(10));
        shiftRightClick(context);

        List<String> main = inventoryRange(server, 9, 36);
        assertPrefix("partial stacks off", main, "dirt x64", "stone x5", "dirt x6");
        assertEmptyFrom("partial stacks off", main, 3);
        assertEquals("partial stacks off keeps every item", before, totals(server));

        context.runOnClient(client -> InventoryWizard.config().allowPartialStacksInventory = true);
        closeScreen(context);
    }

    private enum Input {
        SHIFT_RIGHT_CLICK,
        SORT_KEY_HOVER,
        SORT_KEY_NO_HOVER
    }

    private static void testBlockContainer(ClientGameTestContext context, TestServerContext server, Block block, BlockPos offset,
                                           Class<? extends AbstractContainerScreen<?>> screenClass, Input input, String name) {
        BlockPos pos = server.computeOnServer(s -> {
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
        context.takeScreenshot("inventorywizard-" + name + "-sorted");
        pace(context);

        List<String> items = server.computeOnServer(s -> describe((Container) s.overworld().getBlockEntity(pos)));
        assertSortedScrambled(name, items);
        closeScreen(context);
    }

    private static void testEnderChest(ClientGameTestContext context, TestServerContext server) {
        server.runOnServer(s -> {
            ServerPlayer player = player(s);
            fillScrambled(player.getEnderChestInventory());
            player.openMenu(new SimpleMenuProvider(
                    (id, inventory, p) -> ChestMenu.threeRows(id, inventory, p.getEnderChestInventory()),
                    Component.literal("Ender Chest")));
        });
        context.waitForScreen(ContainerScreen.class);

        sortOpenContainer(context, Input.SORT_KEY_NO_HOVER);
        context.takeScreenshot("inventorywizard-ender_chest-sorted");
        pace(context);

        List<String> items = server.computeOnServer(s -> describe(player(s).getEnderChestInventory()));
        assertSortedScrambled("ender_chest", items);
        closeScreen(context);
    }

    private static void testDisabledFeature(ClientGameTestContext context, TestServerContext server) {
        context.runOnClient(client -> InventoryWizard.config().features.barrelSorting = false);
        BlockPos pos = server.computeOnServer(s -> {
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
        List<String> before = server.computeOnServer(s -> describe((Container) s.overworld().getBlockEntity(pos)));

        sortOpenContainer(context, Input.SORT_KEY_NO_HOVER);
        context.takeScreenshot("inventorywizard-barrel-disabled");
        pace(context);

        List<String> after = server.computeOnServer(s -> describe((Container) s.overworld().getBlockEntity(pos)));
        assertEquals("disabled barrel sorting leaves the barrel alone", before, after);

        context.runOnClient(client -> InventoryWizard.config().features.barrelSorting = true);
        closeScreen(context);
    }

    // ---- multiplayer ---------------------------------------------------------

    private static void testDedicatedServerDoesNothing(ClientGameTestContext context) {
        try (TestDedicatedServerContext server = context.worldBuilder().createServer()) {
            try (var connection = server.connect()) {
                connection.waitForChunksRender();
                server.runCommand("gamemode survival @a");
                context.waitTicks(5);
                server.runOnServer(s -> {
                    Inventory inv = player(s).getInventory();
                    inv.clearContent();
                    inv.setItem(9, new ItemStack(Items.DIRT, 20));
                    inv.setItem(20, new ItemStack(Items.STONE, 10));
                    inv.setItem(30, new ItemStack(Items.DIRT, 30));
                });
                context.waitTicks(5);
                List<String> before = inventoryRange(server, 9, 36);

                openInventory(context);
                hoverSlot(context, inventorySlot(20));
                context.getInput().pressKey(SORT_KEY);
                context.waitTicks(SORT_WAIT_TICKS);
                context.takeScreenshot("inventorywizard-multiplayer-noop");
                pace(context);

                boolean singleplayer = context.computeOnClient(client -> client.hasSingleplayerServer());
                assertEquals("connected to a remote server", false, singleplayer);
                assertEquals("sort key does nothing on a multiplayer server", before, inventoryRange(server, 9, 36));
                closeScreen(context);
            }
        }
    }

    // ---- input helpers -------------------------------------------------------

    private static void pace(ClientGameTestContext context) {
        if (PACE_TICKS > 0) {
            context.waitTicks(PACE_TICKS);
        }
    }

    private static void openInventory(ClientGameTestContext context) {
        context.getInput().pressKey(options -> options.keyInventory);
        context.waitForScreen(InventoryScreen.class);
        pace(context);
    }

    private static void closeScreen(ClientGameTestContext context) {
        pace(context);
        context.runOnClient(client -> client.player.closeContainer());
        context.waitTicks(3);
    }

    /**
     * TestInput.pressMouse always sends modifiers = 0, so feed the mouse handler a
     * click carrying the Shift modifier like a real Shift+Right-click does.
     */
    private static void shiftRightClick(ClientGameTestContext context) {
        context.getInput().holdShift();
        context.runOnClient(client -> {
            long window = client.getWindow().handle();
            MouseButtonInfo click = new MouseButtonInfo(InputConstants.MOUSE_BUTTON_RIGHT, InputConstants.MOD_SHIFT);
            client.mouseHandler.onButton(window, click, InputConstants.PRESS);
            client.mouseHandler.onButton(window, click, InputConstants.RELEASE);
        });
        context.getInput().releaseShift();
        context.waitTicks(SORT_WAIT_TICKS);
    }

    private static void sortOpenContainer(ClientGameTestContext context, Input input) {
        switch (input) {
            case SHIFT_RIGHT_CLICK -> {
                hoverSlot(context, containerSlot(13));
                shiftRightClick(context);
            }
            case SORT_KEY_HOVER -> {
                hoverSlot(context, containerSlot(13));
                context.getInput().pressKey(SORT_KEY);
                context.waitTicks(SORT_WAIT_TICKS);
            }
            case SORT_KEY_NO_HOVER -> {
                context.getInput().setCursorPos(1, 1);
                context.waitTicks(2);
                context.getInput().pressKey(SORT_KEY);
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

    /** Moves the real cursor over a slot and checks the screen agrees it's hovered. */
    /** Option buttons are labelled "Caption: Value", which clickScreenButton can't match, so find them by text. */
    private static void clickWidget(ClientGameTestContext context, String labelPrefix) {
        double[] position = context.computeOnClient(client -> {
            AbstractWidget widget = findWidget(client.gui.screen(), labelPrefix);
            if (widget == null) {
                throw new AssertionError("No widget labelled '" + labelPrefix + "...' on screen");
            }
            double scale = (double) client.getWindow().getScreenWidth() / client.getWindow().getGuiScaledWidth();
            return new double[] {
                    (widget.getX() + widget.getWidth() / 2.0) * scale,
                    (widget.getY() + widget.getHeight() / 2.0) * scale
            };
        });
        context.getInput().setCursorPos(position[0], position[1]);
        context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
        context.waitTicks(2);
    }

    private static AbstractWidget findWidget(ContainerEventHandler parent, String labelPrefix) {
        for (GuiEventListener child : parent.children()) {
            if (child instanceof AbstractWidget widget && widget.getMessage().getString().startsWith(labelPrefix)) {
                return widget;
            }
            if (child instanceof ContainerEventHandler container) {
                AbstractWidget found = findWidget(container, labelPrefix);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static void hoverSlot(ClientGameTestContext context, Predicate<Slot> which) {
        double[] position = context.computeOnClient(client -> {
            AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) client.gui.screen();
            Slot slot = screen.getMenu().slots.stream().filter(which).findFirst().orElseThrow();
            ScreenPositionAccessor accessor = (ScreenPositionAccessor) screen;
            double scale = (double) client.getWindow().getScreenWidth() / client.getWindow().getGuiScaledWidth();
            return new double[] {
                    (accessor.inventorywizardtest$getLeftPos() + slot.x + 8) * scale,
                    (accessor.inventorywizardtest$getTopPos() + slot.y + 8) * scale
            };
        });
        context.getInput().setCursorPos(position[0], position[1]);
        context.waitTicks(2);

        boolean hovered = context.computeOnClient(client -> {
            Slot slot = ((AbstractContainerScreenAccessor) client.gui.screen()).inventorywizard$getHoveredSlot();
            return slot != null && which.test(slot);
        });
        if (!hovered) {
            throw new AssertionError("Cursor is not over the expected slot");
        }
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

    private static List<String> inventoryRange(TestServerContext server, int start, int end) {
        return server.computeOnServer(s -> describe(player(s).getInventory()).subList(start, end));
    }

    private static Map<String, Integer> totals(TestServerContext server) {
        return server.computeOnServer(s -> {
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
