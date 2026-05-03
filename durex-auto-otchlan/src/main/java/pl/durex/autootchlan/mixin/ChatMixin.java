package pl.durex.autootchlan.mixin;

import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.durex.autootchlan.scanner.OtchlanScanner;

@Mixin(ChatComponent.class)
public class ChatMixin {

    @Inject(
        method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V",
        at = @At("HEAD")
    )
    private void onChatMessage(Component message,
                                net.minecraft.network.chat.MessageSignature sig,
                                net.minecraft.client.GuiMessageTag tag,
                                CallbackInfo ci) {
        String raw = message.getString();
        String lower = raw.toLowerCase();

        // Wykryj odliczanie: "otchlan zostanie otwarta za X"
        // Przykłady: "Otchłań zostanie otwarta za 10 sekund"
        //            "Otchłań zostanie otwarta za 1 sekundę"
        if (lower.contains("otch") && (lower.contains("zostanie otwarta") || lower.contains("otwarta za"))) {
            int seconds = extractSeconds(lower);
            if (seconds > 0) {
                OtchlanScanner.onCountdown(seconds);
            }
        }

        // Wykryj otwarcie: "otchlan zostala otwarta" / "otchlan jest otwarta"
        if (lower.contains("otch") && (lower.contains("zostala otwarta") || lower.contains("została otwarta")
                || lower.contains("jest otwarta") || lower.contains("jest teraz otwarta"))) {
            OtchlanScanner.onOtchlanOpened();
        }
    }

    /**
     * Wyciąga liczbę sekund z wiadomości.
     * Np. "otchlan zostanie otwarta za 10 sekund" -> 10
     *     "otchlan zostanie otwarta za 1 sekunde" -> 1
     */
    private int extractSeconds(String lower) {
        // Szukaj "za X" gdzie X to liczba
        int idx = lower.indexOf("za ");
        if (idx < 0) return -1;

        String after = lower.substring(idx + 3).trim();
        StringBuilder num = new StringBuilder();
        for (char c : after.toCharArray()) {
            if (Character.isDigit(c)) num.append(c);
            else if (num.length() > 0) break;
        }

        if (num.length() == 0) return -1;
        try {
            return Integer.parseInt(num.toString());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
