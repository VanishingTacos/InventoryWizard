package com.inventorywizard;

import com.google.gson.JsonObject;
import com.inventorywizard.platform.Platform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Pseudonymous usage stats, only with the player's consent: at game start, one small JSON message to
 * api.vanishingtacos.com with a random install id and the mod, Minecraft, loader, Java and OS versions
 * (see PRIVACY.md). The player is asked once, on the title screen; nothing is sent until they choose to
 * share. Turning it off asks the server to delete this install's data. Dev runs never ask or send
 * ({@code -Dinventorywizard.usageStats=false}). Failures are logged and otherwise ignored.
 */
public final class UsageStats {

    private static final URI ENDPOINT = URI.create(System.getProperty(
            "inventorywizard.usageStats.url", "https://api.vanishingtacos.com/v1/inventorywizard/heartbeat"));

    private static boolean askOnNextTick;

    private UsageStats() {}

    /** Whether this run (a dev or test run) has usage stats switched off entirely. */
    private static boolean disabledForRun() {
        return "false".equalsIgnoreCase(System.getProperty("inventorywizard.usageStats"));
    }

    /** At game start: send stats if the player agreed, or finish deleting them if they withdrew. */
    public static void start() {
        InventoryWizardConfig config = InventoryWizard.config();
        if (disabledForRun()) {
            return;
        }
        if (!config.usageStats) {
            // A delete that failed last time (offline, server down) is retried until it goes through
            forget();
            return;
        }
        if (!config.usageStatsAsked) {
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

        send("POST", body).thenAccept(status -> {
            if (status / 100 == 2) {
                InventoryWizard.LOGGER.info("Sent usage stats (turn off Usage Stats in the settings to stop and delete them)");
            } else if (status > 0) {
                InventoryWizard.LOGGER.warn("Usage stats not accepted: HTTP {}", status);
            }
        });
    }

    /**
     * Asks the server to delete everything stored for this install, then forgets the install id so a
     * later opt-in starts fresh. Does nothing if stats were never sent.
     */
    public static void forget() {
        InventoryWizardConfig config = InventoryWizard.config();
        String installId = config.installId;
        if (disabledForRun() || installId == null) {
            return;
        }
        JsonObject body = new JsonObject();
        body.addProperty("installId", installId);
        send("DELETE", body).thenAccept(status -> {
            if (status / 100 != 2) {
                if (status > 0) {
                    InventoryWizard.LOGGER.warn("Usage stats not deleted yet: HTTP {}; will retry at next start", status);
                }
                return;
            }
            InventoryWizard.LOGGER.info("Deleted this install's usage stats");
            Minecraft.getInstance().execute(() -> {
                // Unless the player opted back in meanwhile, which keeps using the same id
                if (!config.usageStats && installId.equals(config.installId)) {
                    config.installId = null;
                    config.save();
                }
            });
        });
    }

    /** Sends {@code body} to the endpoint; completes with the HTTP status, or 0 if the request failed. */
    private static CompletableFuture<Integer> send(String method, JsonObject body) {
        HttpRequest request = HttpRequest.newBuilder(ENDPOINT)
                .header("User-Agent", "VanishingTacos/InventoryWizard/" + Platform.INSTANCE.modVersion() + " (github.com/VanishingTacos/InventoryWizard)")
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(10))
                .method(method, HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();
        return HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()
                .sendAsync(request, HttpResponse.BodyHandlers.discarding())
                .thenApply(HttpResponse::statusCode)
                .exceptionally(e -> {
                    InventoryWizard.LOGGER.warn("Usage stats request failed: {}", e.toString());
                    return 0;
                });
    }

    // ---------- Consent ----------

    /** Called for every screen; the question waits for the title screen, once per install. */
    public static void onScreenInit(Screen screen) {
        InventoryWizardConfig config = InventoryWizard.config();
        if (screen instanceof TitleScreen && !config.usageStatsAsked && !disabledForRun()) {
            // Opening a screen from inside another screen's init is fragile; do it on the next tick
            askOnNextTick = true;
        }
    }

    public static void onClientTick(Minecraft client, Screen current) {
        if (!askOnNextTick) {
            return;
        }
        askOnNextTick = false;
        if (!(current instanceof TitleScreen title) || InventoryWizard.config().usageStatsAsked) {
            return;
        }
        setScreen(client, new ConfirmScreen(
                share -> {
                    setChoice(share);
                    setScreen(client, title);
                },
                Component.translatable("inventorywizard.usage_stats.consent.title"),
                Component.translatable("inventorywizard.usage_stats.consent.message"),
                Component.translatable("inventorywizard.usage_stats.consent.yes"),
                Component.translatable("inventorywizard.usage_stats.consent.no")));
    }

    /** Records the player's answer from the consent screen or the settings toggle. */
    public static void setChoice(boolean share) {
        InventoryWizardConfig config = InventoryWizard.config();
        boolean withdrawn = config.usageStats && !share;
        config.usageStats = share;
        config.usageStatsAsked = true;
        config.save();
        if (withdrawn) {
            forget();
        }
    }

    private static void setScreen(Minecraft client, Screen screen) {
        //? if >=26.2 {
        client.gui.setScreen(screen);
        //?} else
        //client.setScreen(screen);
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
