package pl.durex.autorynek.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import pl.durex.autorynek.scanner.MarketScanner;
import pl.durex.autorynek.scanner.MarketScanner.SnipeEntry;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class SnipeHistoryScreen extends Screen {

    // ── Colours ──────────────────────────────────────────────────────────────
    private static final int COL_PANEL  = 0xFF1A1A2E;
    private static final int COL_PANEL2 = 0xFF16213E;
    private static final int COL_ACCENT = 0xFF00D4FF;
    private static final int COL_GREEN  = 0xFF00FF88;
    private static final int COL_RED    = 0xFFFF4444;
    private static final int COL_YELLOW = 0xFFFFCC00;
    private static final int COL_GRAY   = 0xFF888888;
    private static final int COL_WHITE  = 0xFFFFFFFF;
    private static final int COL_BORDER = 0xFF2A2A4A;
    private static final int COL_ROW_EVEN = 0xFF111122;
    private static final int COL_ROW_ODD  = 0xFF0D0D1A;
    private static final int COL_ROW_HOV  = 0xFF1E1E3A;

    private static final int ROW_H  = 18;
    private static final int MARGIN = 10;

    private final Screen parent;
    private double scrollOffset = 0;

    private int px, py, pw, ph;

    private static final SimpleDateFormat SDF = new SimpleDateFormat("HH:mm:ss");

    public SnipeHistoryScreen(Screen parent) {
        super(Component.literal("Historia Snipe"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        pw = Math.min(520, width - 20);
        ph = Math.min(320, (int)(height * 0.88));
        px = (width  - pw) / 2;
        py = (height - ph) / 2;

        // Clear history button
        addRenderableWidget(Button.builder(Component.literal("§cWyczysc historie"), btn -> {
            MarketScanner.clearHistory();
        }).bounds(px + pw - 130, py + ph - MARGIN - 16, 120, 16).build());

        // Back button
        addRenderableWidget(Button.builder(Component.literal("§7< Wstecz"), btn -> minecraft.setScreen(parent))
            .bounds(px + MARGIN, py + ph - MARGIN - 16, 80, 16).build());

        // Close X
        addRenderableWidget(Button.builder(Component.literal("X"), btn -> onClose())
            .bounds(px + pw - 18, py + 2, 16, 14).build());
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        g.fill(0, 0, width, height, 0xBB000000);

        // Panel
        g.fill(px + 3, py + 3, px + pw + 3, py + ph + 3, 0x55000000);
        g.fill(px, py, px + pw, py + ph, COL_PANEL);
        drawBorder(g, px, py, pw, ph, COL_BORDER);
        g.fill(px, py, px + pw, py + 2, COL_ACCENT);

        // Header
        g.fill(px, py + 2, px + pw, py + 28, COL_PANEL2);
        g.fill(px, py + 28, px + pw, py + 29, COL_BORDER);
        g.drawCenteredString(g.pose() != null ? font : font, "§b§lHistoria Snipe", px + pw / 2, py + 10, COL_WHITE);

        List<SnipeEntry> history = MarketScanner.getSnipeHistory();

        // Total profit summary
        double totalProfit = history.stream().mapToDouble(e -> e.sellPrice - e.price).sum();
        g.drawString(font, "§7Lacznie zakupow: §f" + history.size(), px + MARGIN, py + 32, COL_WHITE);
        g.drawString(font, "§7Lacznie profit: " + (totalProfit >= 0 ? "§a" : "§c") + formatPrice(totalProfit) + "$",
            px + MARGIN + 160, py + 32, COL_WHITE);

        // Column headers
        int listY = py + 46;
        int listH = ph - 46 - MARGIN - 22;
        g.fill(px, listY, px + pw, listY + listH, 0xFF0A0A18);
        drawBorder(g, px, listY, pw, listH, COL_BORDER);

        int colY = listY + 2;
        g.fill(px, colY, px + pw, colY + 13, 0xFF111130);
        g.drawString(font, "§7Przedmiot",  px + MARGIN,       colY + 3, COL_GRAY);
        g.drawString(font, "§7Kupiono za", px + pw - 240,     colY + 3, COL_GRAY);
        g.drawString(font, "§7Sprzedaj za",px + pw - 170,     colY + 3, COL_GRAY);
        g.drawString(font, "§7Profit",     px + pw - 100,     colY + 3, COL_GRAY);
        g.drawString(font, "§7Czas",       px + pw - 55,      colY + 3, COL_GRAY);
        colY += 13;

        int visibleRows = (listH - 15) / ROW_H;
        int startIdx    = (int) scrollOffset;

        for (int i = 0; i < visibleRows && (startIdx + i) < history.size(); i++) {
            int idx   = startIdx + i;
            SnipeEntry entry = history.get(idx);
            int rowY  = colY + i * ROW_H;
            boolean hovered = mx >= px && mx < px + pw - 5 && my >= rowY && my < rowY + ROW_H;

            int rowBg = hovered ? COL_ROW_HOV : (i % 2 == 0 ? COL_ROW_EVEN : COL_ROW_ODD);
            g.fill(px + 1, rowY, px + pw - 5, rowY + ROW_H - 1, rowBg);

            // Name
            String name = entry.name.length() > 24 ? entry.name.substring(0, 22) + ".." : entry.name;
            g.drawString(font, "§f" + name, px + MARGIN, rowY + 5, COL_WHITE);

            // Price paid
            g.drawString(font, "§e" + formatPrice(entry.price) + "$", px + pw - 240, rowY + 5, COL_WHITE);

            // Sell price
            g.drawString(font, "§a" + formatPrice(entry.sellPrice) + "$", px + pw - 170, rowY + 5, COL_WHITE);

            // Profit
            double profit = entry.sellPrice - entry.price;
            String profitStr = (profit >= 0 ? "§a+" : "§c") + formatPrice(profit) + "$";
            g.drawString(font, profitStr, px + pw - 100, rowY + 5, COL_WHITE);

            // Timestamp
            String time = SDF.format(new Date(entry.timestamp));
            g.drawString(font, "§8" + time, px + pw - 55, rowY + 5, COL_GRAY);
        }

        // Scrollbar
        if (history.size() > visibleRows && visibleRows > 0) {
            int sbX = px + pw - 4;
            int sbH = listH - 15;
            int sbY = colY;
            g.fill(sbX, sbY, sbX + 3, sbY + sbH, 0xFF222233);
            int thumbH = Math.max(10, sbH * visibleRows / history.size());
            int maxScroll = history.size() - visibleRows;
            int thumbY = sbY + (maxScroll > 0 ? (int)((sbH - thumbH) * scrollOffset / maxScroll) : 0);
            g.fill(sbX, thumbY, sbX + 3, thumbY + thumbH, COL_ACCENT);
        }

        if (history.isEmpty()) {
            g.drawCenteredString(font, "§7Brak historii zakupow", px + pw / 2, listY + listH / 2 - 4, COL_GRAY);
        }

        super.render(g, mx, my, delta);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hScroll, double vScroll) {
        List<SnipeEntry> history = MarketScanner.getSnipeHistory();
        int listH = ph - 46 - MARGIN - 22;
        int visibleRows = (listH - 15) / ROW_H;
        scrollOffset = Math.max(0, Math.min(
            Math.max(0, history.size() - visibleRows),
            scrollOffset - vScroll
        ));
        return true;
    }

    private void drawBorder(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x,         y,         x + w,     y + 1,     color);
        g.fill(x,         y + h - 1, x + w,     y + h,     color);
        g.fill(x,         y,         x + 1,     y + h,     color);
        g.fill(x + w - 1, y,         x + w,     y + h,     color);
    }

    private String formatPrice(double price) {
        if (Math.abs(price) >= 1_000_000_000) return String.format("%.1fmld", price / 1_000_000_000);
        if (Math.abs(price) >= 1_000_000)     return String.format("%.1fm",   price / 1_000_000);
        if (Math.abs(price) >= 1_000)         return String.format("%.1fk",   price / 1_000);
        return String.valueOf((int) price);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
