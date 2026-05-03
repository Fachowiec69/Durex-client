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

public class AddItemScreen extends Screen {

    // ── Colours ──────────────────────────────────────────────────────────────
    private static final int COL_PANEL  = 0xFF1A1A2E;
    private static final int COL_PANEL2 = 0xFF16213E;
    private static final int COL_ACCENT = 0xFF00D4FF;
    private static final int COL_GREEN  = 0xFF00FF88;
    private static final int COL_RED    = 0xFFFF4444;
    private static final int COL_GRAY   = 0xFF888888;
    private static final int COL_WHITE  = 0xFFFFFFFF;
    private static final int COL_BORDER = 0xFF2A2A4A;

    private final Screen parent;
    private final PriceEntry editingEntry; // null = add mode

    // Fields
    private EditBox nameField;
    private EditBox materialField;
    private EditBox maxPriceField;
    private EditBox sellPriceField;
    private EditBox minCountField;
    private EditBox sellCountField;
    private EditBox loreField;
    private EditBox enchantsField;

    // Toggles
    private boolean buyEnabled  = true;
    private boolean sellEnabled = false;

    // Panel geometry
    private int px, py, pw, ph;

    public AddItemScreen(Screen parent, PriceEntry entry) {
        super(Component.literal(entry == null ? "Dodaj przedmiot" : "Edytuj przedmiot"));
        this.parent       = parent;
        this.editingEntry = entry;
        if (entry != null) {
            this.buyEnabled  = Boolean.TRUE.equals(entry.buyEnabled);
            this.sellEnabled = Boolean.TRUE.equals(entry.sellEnabled);
        }
    }

    @Override
    protected void init() {
        pw = Math.min(420, width - 20);
        ph = Math.min(310, (int)(height * 0.88));
        px = (width  - pw) / 2;
        py = (height - ph) / 2;

        int cx  = px + pw / 2;
        int fw  = pw - 40;
        int fx  = px + 20;
        int fy  = py + 36;
        int fh  = 14;
        int gap = 22;

        // ── Text fields ──────────────────────────────────────────────────────
        nameField = makeField(fx, fy, fw, fh, "Nazwa przedmiotu");
        fy += gap;

        materialField = makeField(fx, fy, fw, fh, "Material (np. minecraft:diamond_sword)");
        fy += gap;

        maxPriceField = makeField(fx, fy, fw / 2 - 4, fh, "Max cena kupna ($)");
        sellPriceField = makeField(fx + fw / 2 + 4, fy, fw / 2 - 4, fh, "Cena sprzedazy ($)");
        fy += gap;

        minCountField  = makeField(fx, fy, fw / 2 - 4, fh, "Ilosc min");
        sellCountField = makeField(fx + fw / 2 + 4, fy, fw / 2 - 4, fh, "Ilosc sprzedazy");
        fy += gap;

        loreField = makeField(fx, fy, fw, fh, "Lore (opcjonalne)");
        fy += gap;

        enchantsField = makeField(fx, fy, fw, fh, "Enchanty (opcjonalne)");
        fy += gap + 4;

        // ── Toggle buttons ───────────────────────────────────────────────────
        int tbw = fw / 2 - 4;
        addRenderableWidget(Button.builder(
            Component.literal("Kupno: " + (buyEnabled ? "§aON" : "§cOFF")),
            btn -> {
                buyEnabled = !buyEnabled;
                btn.setMessage(Component.literal("Kupno: " + (buyEnabled ? "§aON" : "§cOFF")));
            }
        ).bounds(fx, fy, tbw, 16).build());

        addRenderableWidget(Button.builder(
            Component.literal("Sprzedaz: " + (sellEnabled ? "§aON" : "§cOFF")),
            btn -> {
                sellEnabled = !sellEnabled;
                btn.setMessage(Component.literal("Sprzedaz: " + (sellEnabled ? "§aON" : "§cOFF")));
            }
        ).bounds(fx + tbw + 8, fy, tbw, 16).build());
        fy += 22;

        // ── Auto-fill from hand ───────────────────────────────────────────────
        addRenderableWidget(Button.builder(Component.literal("Auto-wypelnij z reki"), btn -> fillFromHand())
            .bounds(fx, fy, fw, 16).build());
        fy += 22;

        // ── Save / Cancel ─────────────────────────────────────────────────────
        int bw = fw / 2 - 4;
        addRenderableWidget(Button.builder(Component.literal("§a✓ Zapisz"), btn -> save())
            .bounds(fx, fy, bw, 18).build());
        addRenderableWidget(Button.builder(Component.literal("§c✕ Anuluj"), btn -> minecraft.setScreen(parent))
            .bounds(fx + bw + 8, fy, bw, 18).build());

        // ── Pre-fill if editing ───────────────────────────────────────────────
        if (editingEntry != null) {
            if (editingEntry.name     != null) nameField.setValue(editingEntry.name);
            if (editingEntry.material != null) materialField.setValue(editingEntry.material);
            if (editingEntry.maxPrice  > 0)    maxPriceField.setValue(String.valueOf((int)editingEntry.maxPrice));
            if (editingEntry.sellPrice > 0)    sellPriceField.setValue(String.valueOf((int)editingEntry.sellPrice));
            if (editingEntry.requiredCount != null) minCountField.setValue(String.valueOf(editingEntry.requiredCount));
            if (editingEntry.sellCount     != null) sellCountField.setValue(String.valueOf(editingEntry.sellCount));
            if (editingEntry.lore     != null) loreField.setValue(editingEntry.lore);
            if (editingEntry.enchants != null) enchantsField.setValue(editingEntry.enchants);
        }
    }

    private EditBox makeField(int x, int y, int w, int h, String hint) {
        EditBox box = new EditBox(font, x, y, w, h, Component.literal(""));
        box.setHint(Component.literal("§8" + hint));
        box.setBordered(true);
        addRenderableWidget(box);
        return box;
    }

    private void fillFromHand() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        ItemStack held = mc.player.getMainHandItem();
        if (held.isEmpty()) return;
        String rawName = held.getHoverName().getString().replaceAll("§[0-9a-fk-or]", "").trim();
        String mat     = BuiltInRegistries.ITEM.getKey(held.getItem()).toString();
        nameField.setValue(rawName);
        materialField.setValue(mat);
    }

    private void save() {
        String name = nameField.getValue().trim();
        if (name.isEmpty()) return;

        double maxPrice = 0, sellPrice = 0;
        int minCount = 1, sellCount = 1;
        try { maxPrice  = Double.parseDouble(maxPriceField.getValue().replace(",", ".")); }  catch (Exception ignored) {}
        try { sellPrice = Double.parseDouble(sellPriceField.getValue().replace(",", ".")); } catch (Exception ignored) {}
        try { minCount  = Integer.parseInt(minCountField.getValue().trim()); }               catch (Exception ignored) {}
        try { sellCount = Integer.parseInt(sellCountField.getValue().trim()); }              catch (Exception ignored) {}

        Minecraft mc = Minecraft.getInstance();
        String serverIp = mc.getCurrentServer() != null ? mc.getCurrentServer().ip : "";
        ServerProfile profile = ConfigManager.findProfile(serverIp);
        if (profile == null) return;

        PriceEntry e = editingEntry != null ? editingEntry : new PriceEntry();
        e.name          = name;
        e.material      = materialField.getValue().trim();
        e.maxPrice      = maxPrice;
        e.sellPrice     = sellPrice;
        e.buyEnabled    = buyEnabled;
        e.sellEnabled   = sellEnabled;
        e.requiredCount = minCount;
        e.sellCount     = sellCount;
        e.lore          = loreField.getValue().trim().isEmpty()     ? null : loreField.getValue().trim();
        e.enchants      = enchantsField.getValue().trim().isEmpty() ? null : enchantsField.getValue().trim();
        e.componentCount = 0;

        if (editingEntry == null) {
            profile.prices.add(e);
        }
        ConfigManager.save();
        minecraft.setScreen(parent);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        // Dim
        g.fill(0, 0, width, height, 0xBB000000);

        // Panel
        g.fill(px + 3, py + 3, px + pw + 3, py + ph + 3, 0x55000000);
        g.fill(px, py, px + pw, py + ph, COL_PANEL);
        drawBorder(g, px, py, pw, ph, COL_BORDER);
        g.fill(px, py, px + pw, py + 2, COL_ACCENT);

        // Header
        g.fill(px, py + 2, px + pw, py + 28, COL_PANEL2);
        g.fill(px, py + 28, px + pw, py + 29, COL_BORDER);
        String title = editingEntry == null ? "§b§lDodaj przedmiot" : "§b§lEdytuj przedmiot";
        g.drawCenteredString(font, title, px + pw / 2, py + 10, COL_WHITE);

        // Field labels
        int fx = px + 20;
        int fy = py + 36;
        int gap = 22;
        drawLabel(g, fx, fy - 9, "Nazwa");                fy += gap;
        drawLabel(g, fx, fy - 9, "Material");             fy += gap;
        drawLabel(g, fx, fy - 9, "Max cena / Sprzedaz");  fy += gap;
        drawLabel(g, fx, fy - 9, "Ilosc min / Sprzedazy");fy += gap;
        drawLabel(g, fx, fy - 9, "Lore");                 fy += gap;
        drawLabel(g, fx, fy - 9, "Enchanty");

        // Item preview
        String mat = materialField.getValue().trim();
        if (!mat.isEmpty()) {
            try {
                var opt = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(mat));
                if (opt.isPresent()) {
                    ItemStack stack = new ItemStack(opt.get());
                    if (!stack.isEmpty()) {
                        g.renderItem(stack, px + pw - 30, py + 36);
                    }
                }
            } catch (Exception ignored) {}
        }

        super.render(g, mx, my, delta);
    }

    private void drawLabel(GuiGraphics g, int x, int y, String text) {
        g.drawString(font, "§8" + text, x, y, COL_GRAY);
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
