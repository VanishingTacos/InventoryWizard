package com.inventorywizard;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
//? if >=1.21.11 {
import net.minecraft.world.entity.vehicle.boat.AbstractChestBoat;
import net.minecraft.world.entity.vehicle.minecart.MinecartChest;
//?} else if >=1.21.2 {
/*import net.minecraft.world.entity.vehicle.AbstractChestBoat;
import net.minecraft.world.entity.vehicle.MinecartChest;
*///?} else {
/*import net.minecraft.world.entity.vehicle.ChestBoat;
import net.minecraft.world.entity.vehicle.MinecartChest;
*///?}
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

/**
 * Runs sorts on the integrated (singleplayer or LAN-host) server and reports back
 * on the client. Changes are synced to the open screen by broadcastChanges(), so no
 * custom networking is needed. On a remote server running the InventoryWizard Paper
 * plugin, sorts are sent as its commands instead; on any other remote server there is
 * nothing to run on.
 */
public final class SortActions {

    /**
     * The Paper plugin's command, namespaced by Bukkit. The plain /iwiz is also this mod's client command,
     * which the loaders would run here instead of sending it, so remote sorts must use this one.
     */
    private static final String PLUGIN_COMMAND = "inventorywizard:iwiz";

    private static final int HOTBAR_START = 0;
    private static final int INVENTORY_START = 9;
    private static final int INVENTORY_END = 36;

    private SortActions() {
    }

    public enum PlayerTarget {
        HOTBAR,
        INVENTORY,
        ALL
    }

    private enum ContainerKind {
        CHEST("chest"),
        SHULKER_BOX("shulker_box"),
        BARREL("barrel"),
        ENDER_CHEST("ender_chest");

        private final String key;

        ContainerKind(String key) {
            this.key = key;
        }

        Component displayName() {
            return Component.translatable("inventorywizard.container." + key);
        }

        boolean enabled(InventoryWizardConfig.Features features) {
            return switch (this) {
                case CHEST -> features.chestSorting;
                case SHULKER_BOX -> features.shulkerSorting;
                case BARREL -> features.barrelSorting;
                case ENDER_CHEST -> features.enderChestSorting;
            };
        }
    }

    private record Feedback(Component message, float pitchOffset) {
    }

    /** Whether a slot in the open menu is something we can sort. Decided client-side, before cancelling the click. */
    public static boolean isSortableSlot(AbstractContainerMenu menu, Slot slot) {
        if (slot.container instanceof Inventory) {
            return slot.getContainerSlot() >= HOTBAR_START && slot.getContainerSlot() < INVENTORY_END;
        }
        return menu instanceof ChestMenu || menu instanceof ShulkerBoxMenu;
    }

    /** Player inventory slots (hotbar and main inventory) can be locked; containers are shared with other players. */
    public static boolean isLockableSlot(Slot slot) {
        return slot.container instanceof Inventory && slot.getContainerSlot() >= HOTBAR_START && slot.getContainerSlot() < INVENTORY_END;
    }

    public static boolean isLocked(Slot slot) {
        return isLockableSlot(slot) && InventoryWizard.config().lockedSlots.contains(slot.getContainerSlot());
    }

    public static void toggleLock(Minecraft client, Slot slot) {
        InventoryWizardConfig config = InventoryWizard.config();
        int index = slot.getContainerSlot();
        boolean locked = config.lockedSlots.add(index);
        if (!locked) {
            config.lockedSlots.remove(index);
        }
        config.save();
        String key = locked ? "slot_locked" : "slot_unlocked";
        show(client, new Feedback(Component.translatable("inventorywizard.message." + key), locked ? 0.4f : 0.2f));
    }

    public static boolean isHotbarSlot(Slot slot) {
        return slot.container instanceof Inventory && slot.getContainerSlot() >= HOTBAR_START && slot.getContainerSlot() < INVENTORY_START;
    }

    /** Sorts whatever the given slot belongs to: the hotbar, the main inventory, or the open container. */
    public static void sortSlot(Minecraft client, AbstractContainerMenu menu, Slot slot) {
        if (isHotbarSlot(slot)) {
            sortPlayer(client, PlayerTarget.ALL);
        } else if (slot.container instanceof Inventory) {
            sortPlayer(client, PlayerTarget.INVENTORY);
        } else {
            sortContainer(client, menu.containerId, slot.index);
        }
    }

    /** Sorts the container part of the open menu, without needing a hovered slot. */
    public static void sortOpenContainer(Minecraft client, AbstractContainerMenu menu) {
        menu.slots.stream()
                .filter(s -> !(s.container instanceof Inventory))
                .findFirst()
                .ifPresent(s -> sortSlot(client, menu, s));
    }

    public static void sortPlayer(Minecraft client, PlayerTarget target) {
        if (sendToPlugin(client, target.name().toLowerCase(Locale.ROOT))) {
            return;
        }
        InventoryWizardConfig config = InventoryWizard.config();
        SortMode mode = config.sortMode;
        // Copied here: the sort runs on the server thread while the client may toggle locks
        Set<Integer> locked = Set.copyOf(config.lockedSlots);

        runOnServer(client, player -> {
            boolean hotbar = target != PlayerTarget.INVENTORY && config.features.hotbarSorting;
            boolean inventory = target != PlayerTarget.HOTBAR && config.features.inventorySorting;
            if (!hotbar && !inventory) {
                return new Feedback(Component.translatable("inventorywizard.message.disabled"), 0f);
            }

            Inventory playerInventory = player.getInventory();
            if (inventory) {
                sortInventoryRange(playerInventory, INVENTORY_START, INVENTORY_END, locked, mode, InventorySorter.Region.INVENTORY, config);
            }
            if (hotbar) {
                sortInventoryRange(playerInventory, HOTBAR_START, INVENTORY_START, locked, mode, InventorySorter.Region.HOTBAR, config);
            }
            player.containerMenu.broadcastChanges();

            String key = hotbar && inventory ? "all_sorted" : hotbar ? "hotbar_sorted" : "inventory_sorted";
            float pitchOffset = hotbar && inventory ? -0.2f : hotbar ? 0.2f : 0f;
            return new Feedback(Component.translatable("inventorywizard.message." + key, mode.displayName()), pitchOffset);
        });
    }

    private static void sortContainer(Minecraft client, int containerId, int slotIndex) {
        // The plugin sorts whichever container is open, which is the only one a slot here can belong to
        if (sendToPlugin(client, "container")) {
            return;
        }
        InventoryWizardConfig config = InventoryWizard.config();
        SortMode mode = config.sortMode;

        runOnServer(client, player -> {
            AbstractContainerMenu menu = player.containerMenu;
            if (menu.containerId != containerId || slotIndex < 0 || slotIndex >= menu.slots.size()) {
                return null;
            }

            Container container = menu.getSlot(slotIndex).container;
            ContainerKind kind = classify(container);
            if (kind == null) {
                // The click was already cancelled on the client, so say why nothing happened
                return new Feedback(Component.translatable("inventorywizard.message.unsupported_container"), 0f);
            }
            if (!kind.enabled(config.features)) {
                return new Feedback(Component.translatable("inventorywizard.message.disabled"), 0f);
            }

            List<Slot> slots = new ArrayList<>();
            for (Slot slot : menu.slots) {
                if (slot.container == container) {
                    slots.add(slot);
                }
            }
            List<ItemStack> items = new ArrayList<>();
            for (Slot slot : slots) {
                items.add(slot.getItem().copy());
            }

            List<ItemStack> sorted = InventorySorter.sort(items, mode, InventorySorter.Region.CONTAINER, config);
            if (sorted.size() > slots.size()) {
                return null;
            }
            for (int i = 0; i < slots.size(); i++) {
                slots.get(i).set(i < sorted.size() ? sorted.get(i) : ItemStack.EMPTY);
            }
            menu.broadcastChanges();

            return new Feedback(Component.translatable("inventorywizard.message.container_sorted", kind.displayName(), mode.displayName()), 0f);
        });
    }

    public static void cycleSortMode(Minecraft client) {
        InventoryWizardConfig config = InventoryWizard.config();
        config.sortMode = config.sortMode.next();
        config.save();
        // Set rather than cycled on the plugin, so both end up on the mode shown here; the plugin confirms it
        if (sendToPlugin(client, "mode " + config.sortMode.name().toLowerCase(Locale.ROOT))) {
            return;
        }
        show(client, new Feedback(Component.translatable("inventorywizard.message.mode_changed", config.sortMode.displayName()), 0.4f));
    }

    /** Sorts slots [start, end) in place; locked slots keep their item and are skipped over. */
    private static void sortInventoryRange(Inventory inventory, int start, int end, Set<Integer> locked, SortMode mode,
                                           InventorySorter.Region region, InventoryWizardConfig config) {
        List<Integer> slots = new ArrayList<>();
        List<ItemStack> items = new ArrayList<>();
        for (int i = start; i < end; i++) {
            if (!locked.contains(i)) {
                slots.add(i);
                items.add(inventory.getItem(i).copy());
            }
        }

        List<ItemStack> sorted = InventorySorter.sort(items, mode, region, config);
        if (sorted.size() > slots.size()) {
            return;
        }
        for (int i = 0; i < slots.size(); i++) {
            inventory.setItem(slots.get(i), i < sorted.size() ? sorted.get(i) : ItemStack.EMPTY);
        }
    }

    private static ContainerKind classify(Container container) {
        if (container instanceof ShulkerBoxBlockEntity) return ContainerKind.SHULKER_BOX;
        if (container instanceof BarrelBlockEntity) return ContainerKind.BARREL;
        if (container instanceof PlayerEnderChestContainer) return ContainerKind.ENDER_CHEST;
        if (container instanceof ChestBlockEntity || container instanceof CompoundContainer
                //? if >=1.21.2 {
                || container instanceof MinecartChest || container instanceof AbstractChestBoat) {
                //?} else
                //|| container instanceof MinecartChest || container instanceof ChestBoat) {
            return ContainerKind.CHEST;
        }
        return null;
    }

    /** Whether sorting can run here: in singleplayer, as the LAN host, or on a server with the InventoryWizard plugin. */
    public static boolean canSort(Minecraft client) {
        return client.getSingleplayerServer() != null || hasPlugin(client);
    }

    /**
     * Whether this is a remote server that runs the InventoryWizard Paper plugin. Servers only send the
     * commands a player may use, so this also means the player has the plugin's inventorywizard.use permission.
     */
    public static boolean hasPlugin(Minecraft client) {
        ClientPacketListener connection = client.getConnection();
        return client.getSingleplayerServer() == null && connection != null
                && connection.getCommands().getRoot().getChild(PLUGIN_COMMAND) != null;
    }

    /** Sends {@code /inventorywizard:iwiz <args>} if the server has the plugin; the plugin sends its own feedback. */
    private static boolean sendToPlugin(Minecraft client, String args) {
        if (!hasPlugin(client)) {
            return false;
        }
        client.getConnection().sendCommand(PLUGIN_COMMAND + " " + args);
        return true;
    }

    private static void runOnServer(Minecraft client, Function<ServerPlayer, Feedback> action) {
        IntegratedServer server = client.getSingleplayerServer();
        if (server == null || client.player == null) {
            show(client, new Feedback(Component.translatable("inventorywizard.message.singleplayer_only"), 0f));
            return;
        }

        UUID playerId = client.player.getUUID();
        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player == null || player.isSpectator()) {
                return;
            }
            Feedback feedback = action.apply(player);
            if (feedback != null) {
                client.execute(() -> show(client, feedback));
            }
        });
    }

    private static void show(Minecraft client, Feedback feedback) {
        InventoryWizardConfig config = InventoryWizard.config();

        if (config.playSound) {
            playSound(client, feedback.pitchOffset());
        }
        if (config.showMessages && client.player != null) {
            //? if >=26.1 {
            client.player.sendOverlayMessage(feedback.message());
            //?} else
            //client.player.displayClientMessage(feedback.message(), true);
        }
    }

    /** Plays the configured sort sound, whether or not sounds are turned on; the settings screen previews it this way. */
    public static void playSound(Minecraft client, float pitchOffset) {
        InventoryWizardConfig.Sound config = InventoryWizard.config().sound;
        Identifier id = Identifier.tryParse(config.id);
        //? if >=1.21.2 {
        SoundEvent sound = id == null ? null : BuiltInRegistries.SOUND_EVENT.getValue(id);
        //?} else
        //SoundEvent sound = id == null ? null : BuiltInRegistries.SOUND_EVENT.get(id);
        if (sound != null) {
            client.getSoundManager().play(SimpleSoundInstance.forUI(sound, config.pitch + pitchOffset, config.volume));
        }
    }
}
