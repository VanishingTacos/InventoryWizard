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
import net.minecraft.client.multiplayer.ClientLevel;
//? if >=1.21.9 {
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonInfo;
//?}
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
import java.lang.reflect.Method;
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
                Screen screen = computeOnClient(client -> screen(client));
                throw new AssertionError("Timed out waiting for " + what + " (screen: " + screen + ")");
            }
            sleep(TICK_MILLIS);
        }
    }

    public void waitForScreen(Class<? extends Screen> type) {
        waitFor(type.getSimpleName(), client -> type.isInstance(screen(client)), DEFAULT_TIMEOUT);
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
        waitFor("the game to finish loading", client -> !isLoading(client) && screen(client) != null, WORLD_TIMEOUT);
        Screen first = computeOnClient(client -> screen(client));
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
            showScreen(client, new TitleScreen());
        });
        waitForScreen(TitleScreen.class);
    }

    /** Creates a fresh superflat survival world on peaceful and waits until the player is in it. */
    public void createWorld(String name) {
        //? if >=26.1 {
        LevelSettings settings = new LevelSettings(name, GameType.SURVIVAL, new LevelSettings.DifficultySettings(Difficulty.PEACEFUL, false, false),
                true, WorldDataConfiguration.DEFAULT);
        //?} else if >=1.21.11 {
        /*LevelSettings settings = new LevelSettings(name, GameType.SURVIVAL, false, Difficulty.PEACEFUL, true,
                new net.minecraft.world.level.gamerules.GameRules(WorldDataConfiguration.DEFAULT.enabledFeatures()), WorldDataConfiguration.DEFAULT);
        *///?} else if >=1.21.2 {
        /*LevelSettings settings = new LevelSettings(name, GameType.SURVIVAL, false, Difficulty.PEACEFUL, true,
                new net.minecraft.world.level.GameRules(WorldDataConfiguration.DEFAULT.enabledFeatures()), WorldDataConfiguration.DEFAULT);
        *///?} else {
        /*LevelSettings settings = new LevelSettings(name, GameType.SURVIVAL, false, Difficulty.PEACEFUL, true,
                new net.minecraft.world.level.GameRules(), WorldDataConfiguration.DEFAULT);
        *///?}
        runOnClient(client -> client.createWorldOpenFlows().createFreshLevel(
                name,
                settings,
                new WorldOptions(0L, false, false),
                //? if >=1.21.2 {
                registries -> registries.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
                //?} else
                //registries -> registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
                new TitleScreen()));
        waitFor("the world to load", client -> client.level != null && client.player != null && screen(client) == null, WORLD_TIMEOUT);
        waitTicks(40);
    }

    public void leaveWorld() {
        runOnClient(client -> {
            // Like the pause menu: closing the connection first is what stops the integrated server
            //? if >=1.21.6 {
            client.level.disconnect(ClientLevel.DEFAULT_QUIT_MESSAGE);
            //?} else
            //client.level.disconnect();
            client.disconnect(new TitleScreen(), false);
        });
        waitForScreen(TitleScreen.class);
    }

    public void setScreen(Supplier<Screen> screen) {
        runOnClient(client -> showScreen(client, screen.get()));
    }

    // ---- input ----------------------------------------------------------------------

    public void pressKey(int key) {
        pressKey(key, 0);
    }

    // Before 26.3 most of these input handlers are private, so they are called reflectively
    public void pressKey(int key, int modifiers) {
        runOnClient(client -> {
            long window = windowHandle(client);
            //? if >=26.3 {
            KeyEvent event = new KeyEvent(key, 0, modifiers);
            client.keyboardHandler.keyPress(window, InputConstants.PRESS, event);
            client.keyboardHandler.keyPress(window, InputConstants.RELEASE, event);
            //?} else if >=1.21.9 {
            /*KeyEvent event = new KeyEvent(key, 0, modifiers);
            Class<?>[] types = {long.class, int.class, KeyEvent.class};
            invoke(client.keyboardHandler, "keyPress", types, window, InputConstants.PRESS, event);
            invoke(client.keyboardHandler, "keyPress", types, window, InputConstants.RELEASE, event);
            *///?} else {
            /*client.keyboardHandler.keyPress(window, key, 0, InputConstants.PRESS, modifiers);
            client.keyboardHandler.keyPress(window, key, 0, InputConstants.RELEASE, modifiers);
            *///?}
        });
    }

    public void click(int button, int modifiers) {
        runOnClient(client -> {
            long window = windowHandle(client);
            //? if >=26.3 {
            MouseButtonInfo info = new MouseButtonInfo(button, modifiers);
            client.mouseHandler.onButton(window, info, InputConstants.PRESS);
            client.mouseHandler.onButton(window, info, InputConstants.RELEASE);
            //?} else if >=1.21.9 {
            /*MouseButtonInfo info = new MouseButtonInfo(button, modifiers);
            Class<?>[] types = {long.class, MouseButtonInfo.class, int.class};
            invoke(client.mouseHandler, "onButton", types, window, info, InputConstants.PRESS);
            invoke(client.mouseHandler, "onButton", types, window, info, InputConstants.RELEASE);
            *///?} else {
            /*Class<?>[] types = {long.class, int.class, int.class, int.class};
            invoke(client.mouseHandler, "onPress", types, window, button, InputConstants.PRESS, modifiers);
            invoke(client.mouseHandler, "onPress", types, window, button, InputConstants.RELEASE, modifiers);
            *///?}
        });
    }

    /** One vertical wheel step per call; positive scrolls up. */
    public void scroll(double amount) {
        runOnClient(client -> {
            //? if >=26.3 {
            client.mouseHandler.onScroll(windowHandle(client), 0, amount);
            //?} else
            //invoke(client.mouseHandler, "onScroll", new Class<?>[] {long.class, double.class, double.class}, windowHandle(client), 0.0, amount);
        });
    }

    /** Moves the cursor to a GUI-scaled position, as the mouse would. */
    public void setCursorGui(double x, double y) {
        runOnClient(client -> {
            Window window = client.getWindow();
            double scale = (double) window.getScreenWidth() / window.getGuiScaledWidth();
            //? if >=26.3 {
            client.mouseHandler.onMove(windowHandle(client), x * scale, y * scale, 0, 0);
            //?} else
            //invoke(client.mouseHandler, "onMove", new Class<?>[] {long.class, double.class, double.class}, windowHandle(client), x * scale, y * scale);
        });
    }

    /** Moves the cursor over a slot of the open container screen and waits until the screen reports it hovered. */
    public void hoverSlot(Predicate<Slot> which) {
        double[] position = computeOnClient(client -> {
            AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) screen(client);
            Slot slot = screen.getMenu().slots.stream().filter(which).findFirst()
                    .orElseThrow(() -> new AssertionError("No such slot on " + screen));
            return new double[] {intField(LEFT_POS, screen) + slot.x + 8, intField(TOP_POS, screen) + slot.y + 8};
        });
        setCursorGui(position[0], position[1]);
        // Hovering is worked out while rendering, so wait for a frame with the new position
        waitFor("the cursor to be over the slot", client -> {
            Slot hovered = ((AbstractContainerScreenAccessor) screen(client)).inventorywizard$getHoveredSlot();
            return hovered != null && which.test(hovered);
        }, Duration.ofSeconds(5));
    }

    /** Option buttons are labelled "Caption: Value", so find them by the start of their text. */
    public void clickWidget(String labelPrefix) {
        double[] position = computeOnClient(client -> {
            AbstractWidget widget = findWidget(screen(client), labelPrefix);
            if (widget == null) {
                throw new AssertionError("No widget labelled '" + labelPrefix + "...' on " + screen(client));
            }
            return new double[] {widget.getX() + widget.getWidth() / 2.0, widget.getY() + widget.getHeight() / 2.0};
        });
        setCursorGui(position[0], position[1]);
        waitTicks(2);
        click(InputConstants.MOUSE_BUTTON_LEFT, 0);
        waitTicks(2);
    }

    public boolean hasWidget(String labelPrefix) {
        return computeOnClient(client -> findWidget(screen(client), labelPrefix) != null);
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

    // ---- version differences ------------------------------------------------------------

    /** The open screen, which Minecraft keeps in Gui since 26.2. */
    static Screen screen(Minecraft client) {
        //? if >=26.2 {
        return client.gui.screen();
        //?} else
        //return client.screen;
    }

    private static void showScreen(Minecraft client, Screen screen) {
        //? if >=26.2 {
        client.gui.setScreen(screen);
        //?} else
        //client.setScreen(screen);
    }

    /** Whether the loading overlay (resource reload) is still up. */
    private static boolean isLoading(Minecraft client) {
        //? if >=26.2 {
        return client.gui.overlay() != null;
        //?} else
        //return client.getOverlay() != null;
    }

    private static long windowHandle(Minecraft client) {
        //? if >=1.21.9 {
        return client.getWindow().handle();
        //?} else
        //return client.getWindow().getWindow();
    }

    private static void invoke(Object target, String name, Class<?>[] types, Object... args) {
        try {
            Method method = target.getClass().getDeclaredMethod(name, types);
            method.setAccessible(true);
            method.invoke(target, args);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Could not call " + target.getClass().getSimpleName() + "." + name, e);
        }
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
