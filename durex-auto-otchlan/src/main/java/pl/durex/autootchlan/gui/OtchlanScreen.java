package pl.durex.autootchlan.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import pl.durex.autootchlan.config.ConfigManager;
import pl.durex.autootchlan.config.ModConfig;
import pl.durex.autootchlan.config.PriorityEntry;
import pl.durex.autootchlan.scanner.OtchlanScanner;

import java.util.List;

public class OtchlanScreen extends Screen {

    private static final int W = 480;
    private static final int H = 280;

    // kolory
    private static final int C_BG     = 0xEE111111;
    private static final int C_HEAD   = 0xFF1A1A1A;
    private static final int C_BORDER = 0xFF333333;
    private static final int C_WHITE  = 0xFFFFFFFF;
    private static final int C_GRAY   = 0xFF888888;
    private static final int C_DGRAY  = 0xFF444444;
    private static final int C_GREEN  = 0xFF4CAF50;
    private static final int C_RED    = 0xFFFF5252;

    // layout (ustawiane w init)
    private int x, y;

    // pola config
    private EditBox fCmd, fSlot, fOpenDelay, fPageDelay, fSpamCount, fSpamInterval;

    // priorytety
    private int hovRow = -1;
    private int ctxRow = -1;
    private boolean showCtx;
    private int ctxX, ctxY;

    private final Screen back;

    public OtchlanScreen(Screen back) {
        super(Component.literal("DAO - Auto Otchlan"));
        this.back = back;
    }

    @Override
    protected void init() {
        super.init();
        x = (width  - W) / 2;
        y = (height - H) / 2;

        ModConfig c = ConfigManager.get();
        int lx = x + 8;
        int fw = 155;

        // pola konfiguracji — lewy panel
        int fy = y + 38;
        fCmd          = field(lx, fy, fw, c.command,                    "Komenda");       fy += 22;
        fSlot         = field(lx, fy, fw, String.valueOf(c.nextPageSlot),"Slot nast.str"); fy += 22;
        fOpenDelay    = field(lx, fy, fw, String.valueOf(c.openDelayMs), "Delay otw.(ms)");fy += 22;
        fPageDelay    = field(lx, fy, fw, String.valueOf(c.pageDelayMs), "Delay str.(ms)");fy += 22;
        fSpamCount    = field(lx, fy, fw, String.valueOf(c.spamCount),   "Spam count");    fy += 22;
        fSpamInterval = field(lx, fy, fw, String.valueOf(c.spamIntervalMs),"Spam ms");

        fCmd.setResponder(v -> ConfigManager.setCommand(v.trim()));
        fSlot.setResponder(v -> { try { ConfigManager.setNextPageSlot(Integer.parseInt(v.trim())); } catch (Exception ignored) {} });
        fOpenDelay.setResponder(v -> { try { ConfigManager.setOpenDelayMs(Integer.parseInt(v.trim())); } catch (Exception ignored) {} });
        fPageDelay.setResponder(v -> { try { ConfigManager.setPageDelayMs(Integer.parseInt(v.trim())); } catch (Exception ignored) {} });
        fSpamCount.setResponder(v -> { try { ConfigManager.setSpamCount(Integer.parseInt(v.trim())); } catch (Exception ignored) {} });
        fSpamInterval.setResponder(v -> { try { ConfigManager.setSpamIntervalMs(Integer.parseInt(v.trim())); } catch (Exception ignored) {} });

        // przycisk START/STOP
        addRenderableWidget(Button.builder(
            Component.literal(OtchlanScanner.isActive() ? "■ STOP" : "▶ START"),
            btn -> {
                OtchlanScanner.toggle();
                btn.setMessage(Component.literal(OtchlanScanner.isActive() ? "■ STOP" : "▶ START"));
            }
        ).bounds(x + 8, y + H - 28, 80, 18).build());

        // przycisk + Dodaj priorytet
        addRenderableWidget(Button.builder(
            Component.literal("+ Dodaj"),
            btn -> minecraft.setScreen(new AddPriorityScreen(this, null, -1))
        ).bounds(x + W / 2 + 8, y + H - 28, 90, 18).build());

        // przycisk X
        addRenderableWidget(Button.builder(
            Component.literal("X"),
            btn -> onClose()
        ).bounds(x + W - 20, y + 4, 16, 14).build());
    }

    private EditBox field(int fx, int fy, int fw, String val, String hint) {
        EditBox b = new EditBox(font, fx, fy, fw, 14, Component.literal(""));
        b.setHint(Component.literal("§8" + hint));
        b.setBordered(true);
        b.setValue(val);
        addRenderableWidget(b);
        return b;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float dt) {
        // przyciemnij tło
        g.fill(0, 0, width, height, 0xAA000000);

        // panel
        g.fill(x, y, x + W, y + H, C_BG);
        border(g, x, y, W, H, C_BORDER);

        // nagłówek
        g.fill(x, y, x + W, y + 26, C_HEAD);
        g.fill(x, y + 25, x + W, y + 26, C_BORDER);

        // traffic lights
        circle(g, x + 10, y + 13, 4, 0xFFFF5F57);
        circle(g, x + 22, y + 13, 4, 0xFFFFBD2E);
        circle(g, x + 34, y + 13, 4, 0xFF28C840);

        g.drawString(font, "§fAuto Otchlan  §8/dao gui | F8=GUI | F9=toggle",
            x + 46, y + 9, C_WHITE, false);

        // status
        boolean active = OtchlanScanner.isActive();
        g.drawString(font, active ? "§a● AKTYWNY" : "§7○ IDLE",
            x + W - 80, y + 9, C_WHITE, false);

        // divider lewy/prawy
        int divX = x + 175;
        g.fill(divX, y + 26, divX + 1, y + H - 1, C_BORDER);

        // etykiety pól (lewy panel)
        int lx = x + 8;
        int ly = y + 30;
        String[] labels = {"Komenda", "Slot nast.str", "Delay otw.(ms)",
                           "Delay str.(ms)", "Spam count", "Spam ms"};
        for (String lbl : labels) {
            g.drawString(font, "§8" + lbl, lx, ly, C_DGRAY, false);
            ly += 22;
        }

        // prawy panel — priorytety
        int rx = divX + 6;
        int rw = W - 175 - 6;
        g.drawString(font, "§8PRIORYTETY", rx, y + 30, C_DGRAY, false);

        List<PriorityEntry> entries = ConfigManager.getPriorities();
        hovRow = -1;
        int rowY = y + 42;
        for (int i = 0; i < entries.size(); i++) {
            PriorityEntry e = entries.get(i);
            boolean hov = mx >= rx && mx < x + W - 4 && my >= rowY && my < rowY + 20;
            if (hov) hovRow = i;

            g.fill(rx, rowY, x + W - 4, rowY + 20,
                hov ? 0xFF222222 : (i % 2 == 0 ? 0xFF181818 : 0xFF141414));

            // kostka
            cube(g, rx + 2, rowY + 3, e.priority >= 7);

            // nazwa
            String name = e.displayName != null ? e.displayName : e.keyword;
            if (name.length() > 14) name = name.substring(0, 12) + "..";
            g.drawString(font, "§f" + name, rx + 18, rowY + 3, C_WHITE, false);
            g.drawString(font, "§8P:" + e.priority, rx + 18, rowY + 12, C_DGRAY, false);

            // toggle
            boolean on = e.priority > 0;
            toggle(g, x + W - 50, rowY + 4, on);

            // ...
            g.drawString(font, "§7•••", x + W - 18, rowY + 6,
                (ctxRow == i && showCtx) ? C_WHITE : C_GRAY, false);

            g.fill(rx, rowY + 19, x + W - 4, rowY + 20, C_BORDER);
            rowY += 20;
        }

        if (entries.isEmpty()) {
            g.drawCenteredString(font, "§8Brak priorytetów",
                divX + rw / 2, y + 60, C_DGRAY);
            g.drawCenteredString(font, "§8Kliknij + Dodaj",
                divX + rw / 2, y + 72, C_DGRAY);
        }

        // context menu
        if (showCtx && ctxRow >= 0 && ctxRow < entries.size()) {
            drawCtx(g, mx, my);
        }

        super.render(g, mx, my, dt);
    }

    private void drawCtx(GuiGraphics g, int mx, int my) {
        int cw = 90, ch = 60;
        int cx2 = Math.min(ctxX, x + W - cw - 2);
        int cy2 = Math.min(ctxY, y + H - ch - 2);
        g.fill(cx2, cy2, cx2 + cw, cy2 + ch, 0xFF1E1E1E);
        border(g, cx2, cy2, cw, ch, C_BORDER);
        String[] opts = {"§fEdytuj", "§cUsuń", "§7↑ Wyżej"};
        for (int i = 0; i < 3; i++) {
            int iy = cy2 + i * 20;
            if (mx >= cx2 && mx < cx2 + cw && my >= iy && my < iy + 20)
                g.fill(cx2 + 1, iy, cx2 + cw - 1, iy + 20, 0xFF282828);
            g.drawString(font, opts[i], cx2 + 6, iy + 6, C_WHITE, false);
            if (i < 2) g.fill(cx2 + 1, iy + 19, cx2 + cw - 1, iy + 20, C_BORDER);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        List<PriorityEntry> entries = ConfigManager.getPriorities();

        // zamknij ctx menu
        if (showCtx) {
            int cw = 90, ch = 60;
            int cx2 = Math.min(ctxX, x + W - cw - 2);
            int cy2 = Math.min(ctxY, y + H - ch - 2);
            if (mx >= cx2 && mx < cx2 + cw && my >= cy2 && my < cy2 + ch) {
                int item = (int)((my - cy2) / 20);
                switch (item) {
                    case 0 -> { showCtx = false; minecraft.setScreen(new AddPriorityScreen(this, entries.get(ctxRow), ctxRow)); }
                    case 1 -> { ConfigManager.removePriority(ctxRow); showCtx = false; clearWidgets(); init(); }
                    case 2 -> {
                        if (ctxRow > 0) { PriorityEntry e = entries.remove(ctxRow); entries.add(ctxRow - 1, e); ConfigManager.save(); }
                        showCtx = false;
                    }
                }
                return true;
            }
            showCtx = false;
            return true;
        }

        // wiersze priorytetów
        int divX = x + 175;
        int rowY = y + 42;
        for (int i = 0; i < entries.size(); i++) {
            if (my >= rowY && my < rowY + 20 && mx >= divX + 6 && mx < x + W - 4) {
                // toggle
                if (mx >= x + W - 50 && mx < x + W - 24) {
                    PriorityEntry e = entries.get(i);
                    e.priority = e.priority > 0 ? 0 : 5;
                    ConfigManager.save();
                    return true;
                }
                // ...
                if (mx >= x + W - 22) {
                    ctxRow = i; ctxX = (int)mx; ctxY = (int)my; showCtx = true;
                    return true;
                }
            }
            rowY += 20;
        }

        return super.mouseClicked(mx, my, btn);
    }

    @Override public boolean isPauseScreen() { return false; }
    @Override public void onClose() { minecraft.setScreen(back); }

    // ── helpers ───────────────────────────────────────────────────────────────
    private void toggle(GuiGraphics g, int tx, int ty, boolean on) {
        g.fill(tx,     ty + 1, tx + 22,     ty + 9,  on ? 0xFF2E7D32 : 0xFF333333);
        g.fill(tx + 1, ty,     tx + 21,     ty + 10, on ? 0xFF2E7D32 : 0xFF333333);
        int tx2 = on ? tx + 13 : tx + 1;
        g.fill(tx2, ty + 1, tx2 + 8, ty + 9, C_WHITE);
    }

    private void cube(GuiGraphics g, int cx, int cy, boolean hi) {
        int s = 14, h = s / 2, q = s / 4;
        g.fill(cx,     cy + q, cx + h,     cy + s - q, hi ? 0xFF4FC3F7 : 0xFF29B6F6);
        g.fill(cx + h, cy + q, cx + s,     cy + s - q, hi ? 0xFF0288D1 : 0xFF01579B);
        g.fill(cx + q, cy,     cx + s - q, cy + h,     hi ? 0xFF81D4FA : 0xFF4FC3F7);
    }

    private void border(GuiGraphics g, int bx, int by, int bw, int bh, int c) {
        g.fill(bx,          by,          bx + bw,      by + 1,      c);
        g.fill(bx,          by + bh - 1, bx + bw,      by + bh,     c);
        g.fill(bx,          by,          bx + 1,        by + bh,     c);
        g.fill(bx + bw - 1, by,          bx + bw,       by + bh,     c);
    }

    private void circle(GuiGraphics g, int cx, int cy, int r, int color) {
        g.fill(cx - r + 1, cy - r,     cx + r - 1, cy + r,     color);
        g.fill(cx - r,     cy - r + 1, cx + r,     cy + r - 1, color);
    }
}
