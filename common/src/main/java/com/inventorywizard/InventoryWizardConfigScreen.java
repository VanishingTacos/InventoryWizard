package com.inventorywizard;

import com.inventorywizard.gui.SectionHeader;
import com.inventorywizard.gui.SortPreview;
import com.inventorywizard.gui.TitleBanner;
import com.inventorywizard.gui.WidgetIcon;
import com.mojang.serialization.Codec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.Arrays;
import java.util.function.Consumer;

/**
 * Settings screen, opened from Mod Settings (pause menu / F6) or Mod Menu: vanilla option widgets
 * dressed up with a shimmering title, item icons and a live preview of the sort mode (see the gui package).
 * Changes apply immediately and are written to config/inventorywizard.json when the screen closes.
 * The sound id and hotbar priorities stay JSON-only.
 */
public class InventoryWizardConfigScreen extends OptionsSubScreen {

    /** Width of a full-width row in the options list (two 150-wide buttons and the gap between them). */
    private static final int ROW_WIDTH = 310;
    /** Room for the double-size title and the tip line under it. */
    private static final int HEADER_HEIGHT = 44;
    private static final int ON_COLOR = 0x6BE07A;
    private static final int OFF_COLOR = 0xE0605A;

    private final InventoryWizardConfig config = InventoryWizard.config();

    private TitleBanner banner;
    private SortPreview preview;
    // Kept to grey out the options that do nothing with the current settings
    private OptionInstance<Double> volume;
    private OptionInstance<Integer> pitch;
    private Button previewSound;
    private OptionInstance<Boolean> partialStacksChest;
    private OptionInstance<Boolean> partialStacksInventory;
    private Button unlockSlots;

    public InventoryWizardConfigScreen(Screen lastScreen) {
        super(lastScreen, Minecraft.getInstance().options, Component.translatable("inventorywizard.config.title"));
    }

    @Override
    protected void addTitle() {
        layout.setHeaderHeight(HEADER_HEIGHT);
        banner = layout.addToHeader(new TitleBanner(width, HEADER_HEIGHT, title));
    }

    @Override
    protected void repositionElements() {
        if (banner != null) {
            banner.setWidth(width);
        }
        super.repositionElements();
    }

    @Override
    protected void addOptions() {
        header("sorting");
        OptionInstance<SortMode> sortMode = new OptionInstance<>(
                "inventorywizard.config.sort_mode",
                mode -> Tooltip.create(mode.description()),
                // CycleButton adds the caption itself
                (caption, mode) -> mode.displayName(),
                new OptionInstance.Enum<>(Arrays.asList(SortMode.values()), Codec.INT.xmap(i -> SortMode.values()[i], Enum::ordinal)),
                config.sortMode,
                mode -> {
                    config.sortMode = mode;
                    updateActiveStates();
                });
        list.addBig(sortMode);
        icon(sortMode, Items.HOPPER);
        preview = new SortPreview(ROW_WIDTH);
        list.addSmall(preview, null);
        addIconRow(
                bool("show_buttons", config.showButtons, v -> config.showButtons = v), Items.STONE_BUTTON,
                bool("scroll_adjusts_stack", config.scrollAdjustsStack, v -> config.scrollAdjustsStack = v), Items.FEATHER);

        header("feedback");
        addIconRow(
                bool("show_messages", config.showMessages, v -> config.showMessages = v), Items.WRITABLE_BOOK,
                bool("play_sound", config.playSound, v -> {
                    config.playSound = v;
                    updateActiveStates();
                }), Items.NOTE_BLOCK);
        volume = new OptionInstance<>(
                "inventorywizard.config.sound_volume",
                tooltip("sound_volume"),
                (caption, value) -> Options.genericValueLabel(caption, Component.literal(Math.round(value * 100) + "%")),
                OptionInstance.UnitDouble.INSTANCE,
                (double) Math.clamp(config.sound.volume, 0f, 1f),
                value -> config.sound.volume = value.floatValue());
        // In tenths, so the slider steps through 0.5x, 0.6x ... 2.0x like the game's pitch range
        pitch = new OptionInstance<>(
                "inventorywizard.config.sound_pitch",
                tooltip("sound_pitch"),
                (caption, value) -> Options.genericValueLabel(caption, Component.literal(value / 10 + "." + value % 10 + "x")),
                new OptionInstance.IntRange(5, 20),
                Math.clamp(Math.round(config.sound.pitch * 10), 5, 20),
                value -> config.sound.pitch = value / 10f);
        list.addSmall(volume, pitch);
        previewSound = Button.builder(Component.translatable("inventorywizard.config.preview_sound"),
                        b -> SortActions.playSound(minecraft, 0f))
                .tooltip(Tooltip.create(Component.translatable("inventorywizard.config.preview_sound.tooltip")))
                .width(ROW_WIDTH)
                .build();
        WidgetIcon.set(previewSound, Items.JUKEBOX);
        list.addSmall(previewSound, null);

        InventoryWizardConfig.Features features = config.features;
        header("features");
        addIconRow(
                bool("feature.chest", features.chestSorting, v -> features.chestSorting = v), Items.CHEST,
                bool("feature.barrel", features.barrelSorting, v -> features.barrelSorting = v), Items.BARREL);
        addIconRow(
                bool("feature.shulker_box", features.shulkerSorting, v -> features.shulkerSorting = v), Items.SHULKER_BOX,
                bool("feature.ender_chest", features.enderChestSorting, v -> features.enderChestSorting = v), Items.ENDER_CHEST);
        addIconRow(
                bool("feature.inventory", features.inventorySorting, v -> features.inventorySorting = v), Items.CRAFTING_TABLE,
                bool("feature.hotbar", features.hotbarSorting, v -> features.hotbarSorting = v), Items.DIAMOND_SWORD);

        header("partial_stacks");
        partialStacksChest = bool("partial_stacks.chest", config.allowPartialStacksChest, v -> {
            config.allowPartialStacksChest = v;
            updateActiveStates();
        });
        partialStacksInventory = bool("partial_stacks.inventory", config.allowPartialStacksInventory, v -> {
            config.allowPartialStacksInventory = v;
            updateActiveStates();
        });
        addIconRow(partialStacksChest, Items.CHEST_MINECART, partialStacksInventory, Items.BUNDLE);

        header("other");
        AbstractWidget checkForUpdates = bool("check_for_updates", config.checkForUpdates, v -> config.checkForUpdates = v)
                .createButton(options);
        WidgetIcon.set(checkForUpdates, Items.CLOCK);
        // Choosing here counts as having seen the notice, so turning it on starts sending from the next game start
        AbstractWidget usageStats = bool("usage_stats", config.usageStats, v -> {
            config.usageStats = v;
            config.usageStatsNoticeShown = true;
        }).createButton(options);
        WidgetIcon.set(usageStats, Items.SPYGLASS);
        list.addSmall(checkForUpdates, usageStats);
        Button keyBinds = Button.builder(Component.translatable("controls.keybinds"),
                        b -> open(new KeyBindsScreen(this, options)))
                .tooltip(Tooltip.create(Component.translatable("inventorywizard.config.keybinds.tooltip")))
                .width(ROW_WIDTH)
                .build();
        WidgetIcon.set(keyBinds, Items.LEVER);
        list.addSmall(keyBinds, null);
        unlockSlots = Button.builder(unlockSlotsLabel(), b -> {
                    config.lockedSlots.clear();
                    b.setMessage(unlockSlotsLabel());
                    updateActiveStates();
                })
                .tooltip(Tooltip.create(Component.translatable("inventorywizard.config.unlock_slots.tooltip")))
                .width(ROW_WIDTH)
                .build();
        WidgetIcon.set(unlockSlots, Items.TRIPWIRE_HOOK);
        list.addSmall(unlockSlots, null);

        updateActiveStates();
    }

    @Override
    protected void addFooter() {
        LinearLayout footer = layout.addToFooter(LinearLayout.horizontal().spacing(8));
        footer.addChild(Button.builder(Component.translatable("inventorywizard.config.reset"), b -> confirmReset())
                .tooltip(Tooltip.create(Component.translatable("inventorywizard.config.reset.tooltip")))
                .width(150)
                .build());
        footer.addChild(Button.builder(CommonComponents.GUI_DONE, b -> onClose()).width(150).build());
    }

    private void confirmReset() {
        open(new ConfirmScreen(
                confirmed -> {
                    if (confirmed) {
                        InventoryWizard.resetConfig();
                    }
                    // A fresh screen picks up the new values
                    open(new InventoryWizardConfigScreen(lastScreen));
                },
                Component.translatable("inventorywizard.config.reset.confirm.title"),
                Component.translatable("inventorywizard.config.reset.confirm.message")));
    }

    private void open(Screen screen) {
        //? if >=26.2 {
        minecraft.gui.setScreen(screen);
        //?} else
        //minecraft.setScreen(screen);
    }

    private void updateActiveStates() {
        // The preview follows the sort mode and the partial stack settings
        preview.refresh(config);
        setActive(volume, config.playSound);
        setActive(pitch, config.playSound);
        previewSound.active = config.playSound;
        // Stack-based sorting never merges stacks, so there are no leftovers to place
        setActive(partialStacksChest, config.sortMode != SortMode.STACK_BASED);
        setActive(partialStacksInventory, config.sortMode != SortMode.STACK_BASED);
        unlockSlots.active = !config.lockedSlots.isEmpty();
    }

    private void setActive(OptionInstance<?> option, boolean active) {
        AbstractWidget widget = list.findOption(option);
        if (widget != null) {
            widget.active = active;
        }
    }

    private void addIconRow(OptionInstance<?> left, Item leftIcon, OptionInstance<?> right, Item rightIcon) {
        list.addSmall(left, right);
        icon(left, leftIcon);
        icon(right, rightIcon);
    }

    private void icon(OptionInstance<?> option, Item item) {
        WidgetIcon.set(list.findOption(option), item);
    }

    private Component unlockSlotsLabel() {
        return Component.translatable("inventorywizard.config.unlock_slots", config.lockedSlots.size());
    }

    private void header(String key) {
        list.addSmall(new SectionHeader(ROW_WIDTH, Component.translatable("inventorywizard.config.header." + key)), null);
    }

    private static OptionInstance<Boolean> bool(String key, boolean value, Consumer<Boolean> setter) {
        String translationKey = "inventorywizard.config." + key;
        // CycleButton adds the caption itself
        return OptionInstance.createBoolean(translationKey, tooltip(key),
                (caption, on) -> on
                        ? CommonComponents.OPTION_ON.copy().withColor(ON_COLOR)
                        : CommonComponents.OPTION_OFF.copy().withColor(OFF_COLOR),
                value, setter::accept);
    }

    private static <T> OptionInstance.TooltipSupplier<T> tooltip(String key) {
        return OptionInstance.cachedConstantTooltip(Component.translatable("inventorywizard.config." + key + ".tooltip"));
    }

    @Override
    public void removed() {
        super.removed();
        config.save();
    }
}
