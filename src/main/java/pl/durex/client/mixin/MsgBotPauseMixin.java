package pl.durex.client.mixin;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pl.durex.client.module.MsgBotModule;

/**
 * Gdy MsgBot jest włączony — gra nie pauzuje się przy utracie fokusa okna.
 * Dzięki temu jump/spam działa przy zminimalizowanym Minecrafcie.
 */
@Mixin(MinecraftClient.class)
public class MsgBotPauseMixin {

    @Inject(method = "isWindowFocused", at = @At("RETURN"), cancellable = true)
    private void durex$alwaysFocusedWhenMsgBot(CallbackInfoReturnable<Boolean> cir) {
        if (MsgBotModule.isEnabled()) {
            cir.setReturnValue(true);
        }
    }
}
