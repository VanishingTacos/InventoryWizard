package com.inventorywizard.platform;

//? if >=1.21.9 {
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
//?}

import java.nio.file.Path;
import java.util.List;
import java.util.ServiceLoader;

/**
 * The few things common code needs from the mod loader. Each loader subproject provides one
 * implementation, listed in its META-INF/services/com.inventorywizard.platform.Platform.
 */
public interface Platform {

    Platform INSTANCE = ServiceLoader.load(Platform.class, Platform.class.getClassLoader())
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No " + Platform.class.getName() + " service found"));

    Path configDir();

    /** This mod's version, e.g. {@code 2.0.2}. */
    String modVersion();

    String minecraftVersion();

    /** Modrinth loader tags whose builds run on this loader, used by the update check. */
    List<String> modrinthLoaders();

    /** Creates the key mapping category in whatever way this loader wants it registered. */
    //? if >=1.21.9
    KeyMapping.Category keyMappingCategory(Identifier id);
}
