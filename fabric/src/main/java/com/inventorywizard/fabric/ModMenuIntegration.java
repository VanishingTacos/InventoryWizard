package com.inventorywizard.fabric;

import com.inventorywizard.InventoryWizardConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/** Picked up by Mod Settings (pause menu button / F6) and Mod Menu; only loaded when one of them is installed. */
public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return InventoryWizardConfigScreen::new;
    }
}
