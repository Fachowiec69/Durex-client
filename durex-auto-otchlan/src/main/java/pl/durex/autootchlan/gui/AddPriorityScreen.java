package pl.durex.autootchlan.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import pl.durex.autootchlan.config.ConfigManager;
import pl.durex.autootchlan.config.PriorityEntry;

/**
 * Ekran dodawania / edycji priorytetu.
 * Styl: ciemny, minimalistyczny jak KateClient.
 */
public class AddPriorityScreen extends Screen {

    private static final int COL_BG     = 0xFF1A1A1A;
    private static final int COL_HEADER = 0xFF222222;
    private static final int COL_BORDER = 0xFF333333;
    private static final int COL_WHITE  = 0xFFFFFFFF;
    private static final int COL_GRAY   = 0xFF888888;
    private static final int COL_BLUE   = 0xFF4FC3F7;

    private final Screen parent;
    private final PriorityEntry editingEntry;
    private final int editingIndex;

    private EditBox keywordField;
    private EditBox displayNameField;
    private EditBox priorityField;

    private int px, py, pw, ph;

    public AddPriorityScreen(Screen parent, PriorityEntry entry, int index) {
        super(Component.literal(entry == null ? "Dodaj priorytet" : "Edytuj priorytet"));
        this.parent = parent;
        this.editingEntry = entry;
        this.editingIndex = index;
    }

    @Override
    protected void init() {
        pw = 300;
        ph = 180;
        px = (width  - pw) / 2;
        py = (height - ph) / 2;

        int fx = px + 16;
        int fy = py + 40;
        int fw = pw - 32;

        // Keyword
        keywordField = new EditBox(font, fx, fy, fw, 14, Component.literal(""));
        keywordField.setHint(Component.literal("§8Słowo kluczowe (np. shulker, spawner)"));
        keywordField.setBordered(true);
        addRenderableWidget(keywordField);
        fy += 22;

        // Display name
        displayNameField = new EditBox(font, fx, fy, fw, 14, Component.literal(""));
        displayNameField.setHint(Component.literal("§8Nazwa wyświetlana (np. Shulker Box)"));
        displayNameField.setBordered(true);
        addRenderableWidget(displayNameField);
        fy += 22;

        // Priority
        priorityField = new EditBox(font, fx, fy, fw / 2, 14, Component.literal(""));
        priorityField.setHint(Component.literal("§8Priorytet (1-10)"));
        priorityField.setBordered(true);
        addRenderableWidget(priorityField);
        fy += 26;

        // Buttons
        int bw = (fw - 8) / 2;
        addRenderableWidget(Button.builder(Component.literal("§a✓ Zapisz"), btn -> save())
            .bounds(fx, fy, bw, 18).build());
        addRenderableWidget(Button.builder(Component.literal("§c✕ Anuluj"), btn -> minecraft.setScreen(parent))
            .bounds(fx + bw + 8, fy, bw, 18).build());

        // Auto-fill z ręki
        addRenderableWidget(Button.builder(Component.literal("§7← Z ręki"), btn -> fillFromHand())
            .bounds(px + pw - 70, py + 6, 60, 14).build());

        // Pre-fill jeśli edycja
        if (editingEntry != null) {
            if (editingEntry.keyword     != null) keywordField.setValue(editingEntry.keyword);
            if (editingEntry.displayName != null) displayNameField.setValue(editingEntry.displayName);
            priorityField.setValue(String.valueOf(editingEntry.priority));
        } else {
            priorityField.setValue("5");
        }
    }

    private void fillFromHand() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        ItemStack held = mc.player.getMainHandItem();
        if (held.isEmpty()) return;
        String name = held.getHoverName().getString().replaceAll("§[0-9a-fk-or]", "").trim();
        keywordField.setValue(name.toLowerCase());
        displayNameField.setValue(name);
    }

    private void save() {
        String keyword = keywordField.getValue().trim();
        if (keyword.isEmpty()) return;

        String displayName = displayNameField.getValue().trim();
        if (displayName.isEmpty()) displayName = keyword;

        int priority = 5;
        try { priority = Integer.parseInt(priorityField.getValue().trim()); } catch (Exception ignored) {}
        priority = Math.max(0, Math.min(10, priority));

        if (editingEntry != null && editingIndex >= 0) {
            // Edycja istniejącego — modyfikuj in-place i zapisz
            editingEntry.keyword     = keyword;
            editingEntry.displayName = displayName;
            editingEntry.priority    = priority;
            ConfigManager.save(); // auto-save
        } else {
            // Nowy wpis — auto-save w addPriority()
            ConfigManager.addPriority(new PriorityEntry(keyword, priority, displayName));
        }

        minecraft.setScreen(parent);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        // Dim
        g.fill(0, 0, width, height, 0x88000000);

        // Panel
        g.fill(px + 3, py + 3, px + pw + 3, py + ph + 3, 0x44000000);
        fillRounded(g, px, py, pw, ph, COL_BG);
        drawBorder(g, px, py, pw, ph, COL_BORDER);

        // Header
        fillRounded(g, px, py, pw, 28, COL_HEADER);
        g.fill(px, py + 27, px + pw, py + 28, COL_BORDER);

        String title = editingEntry == null ? "§fDodaj priorytet" : "§fEdytuj priorytet";
        g.drawString(font, title, px + 12, py + 10, COL_WHITE, false);

        // Etykiety pól
        int fx = px + 16;
        int fy = py + 40;
        drawLabel(g, fx, fy - 9, "Słowo kluczowe");  fy += 22;
        drawLabel(g, fx, fy - 9, "Nazwa");            fy += 22;
        drawLabel(g, fx, fy - 9, "Priorytet (0-10)");

        super.render(g, mx, my, delta);
    }

    private void drawLabel(GuiGraphics g, int x, int y, String text) {
        g.drawString(font, "§8" + text, x, y, COL_GRAY, false);
    }

    private void fillRounded(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x + 1, y,     x + w - 1, y + h,     color);
        g.fill(x,     y + 1, x + w,     y + h - 1, color);
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
