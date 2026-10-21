package com.inventorywizard.forge;

import com.inventorywizard.InventoryWizard;
import com.inventorywizard.InventoryWizardConfigScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/** Client-only (clientSideOnly in mods.toml). */
@Mod(InventoryWizard.MOD_ID)
public class InventoryWizardForge {

    public InventoryWizardForge(FMLJavaModLoadingContext context) {
        RegisterKeyMappingsEvent.BUS.addListener(event -> {
            event.register(InventoryWizard.SORT_KEY);
            event.register(InventoryWizard.CYCLE_MODE_KEY);
        });

        // Shows a Config button in the mod list
        context.registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(InventoryWizardConfigScreen::new));

        InventoryWizard.init();

        TickEvent.ClientTickEvent.Post.BUS.addListener(event -> InventoryWizard.onClientTick(Minecraft.getInstance()));

        // Returning true cancels the event
        ScreenEvent.MouseButtonPressed.Pre.BUS.addListener(event -> InventoryWizard.handlesScreen(event.getScreen())
                && InventoryWizard.onMouseClick(Minecraft.getInstance(), (AbstractContainerScreen<?>) event.getScreen(), event.getInfo()));
        ScreenEvent.KeyPressed.Pre.BUS.addListener(event -> InventoryWizard.handlesScreen(event.getScreen())
                && InventoryWizard.onKeyPress(Minecraft.getInstance(), (AbstractContainerScreen<?>) event.getScreen(), event.getInfo()));

        RegisterClientCommandsEvent.BUS.addListener(event -> InventoryWizard.registerCommands(event.getDispatcher()));

        ClientPlayerNetworkEvent.LoggingIn.BUS.addListener(event -> InventoryWizard.onJoinWorld(Minecraft.getInstance()));
    }
}
