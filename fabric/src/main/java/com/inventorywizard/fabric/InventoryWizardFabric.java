package com.inventorywizard.fabric;

import com.inventorywizard.InventoryWizard;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
//? if >=26.1 {
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
//?} else
//import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

public class InventoryWizardFabric implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        //? if >=26.1 {
        KeyMappingHelper.registerKeyMapping(InventoryWizard.SORT_KEY);
        KeyMappingHelper.registerKeyMapping(InventoryWizard.CYCLE_MODE_KEY);
        //?} else {
        /*KeyBindingHelper.registerKeyBinding(InventoryWizard.SORT_KEY);
        KeyBindingHelper.registerKeyBinding(InventoryWizard.CYCLE_MODE_KEY);
        *///?}

        InventoryWizard.init();

        ClientTickEvents.END_CLIENT_TICK.register(InventoryWizard::onClientTick);

        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            InventoryWizard.onScreenInit(client, screen);
            if (!InventoryWizard.handlesScreen(screen)) {
                return;
            }
            AbstractContainerScreen<?> containerScreen = (AbstractContainerScreen<?>) screen;
            //? if >=1.21.9 {
            ScreenMouseEvents.allowMouseClick(screen).register((s, event) -> !InventoryWizard.onMouseClick(client, containerScreen, event));
            ScreenKeyboardEvents.allowKeyPress(screen).register((s, event) -> !InventoryWizard.onKeyPress(client, containerScreen, event));
            //?} else {
            /*ScreenMouseEvents.allowMouseClick(screen).register((s, mouseX, mouseY, button) ->
                    !InventoryWizard.onMouseClick(client, containerScreen, button));
            ScreenKeyboardEvents.allowKeyPress(screen).register((s, key, scancode, modifiers) ->
                    !InventoryWizard.onKeyPress(client, containerScreen, key, scancode));
            *///?}
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) -> InventoryWizard.registerCommands(dispatcher));

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> InventoryWizard.onJoinWorld(client));
    }
}
