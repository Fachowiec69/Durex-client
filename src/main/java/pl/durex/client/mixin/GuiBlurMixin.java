package pl.durex.client.mixin;

import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class GuiBlurMixin {
    @Inject(method = "applyBlur", at = @At("HEAD"), cancellable = true)
    private void durex$cancelBlur(CallbackInfo ci) {
        ci.cancel();
    }
}
