package pl.durex.client.module;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * NoPush - blokuje wypychanie gracza przez bloki (pushOutOfBlocks).
 * Logika cancel jest w NoPushMixin.
 */
@Environment(EnvType.CLIENT)
public class NoPushModule {

    private static boolean enabled = false;

    public static boolean isEnabled() {
        return enabled;
    }

    public static boolean toggle() {
        enabled = !enabled;
        return enabled;
    }

    public void register() {
        // Mixin NoPushMixin sprawdza isEnabled() przy każdym wywołaniu pushOutOfBlocks
    }
}
