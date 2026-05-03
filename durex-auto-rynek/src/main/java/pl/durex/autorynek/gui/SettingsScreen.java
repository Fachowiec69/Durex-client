package pl.durex.autorynek.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import pl.durex.autorynek.config.ConfigManager;
import pl.durex.autorynek.config.ServerProfile;

public class SettingsScreen extends Screen {

    // ── Colours ──────────────────────────────────────────────────────────────
    private static final int COL_PANEL  = 0xFF1A1A2E;
    private static final int COL_PANEL2 = 0xFF16213E;
    private static final int COL_ACCENT = 0xFF00D4FF;
    private static final int COL_GRAY   = 0xFF888888;
    private static final int COL_WHITE  = 0xFFFFFFFF;
    private static final int COL_BORDER = 0xFF2A2A4A;

    private static final int MARGIN = 10;

    private final Screen parent;
    private ServerProfile profile;

    // Fields – market
    private EditBox loreRegexField;
    private EditBox marketGuiTitleField;
    private EditBox nextPageNameField;
    private EditBox nextPageMaterialField;
    private EditBox nextPageSlotField;
    private EditBox sortingSlotField;
    private EditBox sortingKeywordField;
    private EditBox loginCommandField;

    // Fields – delays
    private EditBox nextDelayField;
    private EditBox actionDelayField;
    private EditBox confirmDelayField;
    private EditBox openDelayField;
    private EditBox closeDelayField;

    // Toggles
    private boolean autoReconnect;
    private boolean aggressiveMode;
    private boolean autoLearnEnabled;

    // Panel geometry
    private int px, py, pw, ph;

    public SettingsScreen(Screen parent) {
        super(Component.literal("Ustawienia"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        pw = Math.min(500, width - 20);
        ph = Math.min(380, (int)(height * 0.92));
        px = (width  - pw) / 2;
        py = (height - ph) / 2;

        String serverIp = Minecraft.getInstance().getCurrentServer() != null
            ? Minecraft.getInstance().getCurrentServer().ip : "";
        profile = ConfigManager.findProfile(serverIp);

        if (profile == null) {
            // No profile – just show back button
            addRenderableWidget(Button.builder(Component.literal("§7< Wstecz"), btn -> minecraft.setScreen(parent))
                .bounds(px + MARGIN, py + ph - MARGIN - 16, 80, 16).build());
            return;
        }

        // Init toggle states
        autoReconnect    = Boolean.TRUE.equals(profile.autoReconnect);
        aggressiveMode   = Boolean.TRUE.equals(profile.aggressiveMode);
        autoLearnEnabled = Boolean.TRUE.equals(profile.autoLearnEnabled);

        int fx  = px + MARGIN;
        int fw  = pw - MARGIN * 2;
        int hfw = fw / 2 - 4;
        int fy  = py + 32;
        int fh  = 14;
        int gap = 22;

        // ── Market settings ───────────────────────────────────────────────────
        loreRegexField       = makeField(fx, fy, fw, fh, "Lore Regex");
        if (profile.loreRegex != null) loreRegexField.setValue(profile.loreRegex);
        fy += gap;

        marketGuiTitleField  = makeField(fx, fy, hfw, fh, "Tytul GUI rynku");
        if (profile.marketGuiTitle != null) marketGuiTitleField.setValue(profile.marketGuiTitle);

        nextPageNameField    = makeField(fx + hfw + 8, fy, hfw, fh, "Nazwa nast. strony");
        if (profile.marketNextPageName != null) nextPageNameField.setValue(profile.marketNextPageName);
        fy += gap;

        nextPageMaterialField = makeField(fx, fy, hfw, fh, "Material nast. strony");
        if (profile.marketNextPageMaterial != null) nextPageMaterialField.setValue(profile.marketNextPageMaterial);

        nextPageSlotField    = makeField(fx + hfw + 8, fy, hfw / 2 - 4, fh, "Slot nast. strony");
        if (profile.marketNextPageSlot != null) nextPageSlotField.setValue(String.valueOf(profile.marketNextPageSlot));

        sortingSlotField     = makeField(fx + hfw + 8 + hfw / 2 + 4, fy, hfw / 2 - 4, fh, "Slot sortowania");
        if (profile.sortingSlot != null) sortingSlotField.setValue(String.valueOf(profile.sortingSlot));
        fy += gap;

        sortingKeywordField  = makeField(fx, fy, hfw, fh, "Slowo kluczowe sortowania");
        if (profile.sortingKeyword != null) sortingKeywordField.setValue(profile.sortingKeyword);

        loginCommandField    = makeField(fx + hfw + 8, fy, hfw, fh, "Komenda logowania");
        if (profile.loginCommand != null) loginCommandField.setValue(profile.loginCommand);
        fy += gap;

        // ── Delay settings ────────────────────────────────────────────────────
        int qfw = fw / 4 - 4;
        nextDelayField    = makeField(fx,                fy, qfw, fh, "Delay strony (ms)");
        actionDelayField  = makeField(fx + qfw + 4,     fy, qfw, fh, "Delay akcji (ms)");
        confirmDelayField = makeField(fx + (qfw + 4)*2, fy, qfw, fh, "Delay potw. (ms)");
        openDelayField    = makeField(fx + (qfw + 4)*3, fy, qfw, fh, "Delay otw. (ms)");
        if (profile.marketNextDelayMs    != null) nextDelayField.setValue(String.valueOf(profile.marketNextDelayMs));
        if (profile.marketActionDelayMs  != null) actionDelayField.setValue(String.valueOf(profile.marketActionDelayMs));
        if (profile.marketConfirmDelayMs != null) confirmDelayField.setValue(String.valueOf(profile.marketConfirmDelayMs));
        if (profile.marketOpenDelayMs    != null) openDelayField.setValue(String.valueOf(profile.marketOpenDelayMs));
        fy += gap;

        closeDelayField = makeField(fx, fy, qfw, fh, "Delay zamk. (ms)");
        if (profile.marketCloseDelayMs != null) closeDelayField.setValue(String.valueOf(profile.marketCloseDelayMs));
        fy += gap + 4;

        // ── Toggle buttons ────────────────────────────────────────────────────
        int tbw = fw / 3 - 4;
        addRenderableWidget(Button.builder(
            Component.literal("AutoReconnect: " + (autoReconnect ? "§aON" : "§cOFF")),
            btn -> {
                autoReconnect = !autoReconnect;
                btn.setMessage(Component.literal("AutoReconnect: " + (autoReconnect ? "§aON" : "§cOFF")));
            }
        ).bounds(fx, fy, tbw, 16).build());

        addRenderableWidget(Button.builder(
            Component.literal("Agresywny: " + (aggressiveMode ? "§aON" : "§cOFF")),
            btn -> {
                aggressiveMode = !aggressiveMode;
                btn.setMessage(Component.literal("Agresywny: " + (aggressiveMode ? "§aON" : "§cOFF")));
            }
        ).bounds(fx + tbw + 4, fy, tbw, 16).build());

        addRenderableWidget(Button.builder(
            Component.literal("AutoNauka: " + (autoLearnEnabled ? "§aON" : "§cOFF")),
            btn -> {
                autoLearnEnabled = !autoLearnEnabled;
                btn.setMessage(Component.literal("AutoNauka: " + (autoLearnEnabled ? "§aON" : "§cOFF")));
            }
        ).bounds(fx + (tbw + 4) * 2, fy, tbw, 16).build());
        fy += 22;

        // ── Save / Cancel ─────────────────────────────────────────────────────
        int bw = fw / 2 - 4;
        addRenderableWidget(Button.builder(Component.literal("§a✓ Zapisz"), btn -> save())
            .bounds(fx, fy, bw, 18).build());
        addRenderableWidget(Button.builder(Component.literal("§7< Wstecz"), btn -> minecraft.setScreen(parent))
            .bounds(fx + bw + 8, fy, bw, 18).build());

        // Close X
        addRenderableWidget(Button.builder(Component.literal("X"), btn -> onClose())
            .bounds(px + pw - 18, py + 2, 16, 14).build());
    }

    private EditBox makeField(int x, int y, int w, int h, String hint) {
        EditBox box = new EditBox(font, x, y, w, h, Component.literal(""));
        box.setHint(Component.literal("§8" + hint));
        box.setBordered(true);
        addRenderableWidget(box);
        return box;
    }

    private void save() {
        if (profile == null) return;

        profile.loreRegex            = loreRegexField.getValue().trim();
        profile.marketGuiTitle       = marketGuiTitleField.getValue().trim();
        profile.marketNextPageName   = nextPageNameField.getValue().trim();
        profile.marketNextPageMaterial = nextPageMaterialField.getValue().trim();
        profile.sortingKeyword       = sortingKeywordField.getValue().trim();
        profile.loginCommand         = loginCommandField.getValue().trim();
        profile.autoReconnect        = autoReconnect;
        profile.aggressiveMode       = aggressiveMode;
        profile.autoLearnEnabled     = autoLearnEnabled;

        trySetInt(nextPageSlotField,    v -> profile.marketNextPageSlot    = v);
        trySetInt(sortingSlotField,     v -> profile.sortingSlot           = v);
        trySetInt(nextDelayField,       v -> profile.marketNextDelayMs     = v);
        trySetInt(actionDelayField,     v -> profile.marketActionDelayMs   = v);
        trySetInt(confirmDelayField,    v -> profile.marketConfirmDelayMs  = v);
        trySetInt(openDelayField,       v -> profile.marketOpenDelayMs     = v);
        trySetInt(closeDelayField,      v -> profile.marketCloseDelayMs    = v);

        ConfigManager.save();
        minecraft.setScreen(parent);
    }

    @FunctionalInterface
    private interface IntConsumer { void accept(int v); }

    private void trySetInt(EditBox field, IntConsumer consumer) {
        try { consumer.accept(Integer.parseInt(field.getValue().trim())); } catch (Exception ignored) {}
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
        g.drawCenteredString(font, "§b§lUstawienia profilu", px + pw / 2, py + 10, COL_WHITE);

        if (profile == null) {
            g.drawCenteredString(font, "§cBrak profilu dla tego serwera", px + pw / 2, py + ph / 2, 0xFFFF4444);
        } else {
            g.drawString(font, "§8Profil: §7" + profile.profileName, px + MARGIN, py + 20, COL_GRAY);
        }

        super.render(g, mx, my, delta);
    }

    private void drawBorder(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x,         y,         x + w,     y + 1,     color);
        g.fill(x,         y + h - 1, x + w,     y + h,     color);
        g.fill(x,         y,         x + 1,     y + h,     color);
        g.fill(x + w - 1, y,         x + w,     y + h,     color);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
