package com.inventorywizard.gametest;

import com.inventorywizard.InventoryWizard;
import com.inventorywizard.mixin.AbstractContainerScreenAccessor;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * The parts of Fabric's ClientGameTestContext the InventoryWizard tests use, built on vanilla only.
 * Every method runs on the test thread and hands work to the client or server thread; every wait has a timeout.
 */
public final class ClientTestContext {

    private static final long TICK_MILLIS = 50;
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration WORLD_TIMEOUT = Duration.ofMinutes(3);

    private static final Field LEFT_POS = field("leftPos");
    private static final Field TOP_POS = field("topPos");

    // ---- threads ----------------------------------------------------------------

    public void runOnClient(Consumer<Minecraft> action) {
        computeOnClient(client -> {
            action.accept(client);
            return null;
        });
    }

    public <T> T computeOnClient(Function<Minecraft, T> action) {
        Minecraft client = Minecraft.getInstance();
        return join(client.submit(() -> action.apply(client)), "client task");
    }

    public void runOnServer(Consumer<MinecraftServer> action) {
        computeOnServer(server -> {
            action.accept(server);
            return null;
        });
    }

    public <T> T computeOnServer(Function<MinecraftServer, T> action) {
        MinecraftServer server = computeOnClient(Minecraft::getSingleplayerServer);
        if (server == null) {
            throw new AssertionError("No singleplayer server is running");
        }
        return join(server.submit(() -> action.apply(server)), "server task");
    }

    public void runCommand(String command) {
        runOnServer(server -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command));
    }

    /** Waits roughly {@code ticks} game ticks, then for the client and (if any) server to catch up. */
    public void waitTicks(int ticks) {
        sleep(ticks * TICK_MILLIS);
        boolean hasServer = computeOnClient(client -> client.getSingleplayerServer() != null);
        if (hasServer) {
            runOnServer(server -> {});
        }
        runOnClient(client -> {});
    }

    public void waitFor(String what, Predicate<Minecraft> condition, Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (!computeOnClient(condition::test)) {
            if (System.nanoTime() > deadline) {
                Screen screen = computeOnClient(client -> client.gui.screen());
                throw new AssertionError("Timed out waiting for " + what + " (screen: " + screen + ")");
            }
            sleep(TICK_MILLIS);
        }
    }

    public void waitForScreen(Class<? extends Screen> type) {
        waitFor(type.getSimpleName(), client -> type.isInstance(client.gui.screen()), DEFAULT_TIMEOUT);
    }

    // ---- game lifecycle -----------------------------------------------------------

    /** Waits for loading to finish and skips first-launch screens such as the accessibility onboarding. */
    void waitForTitleScreen() {
        long deadline = System.nanoTime() + WORLD_TIMEOUT.toNanos();
        while (Minecraft.getInstance() == null) {
            if (System.nanoTime() > deadline) {
                throw new AssertionError("Minecraft never started");
            }
            sleep(TICK_MILLIS);
        }
        waitFor("the game to finish loading", client -> client.gui.overlay() == null && client.gui.screen() != null, WORLD_TIMEOUT);
        Screen first = computeOnClient(client -> client.gui.screen());
        // NeoForge and Forge report mod loading errors and warnings on a LoadingErrorScreen; skipping it
        // hides the problem and leaves the game half set up, so fail with what it says instead
        if (first.getClass().getSimpleName().contains("LoadingError")) {
            throw new AssertionError("The loader reported problems while loading mods (" + first.getTitle().getString()
                    + "); see the log above");
        }
        InventoryWizard.LOGGER.info("[gametest] Game loaded on {}; skipping to the title screen", first.getClass().getSimpleName());
        runOnClient(client -> {
            client.options.onboardAccessibility = false;
            client.options.save();
            client.gui.setScreen(new TitleScreen());
        });
        waitForScreen(TitleScreen.class);
    }

    /** Creates a fresh superflat survival world on peaceful and waits until the player is in it. */
    public void createWorld(String name) {
        runOnClient(client -> client.createWorldOpenFlows().createFreshLevel(
                name,
                new LevelSettings(name, GameType.SURVIVAL, new LevelSettings.DifficultySettings(Difficulty.PEACEFUL, false, false),
                        true, WorldDataConfiguration.DEFAULT),
                new WorldOptions(0L, false, false),
                registries -> registries.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
                new TitleScreen()));
        waitFor("the world to load", client -> client.level != null && client.player != null && client.gui.screen() == null, WORLD_TIMEOUT);
        waitTicks(40);
    }

    public void leaveWorld() {
        runOnClient(client -> client.disconnect(new TitleScreen(), false));
        waitForScreen(TitleScreen.class);
    }

    public void setScreen(Supplier<Screen> screen) {
        runOnClient(client -> client.gui.setScreen(screen.get()));
    }

    // ---- input ----------------------------------------------------------------------

    public void pressKey(int key) {
        pressKey(key, 0);
    }

    public void pressKey(int key, int modifiers) {
        runOnClient(client -> {
            long window = client.getWindow().handle();
            KeyEvent event = new KeyEvent(key, 0, modifiers);
            client.keyboardHandler.keyPress(window, InputConstants.PRESS, event);
            client.keyboardHandler.keyPress(window, InputConstants.RELEASE, event);
        });
    }

    public void click(int button, int modifiers) {
        runOnClient(client -> {
            long window = client.getWindow().handle();
            MouseButtonInfo info = new MouseButtonInfo(button, modifiers);
            client.mouseHandler.onButton(window, info, InputConstants.PRESS);
            client.mouseHandler.onButton(window, info, InputConstants.RELEASE);
        });
    }

    /** Moves the cursor to a GUI-scaled position, as the mouse would. */
    public void setCursorGui(double x, double y) {
        runOnClient(client -> {
            Window window = client.getWindow();
            double scale = (double) window.getScreenWidth() / window.getGuiScaledWidth();
            client.mouseHandler.onMove(window.handle(), x * scale, y * scale, 0, 0);
        });
    }

    /** Moves the cursor over a slot of the open container screen and waits until the screen reports it hovered. */
    public void hoverSlot(Predicate<Slot> which) {
        double[] position = computeOnClient(client -> {
            AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) client.gui.screen();
            Slot slot = screen.getMenu().slots.stream().filter(which).findFirst()
                    .orElseThrow(() -> new AssertionError("No such slot on " + screen));
            return new double[] {intField(LEFT_POS, screen) + slot.x + 8, intField(TOP_POS, screen) + slot.y + 8};
        });
        setCursorGui(position[0], position[1]);
        // Hovering is worked out while rendering, so wait for a frame with the new position
        waitFor("the cursor to be over the slot", client -> {
            Slot hovered = ((AbstractContainerScreenAccessor) client.gui.screen()).inventorywizard$getHoveredSlot();
            return hovered != null && which.test(hovered);
        }, Duration.ofSeconds(5));
    }

    /** Option buttons are labelled "Caption: Value", so find them by the start of their text. */
    public void clickWidget(String labelPrefix) {
        double[] position = computeOnClient(client -> {
            AbstractWidget widget = findWidget(client.gui.screen(), labelPrefix);
            if (widget == null) {
                throw new AssertionError("No widget labelled '" + labelPrefix + "...' on " + client.gui.screen());
            }
            return new double[] {widget.getX() + widget.getWidth() / 2.0, widget.getY() + widget.getHeight() / 2.0};
        });
        setCursorGui(position[0], position[1]);
        waitTicks(2);
        click(InputConstants.MOUSE_BUTTON_LEFT, 0);
        waitTicks(2);
    }

    private static AbstractWidget findWidget(ContainerEventHandler parent, String labelPrefix) {
        for (GuiEventListener child : parent.children()) {
            if (child instanceof AbstractWidget widget && widget.getMessage().getString().startsWith(labelPrefix)) {
                return widget;
            }
            if (child instanceof ContainerEventHandler container) {
                AbstractWidget found = findWidget(container, labelPrefix);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    // ---- plumbing ---------------------------------------------------------------------

    private static <T> T join(CompletableFuture<T> future, String what) {
        try {
            return future.get(DEFAULT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtime) throw runtime;
            if (cause instanceof Error error) throw error;
            throw new AssertionError(what + " failed", cause);
        } catch (TimeoutException e) {
            throw new AssertionError(what + " did not finish within " + DEFAULT_TIMEOUT.toSeconds() + "s");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(what + " interrupted", e);
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted", e);
        }
    }

    // leftPos/topPos are protected; this is test-only code, so read them reflectively instead of adding a mixin
    private static Field field(String name) {
        try {
            Field field = AbstractContainerScreen.class.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static int intField(Field field, Object target) {
        try {
            return field.getInt(target);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }
}
