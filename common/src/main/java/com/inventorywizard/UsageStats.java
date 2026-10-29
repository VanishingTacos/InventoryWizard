package com.inventorywizard;

import com.google.gson.JsonObject;
import com.inventorywizard.platform.Platform;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.UUID;

/**
 * Anonymous usage stats: at game start, one small JSON message to api.vanishingtacos.com with a random
 * install id and the mod, Minecraft, loader, Java and OS versions (see PRIVACY.md). Nothing is sent
 * until the one-time chat notice has been shown, while the Usage Stats setting is off, or in dev runs
 * ({@code -Dinventorywizard.usageStats=false}). Failures are ignored; the game never waits on it.
 */
public final class UsageStats {

    private static final URI ENDPOINT = URI.create(System.getProperty(
            "inventorywizard.usageStats.url", "https://api.vanishingtacos.com/v1/inventorywizard/heartbeat"));
    private static final URI PRIVACY_URL = URI.create("https://github.com/VanishingTacos/InventoryWizard/blob/main/PRIVACY.md");

    private UsageStats() {}

    /** Whether this run (a dev or test run) has usage stats switched off entirely. */
    private static boolean disabledForRun() {
        return "false".equalsIgnoreCase(System.getProperty("inventorywizard.usageStats"));
    }

    public static void start() {
        InventoryWizardConfig config = InventoryWizard.config();
        if (disabledForRun() || !config.usageStats || !config.usageStatsNoticeShown) {
            return;
        }
        if (!isInstallId(config.installId)) {
            config.installId = UUID.randomUUID().toString();
            config.save();
        }

        Platform platform = Platform.INSTANCE;
        JsonObject body = new JsonObject();
        body.addProperty("installId", config.installId);
        body.addProperty("modVersion", platform.modVersion());
        body.addProperty("minecraftVersion", platform.minecraftVersion());
        body.addProperty("loader", platform.loaderName());
        body.addProperty("javaVersion", String.valueOf(Runtime.version().feature()));
        body.addProperty("os", osName());

        HttpRequest request = HttpRequest.newBuilder(ENDPOINT)
                .header("User-Agent", "VanishingTacos/InventoryWizard/" + platform.modVersion() + " (github.com/VanishingTacos/InventoryWizard)")
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()
                .sendAsync(request, HttpResponse.BodyHandlers.discarding())
                .thenAccept(response -> {
                    if (response.statusCode() / 100 == 2) {
                        InventoryWizard.LOGGER.info("Sent anonymous usage stats (turn off Usage Stats in the settings to stop)");
                    } else {
                        InventoryWizard.LOGGER.warn("Usage stats not accepted: HTTP {}", response.statusCode());
                    }
                })
                .exceptionally(e -> {
                    InventoryWizard.LOGGER.warn("Usage stats not sent: {}", e.toString());
                    return null;
                });
    }

    /**
     * Tells the player once, the first time they join a world, that usage stats are on and how to turn
     * them off. Stats start being sent from the next game start.
     */
    public static void notifyPlayer(Minecraft client) {
        InventoryWizardConfig config = InventoryWizard.config();
        if (disabledForRun() || config.usageStatsNoticeShown || !config.usageStats || client.player == null) {
            return;
        }
        config.usageStatsNoticeShown = true;
        config.save();

        Component privacy = ChatMessages.link(Component.translatable("inventorywizard.usage_stats.notice.privacy"), PRIVACY_URL);
        ChatMessages.send(client, Component.translatable("inventorywizard.usage_stats.notice", privacy).withStyle(ChatFormatting.GRAY));
    }

    private static boolean isInstallId(String value) {
        try {
            return value != null && UUID.fromString(value).toString().equals(value);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /** Just the OS family; the version and architecture aren't sent. */
    static String osName() {
        String name = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (name.startsWith("windows")) return "Windows";
        if (name.startsWith("mac")) return "macOS";
        if (name.contains("linux")) return "Linux";
        return "Other";
    }
}
