package com.inventorywizard;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.inventorywizard.platform.Platform;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.stream.Collectors;

/**
 * Asks Modrinth for the newest release for the running loader built for the running Minecraft version and,
 * if it is newer than this jar, tells the player in chat once per session after they join a world.
 */
public final class UpdateChecker {

    private static final String PROJECT = "inventorywizard";
    private static final String PROJECT_URL = "https://modrinth.com/mod/" + PROJECT;

    /** Set from the HTTP thread, read on the client thread. */
    private static volatile Update available;
    private static boolean notified;

    private record Update(String version, URI page) {}

    private UpdateChecker() {}

    public static void start() {
        String current = Platform.INSTANCE.modVersion();
        String minecraft = Platform.INSTANCE.minecraftVersion();
        String loaders = Platform.INSTANCE.modrinthLoaders().stream().map(l -> "\"" + l + "\"").collect(Collectors.joining(",", "[", "]"));

        String query = "loaders=" + encode(loaders) + "&game_versions=" + encode("[\"" + minecraft + "\"]");
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.modrinth.com/v2/project/" + PROJECT + "/version?" + query))
                // Modrinth asks API clients to identify themselves
                .header("User-Agent", "VanishingTacos/InventoryWizard/" + current + " (github.com/VanishingTacos/InventoryWizard)")
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()
                .sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() != 200) {
                        InventoryWizard.LOGGER.warn("Update check failed: HTTP {}", response.statusCode());
                        return;
                    }
                    available = newestRelease(response.body(), current);
                    if (available != null) {
                        InventoryWizard.LOGGER.info("InventoryWizard {} is available (you have {}): {}",
                                available.version(), current, available.page());
                        Minecraft client = Minecraft.getInstance();
                        // The player may already be in a world by the time the response arrives
                        client.execute(() -> notifyPlayer(client));
                    }
                })
                .exceptionally(e -> {
                    InventoryWizard.LOGGER.warn("Update check failed: {}", e.toString());
                    return null;
                });
    }

    /** Modrinth lists versions newest first; picks the first full release that beats {@code current}. */
    private static Update newestRelease(String body, String current) {
        JsonArray versions = JsonParser.parseString(body).getAsJsonArray();
        for (JsonElement element : versions) {
            JsonObject version = element.getAsJsonObject();
            if (!"release".equals(version.get("version_type").getAsString())) {
                continue;
            }
            String number = version.get("version_number").getAsString();
            try {
                if (compareVersions(number, current) > 0) {
                    return new Update(number, URI.create(PROJECT_URL + "/version/" + version.get("id").getAsString()));
                }
            } catch (NumberFormatException e) {
                InventoryWizard.LOGGER.debug("Skipping unparseable version {}", number);
                continue;
            }
            return null;
        }
        return null;
    }

    public static void notifyPlayer(Minecraft client) {
        Update update = available;
        if (update == null || notified || client.player == null || !InventoryWizard.config().checkForUpdates) {
            return;
        }
        notified = true;

        Component link = Component.translatable("inventorywizard.update.download")
                .withStyle(style -> style
                        .withColor(ChatFormatting.AQUA)
                        .withUnderlined(true)
                        //? if >=1.21.5 {
                        .withClickEvent(new ClickEvent.OpenUrl(update.page()))
                        .withHoverEvent(new HoverEvent.ShowText(Component.literal(update.page().toString()))));
                        //?} else {
                        /*.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, update.page().toString()))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(update.page().toString()))));
                        *///?}
        Component message = Component.translatable("inventorywizard.update.available", update.version(), link).withStyle(ChatFormatting.GOLD);
        //? if >=26.1 {
        client.player.sendSystemMessage(message);
        //?} else
        //client.player.displayClientMessage(message, false);
    }

    /**
     * Compares the dotted numeric part of two versions, so {@code v2.0.10+neoforge} beats {@code 2.0.9}.
     * Anything after {@code -} or {@code +} is ignored since only full releases are compared.
     *
     * @throws NumberFormatException if either version does not start with numbers
     */
    static int compareVersions(String a, String b) {
        String[] left = numericCore(a);
        String[] right = numericCore(b);
        for (int i = 0; i < Math.max(left.length, right.length); i++) {
            int l = i < left.length ? Integer.parseInt(left[i]) : 0;
            int r = i < right.length ? Integer.parseInt(right[i]) : 0;
            if (l != r) {
                return Integer.compare(l, r);
            }
        }
        return 0;
    }

    private static String[] numericCore(String version) {
        String core = version.startsWith("v") || version.startsWith("V") ? version.substring(1) : version;
        int suffix = core.indexOf('-');
        int build = core.indexOf('+');
        int end = suffix < 0 ? build : build < 0 ? suffix : Math.min(suffix, build);
        return (end < 0 ? core : core.substring(0, end)).split("\\.");
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
