package com.example.msgbot.mixin;

import com.example.msgbot.AutoChHandler;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
public class ScreenHandlerMixin {

    @Shadow protected ScreenHandler handler;

    @Inject(method = "init", at = @At("TAIL"))
    private void onHandledScreenInit(CallbackInfo ci) {
        HandledScreen<?> self = (HandledScreen<?>)(Object)this;
        String title = self.getTitle().getString();

        if (title.contains("Kana") || title.contains("Channel") || title.contains("kanal")) {
            AutoChHandler.onChannelScreenOpen(self);
        }
    }
}
