package com.example.msgbot.mixin;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public class PauseMixin {

    // Minecraft pauzuje gre gdy okno traci fokus - blokuje to
    @Inject(method = "isWindowFocused", at = @At("RETURN"), cancellable = true)
    private void alwaysFocused(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(true);
    }
}
