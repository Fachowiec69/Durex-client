package pl.durex.autorynek.mixin;

import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.durex.autorynek.scanner.MarketScanner;

@Mixin(ChatComponent.class)
public class ChatMixin {

    @Inject(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V", at = @At("HEAD"))
    private void onChatMessage(Component message, net.minecraft.network.chat.MessageSignature sig,
                                net.minecraft.client.GuiMessageTag tag, CallbackInfo ci) {
        MarketScanner.onChatMessage(message.getString());
    }
}
