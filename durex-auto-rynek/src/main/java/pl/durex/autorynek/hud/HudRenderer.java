package pl.durex.autorynek.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import pl.durex.autorynek.config.ConfigManager;
import pl.durex.autorynek.scanner.MarketScanner;
import pl.durex.autorynek.scanner.PriceLearnController;
import pl.durex.autorynek.util.PriceParser;

import java.util.List;

public class HudRenderer {

    // ── Kolory (jak BK-Rynek) ─────────────────────────────────────────────────
    private static final int COL_BG        = 0xEE0D1A0D;  // ciemny zielonkawy
    private static final int COL_BG2       = 0xCC0A120A;
    private static final int COL_BORDER    = 0xFF1A3A1A;
    private static final int COL_GREEN     = 0xFF55FF55;
    private static final int COL_YELLOW    = 0xFFFFAA00;
    private static final int COL_CYAN      = 0xFF55FFFF;
    private static final int COL_WHITE     = 0xFFFFFFFF;
    private static final int COL_GRAY      = 0xFF888888;
    private static final int COL_RED       = 0xFFFF5555;
    private static final int COL_GOLD      = 0xFFFFAA00;
    private static final int COL_DIM       = 0xFF666666;

    private static final int HUD_W  = 155;
    private static final int LOG_W  = 165;
    private static final int ROW_H  = 12;

    // ── Render na ekranie rynku ───────────────────────────────────────────────
    public static void renderOnScreen(GuiGraphics g, AbstractContainerScreen<?> screen) {
        if (!MarketScanner.isActive()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.font == null) return;
        renderHud(g, mc.font);
    }

    // ── Render na głównym HUD (ClientTickEvents) ──────────────────────────────
    public static void renderInGame(GuiGraphics g) {
        if (!MarketScanner.isActive()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.font == null) return;
        renderHud(g, mc.font);
    }

    // ── Główny render ─────────────────────────────────────────────────────────
    private static void renderHud(GuiGraphics g, Font font) {
        if (ConfigManager.config == null || !Boolean.TRUE.equals(ConfigManager.config.hudEnabled)) return;

        int hudX = ConfigManager.config.hudX;
        int hudY = ConfigManager.config.hudY;

        boolean buying = MarketScanner.getStatus().contains("KUPUJE");
        int hudH = 90;

        // ── Panel główny ──────────────────────────────────────────────────────
        drawRoundedRect(g, hudX, hudY, HUD_W, hudH, 4, COL_BG);
        drawRoundedRectOutline(g, hudX, hudY, HUD_W, hudH, 4, COL_BORDER);

        // ── Nagłówek: kwadrat statusu + nazwa + timer ─────────────────────────
        int headerY = hudY + 7;
        // Zielony/czerwony kwadrat (jak BK-Rynek)
        int dotColor = buying ? COL_RED : COL_GREEN;
        g.fill(hudX + 8, headerY, hudX + 14, headerY + 6, dotColor);

        g.drawString(font, "Durex Auto Rynek", hudX + 18, headerY, COL_WHITE, false);

        // Timer sesji
        long elapsed = (System.currentTimeMillis() - MarketScanner.getSessionStart()) / 1000;
        String timeStr = String.format("%02d:%02d", elapsed / 60, elapsed % 60);
        int timeW = font.width(timeStr);
        g.drawString(font, timeStr, hudX + HUD_W - timeW - 6, headerY, COL_GRAY, false);

        // ── Separator ─────────────────────────────────────────────────────────
        g.fill(hudX + 1, hudY + 18, hudX + HUD_W - 1, hudY + 19, COL_BORDER);

        // ── 3 stat boxy: Bought / Sold / Failed ──────────────────────────────
        int boxY = hudY + 22;
        int boxW = (HUD_W - 16) / 3;
        drawStatBox(g, font, hudX + 6,              boxY, boxW, 24, "Bought", String.valueOf(MarketScanner.getSessionBought()), COL_GREEN);
        drawStatBox(g, font, hudX + 6 + boxW + 4,   boxY, boxW, 24, "Failed", String.valueOf(MarketScanner.getSessionFailed()), COL_YELLOW);
        drawStatBox(g, font, hudX + HUD_W - boxW - 6, boxY, boxW, 24, "Learn",
            PriceLearnController.isActive() ? "ON" : "OFF", COL_CYAN);

        // ── Profit ────────────────────────────────────────────────────────────
        int profitY = hudY + 52;
        drawRoundedRect(g, hudX + 6, profitY, HUD_W - 12, 14, 2, 0x22FFFFFF);
        g.drawString(font, "Profit:", hudX + 10, profitY + 4, COL_GRAY, false);

        double profit = MarketScanner.getSessionProfit();
        String profitStr = (profit >= 0 ? "+" : "") + PriceParser.format(profit) + "$";
        int profitColor = profit >= 0 ? COL_GREEN : COL_RED;
        int profitW = font.width(profitStr);
        g.drawString(font, profitStr, hudX + HUD_W - profitW - 10, profitY + 4, profitColor, false);

        // ── Status (aktualny stan) ────────────────────────────────────────────
        int statusY = hudY + 70;
        String status = MarketScanner.getStatus();
        g.drawString(font, status, hudX + 8, statusY, COL_WHITE, false);

        // ── Snipe Log ─────────────────────────────────────────────────────────
        renderSnipeLog(g, font, hudX, hudY + hudH + 8);
    }

    // ── Snipe Log panel ───────────────────────────────────────────────────────
    private static void renderSnipeLog(GuiGraphics g, Font font, int x, int y) {
        // Pozycja logu — z configu jeśli ustawiona, inaczej pod HUD
        int logX = (ConfigManager.config.logX != 0 || ConfigManager.config.logY != 0)
            ? ConfigManager.config.logX : x;
        int logY = (ConfigManager.config.logX != 0 || ConfigManager.config.logY != 0)
            ? ConfigManager.config.logY : y;

        List<MarketScanner.SnipeEntry> history = MarketScanner.getSnipeHistory();
        if (history == null || history.isEmpty()) return;

        int logH = 18 + history.size() * ROW_H + 4;
        drawRoundedRect(g, logX, logY, LOG_W, logH, 4, 0xEE0A120A);
        drawRoundedRectOutline(g, logX, logY, LOG_W, logH, 4, COL_BORDER);

        // Nagłówek "♦ SNIPE LOG"
        g.drawString(font, "§6✦ SNIPE LOG", logX + 8, logY + 6, COL_GOLD, false);

        long now = System.currentTimeMillis();
        for (int i = 0; i < history.size(); i++) {
            MarketScanner.SnipeEntry entry = history.get(i);
            int rowY = logY + 18 + i * ROW_H;

            // Czas temu
            long diff = (now - entry.timestamp) / 1000;
            String timeAgo = diff < 60 ? diff + "s ago"
                : diff < 3600 ? (diff / 60) + "m ago"
                : (diff / 3600) + "h ago";
            int timeW = font.width(timeAgo);

            // Cena
            String priceStr = "$" + PriceParser.format(entry.price) + "$";
            int priceW = font.width(priceStr);

            // x1 (count)
            String countStr = " x1";
            int countW = font.width(countStr);

            // Nazwa — skróć jeśli za długa
            int maxNameW = LOG_W - 16 - priceW - timeW - countW - 8;
            String name = entry.name;
            if (font.width(name) > maxNameW) {
                while (name.length() > 0 && font.width(name + "...") > maxNameW) {
                    name = name.substring(0, name.length() - 1);
                }
                name = name + "...";
            }

            g.drawString(font, name, logX + 8, rowY, COL_WHITE, false);
            g.drawString(font, countStr, logX + 8 + font.width(name), rowY, COL_GREEN, false);
            g.drawString(font, priceStr, logX + 8 + font.width(name) + countW + 2, rowY, COL_GREEN, false);
            g.drawString(font, timeAgo, logX + LOG_W - timeW - 8, rowY, COL_DIM, false);
        }
    }

    // ── Stat box (jak BK-Rynek) ───────────────────────────────────────────────
    private static void drawStatBox(GuiGraphics g, Font font, int x, int y, int w, int h,
                                     String label, String value, int accent) {
        drawRoundedRect(g, x, y, w, h, 2, 0x22FFFFFF);
        // Lewa kreska akcentu
        g.fill(x, y + 2, x + 2, y + h - 2, accent);
        g.drawString(font, value, x + 5, y + 4, COL_WHITE, false);
        g.drawString(font, label, x + 5, y + 14, COL_GRAY, false);
    }

    // ── Zaokrąglony prostokąt (port z BK-Rynek Panel.java) ───────────────────
    public static void drawRoundedRect(GuiGraphics g, int x, int y, int w, int h, int r, int color) {
        g.fill(x + r, y,     x + w - r, y + h,     color);
        g.fill(x,     y + r, x + w,     y + h - r, color);
        drawCircleQuarter(g, x + r,     y + r,     r, 0, color);
        drawCircleQuarter(g, x + w - r, y + r,     r, 1, color);
        drawCircleQuarter(g, x + r,     y + h - r, r, 2, color);
        drawCircleQuarter(g, x + w - r, y + h - r, r, 3, color);
    }

    public static void drawRoundedRectOutline(GuiGraphics g, int x, int y, int w, int h, int r, int color) {
        g.fill(x + r,     y,         x + w - r, y + 1,     color);
        g.fill(x + r,     y + h - 1, x + w - r, y + h,     color);
        g.fill(x,         y + r,     x + 1,     y + h - r, color);
        g.fill(x + w - 1, y + r,     x + w,     y + h - r, color);
        drawCircleQuarterOutline(g, x + r,     y + r,     r, 0, color);
        drawCircleQuarterOutline(g, x + w - r, y + r,     r, 1, color);
        drawCircleQuarterOutline(g, x + r,     y + h - r, r, 2, color);
        drawCircleQuarterOutline(g, x + w - r, y + h - r, r, 3, color);
    }

    private static void drawCircleQuarter(GuiGraphics g, int cx, int cy, int r, int q, int color) {
        int r2 = r * r;
        for (int i = 1; i <= r; i++) {
            for (int j = 1; j <= r; j++) {
                if (i * i + j * j >= r2) continue;
                drawCornerPixel(g, cx, cy, i, j, q, color);
            }
        }
    }

    private static void drawCircleQuarterOutline(GuiGraphics g, int cx, int cy, int r, int q, int color) {
        int r2 = r * r;
        for (int i = 0; i <= r; i++) {
            int j = (int) Math.round(Math.sqrt(Math.max(0, r2 - i * i)));
            if (i > r || j > r) continue;
            drawCornerPixel(g, cx, cy, i, j, q, color);
            drawCornerPixel(g, cx, cy, j, i, q, color);
        }
    }

    private static void drawCornerPixel(GuiGraphics g, int cx, int cy, int dx, int dy, int q, int color) {
        int px, py;
        if      (q == 0) { px = cx - dx;     py = cy - dy; }
        else if (q == 1) { px = cx + dx - 1; py = cy - dy; }
        else if (q == 2) { px = cx - dx;     py = cy + dy - 1; }
        else             { px = cx + dx - 1; py = cy + dy - 1; }
        g.fill(px, py, px + 1, py + 1, color);
    }

    public static void drawCircle(GuiGraphics g, int x, int y, int r, int color) {
        int r2 = r * r;
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                if (dx * dx + dy * dy >= r2) continue;
                g.fill(x + dx, y + dy, x + dx + 1, y + dy + 1, color);
            }
        }
    }
}
