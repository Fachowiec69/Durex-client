package com.example.msgbot.mixin;

import com.example.msgbot.SpamScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class HudRenderMixin {

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        int x = context.getScaledWindowWidth() - 5;
        int y = 5;
        int lineHeight = 10;

        // Pobierz statystyki
        String spamStatus = SpamScreen.isSpamming() ? "§aON" : "§7OFF";
        String jumpStatus = SpamScreen.isJumping() ? "§aON" : "§7OFF";
        String chStatus = SpamScreen.isAutoCh() ? "§aON" : "§7OFF";
        int totalSpammed = SpamScreen.getTotalSpammed();
        int nearbyCount = SpamScreen.getNearbyCount();
        String lastTarget = SpamScreen.getLastTarget();
        boolean cooldown = SpamScreen.isSectorCooldown();

        // Renderuj statystyki (wyrównane do prawej)
        drawRightAlignedText(context, mc, "§6[MsgBot]", x, y);
        y += lineHeight;
        
        drawRightAlignedText(context, mc, "§7Spam: " + spamStatus, x, y);
        y += lineHeight;
        
        if (cooldown) {
            drawRightAlignedText(context, mc, "§eCooldown...", x, y);
            y += lineHeight;
        }
        
        drawRightAlignedText(context, mc, "§7Spamowanych: §a" + totalSpammed, x, y);
        y += lineHeight;
        
        drawRightAlignedText(context, mc, "§7W zasiegu: §e" + nearbyCount, x, y);
        y += lineHeight;
        
        if (!lastTarget.isEmpty()) {
            drawRightAlignedText(context, mc, "§7Cel: §b" + lastTarget, x, y);
            y += lineHeight;
        }
        
        drawRightAlignedText(context, mc, "§7Jump: " + jumpStatus, x, y);
        y += lineHeight;
        
        drawRightAlignedText(context, mc, "§7CH: " + chStatus, x, y);
    }

    private void drawRightAlignedText(DrawContext context, MinecraftClient mc, String text, int x, int y) {
        int width = mc.textRenderer.getWidth(text);
        context.drawTextWithShadow(mc.textRenderer, Text.literal(text), x - width, y, 0xFFFFFF);
    }
}
