package com.inventorywizard;

import com.mojang.serialization.Codec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.function.Consumer;

/**
 * Vanilla-style settings screen, opened from Mod Settings (pause menu / F6) or Mod Menu.
 * Changes apply immediately and are written to config/inventorywizard.json when the screen closes.
 * Sound id/pitch and hotbar priorities stay JSON-only.
 */
public class InventoryWizardConfigScreen extends OptionsSubScreen {

    private final InventoryWizardConfig config = InventoryWizard.config();

    public InventoryWizardConfigScreen(Screen lastScreen) {
        super(lastScreen, Minecraft.getInstance().options, Component.translatable("inventorywizard.config.title"));
    }

    @Override
    protected void addOptions() {
        list.addBig(new OptionInstance<>(
                "inventorywizard.config.sort_mode",
                OptionInstance.noTooltip(),
                // CycleButton adds the caption itself
                (caption, mode) -> mode.displayName(),
                new OptionInstance.Enum<>(Arrays.asList(SortMode.values()), Codec.INT.xmap(i -> SortMode.values()[i], Enum::ordinal)),
                config.sortMode,
                mode -> config.sortMode = mode));

        //? if >=1.21.11
        list.addHeader(Component.translatable("inventorywizard.config.header.feedback"));
        list.addSmall(
                bool("show_messages", config.showMessages, v -> config.showMessages = v),
                bool("show_buttons", config.showButtons, v -> config.showButtons = v),
                bool("play_sound", config.playSound, v -> config.playSound = v),
                bool("check_for_updates", config.checkForUpdates, v -> config.checkForUpdates = v));
        list.addBig(new OptionInstance<>(
                "inventorywizard.config.sound_volume",
                OptionInstance.noTooltip(),
                (caption, volume) -> Options.genericValueLabel(caption, Component.literal(Math.round(volume * 100) + "%")),
                OptionInstance.UnitDouble.INSTANCE,
                (double) Math.clamp(config.sound.volume, 0f, 1f),
                volume -> config.sound.volume = volume.floatValue()));

        InventoryWizardConfig.Features features = config.features;
        //? if >=1.21.11
        list.addHeader(Component.translatable("inventorywizard.config.header.features"));
        list.addSmall(
                bool("feature.chest", features.chestSorting, v -> features.chestSorting = v),
                bool("feature.barrel", features.barrelSorting, v -> features.barrelSorting = v),
                bool("feature.shulker_box", features.shulkerSorting, v -> features.shulkerSorting = v),
                bool("feature.ender_chest", features.enderChestSorting, v -> features.enderChestSorting = v),
                bool("feature.inventory", features.inventorySorting, v -> features.inventorySorting = v),
                bool("feature.hotbar", features.hotbarSorting, v -> features.hotbarSorting = v),
                bool("scroll_adjusts_stack", config.scrollAdjustsStack, v -> config.scrollAdjustsStack = v));

        //? if >=1.21.11
        list.addHeader(Component.translatable("inventorywizard.config.header.partial_stacks"));
        list.addSmall(
                bool("partial_stacks.chest", config.allowPartialStacksChest, v -> config.allowPartialStacksChest = v),
                bool("partial_stacks.inventory", config.allowPartialStacksInventory, v -> config.allowPartialStacksInventory = v));
    }

    private static OptionInstance<Boolean> bool(String key, boolean value, Consumer<Boolean> setter) {
        String translationKey = "inventorywizard.config." + key;
        return OptionInstance.createBoolean(
                translationKey,
                OptionInstance.cachedConstantTooltip(Component.translatable(translationKey + ".tooltip")),
                value,
                setter::accept);
    }

    @Override
    public void removed() {
        super.removed();
        config.save();
    }
}
