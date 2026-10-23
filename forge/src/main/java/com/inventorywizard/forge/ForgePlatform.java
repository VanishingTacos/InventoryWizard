package com.inventorywizard.forge;

import com.inventorywizard.InventoryWizard;
import com.inventorywizard.platform.Platform;
//? if >=1.21.9 {
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
//?}
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;
import java.util.List;

public class ForgePlatform implements Platform {

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
        return List.of("forge");
    }

    //? if >=1.21.9 {
    /** Forge has no category event; the vanilla registry is how it is done there. */
    @Override
    public KeyMapping.Category keyMappingCategory(Identifier id) {
        return KeyMapping.Category.register(id);
    }
    //?}

    private static String version(String modId) {
        //? if >=26.1 {
        return ModList.getModContainerById(modId).orElseThrow().getModInfo().getVersion().toString();
        //?} else
        //return ModList.get().getModContainerById(modId).orElseThrow().getModInfo().getVersion().toString();
    }
}
