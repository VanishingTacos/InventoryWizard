package com.inventorywizard;

import com.inventorywizard.mixin.AbstractContainerScreenAccessor;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.inventory.Slot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InventoryWizardClient implements ClientModInitializer {

    public static final String MOD_ID = "inventorywizard";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /** Shift+Right-click on this hotbar slot cycles the sort mode instead of sorting. */
    private static final int CYCLE_MODE_HOTBAR_SLOT = 4;

    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));

    private static InventoryWizardConfig config;
    private static KeyMapping sortKey;
    private static KeyMapping cycleModeKey;

    public static InventoryWizardConfig config() {
        return config;
    }

    @Override
    public void onInitializeClient() {
        config = InventoryWizardConfig.load();

        sortKey = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.inventorywizard.sort", InputConstants.KEY_R, CATEGORY));
        cycleModeKey = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.inventorywizard.cycle_mode", InputConstants.UNKNOWN.getValue(), CATEGORY));

        // Outside of screens: sort inventory + hotbar, or cycle the mode
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (sortKey.consumeClick()) {
                SortActions.sortPlayer(client, SortActions.PlayerTarget.ALL);
            }
            while (cycleModeKey.consumeClick()) {
                SortActions.cycleSortMode(client);
            }
        });

        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            // The creative inventory has its own item picker and search box; leave it alone
            if (!(screen instanceof AbstractContainerScreen<?> containerScreen) || screen instanceof CreativeModeInventoryScreen) {
                return;
            }
            ScreenMouseEvents.allowMouseClick(screen).register((s, event) -> !onMouseClick(client, containerScreen, event));
            ScreenKeyboardEvents.allowKeyPress(screen).register((s, event) -> !onKeyPress(client, containerScreen, event));
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) -> registerCommands(dispatcher));

        LOGGER.info("InventoryWizard has awakened! Sort key, Shift+Right-click or /iwiz [hotbar|inventory|all|mode]");
    }

    /** Returns true when the click was used for sorting and vanilla handling should be cancelled. */
    private static boolean onMouseClick(Minecraft client, AbstractContainerScreen<?> screen, MouseButtonEvent event) {
        if (event.button() != InputConstants.MOUSE_BUTTON_RIGHT || !event.hasShiftDown()) {
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
    private static boolean onKeyPress(Minecraft client, AbstractContainerScreen<?> screen, KeyEvent event) {
        if (isTyping(screen)) {
            return false;
        }

        if (sortKey.matches(event)) {
            AbstractContainerMenu menu = screen.getMenu();
            Slot slot = hoveredSlot(screen);
            if (slot != null && SortActions.isSortableSlot(menu, slot)) {
                SortActions.sortSlot(client, menu, slot);
            } else if (menu instanceof ChestMenu || menu instanceof ShulkerBoxMenu) {
                // Not hovering anything: sort the open container
                menu.slots.stream()
                        .filter(s -> !(s.container instanceof Inventory))
                        .findFirst()
                        .ifPresent(s -> SortActions.sortSlot(client, menu, s));
            } else {
                SortActions.sortPlayer(client, SortActions.PlayerTarget.ALL);
            }
            return true;
        }
        if (cycleModeKey.matches(event)) {
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

    private static void registerCommands(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        LiteralArgumentBuilder<FabricClientCommandSource> root = ClientCommands.literal("iwiz")
                .executes(context -> sort(context.getSource(), SortActions.PlayerTarget.INVENTORY))
                .then(ClientCommands.literal("hotbar").executes(context -> sort(context.getSource(), SortActions.PlayerTarget.HOTBAR)))
                .then(ClientCommands.literal("inventory").executes(context -> sort(context.getSource(), SortActions.PlayerTarget.INVENTORY)))
                .then(ClientCommands.literal("all").executes(context -> sort(context.getSource(), SortActions.PlayerTarget.ALL)))
                .then(ClientCommands.literal("mode").executes(context -> {
                    SortActions.cycleSortMode(context.getSource().getClient());
                    return 1;
                }));

        var node = dispatcher.register(root);
        dispatcher.register(ClientCommands.literal("inventorywizard").redirect(node));
    }

    private static int sort(FabricClientCommandSource source, SortActions.PlayerTarget target) {
        SortActions.sortPlayer(source.getClient(), target);
        return 1;
    }
}
