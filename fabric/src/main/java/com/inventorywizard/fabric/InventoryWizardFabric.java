package com.inventorywizard.fabric;

import com.inventorywizard.InventoryWizard;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

public class InventoryWizardFabric implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        KeyMappingHelper.registerKeyMapping(InventoryWizard.SORT_KEY);
        KeyMappingHelper.registerKeyMapping(InventoryWizard.CYCLE_MODE_KEY);

        InventoryWizard.init();

        ClientTickEvents.END_CLIENT_TICK.register(InventoryWizard::onClientTick);

        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!InventoryWizard.handlesScreen(screen)) {
                return;
            }
            AbstractContainerScreen<?> containerScreen = (AbstractContainerScreen<?>) screen;
            ScreenMouseEvents.allowMouseClick(screen).register((s, event) -> !InventoryWizard.onMouseClick(client, containerScreen, event));
            ScreenKeyboardEvents.allowKeyPress(screen).register((s, event) -> !InventoryWizard.onKeyPress(client, containerScreen, event));
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) -> InventoryWizard.registerCommands(dispatcher));

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> InventoryWizard.onJoinWorld(client));
    }
}
