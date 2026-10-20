package com.inventorywizard;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
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

/**
 * Asks Modrinth for the newest Fabric release built for the running Minecraft version and,
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
        FabricLoader loader = FabricLoader.getInstance();
        Version current = loader.getModContainer(InventoryWizardClient.MOD_ID).orElseThrow().getMetadata().getVersion();
        String minecraft = loader.getModContainer("minecraft").orElseThrow().getMetadata().getVersion().getFriendlyString();

        String query = "loaders=" + encode("[\"fabric\"]") + "&game_versions=" + encode("[\"" + minecraft + "\"]");
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.modrinth.com/v2/project/" + PROJECT + "/version?" + query))
                // Modrinth asks API clients to identify themselves
                .header("User-Agent", "VanishingTacos/InventoryWizard/" + current.getFriendlyString() + " (github.com/VanishingTacos/InventoryWizard)")
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()
                .sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() != 200) {
                        InventoryWizardClient.LOGGER.warn("Update check failed: HTTP {}", response.statusCode());
                        return;
                    }
                    available = newestRelease(response.body(), current);
                    if (available != null) {
                        InventoryWizardClient.LOGGER.info("InventoryWizard {} is available (you have {}): {}",
                                available.version(), current.getFriendlyString(), available.page());
                        Minecraft client = Minecraft.getInstance();
                        // The player may already be in a world by the time the response arrives
                        client.execute(() -> notifyPlayer(client));
                    }
                })
                .exceptionally(e -> {
                    InventoryWizardClient.LOGGER.warn("Update check failed: {}", e.toString());
                    return null;
                });
    }

    /** Modrinth lists versions newest first; picks the first full release that beats {@code current}. */
    private static Update newestRelease(String body, Version current) {
        JsonArray versions = JsonParser.parseString(body).getAsJsonArray();
        for (JsonElement element : versions) {
            JsonObject version = element.getAsJsonObject();
            if (!"release".equals(version.get("version_type").getAsString())) {
                continue;
            }
            String number = version.get("version_number").getAsString();
            try {
                if (Version.parse(stripPrefix(number)).compareTo(current) > 0) {
                    return new Update(number, URI.create(PROJECT_URL + "/version/" + version.get("id").getAsString()));
                }
            } catch (VersionParsingException e) {
                InventoryWizardClient.LOGGER.debug("Skipping unparseable version {}", number);
                continue;
            }
            return null;
        }
        return null;
    }

    public static void notifyPlayer(Minecraft client) {
        Update update = available;
        if (update == null || notified || client.player == null || !InventoryWizardClient.config().checkForUpdates) {
            return;
        }
        notified = true;

        Component link = Component.translatable("inventorywizard.update.download")
                .withStyle(style -> style
                        .withColor(ChatFormatting.AQUA)
                        .withUnderlined(true)
                        .withClickEvent(new ClickEvent.OpenUrl(update.page()))
                        .withHoverEvent(new HoverEvent.ShowText(Component.literal(update.page().toString()))));
        client.player.sendSystemMessage(
                Component.translatable("inventorywizard.update.available", update.version(), link).withStyle(ChatFormatting.GOLD));
    }

    private static String stripPrefix(String version) {
        return version.startsWith("v") || version.startsWith("V") ? version.substring(1) : version;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
