package com.inventorywizard.gametest;

import com.inventorywizard.InventoryWizard;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;

import java.util.concurrent.TimeUnit;

/**
 * Loader-independent stand-in for Fabric's client gametest runner, used on NeoForge and Forge.
 * Started on the first client tick when {@code -Dinventorywizard.clientGameTest=true} is set,
 * it drives the real client on its own thread and exits the game with status 0 (all passed) or 1.
 */
public final class ClientGameTestRunner {

    /** The whole run must finish within this, so a stuck screen fails the build instead of hanging it. */
    private static final long WATCHDOG_MINUTES = 10;

    private ClientGameTestRunner() {}

    public static void start() {
        InventoryWizard.LOGGER.info("[gametest] Starting client gametests");
        Thread runner = new Thread(ClientGameTestRunner::run, "InventoryWizard client gametest");
        runner.setDaemon(true);
        runner.start();

        Thread watchdog = new Thread(() -> {
            try {
                Thread.sleep(TimeUnit.MINUTES.toMillis(WATCHDOG_MINUTES));
            } catch (InterruptedException e) {
                return;
            }
            fail(new AssertionError("Client gametests did not finish within " + WATCHDOG_MINUTES + " minutes"));
        }, "InventoryWizard client gametest watchdog");
        watchdog.setDaemon(true);
        watchdog.start();
    }

    private static void run() {
        try {
            ClientTestContext context = new ClientTestContext();
            context.waitForTitleScreen();
            InventoryWizardClientGameTests.run(context);
            InventoryWizard.LOGGER.info("[gametest] All client gametests passed");
            context.runOnClient(Minecraft::stop);
        } catch (Throwable t) {
            fail(t);
        }
    }

    private static void fail(Throwable t) {
        InventoryWizard.LOGGER.error("[gametest] Client gametests FAILED", t);
        // Flush the log before halting; halt rather than exit so nothing can swallow the status
        LogManager.shutdown();
        Runtime.getRuntime().halt(1);
    }
}
