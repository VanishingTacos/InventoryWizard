package com.inventorywizard.test;

import com.inventorywizard.InventoryWizard;
import com.inventorywizard.InventoryWizardConfig;
import com.inventorywizard.InventoryWizardConfigScreen;
import com.inventorywizard.SortActions;
import com.inventorywizard.SortMode;
import com.inventorywizard.mixin.AbstractContainerScreenAccessor;
import com.inventorywizard.test.mixin.ScreenPositionAccessor;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
//? if >=1.21.9
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.commands.Commands;
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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

/**
 * Drives the real game: creates a singleplayer world, sorts with simulated
 * Shift+Right-clicks and the sort key, and checks the server-side result.
 * Then joins a dedicated server to check the mod stays inactive there, and sends
 * its sorts to the server once it has the InventoryWizard plugin's command.
 */
@SuppressWarnings("UnstableApiUsage")
public class InventoryWizardClientGameTest implements FabricClientGameTest {

    private static final int SORT_KEY = InputConstants.KEY_R;
    private static final int SORT_WAIT_TICKS = 5;
    //? if >=1.21.9 {
    private static final int MOD_SHIFT = InputConstants.MOD_SHIFT;
    private static final int MOD_ALT = InputConstants.MOD_ALT;
    //?} else {
    /*// Not part of a click before 1.21.9: holding the key is what counts
    private static final int MOD_SHIFT = 0;
    private static final int MOD_ALT = 0;
    *///?}
    private static final String SORT_INVENTORY_BUTTON = "Sort inventory";
    private static final String SORT_CONTAINER_BUTTON = "Sort container";
    private static final String MODE_BUTTON = "Change sort mode";
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
            //? if >=26.2 {
            singleplayer.getConnection().waitForChunksRender();
            //?} else if >=26.1 {
            /*singleplayer.getClientLevel().waitForChunksRender();
            *///?} else
            //singleplayer.getClientWorld().waitForChunksRender();
            TestServerContext server = singleplayer.getServer();
            server.runCommand("gamemode survival @a");
            server.runCommand("time set day");
            context.waitTicks(5);
            pace(context);

            testInventoryShiftRightClick(context, server);
            testSortKeyOnHotbar(context, server);
            testCycleModeSlot(context, server);
            testPartialStacksOff(context, server);
            testLockedSlots(context, server);
            testScrollAdjustsHeldStack(context, server);
            testBlockContainer(context, server, Blocks.CHEST, new BlockPos(2, 0, 0), ContainerScreen.class, Input.SORT_KEY_NO_HOVER, "chest");
            testBlockContainer(context, server, Blocks.BARREL, new BlockPos(0, 0, 2), ContainerScreen.class, Input.SHIFT_RIGHT_CLICK, "barrel");
            testBlockContainer(context, server, Blocks.SHULKER_BOX, new BlockPos(0, 0, -2), ShulkerBoxScreen.class, Input.SORT_KEY_HOVER, "shulker_box");
            testEnderChest(context, server);
            testDisabledFeature(context, server);
            testInventoryButtons(context, server);
            testBlockContainer(context, server, Blocks.CHEST, new BlockPos(0, 0, 3), ContainerScreen.class, Input.SORT_BUTTON, "chest-button");
            testButtonsOff(context);
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

    private static void testLockedSlots(ClientGameTestContext context, TestServerContext server) {
        context.runOnClient(client -> InventoryWizard.config().lockedSlots.clear());
        server.runOnServer(s -> {
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
        Map<String, Integer> before = totals(server);

        openInventory(context);
        for (int slot : new int[] {5, 8, 9, 20}) {
            hoverSlot(context, inventorySlot(slot));
            altClick(context);
        }
        // A second Alt+click unlocks again
        hoverSlot(context, inventorySlot(20));
        altClick(context);
        context.takeScreenshot("inventorywizard-locked-slots");
        pace(context);

        Object locked = context.computeOnClient(client -> List.copyOf(InventoryWizard.config().lockedSlots));
        assertEquals("slots locked by Alt+click", List.of(5, 8, 9), locked);
        assertEquals("Alt+click must not pick items up", before, totals(server));

        hoverSlot(context, inventorySlot(1));
        context.getInput().pressKey(SORT_KEY);
        context.waitTicks(SORT_WAIT_TICKS);

        assertEquals("hotbar sort skips locked slots",
                List.of("iron_sword x1", "bread x3", "cobblestone x10", "-", "-", "diamond_pickaxe x1", "-", "-", "totem_of_undying x1"),
                inventoryRange(server, 0, 9));
        List<String> main = inventoryRange(server, 9, 36);
        assertPrefix("inventory sort skips locked slots", main, "diamond_sword x1", "stone x10", "dirt x50");
        assertEmptyFrom("inventory sort skips locked slots", main, 3);
        assertEquals("item totals after sorting around locked slots", before, totals(server));

        context.runOnClient(client -> {
            InventoryWizard.config().lockedSlots.clear();
            InventoryWizard.config().save();
        });
        closeScreen(context);
    }

    private static void testScrollAdjustsHeldStack(ClientGameTestContext context, TestServerContext server) {
        server.runOnServer(s -> {
            Inventory inv = player(s).getInventory();
            inv.clearContent();
            inv.setItem(9, new ItemStack(Items.DIRT, 10));
        });

        openInventory(context);
        hoverSlot(context, inventorySlot(9));
        context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
        context.waitTicks(2);
        assertEquals("carried after picking up", "dirt x10", carried(server));

        // Away from the origin slot, so only the mod moves items
        hoverSlot(context, inventorySlot(20));
        context.getInput().scroll(-1);
        context.getInput().scroll(-1);
        context.waitTicks(SORT_WAIT_TICKS);
        assertEquals("carried after scrolling down twice", "dirt x8", carried(server));
        assertEquals("origin after scrolling down twice", "dirt x2", inventoryRange(server, 9, 10).getFirst());

        for (int i = 0; i < 3; i++) {
            context.getInput().scroll(1);
        }
        context.waitTicks(SORT_WAIT_TICKS);
        assertEquals("carried after scrolling up past what the slot had", "dirt x10", carried(server));
        assertEquals("origin emptied by scrolling up", "-", inventoryRange(server, 9, 10).getFirst());
        assertEquals("hovered slot untouched", "-", inventoryRange(server, 20, 21).getFirst());

        hoverSlot(context, inventorySlot(9));
        context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
        context.waitTicks(2);
        assertEquals("stack put back", "dirt x10", inventoryRange(server, 9, 10).getFirst());
        closeScreen(context);
    }

    /** The server's view of the cursor stack, so it also checks the clicks reached the server. */
    private static String carried(TestServerContext server) {
        return server.computeOnServer(s -> describe(player(s).containerMenu.getCarried()));
    }

    private enum Input {
        SHIFT_RIGHT_CLICK,
        SORT_KEY_HOVER,
        SORT_KEY_NO_HOVER,
        SORT_BUTTON
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

    private static void testInventoryButtons(ClientGameTestContext context, TestServerContext server) {
        server.runOnServer(s -> {
            Inventory inv = player(s).getInventory();
            inv.clearContent();
            inv.setItem(9, new ItemStack(Items.DIRT, 20));
            inv.setItem(15, new ItemStack(Items.STONE, 10));
            inv.setItem(20, new ItemStack(Items.DIRT, 30));
            inv.setItem(25, new ItemStack(Items.BREAD, 5));
            inv.setItem(0, new ItemStack(Items.BREAD, 3));
            inv.setItem(7, new ItemStack(Items.IRON_SWORD));
        });
        Map<String, Integer> before = totals(server);

        openInventory(context);
        context.takeScreenshot("inventorywizard-inventory-buttons");
        clickWidget(context, SORT_INVENTORY_BUTTON);
        context.waitTicks(SORT_WAIT_TICKS);
        pace(context);

        List<String> main = inventoryRange(server, 9, 36);
        assertPrefix("inventory sort button", main, "stone x10", "dirt x50", "bread x5");
        assertEmptyFrom("inventory sort button", main, 3);
        assertEquals("hotbar untouched by the inventory sort button",
                List.of("bread x3", "-", "-", "-", "-", "-", "-", "iron_sword x1", "-"),
                inventoryRange(server, 0, 9));
        assertEquals("item totals after the inventory sort button", before, totals(server));
        boolean focused = context.computeOnClient(client -> screen(client).getFocused() instanceof AbstractWidget w
                && w.getMessage().getString().startsWith(SORT_INVENTORY_BUTTON));
        assertEquals("sort button not left highlighted after a click", false, focused);

        clickWidget(context, MODE_BUTTON);
        SortMode mode = context.computeOnClient(client -> InventoryWizard.config().sortMode);
        assertEquals("mode after clicking the mode button", SortMode.ALPHABETICAL, mode);
        assertEquals("the mode button must not move items", main, inventoryRange(server, 9, 36));

        context.runOnClient(client -> InventoryWizard.config().sortMode = SortMode.DEFAULT);
        closeScreen(context);
    }

    private static void testButtonsOff(ClientGameTestContext context) {
        context.runOnClient(client -> InventoryWizard.config().showButtons = false);
        openInventory(context);
        assertEquals("sort button hidden when turned off", false, hasWidget(context, SORT_INVENTORY_BUTTON));
        assertEquals("mode button hidden when turned off", false, hasWidget(context, MODE_BUTTON));
        context.runOnClient(client -> InventoryWizard.config().showButtons = true);
        closeScreen(context);
    }

    // ---- multiplayer ---------------------------------------------------------

    private static void testDedicatedServerDoesNothing(ClientGameTestContext context) {
        try (TestDedicatedServerContext server = context.worldBuilder().createServer()) {
            try (var connection = server.connect()) {
                //? if >=26.2 {
                connection.waitForChunksRender();
                //?} else if >=26.1 {
                /*connection.getClientLevel().waitForChunksRender();
                *///?} else
                //connection.getClientWorld().waitForChunksRender();
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
                assertEquals("no sort button on a multiplayer server", false, hasWidget(context, SORT_INVENTORY_BUTTON));
                hoverSlot(context, inventorySlot(20));
                context.getInput().pressKey(SORT_KEY);
                context.waitTicks(SORT_WAIT_TICKS);
                context.takeScreenshot("inventorywizard-multiplayer-noop");
                pace(context);

                boolean singleplayer = context.computeOnClient(client -> client.hasSingleplayerServer());
                assertEquals("connected to a remote server", false, singleplayer);
                assertEquals("sort key does nothing on a multiplayer server", before, inventoryRange(server, 9, 36));

                // Shift+Right-click must reach the server: vanilla quick-moves the stone into the hotbar
                hoverSlot(context, inventorySlot(20));
                shiftRightClick(context);
                assertEquals("shift+right-click passes through to the server", "-", inventoryRange(server, 20, 21).getFirst());
                assertEquals("stone quick-moved to the hotbar", true, inventoryRange(server, 0, 9).contains("stone x10"));
                closeScreen(context);

                testPluginServer(context, server);
            }
        }
    }

    /**
     * Stands in for the Paper plugin with a command of the same name that records what it's sent,
     * since a Paper server can't run in this test. The plugin's side is covered by :paper:runPaperTest.
     */
    private static void testPluginServer(ClientGameTestContext context, TestDedicatedServerContext server) {
        List<String> received = new CopyOnWriteArrayList<>();
        server.runOnServer(s -> {
            s.getCommands().getDispatcher().register(Commands.literal("inventorywizard:iwiz")
                    .then(Commands.argument("args", StringArgumentType.greedyString()).executes(c -> {
                        received.add(StringArgumentType.getString(c, "args"));
                        return 1;
                    })));
            s.getCommands().sendCommands(player(s));
        });
        context.waitTicks(5);
        boolean detected = context.computeOnClient(SortActions::hasPlugin);
        assertEquals("plugin detected from the server's command list", true, detected);

        openInventory(context);
        context.takeScreenshot("inventorywizard-plugin-server-buttons");
        assertEquals("sort button shown on a server with the plugin", true, hasWidget(context, SORT_INVENTORY_BUTTON));
        clickWidget(context, SORT_INVENTORY_BUTTON);
        context.waitTicks(SORT_WAIT_TICKS);
        clickWidget(context, MODE_BUTTON);
        context.waitTicks(SORT_WAIT_TICKS);
        SortMode mode = context.computeOnClient(client -> InventoryWizard.config().sortMode);
        assertEquals("mode after clicking the mode button on a plugin server", SortMode.ALPHABETICAL, mode);
        closeScreen(context);

        server.runOnServer(s -> player(s).openMenu(new SimpleMenuProvider(
                (id, inventory, p) -> ChestMenu.threeRows(id, inventory), Component.literal("Chest"))));
        context.waitForScreen(ContainerScreen.class);
        clickWidget(context, SORT_CONTAINER_BUTTON);
        context.waitTicks(SORT_WAIT_TICKS);
        closeScreen(context);

        context.getInput().pressKey(SORT_KEY);
        context.waitTicks(SORT_WAIT_TICKS);

        assertEquals("commands sent to the plugin", List.of("inventory", "mode alphabetical", "container", "all"), List.copyOf(received));
        context.runOnClient(client -> InventoryWizard.config().sortMode = SortMode.DEFAULT);
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

    private static void shiftRightClick(ClientGameTestContext context) {
        context.getInput().holdShift();
        clickWithModifiers(context, InputConstants.MOUSE_BUTTON_RIGHT, MOD_SHIFT);
        context.getInput().releaseShift();
        context.waitTicks(SORT_WAIT_TICKS);
    }

    private static void altClick(ClientGameTestContext context) {
        context.getInput().holdAlt();
        clickWithModifiers(context, InputConstants.MOUSE_BUTTON_LEFT, MOD_ALT);
        context.getInput().releaseAlt();
        context.waitTicks(2);
    }

    /**
     * Since 1.21.9 modifiers come from the click itself, and TestInput.pressMouse always sends
     * modifiers = 0, so feed the mouse handler a click carrying them like a real Shift+ or Alt+click
     * does. Before that, holding the key (done by the caller) is enough.
     */
    private static void clickWithModifiers(ClientGameTestContext context, int button, int modifiers) {
        //? if >=26.3 {
        context.runOnClient(client -> {
            long window = client.getWindow().handle();
            MouseButtonInfo click = new MouseButtonInfo(button, modifiers);
            client.mouseHandler.onButton(window, click, InputConstants.PRESS);
            client.mouseHandler.onButton(window, click, InputConstants.RELEASE);
        });
        //?} else if >=1.21.9 {
        /*// MouseHandler.onButton is private before 26.3
        context.runOnClient(client -> {
            long window = client.getWindow().handle();
            MouseButtonInfo click = new MouseButtonInfo(button, modifiers);
            try {
                Method onButton = client.mouseHandler.getClass().getDeclaredMethod("onButton", long.class, MouseButtonInfo.class, int.class);
                onButton.setAccessible(true);
                onButton.invoke(client.mouseHandler, window, click, InputConstants.PRESS);
                onButton.invoke(client.mouseHandler, window, click, InputConstants.RELEASE);
            } catch (ReflectiveOperationException e) {
                throw new AssertionError("Could not click", e);
            }
        });
        *///?} else
        //context.getInput().pressMouse(button);
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
            case SORT_BUTTON -> {
                clickWidget(context, SORT_CONTAINER_BUTTON);
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
            AbstractWidget widget = findWidget(screen(client), labelPrefix);
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

    private static boolean hasWidget(ClientGameTestContext context, String labelPrefix) {
        return context.computeOnClient(client -> findWidget(screen(client), labelPrefix) != null);
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

    /** The open screen, which Minecraft keeps in Gui since 26.2. */
    private static Screen screen(Minecraft client) {
        //? if >=26.2 {
        return client.gui.screen();
        //?} else
        //return client.screen;
    }

    private static void hoverSlot(ClientGameTestContext context, Predicate<Slot> which) {
        double[] position = context.computeOnClient(client -> {
            AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) screen(client);
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
            Slot slot = ((AbstractContainerScreenAccessor) screen(client)).inventorywizard$getHoveredSlot();
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
