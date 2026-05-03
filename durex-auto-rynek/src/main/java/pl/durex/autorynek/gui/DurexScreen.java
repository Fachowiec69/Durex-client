package pl.durex.autorynek.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import pl.durex.autorynek.config.ConfigManager;
import pl.durex.autorynek.config.PriceEntry;
import pl.durex.autorynek.config.ServerProfile;
import pl.durex.autorynek.hud.HudRenderer;
import pl.durex.autorynek.scanner.MarketScanner;
import pl.durex.autorynek.scanner.PriceLearnController;
import pl.durex.autorynek.util.PriceParser;

import java.util.ArrayList;
import java.util.List;

public class DurexScreen extends Screen {

    // ── Colours ──────────────────────────────────────────────────────────────
    private static final int COL_BG        = 0xFF0D0D0D;
    private static final int COL_PANEL     = 0xFF1A1A2E;
    private static final int COL_PANEL2    = 0xFF16213E;
    private static final int COL_ACCENT    = 0xFF00D4FF;
    private static final int COL_GREEN     = 0xFF00FF88;
    private static final int COL_RED       = 0xFFFF4444;
    private static final int COL_YELLOW    = 0xFFFFCC00;
    private static final int COL_GRAY      = 0xFF888888;
    private static final int COL_WHITE     = 0xFFFFFFFF;
    private static final int COL_BORDER    = 0xFF2A2A4A;
    private static final int COL_ROW_EVEN  = 0xFF111122;
    private static final int COL_ROW_ODD   = 0xFF0D0D1A;
    private static final int COL_ROW_HOV   = 0xFF1E1E3A;
    private static final int COL_ROW_SEL   = 0xFF1A1A4A;

    // ── Layout ────────────────────────────────────────────────────────────────
    private int panelX, panelY, panelW, panelH;
    private int leftW, rightX, rightW;
    private static final int MARGIN   = 10;
    private static final int ROW_H    = 22;
    private static final int HEADER_H = 30;
    private static final int TAB_H    = 18;

    // ── State ─────────────────────────────────────────────────────────────────
    private boolean showSellTab = false;
    private double scrollOffset  = 0;
    private int hoveredRow       = -1;
    private int selectedRow      = -1;
    private EditBox searchBox;
    private String searchQuery   = "";
    private ServerProfile currentProfile;
    private final List<PriceEntry> filteredEntries = new ArrayList<>();

    // Slider drag state
    private int draggingSlider = -1; // 0=nextDelay 1=actionDelay 2=confirmDelay

    // Buttons we need refs to for dynamic labels
    private Button learnBtn;
    private Button startBtn;
    private Button stopBtn;

    public DurexScreen() {
        super(Component.literal("Durex Auto Rynek"));
    }

    // ── Init ──────────────────────────────────────────────────────────────────
    @Override
    protected void init() {
        super.init();
        String server = getServer();
        System.out.println("[DurexScreen] Init - server: " + server);
        currentProfile = ConfigManager.findProfile(server);
        System.out.println("[DurexScreen] Found profile: " + (currentProfile != null ? currentProfile.profileName : "NULL"));
        if (currentProfile != null) {
            System.out.println("[DurexScreen] Profile has " + currentProfile.prices.size() + " prices");
        }

        panelW = Math.min(600, width - 20);
        panelH = Math.min(360, (int)(height * 0.90));
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        leftW  = (int)(panelW * 0.38);
        rightX = panelX + leftW + 1;
        rightW = panelW - leftW - 1;

        buildLeftButtons();
        buildRightWidgets();
        refreshList();
    }

    private void buildLeftButtons() {
        int lx = panelX + MARGIN;
        int ly = panelY + HEADER_H + MARGIN;
        int bw = (leftW - MARGIN * 2 - 4) / 2;

        // START / STOP side by side
        startBtn = Button.builder(Component.literal("▶ START"), btn -> {
            MarketScanner.start();
        }).bounds(lx, ly, bw, 18).build();
        addRenderableWidget(startBtn);

        stopBtn = Button.builder(Component.literal("■ STOP"), btn -> {
            MarketScanner.stop();
        }).bounds(lx + bw + 4, ly, bw, 18).build();
        addRenderableWidget(stopBtn);
        ly += 22;

        // UCZ SIE CEN toggle
        learnBtn = Button.builder(
            Component.literal(PriceLearnController.isActive() ? "⏹ STOP NAUKI" : "UCZ SIE CEN"),
            btn -> {
                PriceLearnController.toggle();
                btn.setMessage(Component.literal(
                    PriceLearnController.isActive() ? "⏹ STOP NAUKI" : "UCZ SIE CEN"));
            }
        ).bounds(lx, ly, leftW - MARGIN * 2, 16).build();
        addRenderableWidget(learnBtn);
        ly += 20;

        // RESET STATS
        addRenderableWidget(Button.builder(Component.literal("RESET STATS"), btn -> {
            // stats are reset on next start; just show feedback
        }).bounds(lx, ly, leftW - MARGIN * 2, 16).build());
        ly += 20;

        // HISTORIA SNIPE
        addRenderableWidget(Button.builder(Component.literal("Historia snipe"), btn -> {
            minecraft.setScreen(new SnipeHistoryScreen(this));
        }).bounds(lx, ly, leftW - MARGIN * 2, 16).build());
        ly += 20;

        // USTAWIENIA
        addRenderableWidget(Button.builder(Component.literal("Ustawienia"), btn -> {
            minecraft.setScreen(new SettingsScreen(this));
        }).bounds(lx, ly, leftW - MARGIN * 2, 16).build());
    }

    private void buildRightWidgets() {
        int rx = rightX + MARGIN;
        int ry = panelY + HEADER_H + MARGIN;

        // Search box
        searchBox = new EditBox(font, rx, ry, rightW - MARGIN * 2, 14, Component.literal(""));
        searchBox.setHint(Component.literal("§8Szukaj przedmiotu..."));
        searchBox.setBordered(false);
        searchBox.setResponder(s -> {
            searchQuery = s;
            scrollOffset = 0;
            refreshList();
        });
        addRenderableWidget(searchBox);

        // + DODAJ PRZEDMIOT button at bottom
        int addBtnY = panelY + panelH - MARGIN - 16;
        addRenderableWidget(Button.builder(Component.literal("+ DODAJ PRZEDMIOT"), btn -> {
            minecraft.setScreen(new AddItemScreen(this, null));
        }).bounds(rightX + MARGIN, addBtnY, rightW - MARGIN * 2, 16).build());

        // Close X button
        addRenderableWidget(Button.builder(Component.literal("X"), btn -> onClose())
            .bounds(panelX + panelW - 18, panelY + 2, 16, 14).build());
    }

    private void refreshList() {
        filteredEntries.clear();
        if (currentProfile == null) {
            System.out.println("[DurexScreen] currentProfile is NULL!");
            return;
        }
        System.out.println("[DurexScreen] Refreshing list for profile: " + currentProfile.profileName);
        System.out.println("[DurexScreen] Total prices in profile: " + currentProfile.prices.size());
        
        String q = searchQuery.toLowerCase();
        for (PriceEntry e : currentProfile.prices) {
            if (e.name == null) continue;
            boolean matchesTab = showSellTab
                ? Boolean.TRUE.equals(e.sellEnabled)
                : Boolean.TRUE.equals(e.buyEnabled);
            if (!matchesTab) continue;
            if (!q.isEmpty() && !e.name.toLowerCase().contains(q)) continue;
            filteredEntries.add(e);
        }
        System.out.println("[DurexScreen] Filtered entries: " + filteredEntries.size() + " (showSellTab=" + showSellTab + ")");
    }

    // ── Render ────────────────────────────────────────────────────────────────
    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        // Dim background
        g.fill(0, 0, width, height, 0xBB000000);

        // Main panel — zaokrąglony jak BK-Rynek
        // Cień
        g.fill(panelX + 4, panelY + 4, panelX + panelW + 4, panelY + panelH + 4, 0x44000000);
        HudRenderer.drawRoundedRect(g, panelX, panelY, panelW, panelH, 8, 0xEE0D1A0D);
        HudRenderer.drawRoundedRectOutline(g, panelX, panelY, panelW, panelH, 8, 0xFF1A3A1A);

        drawHeader(g);
        drawLeftPanel(g, mx, my);
        drawRightPanel(g, mx, my);

        // Footer
        g.drawCenteredString(font, "§8durex-auto-rynek", panelX + panelW / 2, panelY + panelH - 8, COL_GRAY);

        super.render(g, mx, my, delta);
    }

    private void drawHeader(GuiGraphics g) {
        // Header bar zaokrąglony
        HudRenderer.drawRoundedRect(g, panelX + 1, panelY + 1, panelW - 2, HEADER_H - 1, 7, 0xFF0A1A0A);
        g.fill(panelX + 1, panelY + HEADER_H, panelX + panelW - 1, panelY + HEADER_H + 1, 0xFF1A3A1A);

        // Traffic lights (jak BK-Rynek)
        HudRenderer.drawCircle(g, panelX + 12, panelY + 11, 3, 0xFFFF5555);
        HudRenderer.drawCircle(g, panelX + 20, panelY + 11, 3, 0xFFFFAA00);
        HudRenderer.drawCircle(g, panelX + 28, panelY + 11, 3, 0xFF55FF55);

        g.drawString(font, "§a§lDurex Auto Rynek §8v1.0", panelX + 38, panelY + 6, COL_WHITE, false);

        boolean isActive = MarketScanner.isActive();
        String scanStatus = isActive ? "§a● AKTYWNY" : "§7○ IDLE";
        g.drawString(font, scanStatus, panelX + panelW - 80, panelY + 6, COL_WHITE, false);

        String srv = getServer();
        if (!srv.isEmpty()) {
            g.drawString(font, "§8" + srv, panelX + 38, panelY + 18, COL_GRAY, false);
        }
        if (PriceLearnController.isActive()) {
            g.drawString(font, "§eNAUKA", panelX + panelW - 80, panelY + 18, COL_YELLOW, false);
        }
    }

    private void drawLeftPanel(GuiGraphics g, int mx, int my) {
        // Left panel background
        g.fill(panelX + 1, panelY + HEADER_H + 1, panelX + leftW, panelY + panelH - 1, 0xCC0A120A);
        // Divider
        g.fill(panelX + leftW, panelY + HEADER_H + 1, panelX + leftW + 1, panelY + panelH - 1, 0xFF1A3A1A);

        int lx = panelX + MARGIN;
        int statsY = panelY + HEADER_H + MARGIN + 5 * 20 + 6;

        // Separator
        g.fill(lx, statsY, panelX + leftW - MARGIN, statsY + 1, 0xFF1A3A1A);
        statsY += 5;

        // Section label
        g.drawString(font, "§8SESSION", lx, statsY, COL_GRAY, false);
        statsY += 11;

        long elapsed = MarketScanner.isActive()
            ? (System.currentTimeMillis() - MarketScanner.getSessionStart()) / 1000 : 0;
        drawStatRow(g, lx, statsY, "Czas:",     "§f" + formatTime(elapsed));       statsY += 11;
        drawStatRow(g, lx, statsY, "Kupiono:",  "§a" + MarketScanner.getSessionBought()); statsY += 11;
        drawStatRow(g, lx, statsY, "Nieudane:", "§c" + MarketScanner.getSessionFailed()); statsY += 11;
        drawStatRow(g, lx, statsY, "Profit:",   "§a" + PriceParser.format(MarketScanner.getSessionProfit()) + "$"); statsY += 11;
        drawStatRow(g, lx, statsY, "Saldo:", "§7N/A"); statsY += 11;

        statsY += 4;
        g.fill(lx, statsY, panelX + leftW - MARGIN, statsY + 1, 0xFF1A3A1A);
        statsY += 5;

        // Sliders
        if (currentProfile != null) {
            g.drawString(font, "§8TIMING", lx, statsY, COL_GRAY, false);
            statsY += 11;

            int sliderW = leftW - MARGIN * 2;
            int nextDelay    = currentProfile.marketNextDelayMs    != null ? currentProfile.marketNextDelayMs    : 200;
            int actionDelay  = currentProfile.marketActionDelayMs  != null ? currentProfile.marketActionDelayMs  : 50;
            int confirmDelay = currentProfile.marketConfirmDelayMs != null ? currentProfile.marketConfirmDelayMs : 100;

            drawSlider(g, lx, statsY, sliderW, "Opoznienie strony",       nextDelay,    50, 2000, 0, mx, my); statsY += 22;
            drawSlider(g, lx, statsY, sliderW, "Opoznienie akcji",        actionDelay,  0,  500,  1, mx, my); statsY += 22;
            drawSlider(g, lx, statsY, sliderW, "Opoznienie potwierdzenia",confirmDelay, 20, 1000, 2, mx, my); statsY += 22;
        }

        int learnY = panelY + panelH - 22;
        if (PriceLearnController.isActive()) {
            g.drawString(font, PriceLearnController.getStatus(), lx, learnY, COL_YELLOW, false);
        }
    }

    private void drawSlider(GuiGraphics g, int x, int y, int w, String label, int value, int min, int max,
                            int sliderId, int mx, int my) {
        g.drawString(font, "§7" + label + ": §e" + value + "ms", x, y, COL_GRAY, false);
        int trackY = y + 10;
        int trackH = 4;
        g.fill(x, trackY, x + w, trackY + trackH, 0xFF111A11);
        int fillW = (int)((double)(value - min) / (max - min) * w);
        g.fill(x, trackY, x + fillW, trackY + trackH, 0xFF55FF55);
        int thumbX = x + fillW - 3;
        g.fill(thumbX, trackY - 2, thumbX + 6, trackY + trackH + 2, COL_WHITE);
    }

    private void drawRightPanel(GuiGraphics g, int mx, int my) {
        int rx = rightX;
        int ry = panelY + HEADER_H + MARGIN;

        // Tabs row
        int tabY = ry + 18;
        int tabW = (rightW - 4) / 2;

        // KUPNO tab
        boolean buyActive = !showSellTab;
        HudRenderer.drawRoundedRect(g, rx, tabY, tabW, TAB_H, 3,
            buyActive ? 0xFF0A2A0A : 0xFF0A150A);
        HudRenderer.drawRoundedRectOutline(g, rx, tabY, tabW, TAB_H, 3,
            buyActive ? 0xFF55FF55 : 0xFF1A3A1A);
        g.drawCenteredString(font, buyActive ? "§a§lKUPNO" : "§7KUPNO", rx + tabW / 2, tabY + 5, COL_WHITE);

        // SPRZEDAZ tab
        boolean sellActive = showSellTab;
        HudRenderer.drawRoundedRect(g, rx + tabW + 4, tabY, tabW, TAB_H, 3,
            sellActive ? 0xFF2A1A00 : 0xFF0A150A);
        HudRenderer.drawRoundedRectOutline(g, rx + tabW + 4, tabY, tabW, TAB_H, 3,
            sellActive ? 0xFFFFAA00 : 0xFF1A3A1A);
        g.drawCenteredString(font, sellActive ? "§6§lSPRZEDAZ" : "§7SPRZEDAZ", rx + tabW + 4 + tabW / 2, tabY + 5, COL_WHITE);

        // List area
        int listY  = tabY + TAB_H + 2;
        int listH  = panelY + panelH - listY - MARGIN - 20;
        HudRenderer.drawRoundedRect(g, rx, listY, rightW, listH, 3, 0xCC050F05);
        HudRenderer.drawRoundedRectOutline(g, rx, listY, rightW, listH, 3, 0xFF1A3A1A);

        // Column headers
        int colY = listY + 2;
        g.fill(rx + 1, colY, rx + rightW - 1, colY + 13, 0xFF0A1A0A);
        g.drawString(font, "§8Przedmiot",   rx + 22,           colY + 3, COL_GRAY, false);
        g.drawString(font, "§8Max cena",    rx + rightW - 130, colY + 3, COL_GRAY, false);
        g.drawString(font, "§8Sprzedaj za", rx + rightW - 80,  colY + 3, COL_GRAY, false);
        g.drawString(font, "§8Status",      rx + rightW - 30,  colY + 3, COL_GRAY, false);
        colY += 13;

        // Rows
        int visibleRows = (listH - 15) / ROW_H;
        int startIdx    = (int) scrollOffset;
        hoveredRow      = -1;

        for (int i = 0; i < visibleRows && (startIdx + i) < filteredEntries.size(); i++) {
            int idx   = startIdx + i;
            PriceEntry entry = filteredEntries.get(idx);
            int rowY  = colY + i * ROW_H;
            boolean hovered  = mx >= rx && mx < rx + rightW - 5 && my >= rowY && my < rowY + ROW_H;
            boolean selected = idx == selectedRow;
            if (hovered) hoveredRow = idx;

            int rowBg = selected ? 0xFF0A2A0A : (hovered ? 0xFF0F200F : (i % 2 == 0 ? 0xFF080F08 : 0xFF050C05));
            g.fill(rx + 1, rowY, rx + rightW - 5, rowY + ROW_H - 1, rowBg);

            ItemStack stack = getItemStack(entry.material);
            if (!stack.isEmpty()) {
                g.renderItem(stack, rx + 3, rowY + 3);
            }

            String name = stripColors(entry.name != null ? entry.name : "?");
            if (name.length() > 20) name = name.substring(0, 18) + "..";
            g.drawString(font, "§f" + name, rx + 22, rowY + 7, COL_WHITE, false);

            String maxP = entry.maxPrice > 0 ? "§a" + formatPrice(entry.maxPrice) : "§8-";
            g.drawString(font, maxP, rx + rightW - 130, rowY + 7, COL_WHITE, false);

            String sellP = entry.sellPrice > 0 ? "§6" + formatPrice(entry.sellPrice) : "§8-";
            g.drawString(font, sellP, rx + rightW - 80, rowY + 7, COL_WHITE, false);

            boolean enabled = showSellTab ? Boolean.TRUE.equals(entry.sellEnabled) : Boolean.TRUE.equals(entry.buyEnabled);
            g.drawString(font, enabled ? "§a●" : "§c●", rx + rightW - 22, rowY + 7, COL_WHITE, false);

            if (hovered) {
                g.drawString(font, "§c✕", rx + rightW - 10, rowY + 7, 0xFFFF5555, false);
            }
        }

        // Scrollbar
        if (filteredEntries.size() > visibleRows && visibleRows > 0) {
            int sbX = rx + rightW - 4;
            int sbH = listH - 15;
            int sbY = colY;
            g.fill(sbX, sbY, sbX + 3, sbY + sbH, 0xFF111A11);
            int thumbH = Math.max(10, sbH * visibleRows / filteredEntries.size());
            int maxScroll = filteredEntries.size() - visibleRows;
            int thumbY = sbY + (maxScroll > 0 ? (int)((sbH - thumbH) * scrollOffset / maxScroll) : 0);
            g.fill(sbX, thumbY, sbX + 3, thumbY + thumbH, 0xFF55FF55);
        }

        g.drawString(font, "§8" + filteredEntries.size() + " przedmiotow",
            rx + MARGIN, panelY + panelH - 10, COL_GRAY, false);
    }

    // ── Mouse ─────────────────────────────────────────────────────────────────
    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        // Tabs
        int tabY = panelY + HEADER_H + MARGIN + 18;
        int tabW = (rightW - 4) / 2;
        if (my >= tabY && my <= tabY + TAB_H) {
            if (mx >= rightX && mx <= rightX + tabW) {
                showSellTab = false; scrollOffset = 0; refreshList(); return true;
            }
            if (mx >= rightX + tabW + 4 && mx <= rightX + tabW * 2 + 4) {
                showSellTab = true; scrollOffset = 0; refreshList(); return true;
            }
        }

        // Row click
        int listY  = tabY + TAB_H + 2 + 13; // +13 header
        int rowIdx = (int)((my - listY) / ROW_H) + (int)scrollOffset;
        if (mx >= rightX && mx < rightX + rightW - 5 && my >= listY && rowIdx >= 0 && rowIdx < filteredEntries.size()) {
            // Check X delete button (last 12px of row)
            if (mx >= rightX + rightW - 14) {
                if (currentProfile != null) {
                    currentProfile.prices.remove(filteredEntries.get(rowIdx));
                    ConfigManager.save();
                    if (selectedRow >= filteredEntries.size()) selectedRow = -1;
                    refreshList();
                }
                return true;
            }
            selectedRow = rowIdx;
            // Double-click to edit
            minecraft.setScreen(new AddItemScreen(this, filteredEntries.get(rowIdx)));
            return true;
        }

        // Slider click
        if (currentProfile != null) {
            int lx = panelX + MARGIN;
            int sliderW = leftW - MARGIN * 2;
            int statsY = panelY + HEADER_H + MARGIN + 5 * 20 + 6 + 11 + 5 * 11 + 4 + 5 + 11;
            for (int i = 0; i < 3; i++) {
                int trackY = statsY + i * 22 + 10;
                if (my >= trackY - 3 && my <= trackY + 7 && mx >= lx && mx <= lx + sliderW) {
                    draggingSlider = i;
                    updateSlider(i, (int)mx, lx, sliderW);
                    return true;
                }
            }
        }

        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (draggingSlider >= 0 && currentProfile != null) {
            int lx = panelX + MARGIN;
            int sliderW = leftW - MARGIN * 2;
            updateSlider(draggingSlider, (int)mx, lx, sliderW);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (draggingSlider >= 0) {
            ConfigManager.save();
            draggingSlider = -1;
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    private void updateSlider(int id, int mx, int lx, int sliderW) {
        double ratio = Math.max(0, Math.min(1, (double)(mx - lx) / sliderW));
        switch (id) {
            case 0 -> currentProfile.marketNextDelayMs    = (int)(50  + ratio * (2000 - 50));
            case 1 -> currentProfile.marketActionDelayMs  = (int)(0   + ratio * 500);
            case 2 -> currentProfile.marketConfirmDelayMs = (int)(20  + ratio * (1000 - 20));
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hScroll, double vScroll) {
        if (mx >= rightX && mx <= rightX + rightW) {
            int tabY = panelY + HEADER_H + MARGIN + 18;
            int listY = tabY + TAB_H + 2 + 13;
            int listH = panelY + panelH - listY - MARGIN - 20;
            int visibleRows = (listH - 15) / ROW_H;
            scrollOffset = Math.max(0, Math.min(
                Math.max(0, filteredEntries.size() - visibleRows),
                scrollOffset - vScroll
            ));
            return true;
        }
        return super.mouseScrolled(mx, my, hScroll, vScroll);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Escape - jesli skaner aktywny to zatrzymaj, inaczej zamknij GUI
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            if (MarketScanner.isActive()) {
                MarketScanner.stop();
                return true;
            }
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private void drawBorder(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x,         y,         x + w,     y + 1,     color);
        g.fill(x,         y + h - 1, x + w,     y + h,     color);
        g.fill(x,         y,         x + 1,     y + h,     color);
        g.fill(x + w - 1, y,         x + w,     y + h,     color);
    }

    private void drawStatRow(GuiGraphics g, int x, int y, String label, String value) {
        g.drawString(font, "§8" + label, x,      y, COL_GRAY,  false);
        g.drawString(font, value,        x + 72, y, COL_WHITE, false);
    }

    private ItemStack getItemStack(String material) {
        if (material == null || material.isEmpty()) return ItemStack.EMPTY;
        try {
            var opt = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(material));
            if (opt.isEmpty()) return ItemStack.EMPTY;
            return new ItemStack(opt.get());
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }

    private String formatPrice(double price) {
        if (price >= 1_000_000_000) return String.format("%.1fmld", price / 1_000_000_000);
        if (price >= 1_000_000)     return String.format("%.1fm",   price / 1_000_000);
        if (price >= 1_000)         return String.format("%.1fk",   price / 1_000);
        return String.valueOf((int) price);
    }

    private String formatTime(long seconds) {
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
    }

    private String stripColors(String s) {
        return s.replaceAll("§[0-9a-fk-or]", "").trim();
    }

    private String getServer() {
        Minecraft mc = Minecraft.getInstance();
        return mc.getCurrentServer() != null ? mc.getCurrentServer().ip : "";
    }
}
