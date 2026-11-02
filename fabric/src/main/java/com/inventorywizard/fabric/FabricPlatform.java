package com.inventorywizard.fabric;

import com.inventorywizard.InventoryWizard;
import com.inventorywizard.platform.Platform;
import net.fabricmc.loader.api.FabricLoader;
//? if >=1.21.9 {
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
//?}

import java.nio.file.Path;
import java.util.List;

/** Also used on Quilt, which implements the Fabric Loader API. */
public class FabricPlatform implements Platform {

    /** Sinytra Connector brings the Fabric Loader API to NeoForge, which then loads this jar as a NeoForge mod. */
    @Override
    public boolean isActive() {
        return Platform.classExists("net.fabricmc.loader.api.FabricLoader")
                && !Platform.classExists("net.neoforged.fml.loading.FMLLoader")
                && !Platform.classExists("net.minecraftforge.fml.loading.FMLLoader");
    }

    @Override
    public Path configDir() {
        return FabricLoader.getInstance().getConfigDir();
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
        return List.of("fabric", "quilt");
    }

    @Override
    public String loaderName() {
        return Platform.classExists("org.quiltmc.loader.api.QuiltLoader") ? "quilt" : "fabric";
    }

    //? if >=1.21.9 {
    @Override
    public KeyMapping.Category keyMappingCategory(Identifier id) {
        return KeyMapping.Category.register(id);
    }
    //?}

    private static String version(String modId) {
        return FabricLoader.getInstance().getModContainer(modId).orElseThrow().getMetadata().getVersion().getFriendlyString();
    }
}
