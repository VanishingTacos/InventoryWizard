package com.inventorywizard;

import net.minecraft.network.chat.Component;

public enum SortMode {
    DEFAULT("default"),
    ALPHABETICAL("alphabetical"),
    STACK_BASED("stack_based");

    private final String key;

    SortMode(String key) {
        this.key = key;
    }

    public Component displayName() {
        return Component.translatable("inventorywizard.mode." + key);
    }

    /** One line on how this mode orders items, for the settings screen. */
    public Component description() {
        return Component.translatable("inventorywizard.mode." + key + ".description");
    }

    public SortMode next() {
        SortMode[] modes = values();
        return modes[(ordinal() + 1) % modes.length];
    }
}
