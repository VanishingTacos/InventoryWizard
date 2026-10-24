package com.inventorywizard.neoforge;

import com.inventorywizard.InventoryWizard;
import com.inventorywizard.InventoryWizardConfigScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = InventoryWizard.MOD_ID, dist = Dist.CLIENT)
public class InventoryWizardNeoForge {

    public InventoryWizardNeoForge(IEventBus modBus, ModContainer container) {
        modBus.addListener(RegisterKeyMappingsEvent.class, event -> {
            //? if >=1.21.9
            event.registerCategory(InventoryWizard.CATEGORY);
            event.register(InventoryWizard.SORT_KEY);
            event.register(InventoryWizard.CYCLE_MODE_KEY);
        });

        // Shows a Config button in the mod list
        container.registerExtensionPoint(IConfigScreenFactory.class, (mod, parent) -> new InventoryWizardConfigScreen(parent));

        InventoryWizard.init();

        IEventBus bus = NeoForge.EVENT_BUS;
        bus.addListener(ClientTickEvent.Post.class, event -> InventoryWizard.onClientTick(Minecraft.getInstance()));

        bus.addListener(ScreenEvent.Init.Post.class, event -> InventoryWizard.onScreenInit(Minecraft.getInstance(), event.getScreen()));
        bus.addListener(ScreenEvent.MouseButtonPressed.Pre.class, event -> {
            if (InventoryWizard.handlesScreen(event.getScreen())
                    && InventoryWizard.onMouseClick(Minecraft.getInstance(), (AbstractContainerScreen<?>) event.getScreen(),
                    /*? if >=1.21.9 {*/ event.getMouseButtonEvent() /*?} else {*/ /*event.getButton() *//*?}*/)) {
                event.setCanceled(true);
            }
        });
        bus.addListener(ScreenEvent.KeyPressed.Pre.class, event -> {
            if (InventoryWizard.handlesScreen(event.getScreen())
                    && InventoryWizard.onKeyPress(Minecraft.getInstance(), (AbstractContainerScreen<?>) event.getScreen(),
                    /*? if >=1.21.9 {*/ event.getKeyEvent() /*?} else {*/ /*event.getKeyCode(), event.getScanCode() *//*?}*/)) {
                event.setCanceled(true);
            }
        });

        bus.addListener(RegisterClientCommandsEvent.class, event -> InventoryWizard.registerCommands(event.getDispatcher()));

        bus.addListener(ClientPlayerNetworkEvent.LoggingIn.class, event -> InventoryWizard.onJoinWorld(Minecraft.getInstance()));
    }
}
