package com.inventorywizard.neoforge;

import com.inventorywizard.InventoryWizard;
import com.inventorywizard.platform.Platform;
//? if >=1.21.9 {
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
//?}
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Path;
import java.util.List;

public class NeoForgePlatform implements Platform {

    @Override
    public boolean isActive() {
        return Platform.classExists("net.neoforged.fml.loading.FMLLoader");
    }

    @Override
    public Path configDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public String modVersion() {
        return version(InventoryWizard.MOD_ID);
    }

    @Override
    public String minecraftVersion() {
        return version("minecraft");
    }

    @Override
    public List<String> modrinthLoaders() {
        return List.of("neoforge");
    }

    @Override
    public String loaderName() {
        return "neoforge";
    }

    //? if >=1.21.9 {
    /** Registered with {@code RegisterKeyMappingsEvent} in {@link InventoryWizardNeoForge}. */
    @Override
    public KeyMapping.Category keyMappingCategory(Identifier id) {
        return new KeyMapping.Category(id);
    }
    //?}

    private static String version(String modId) {
        return ModList.get().getModContainerById(modId).orElseThrow().getModInfo().getVersion().toString();
    }
}
