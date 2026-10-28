package com.inventorywizard;

import com.inventorywizard.mixin.AbstractContainerScreenAccessor;
import com.inventorywizard.mixin.ScreenAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Small sort and mode buttons on the container title row and the "Inventory" label row.
 * They're vanilla sprite buttons, so drawing them needs no version-specific render code.
 */
public final class SortButtons {

    private static final int SIZE = 10;
    private static final int ICON_SIZE = 8;
    private static final int GAP = 1;
    /** The player inventory slots end this far in from the screen's right edge. */
    private static final int RIGHT_MARGIN = 7;

    private static final Identifier SORT_SPRITE = Identifier.fromNamespaceAndPath(InventoryWizard.MOD_ID, "sort");
    private static final Identifier MODE_SPRITE = Identifier.fromNamespaceAndPath(InventoryWizard.MOD_ID, "mode");

    /** Columns count from the right edge; the mode button sits rightmost, sort buttons left of it. */
    private record Placed(Button button, boolean containerRow, int column) {
    }

    private static final Map<Screen, List<Placed>> BUTTONS = new WeakHashMap<>();

    private SortButtons() {
    }

    /** Adds the buttons once the screen has finished init, which every loader reports after vanilla's own widgets. */
    public static void add(Minecraft client, AbstractContainerScreen<?> screen) {
        BUTTONS.remove(screen);
        // A button would do nothing on a remote server without the InventoryWizard plugin
        if (!InventoryWizard.config().showButtons || !SortActions.canSort(client)) {
            return;
        }
        boolean container = screen instanceof ContainerScreen || screen instanceof ShulkerBoxScreen;
        if (!container && !(screen instanceof InventoryScreen)) {
            return;
        }

        List<Placed> placed = new ArrayList<>();
        if (container) {
            placed.add(new Placed(button(SORT_SPRITE, "sort_container",
                    b -> SortActions.sortOpenContainer(client, screen.getMenu())), true, 1));
        }
        placed.add(new Placed(button(SORT_SPRITE, "sort_inventory",
                b -> SortActions.sortPlayer(client, SortActions.PlayerTarget.INVENTORY)), false, 1));

        Button mode = button(MODE_SPRITE, "cycle_mode", b -> {
            SortActions.cycleSortMode(client);
            b.setTooltip(modeTooltip());
        });
        mode.setTooltip(modeTooltip());
        placed.add(new Placed(mode, container, 0));

        for (Placed p : placed) {
            ((ScreenAccessor) screen).inventorywizard$addRenderableWidget(p.button());
        }
        BUTTONS.put(screen, placed);
        reposition(screen, placed);
    }

    public static void tick(Minecraft client, Screen screen) {
        List<Placed> placed = BUTTONS.get(screen);
        if (placed == null) {
            return;
        }
        reposition(screen, placed);

        // Clicking a widget focuses it, and a focused button stays highlighted; keep that only for keyboard navigation
        if (client.getLastInputType().isMouse() && placed.stream().anyMatch(p -> p.button() == screen.getFocused())) {
            screen.setFocused(null);
        }
    }

    /** The recipe book moves the screen sideways without running init again, so follow it every tick. */
    private static void reposition(Screen screen, List<Placed> placed) {
        AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) screen;
        int right = accessor.inventorywizard$getLeftPos() + accessor.inventorywizard$getImageWidth() - RIGHT_MARGIN;
        for (Placed p : placed) {
            int labelY = p.containerRow() ? accessor.inventorywizard$getTitleLabelY() : accessor.inventorywizard$getInventoryLabelY();
            p.button().setPosition(right - SIZE - p.column() * (SIZE + GAP), accessor.inventorywizard$getTopPos() + labelY - 1);
        }
    }

    private static Button button(Identifier sprite, String key, Button.OnPress onPress) {
        Component message = Component.translatable("inventorywizard.button." + key);
        Button button = SpriteIconButton.builder(message, onPress, true)
                .sprite(sprite, ICON_SIZE, ICON_SIZE)
                .size(SIZE, SIZE)
                .build();
        button.setTooltip(Tooltip.create(message));
        return button;
    }

    private static Tooltip modeTooltip() {
        return Tooltip.create(Component.translatable("inventorywizard.button.cycle_mode.tooltip",
                InventoryWizard.config().sortMode.displayName()));
    }
}
