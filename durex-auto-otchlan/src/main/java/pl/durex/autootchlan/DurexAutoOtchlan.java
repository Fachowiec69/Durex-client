package pl.durex.autootchlan;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;
import pl.durex.autootchlan.config.ConfigManager;
import pl.durex.autootchlan.gui.OtchlanScreen;
import pl.durex.autootchlan.hud.HudRenderer;
import pl.durex.autootchlan.scanner.OtchlanScanner;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class DurexAutoOtchlan implements ClientModInitializer {

    public static final String MOD_ID   = "durex-auto-otchlan";
    public static final String MOD_NAME = "Durex Auto Otchlan";

    public static KeyMapping openGuiKey;
    public static KeyMapping toggleKey;

    @Override
    public void onInitializeClient() {
        ConfigManager.load();

        // ── Klawisze ──────────────────────────────────────────────────────────
        openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.durex-auto-otchlan.open_gui",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F8,
            "category.durex-auto-otchlan"
        ));

        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.durex-auto-otchlan.toggle",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F9,
            "category.durex-auto-otchlan"
        ));

        // ── Komendy /dao ──────────────────────────────────────────────────────
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                literal("dao")
                    // /dao — toggle start/stop
                    .executes(ctx -> {
                        OtchlanScanner.toggle();
                        return 1;
                    })
                    // /dao gui — otwórz GUI
                    .then(literal("gui").executes(ctx -> {
                        Minecraft mc = Minecraft.getInstance();
                        mc.execute(() -> mc.setScreen(new OtchlanScreen(null)));
                        return 1;
                    }))
                    // /dao start
                    .then(literal("start").executes(ctx -> {
                        OtchlanScanner.start();
                        return 1;
                    }))
                    // /dao stop
                    .then(literal("stop").executes(ctx -> {
                        OtchlanScanner.stop();
                        return 1;
                    }))
                    // /dao status
                    .then(literal("status").executes(ctx -> {
                        OtchlanScanner.printStatus();
                        return 1;
                    }))
            );
        });

        // ── Tick ──────────────────────────────────────────────────────────────
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (openGuiKey.consumeClick()) {
                client.setScreen(new OtchlanScreen(null));
            }
            if (toggleKey.consumeClick()) {
                OtchlanScanner.toggle();
            }
            OtchlanScanner.onTick(client);
        });

        // ── HUD ───────────────────────────────────────────────────────────────
        HudRenderCallback.EVENT.register((graphics, tickDelta) -> {
            HudRenderer.render(graphics);
        });

        System.out.println("[" + MOD_NAME + "] Zaladowano! /dao | /dao gui | F8=GUI | F9=Toggle");
    }
}
