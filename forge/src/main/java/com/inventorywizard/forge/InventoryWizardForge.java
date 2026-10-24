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
//? if <1.21.6 {
/*import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
*///?}
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/** Client-only (clientSideOnly in mods.toml). */
@Mod(InventoryWizard.MOD_ID)
public class InventoryWizardForge {

    public InventoryWizardForge(FMLJavaModLoadingContext context) {
        //? if >=1.21.9 {
        RegisterKeyMappingsEvent.BUS.addListener(event -> {
        //?} else if >=1.21.6 {
        /*RegisterKeyMappingsEvent.getBus(context.getModBusGroup()).addListener(event -> {
        *///?} else
        //context.getModEventBus().addListener((RegisterKeyMappingsEvent event) -> {
            event.register(InventoryWizard.SORT_KEY);
            event.register(InventoryWizard.CYCLE_MODE_KEY);
        });

        // Shows a Config button in the mod list
        context.registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(InventoryWizardConfigScreen::new));

        InventoryWizard.init();

        //? if >=1.21.6 {
        TickEvent.ClientTickEvent.Post.BUS.addListener(event -> InventoryWizard.onClientTick(Minecraft.getInstance()));
        //?}

        //? if >=1.21.6
        ScreenEvent.Init.Post.BUS.addListener(event -> InventoryWizard.onScreenInit(Minecraft.getInstance(), event.getScreen()));

        // Returning true cancels the event
        //? if >=1.21.9 {
        ScreenEvent.MouseButtonPressed.Pre.BUS.addListener(event -> InventoryWizard.handlesScreen(event.getScreen())
                && InventoryWizard.onMouseClick(Minecraft.getInstance(), (AbstractContainerScreen<?>) event.getScreen(), event.getInfo()));
        ScreenEvent.KeyPressed.Pre.BUS.addListener(event -> InventoryWizard.handlesScreen(event.getScreen())
                && InventoryWizard.onKeyPress(Minecraft.getInstance(), (AbstractContainerScreen<?>) event.getScreen(), event.getInfo()));
        //?} else if >=1.21.6 {
        /*ScreenEvent.MouseButtonPressed.Pre.BUS.addListener(event -> InventoryWizard.handlesScreen(event.getScreen())
                && InventoryWizard.onMouseClick(Minecraft.getInstance(), (AbstractContainerScreen<?>) event.getScreen(), event.getButton()));
        ScreenEvent.KeyPressed.Pre.BUS.addListener(event -> InventoryWizard.handlesScreen(event.getScreen())
                && InventoryWizard.onKeyPress(Minecraft.getInstance(), (AbstractContainerScreen<?>) event.getScreen(), event.getKeyCode(), event.getScanCode()));
        *///?}

        //? if >=1.21.6 {
        RegisterClientCommandsEvent.BUS.addListener(event -> InventoryWizard.registerCommands(event.getDispatcher()));

        ClientPlayerNetworkEvent.LoggingIn.BUS.addListener(event -> InventoryWizard.onJoinWorld(Minecraft.getInstance()));
        //?} else {
        /*// Before Forge's EventBus 7 everything goes through the one game event bus and is cancelled by hand
        IEventBus bus = MinecraftForge.EVENT_BUS;
        bus.addListener((TickEvent.ClientTickEvent.Post event) -> InventoryWizard.onClientTick(Minecraft.getInstance()));
        bus.addListener((ScreenEvent.Init.Post event) -> InventoryWizard.onScreenInit(Minecraft.getInstance(), event.getScreen()));
        bus.addListener((ScreenEvent.MouseButtonPressed.Pre event) -> {
            if (InventoryWizard.handlesScreen(event.getScreen())
                    && InventoryWizard.onMouseClick(Minecraft.getInstance(), (AbstractContainerScreen<?>) event.getScreen(), event.getButton())) {
                event.setCanceled(true);
            }
        });
        bus.addListener((ScreenEvent.KeyPressed.Pre event) -> {
            if (InventoryWizard.handlesScreen(event.getScreen())
                    && InventoryWizard.onKeyPress(Minecraft.getInstance(), (AbstractContainerScreen<?>) event.getScreen(), event.getKeyCode(), event.getScanCode())) {
                event.setCanceled(true);
            }
        });
        bus.addListener((RegisterClientCommandsEvent event) -> InventoryWizard.registerCommands(event.getDispatcher()));
        bus.addListener((ClientPlayerNetworkEvent.LoggingIn event) -> InventoryWizard.onJoinWorld(Minecraft.getInstance()));
        *///?}
    }
}
