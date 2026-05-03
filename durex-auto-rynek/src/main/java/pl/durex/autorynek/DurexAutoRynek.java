package pl.durex.autorynek;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;
import pl.durex.autorynek.config.ConfigManager;
import pl.durex.autorynek.gui.DurexScreen;
import pl.durex.autorynek.gui.SnipeHistoryScreen;
import pl.durex.autorynek.hud.HudRenderer;
import pl.durex.autorynek.scanner.MarketScanner;
import pl.durex.autorynek.scanner.PriceLearnController;

public class DurexAutoRynek implements ClientModInitializer {

    public static final String MOD_ID = "durex-auto-rynek";
    public static final String MOD_NAME = "Durex Auto Rynek";

    public static KeyMapping openGuiKey;
    public static KeyMapping toggleScanKey;
    public static KeyMapping toggleLearnKey;
    public static KeyMapping openHistoryKey;

    @Override
    public void onInitializeClient() {
        ConfigManager.load();

        // F5 = otwórz GUI
        openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.durex-auto-rynek.open_gui",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F5,
            "category.durex-auto-rynek"
        ));

        // F6 = toggle skaner (bez GUI)
        toggleScanKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.durex-auto-rynek.toggle_scan",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F6,
            "category.durex-auto-rynek"
        ));

        // F7 = toggle nauka cen
        toggleLearnKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.durex-auto-rynek.toggle_learn",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F7,
            "category.durex-auto-rynek"
        ));

        // F8 = otwórz historię snipe
        openHistoryKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.durex-auto-rynek.open_history",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F8,
            "category.durex-auto-rynek"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (openGuiKey.consumeClick()) {
                client.setScreen(new DurexScreen());
            }
            if (toggleScanKey.consumeClick()) MarketScanner.toggle();
            if (toggleLearnKey.consumeClick()) PriceLearnController.toggle();
            if (openHistoryKey.consumeClick()) {
                client.setScreen(new SnipeHistoryScreen(null));
            }

            MarketScanner.onTick(client);
            PriceLearnController.onTick(client);
        });

        // HUD overlay — widoczny zawsze gdy skaner aktywny
        HudRenderCallback.EVENT.register((graphics, tickDelta) -> {
            HudRenderer.renderInGame(graphics);
        });

        System.out.println("[" + MOD_NAME + "] Zaladowano! F5=GUI, F6=Skaner, F7=Nauka cen, F8=Historia");
    }
}
