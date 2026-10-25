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
 * A multi-loader jar lists all of them, so the one whose loader is running is picked.
 */
public interface Platform {

    Platform INSTANCE = ServiceLoader.load(Platform.class, Platform.class.getClassLoader()).stream()
            .map(ServiceLoader.Provider::get)
            .filter(Platform::isActive)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No active " + Platform.class.getName() + " service found"));

    /** Whether this implementation's loader is the one running. Must not touch the loader's API. */
    boolean isActive();

    Path configDir();

    /** This mod's version, e.g. {@code 2.0.2}. */
    String modVersion();

    String minecraftVersion();

    /** Modrinth loader tags whose builds run on this loader, used by the update check. */
    List<String> modrinthLoaders();

    /** Creates the key mapping category in whatever way this loader wants it registered. */
    //? if >=1.21.9
    KeyMapping.Category keyMappingCategory(Identifier id);

    /** Whether {@code className} can be loaded, without initializing it. */
    static boolean classExists(String className) {
        try {
            Class.forName(className, false, Platform.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }
}
