package pl.durex.autootchlan.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.durex.autootchlan.gui.OtchlanScreen;
import pl.durex.autootchlan.scanner.OtchlanScanner;

/**
 * Przechwytuje komendy wysyłane przez gracza PRZED wysłaniem na serwer.
 * Dzięki temu /dao gui działa nawet gdy serwer ma własną komendę /dao.
 */
@Mixin(ClientPacketListener.class)
public class SendChatMixin {

    @Inject(method = "sendCommand", at = @At("HEAD"), cancellable = true)
    private void onSendCommand(String command, CallbackInfo ci) {
        String lower = command.trim().toLowerCase();

        // /dao gui — otwórz GUI
        if (lower.equals("dao gui") || lower.equals("dao  gui")) {
            Minecraft mc = Minecraft.getInstance();
            mc.execute(() -> mc.setScreen(new OtchlanScreen(null)));
            ci.cancel(); // nie wysyłaj na serwer
            return;
        }

        // /dao start
        if (lower.equals("dao start")) {
            OtchlanScanner.start();
            ci.cancel();
            return;
        }

        // /dao stop
        if (lower.equals("dao stop")) {
            OtchlanScanner.stop();
            ci.cancel();
            return;
        }

        // /dao status
        if (lower.equals("dao status")) {
            OtchlanScanner.printStatus();
            ci.cancel();
            return;
        }

        // /dao (samo) — toggle
        if (lower.equals("dao")) {
            OtchlanScanner.toggle();
            ci.cancel();
        }
    }
}
