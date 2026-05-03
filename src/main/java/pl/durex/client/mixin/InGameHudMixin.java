package pl.durex.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.gui.hud.InGameHud;
import pl.durex.client.DurexClient;
import pl.durex.client.gui.render.GuiRenderUtils;
import pl.durex.client.module.CooldownHudModule;
import pl.durex.client.module.MsgBotModule;

import java.util.List;

@Mixin(InGameHud.class)
public class InGameHudMixin {

    @Inject(method = "render", at = @At("TAIL"))
    private void durex$renderHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        if (client.currentScreen != null) return;

        // ── CooldownHud ──────────────────────────────────────────────────
        CooldownHudModule hud = DurexClient.getCooldownHudModule();
        if (hud.isEnabled()) {
            List<CooldownHudModule.CooldownEntry> cooldowns = hud.getCooldowns(client);
            if (!cooldowns.isEmpty()) {
                int x = hud.getHudX(), y = hud.getHudY();
                int itemSize = 20, barW = 36, barH = 4;
                int entryH = itemSize + barH + 6;
                for (CooldownHudModule.CooldownEntry entry : cooldowns) {
                    float progress = entry.progress();
                    GuiRenderUtils.drawRoundedRect(context, x - 2, y - 2, barW + 22, entryH, 4, 0xAA0D0020);
                    GuiRenderUtils.drawRoundedOutline(context, x - 2, y - 2, barW + 22, entryH, 4, 1, 0x667700CC);
                    context.drawItem(entry.stack(), x, y);
                    context.drawStackOverlay(client.textRenderer, entry.stack(), x, y);
                    int bx = x + itemSize + 2, by = y + (itemSize - barH) / 2;
                    GuiRenderUtils.drawRoundedRect(context, bx, by, barW, barH, 2, 0xFF1A0040);
                    int filled = (int)((1f - progress) * barW);
                    int barColor = progress > 0.5f ? 0xFFFF4455 : progress > 0.2f ? 0xFFFFCC44 : 0xFF55FF88;
                    if (filled > 0) GuiRenderUtils.drawRoundedRect(context, bx, by, filled, barH, 2, barColor);
                    context.drawTextWithShadow(client.textRenderer,
                        net.minecraft.text.Text.literal(entry.timeText()), bx, by + barH + 2, 0xFFEEDDFF);
                    y += entryH + 3;
                }
            }
        }

        // ── MsgBot HUD ───────────────────────────────────────────────────
        if (MsgBotModule.isSpamming() || MsgBotModule.isAfk() || MsgBotModule.isAutoCh()) {
            int hx = context.getScaledWindowWidth() - 5, hy = 5, lh = 10;
            renderRight(context, client, "§6[MsgBot]", hx, hy); hy += lh;
            renderRight(context, client, "§7Spam: " + (MsgBotModule.isSpamming() ? "§aON" : "§7OFF"), hx, hy); hy += lh;
            if (MsgBotModule.isSectorCooldown()) { renderRight(context, client, "§eCooldown...", hx, hy); hy += lh; }
            renderRight(context, client, "§7Spamowanych: §a" + MsgBotModule.getTotalSpammed(), hx, hy); hy += lh;
            renderRight(context, client, "§7W zasiegu: §e" + MsgBotModule.getNearbyCount(), hx, hy); hy += lh;
            if (!MsgBotModule.getLastTarget().isEmpty()) { renderRight(context, client, "§7Cel: §b" + MsgBotModule.getLastTarget(), hx, hy); hy += lh; }
            renderRight(context, client, "§7AFK: " + (MsgBotModule.isAfk() ? "§aON" : "§7OFF"), hx, hy); hy += lh;
            renderRight(context, client, "§7CH: "  + (MsgBotModule.isAutoCh() ? "§aON" : "§7OFF"), hx, hy);
        }

        // ── Zbrojmistrz HUD ──────────────────────────────────────────────
        pl.durex.client.module.ZbrojmistrzModule.ArmorInfo armor =
            pl.durex.client.module.ZbrojmistrzModule.getTarget(client);
        if (armor != null) {
            renderZbrojmistrzHud(context, client, armor,
                pl.durex.client.module.ZbrojmistrzModule.getHudX(),
                pl.durex.client.module.ZbrojmistrzModule.getHudY());
        }

        // ── Procenciarz HUD ──────────────────────────────────────────────
        pl.durex.client.module.ProcenciarzModule proc = DurexClient.getProcenciarzModule();
        if (proc.isEnabled()) {
            pl.durex.client.module.ProcenciarzModule.TargetInfo target = proc.getTarget(client);
            if (target != null) renderProcenciarz(context, client, proc, target);
        }
    }

    // ── Zbrojmistrz render ────────────────────────────────────────────────

    private void renderZbrojmistrzHud(DrawContext ctx, MinecraftClient mc,
            pl.durex.client.module.ZbrojmistrzModule.ArmorInfo armor, int x, int y) {

        int lh = 10, padX = 6, padY = 5, boxW = 160;
        boolean showBooks = pl.durex.client.module.ZbrojmistrzModule.isShowBooks();

        pl.durex.client.module.ZbrojmistrzModule.ItemData[] slots = {
            armor.helmet(), armor.chest(), armor.legs(), armor.boots()
        };
        String[] slotLabels = {"Helm:   ", "Napier: ", "Spodnie:", "Buty:   "};

        // Oblicz całkowitą wysokość z góry (nick + sloty + dodatkowe linie ksiąg)
        int lines = 1; // nick
        for (var slot : slots) {
            if (slot == null) continue;
            lines++; // linia slotu
            if (showBooks) lines += Math.max(0, slot.books().size() - 1); // dodatkowe księgi
        }
        int boxH = padY * 2 + lines * lh + 2;

        // Tło Durex-style
        GuiRenderUtils.drawShadow(ctx, x, y, boxW, boxH, 4, 0xBB8800EE, 6);
        GuiRenderUtils.drawRoundedRect(ctx, x, y, boxW, boxH, 4, 0xEE080015);
        GuiRenderUtils.drawRoundedOutline(ctx, x, y, boxW, boxH, 4, 1, 0xFF8800EE);

        int cx = x + padX, cy = y + padY;

        // Nick
        ctx.drawTextWithShadow(mc.textRenderer,
            net.minecraft.text.Text.literal("§5⚔ §d" + armor.playerName()), cx, cy, 0xFFFFFF);
        cy += lh + 2;

        // Każdy slot: "Helm:    +6  Niepopalanie I"
        for (int i = 0; i < slots.length; i++) {
            var slot = slots[i];
            if (slot == null) continue;

            // Plus (kolorowy) — jeśli brak w nazwie, pomiń (nie pokazuj "brak")
            String plusPart = slot.plus() != null
                ? colorForPlus(slot.plus()) + slot.plus()
                : "§8---";

            // Pierwsza księga w tej samej linii — żółta
            String bookPart = "";
            if (showBooks && !slot.books().isEmpty()) {
                bookPart = "  §e" + slot.books().get(0);
            }

            ctx.drawTextWithShadow(mc.textRenderer,
                net.minecraft.text.Text.literal("§7" + slotLabels[i] + " " + plusPart + bookPart),
                cx, cy, 0xFFFFFF);
            cy += lh;

            // Pozostałe księgi (od 2. w górę) — wcięte, żółte
            if (showBooks) {
                for (int b = 1; b < slot.books().size(); b++) {
                    ctx.drawTextWithShadow(mc.textRenderer,
                        net.minecraft.text.Text.literal("§8         » §e" + slot.books().get(b)),
                        cx, cy, 0xFFFFFF);
                    cy += lh;
                }
            }
        }
    }

    private static String colorForPlus(String plus) {
        try {
            int lvl = Integer.parseInt(plus.replace("+", "").trim());
            if (lvl >= 7) return "§c";
            if (lvl >= 5) return "§6";
            if (lvl >= 3) return "§e";
            return "§f";
        } catch (NumberFormatException e) { return "§f"; }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private void renderRight(DrawContext ctx, MinecraftClient mc, String text, int x, int y) {
        int w = mc.textRenderer.getWidth(text);
        ctx.drawTextWithShadow(mc.textRenderer, net.minecraft.text.Text.literal(text), x - w, y, 0xFFFFFF);
    }

    private void renderProcenciarz(DrawContext ctx, MinecraftClient client,
            pl.durex.client.module.ProcenciarzModule proc,
            pl.durex.client.module.ProcenciarzModule.TargetInfo target) {

        String pct = target.percentText();
        int tw = client.textRenderer.getWidth(pct);
        int w = tw + 32; // ikona 16px + padding

        List<String> books = target.books();
        // Szerokość dopasowana do najdłuższej linii
        for (String book : books) {
            int bw = client.textRenderer.getWidth("» " + book) + 10;
            if (bw > w) w = bw;
        }

        int h = 24 + (books.isEmpty() ? 0 : books.size() * 10 + 2);

        // Pozycja: jeśli hudX == -1 to auto po prawej (przed scoreboardem ~200px od prawej)
        int hudX = proc.getHudX();
        int hudY = proc.getHudY();
        int x = hudX < 0
            ? ctx.getScaledWindowWidth() - w - 205
            : hudX;
        int y = hudY;

        // Kolor zawsze pomarańczowy
        int textColor   = 0xFFFF9900;
        int borderColor = 0xFFFF9900;

        GuiRenderUtils.drawShadow(ctx, x, y, w, h, 4, 0xBB000000, 6);
        GuiRenderUtils.drawRoundedRect(ctx, x, y, w, h, 4, 0xDD0D0020);
        GuiRenderUtils.drawRoundedOutline(ctx, x, y, w, h, 4, 1, borderColor);
        ctx.drawItem(target.item(), x + 4, y + (24 - 16) / 2);
        ctx.drawTextWithShadow(client.textRenderer,
            net.minecraft.text.Text.literal("§6" + pct), x + 22, y + (24 - 8) / 2, textColor);

        // Księgi pod procentem
        int by = y + 24 + 2;
        for (String book : books) {
            ctx.drawTextWithShadow(client.textRenderer,
                net.minecraft.text.Text.literal("§6» §6" + book), x + 4, by, 0xFFFFFF);
            by += 10;
        }
    }
}
