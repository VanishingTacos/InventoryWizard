package com.inventorywizard;

import com.inventorywizard.mixin.AbstractContainerScreenAccessor;
import com.inventorywizard.platform.Platform;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
//? if >=1.21.9 {
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.resources.Identifier;
//?}
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.inventory.Slot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loader-independent client logic. The Fabric and NeoForge entrypoints register the key mappings,
 * commands and events and forward them here.
 */
public final class InventoryWizard {

    public static final String MOD_ID = "inventorywizard";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /** Shift+Right-click on this hotbar slot cycles the sort mode instead of sorting. */
    private static final int CYCLE_MODE_HOTBAR_SLOT = 4;

    //? if >=1.21.9 {
    public static final KeyMapping.Category CATEGORY =
            Platform.INSTANCE.keyMappingCategory(Identifier.fromNamespaceAndPath(MOD_ID, "main"));
    //?} else
    //public static final String CATEGORY = "key.category." + MOD_ID + ".main";
    public static final KeyMapping SORT_KEY =
            new KeyMapping("key.inventorywizard.sort", InputConstants.KEY_R, CATEGORY);
    public static final KeyMapping CYCLE_MODE_KEY =
            new KeyMapping("key.inventorywizard.cycle_mode", InputConstants.UNKNOWN.getValue(), CATEGORY);

    private static InventoryWizardConfig config;
    private static boolean clientGameTestsStarted;

    private InventoryWizard() {}

    public static InventoryWizardConfig config() {
        return config;
    }

    public static void init() {
        config = InventoryWizardConfig.load();
        if (config.checkForUpdates) {
            UpdateChecker.start();
        }
        LOGGER.info("InventoryWizard has awakened! Sort key, Shift+Right-click or /iwiz [hotbar|inventory|all|mode]");
    }

    /** Outside of screens: sort inventory + hotbar, or cycle the mode. */
    public static void onClientTick(Minecraft client) {
        if (!clientGameTestsStarted) {
            clientGameTestsStarted = true;
            startClientGameTestsIfRequested();
        }
        while (SORT_KEY.consumeClick()) {
            SortActions.sortPlayer(client, SortActions.PlayerTarget.ALL);
        }
        while (CYCLE_MODE_KEY.consumeClick()) {
            SortActions.cycleSortMode(client);
        }
        //? if >=26.2 {
        Screen screen = client.gui.screen();
        //?} else
        //Screen screen = client.screen;
        if (screen != null) {
            SortButtons.tick(client, screen);
        }
    }

    /**
     * NeoForge/Forge client gametests (common/src/gametest, only on the classpath in runClientGameTest).
     * Started from the first client tick rather than mod init: touching client classes from another
     * thread while the loader is still loading mods can deadlock.
     */
    private static void startClientGameTestsIfRequested() {
        if (!Boolean.getBoolean("inventorywizard.clientGameTest")) {
            return;
        }
        try {
            Class.forName("com.inventorywizard.gametest.ClientGameTestRunner").getMethod("start").invoke(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("inventorywizard.clientGameTest is set but the gametest classes are missing", e);
        }
    }

    public static void onJoinWorld(Minecraft client) {
        UpdateChecker.notifyPlayer(client);
    }

    /** Whether mouse and key input on this screen should go to {@link #onMouseClick} and {@link #onKeyPress}. */
    public static boolean handlesScreen(Screen screen) {
        // The creative inventory has its own item picker and search box; leave it alone
        return screen instanceof AbstractContainerScreen<?> && !(screen instanceof CreativeModeInventoryScreen);
    }

    /** Called by each loader after a screen's init, once vanilla has added its own widgets. */
    public static void onScreenInit(Minecraft client, Screen screen) {
        if (handlesScreen(screen)) {
            SortButtons.add(client, (AbstractContainerScreen<?>) screen);
        }
    }

    /** Returns true when the click was used for sorting and vanilla handling should be cancelled. */
    //? if >=1.21.9 {
    public static boolean onMouseClick(Minecraft client, AbstractContainerScreen<?> screen, MouseButtonEvent event) {
        if (event.button() != InputConstants.MOUSE_BUTTON_RIGHT || !event.hasShiftDown()) {
            return false;
        }
    //?} else {
    /*public static boolean onMouseClick(Minecraft client, AbstractContainerScreen<?> screen, int button) {
        if (button != InputConstants.MOUSE_BUTTON_RIGHT || !Screen.hasShiftDown()) {
            return false;
        }
    *///?}
        // On a remote server there is nothing to sort with here; let the click through so vanilla
        // shift-clicking works and a server-side sorter (like the InventoryWizard Paper plugin) gets it
        if (client.getSingleplayerServer() == null) {
            return false;
        }
        Slot slot = hoveredSlot(screen);
        AbstractContainerMenu menu = screen.getMenu();
        if (slot == null || !SortActions.isSortableSlot(menu, slot)) {
            return false;
        }

        if (SortActions.isHotbarSlot(slot) && slot.getContainerSlot() == CYCLE_MODE_HOTBAR_SLOT) {
            SortActions.cycleSortMode(client);
        } else {
            SortActions.sortSlot(client, menu, slot);
        }
        return true;
    }

    /** Returns true when the key press was used and vanilla handling should be cancelled. */
    //? if >=1.21.9 {
    public static boolean onKeyPress(Minecraft client, AbstractContainerScreen<?> screen, KeyEvent event) {
    //?} else
    //public static boolean onKeyPress(Minecraft client, AbstractContainerScreen<?> screen, int keyCode, int scanCode) {
        if (isTyping(screen)) {
            return false;
        }

        //? if >=1.21.9 {
        if (SORT_KEY.matches(event)) {
        //?} else
        //if (SORT_KEY.matches(keyCode, scanCode)) {
            AbstractContainerMenu menu = screen.getMenu();
            Slot slot = hoveredSlot(screen);
            if (slot != null && SortActions.isSortableSlot(menu, slot)) {
                SortActions.sortSlot(client, menu, slot);
            } else if (menu instanceof ChestMenu || menu instanceof ShulkerBoxMenu) {
                // Not hovering anything: sort the open container
                SortActions.sortOpenContainer(client, menu);
            } else {
                SortActions.sortPlayer(client, SortActions.PlayerTarget.ALL);
            }
            return true;
        }
        //? if >=1.21.9 {
        if (CYCLE_MODE_KEY.matches(event)) {
        //?} else
        //if (CYCLE_MODE_KEY.matches(keyCode, scanCode)) {
            SortActions.cycleSortMode(client);
            return true;
        }
        return false;
    }

    private static Slot hoveredSlot(AbstractContainerScreen<?> screen) {
        return ((AbstractContainerScreenAccessor) screen).inventorywizard$getHoveredSlot();
    }

    /** Whether a text field (recipe book search, anvil name, ...) currently has focus. */
    private static boolean isTyping(ContainerEventHandler parent) {
        GuiEventListener focused = parent.getFocused();
        while (focused != null) {
            if (focused instanceof EditBox editBox && editBox.isFocused()) {
                return true;
            }
            focused = focused instanceof ContainerEventHandler container ? container.getFocused() : null;
        }
        return false;
    }

    /** Registers {@code /iwiz} and {@code /inventorywizard}; the source type is whatever the loader's client commands use. */
    public static <S> void registerCommands(CommandDispatcher<S> dispatcher) {
        LiteralArgumentBuilder<S> root = InventoryWizard.<S>literal("iwiz")
                .executes(context -> sort(SortActions.PlayerTarget.INVENTORY))
                .then(InventoryWizard.<S>literal("hotbar").executes(context -> sort(SortActions.PlayerTarget.HOTBAR)))
                .then(InventoryWizard.<S>literal("inventory").executes(context -> sort(SortActions.PlayerTarget.INVENTORY)))
                .then(InventoryWizard.<S>literal("all").executes(context -> sort(SortActions.PlayerTarget.ALL)))
                .then(InventoryWizard.<S>literal("mode").executes(context -> {
                    SortActions.cycleSortMode(Minecraft.getInstance());
                    return 1;
                }));

        var node = dispatcher.register(root);
        dispatcher.register(InventoryWizard.<S>literal("inventorywizard").redirect(node));
    }

    private static <S> LiteralArgumentBuilder<S> literal(String name) {
        return LiteralArgumentBuilder.literal(name);
    }

    private static int sort(SortActions.PlayerTarget target) {
        SortActions.sortPlayer(Minecraft.getInstance(), target);
        return 1;
    }
}
