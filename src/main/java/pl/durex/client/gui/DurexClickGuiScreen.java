package pl.durex.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import pl.durex.client.DurexClient;
import pl.durex.client.gui.render.GuiRenderUtils;

import pl.durex.client.module.AntiKostkaModule;
import pl.durex.client.module.AntiKowalModule;
import pl.durex.client.module.CooldownHudModule;
import pl.durex.client.module.FriendModule;
import pl.durex.client.module.ViewModelModule;

import java.util.ArrayList;
import java.util.List;

public final class DurexClickGuiScreen extends Screen {

    // ── Modules ───────────────────────────────────────────────────────────
    private final AntiKowalModule   antiKowal   = DurexClient.getAntiKowalModule();
    private final FriendModule      friendModule = DurexClient.getFriendModule();
    private final CooldownHudModule cooldownHud  = DurexClient.getCooldownHudModule();
    private final AntiKostkaModule  antiKostka   = DurexClient.getAntiKostkaModule();
    private final ViewModelModule   viewModel    = DurexClient.getViewModelModule();
    private final pl.durex.client.module.ProcenciarzModule  procenciarz  = DurexClient.getProcenciarzModule();
    private final pl.durex.client.module.LeverCobwebModule  leverCobweb  = DurexClient.getLeverCobwebModule();
    private final pl.durex.client.module.AutoDripstoneModule autoDripstone = DurexClient.getAutoDripstoneModule();
    private final pl.durex.client.module.NoPushModule       noPush       = DurexClient.getNoPushModule();

    // MsgBot fields
    private String msgBotMsgInput  = pl.durex.client.module.MsgBotModule.message;
    private String msgBotNickInput = pl.durex.client.module.MsgBotModule.targetPlayer;
    private String msgBotChDelay   = String.valueOf(pl.durex.client.module.MsgBotModule.chDelaySec);
    private int    msgBotEditingField = 0;

    // ── Layout constants ──────────────────────────────────────────────────
    // Left panel (categories)
    private static final int LP_W    = 120;  // left panel width (mniejszy)
    private static final int CAT_H   = 26;   // category row height (mniejszy)
    private static final int LOGO_H  = 110;  // logo area height (mniejszy)
    private static final int BTN_H   = 22;   // bottom button height (mniejszy)

    // Right panel (modules)
    private static final int MOD_H   = 32;   // module row height
    private static final int SUB_H   = 20;   // sub-content row height
    private static final int HOTBAR_H= 28;
    private static final int ICON_SIZE= 16;
    private static final int PAD     = 8;
    private static final int W       = 220;  // kept for sub-content helpers

    // GUI total size — bardziej prostokątne
    private static final int GUI_W   = 520;  // szersze
    private static final int GUI_H   = 340;  // niższe

    // Search
    private String searchQuery = "";
    private boolean searchFocused = false;

    // ── Colors (theme-based) ──────────────────────────────────────────────
    private int COL_BG, COL_LP_BG, COL_RP_BG, COL_BORDER, COL_ACCENT, COL_TEXT, COL_MUTED, COL_ON, COL_OFF;
    private static final int COL_CAT_SEL  = 0xFF1A0035;
    private static final int COL_CAT_HOV  = 0xFF130028;
    private static final int COL_MOD_BG   = 0xFF0F001E;
    private static final int COL_MOD_HOV  = 0xFF180030;
    private static final int COL_BIND     = 0xFFFFCC44;
    private static final int COL_BLUE     = 0xFF4499FF;
    private static final int COL_SUB      = 0x661A0035;
    private static final int COL_HOVER    = 0xAA2A0060;
    private static final int COL_TOGGLE_ON  = 0xFF8800EE;
    private static final int COL_TOGGLE_OFF = 0xFF333344;
    
    private void loadThemeColors() {
        pl.durex.client.settings.ClientSettings.Theme t = pl.durex.client.settings.ClientSettings.getTheme();
        COL_BG = t.bg;
        COL_LP_BG = t.panel;
        COL_RP_BG = t.panel;
        COL_BORDER = t.border;
        COL_ACCENT = t.accent;
        COL_TEXT = t.text;
        COL_MUTED = t.muted;
        COL_ON = t.on;
        COL_OFF = t.off;
    }

    // ── Module definitions ────────────────────────────────────────────────
    private record ModDef(String id, String itemId, String label, String desc, String cat) {}

    private static final ModDef[] ALL_MODS = {
        // Combat
        new ModDef("antiKowal",     "anvil",            "AntiKowal",      "Blokuje naprawy u kowala",      "Combat"),
        new ModDef("noPush",        "ender_pearl",      "No Push",        "Brak odpychania przez graczy",  "Combat"),
        new ModDef("autoShieldBreak","iron_axe",        "Shield Break",   "Auto przełącza na topór",       "Combat"),
        // Player
        new ModDef("antiKostka",    "bone",             "AntiKostka",     "Zapisuje i ładuje hotbar",      "Player"),
        new ModDef("viewModel",     "diamond_sword",    "ViewModel",      "Edytor pozycji ręki/broni",     "Player"),
        new ModDef("leverCobweb",   "lever",            "Nemos Helper",   "Auto dźwignia i pajęczyna",     "Player"),
        new ModDef("autoDripstone", "pointed_dripstone","Auto Dripstone", "Automatyczne stalagmity",       "Player"),
        new ModDef("msgBot",        "paper",            "Msg Bot",        "Automatyczne wiadomości",       "Player"),
        // Visuals
        new ModDef("nametags",      "name_tag",         "Nametags",       "Tagi nad głowami graczy",       "Visuals"),
        new ModDef("tracers",       "ender_eye",        "Tracers",        "Linie do graczy przez ściany",  "Visuals"),
        new ModDef("fullbright",    "torch",            "FullBright",     "Maksymalna jasność (gamma)",    "Visuals"),
        // Utils
        new ModDef("cooldowns",     "clock",            "Cooldowns",      "HUD z cooldownami itemów",      "Utils"),
        new ModDef("procenciarz",   "iron_sword",       "Procenciarz",    "Procent DMG broni celu",        "Utils"),
        new ModDef("zbrojmistrz",   "diamond_chestplate","Zbrojmistrz",   "Zbroja i enchanty celu",        "Utils"),
    };

    private static final String[] CATEGORIES = {"All", "Combat", "Player", "Visuals", "Utils"};
    private static final String[] CAT_ICONS  = {"✦", "✦", "✦", "✦", "✦"};

    // ── State ─────────────────────────────────────────────────────────────
    private static int  selectedCat   = 0;   // selected category index
    private static String expandedMod = null; // id of expanded module

    // Scroll in right panel
    private int rpScrollY = 0;

    // Bind/edit state
    private boolean waitingAntiKowalBind = false;
    private boolean waitingFriendBind    = false;
    private int     waitingHotbarBind    = -1;
    private int     editingSlotName      = -1;
    private final StringBuilder nameBuffer = new StringBuilder();
    private boolean settingPosition    = false;
    private boolean settingProcenciarz = false;
    private boolean settingZbrojmistrz = false;
    private boolean openTracerEditor   = false;
    private boolean delayExpanded      = false;
    private boolean draggingHud        = false;
    private int     hudDragOffX, hudDragOffY;

    // Slider drag
    private String draggingSlider = null;
    private int    sliderPx       = 0;

    // GUI position (centered)
    private int guiX, guiY;

    // Kept for config compatibility (unused in new GUI)
    public static final List<CategoryWidget> categories = new ArrayList<>();
    public static final List<ModWidget> freeMods = new ArrayList<>();
    public static int savedPanelX = -1, savedPanelY = -1;

    // Animation — slide in from left
    private float openAnim = 0f;
    // Config message
    private String configMsg = null;
    private long configMsgTime = 0;
    private boolean openConfigScreen = false;
    private boolean openSettingsScreen = false;

    public DurexClickGuiScreen() {
        super(Text.literal("Durex Client"));
        loadThemeColors();
    }

    @Override
    protected void init() {
        loadThemeColors(); // Reload theme on screen open
        guiX = (width  - GUI_W) / 2;
        guiY = (height - GUI_H) / 2;
        openAnim = 0f;
    }

    // ── Helpers ───────────────────────────────────────────────────────────
    private int lpX() { return guiX; }
    private int lpY() { return guiY; }
    private int rpX() { return guiX + LP_W; }
    private int rpY() { return guiY; }
    private int rpW() { return GUI_W - LP_W; }

    private List<ModDef> modsInCat(String cat) {
        List<ModDef> r = new ArrayList<>();
        String query = searchQuery.toLowerCase();
        
        for (ModDef d : ALL_MODS) {
            // Dla kategorii "All" pokaż wszystkie moduły
            if (!cat.equals("All") && !d.cat().equals(cat)) continue;
            
            // Filtruj według search query
            if (!query.isEmpty()) {
                boolean matches = d.label().toLowerCase().contains(query) ||
                                d.desc().toLowerCase().contains(query) ||
                                d.id().toLowerCase().contains(query);
                if (!matches) continue;
            }
            
            r.add(d);
        }
        return r;
    }

    private boolean isEnabled(String id) {
        return switch (id) {
            case "antiKowal"     -> antiKowal.isEnabled();
            case "cooldowns"     -> cooldownHud.isEnabled();
            case "antiKostka"    -> antiKostka.isEnabled();
            case "viewModel"     -> viewModel.isEnabled();
            case "procenciarz"   -> procenciarz.isEnabled();
            case "leverCobweb"   -> leverCobweb.isEnabled();
            case "autoDripstone" -> pl.durex.client.module.AutoDripstoneModule.isEnabled();
            case "noPush"        -> pl.durex.client.module.NoPushModule.isEnabled();
            case "msgBot"        -> pl.durex.client.module.MsgBotModule.isEnabled();
            case "zbrojmistrz"   -> pl.durex.client.module.ZbrojmistrzModule.isEnabled();
            case "nametags"      -> pl.durex.client.module.NametagsModule.isEnabled();
            case "tracers"       -> pl.durex.client.module.TracerModule.isEnabled();
            case "fullbright"    -> pl.durex.client.module.FullBrightModule.isEnabled();
            case "autoShieldBreak" -> pl.durex.client.module.AutoShieldBreakModule.isEnabled();
            default -> false;
        };
    }

    private void toggle(String id) {
        // Sprawdź stan przed toggle
        boolean wasEnabled = isEnabled(id);
        
        switch (id) {
            case "antiKowal"     -> antiKowal.toggle();
            case "cooldowns"     -> cooldownHud.setEnabled(!cooldownHud.isEnabled());
            case "antiKostka"    -> antiKostka.setEnabled(!antiKostka.isEnabled());
            case "viewModel"     -> viewModel.setEnabled(!viewModel.isEnabled());
            case "procenciarz"   -> procenciarz.setEnabled(!procenciarz.isEnabled());
            case "leverCobweb"   -> leverCobweb.setEnabled(!leverCobweb.isEnabled());
            case "autoDripstone" -> pl.durex.client.module.AutoDripstoneModule.toggle();
            case "noPush"        -> pl.durex.client.module.NoPushModule.toggle();
            case "msgBot"        -> {
                pl.durex.client.module.MsgBotModule.setEnabled(!pl.durex.client.module.MsgBotModule.isEnabled());
                if (!pl.durex.client.module.MsgBotModule.isEnabled()) {
                    pl.durex.client.module.MsgBotModule.stopSpam();
                    pl.durex.client.module.MsgBotModule.stopAfk();
                    pl.durex.client.module.MsgBotModule.stopAutoCh();
                }
            }
            case "zbrojmistrz"   -> pl.durex.client.module.ZbrojmistrzModule.setEnabled(!pl.durex.client.module.ZbrojmistrzModule.isEnabled());
            case "nametags"      -> pl.durex.client.module.NametagsModule.setEnabled(!pl.durex.client.module.NametagsModule.isEnabled());
            case "tracers"       -> pl.durex.client.module.TracerModule.setEnabled(!pl.durex.client.module.TracerModule.isEnabled());
            case "fullbright"    -> pl.durex.client.module.FullBrightModule.toggle();
            case "autoShieldBreak" -> pl.durex.client.module.AutoShieldBreakModule.toggle();
        }
        
        // Odtwórz dźwięk toggle on/off
        boolean isNowEnabled = isEnabled(id);
        if (isNowEnabled != wasEnabled) {
            pl.durex.client.settings.ClientSettings.playSound(
                isNowEnabled ? "asmr_toggle_on" : "asmr_toggle_off"
            );
        }
        
        pl.durex.client.DurexClient.saveNow();
    }

    // ── Sound ─────────────────────────────────────────────────────────────
    private void sound(String name, float pitch) {
        if (client == null) return;
        var se = net.minecraft.registry.Registries.SOUND_EVENT.get(net.minecraft.util.Identifier.of("minecraft", name));
        if (se != null) client.getSoundManager().play(PositionedSoundInstance.master(se, 1f, pitch));
    }
    private void soundSnap()             { pl.durex.client.settings.ClientSettings.playSound("asmr_pop"); }
    private void soundToggle(boolean on) { pl.durex.client.settings.ClientSettings.playSound(on ? "asmr_toggle_on" : "asmr_toggle_off"); }
    private void soundClick()            { pl.durex.client.settings.ClientSettings.playSound("asmr_click"); }
    private void soundPop()              { pl.durex.client.settings.ClientSettings.playSound("asmr_whoosh"); }

    // ── Render ────────────────────────────────────────────────────────────

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        // Open TracerEditor flag
        if (openTracerEditor) {
            openTracerEditor = false;
            net.minecraft.client.MinecraftClient.getInstance().setScreen(new TracerEditorScreen());
            return;
        }
        // Open ConfigScreen flag
        if (openConfigScreen) {
            openConfigScreen = false;
            net.minecraft.client.MinecraftClient.getInstance().setScreen(new ConfigScreen());
            return;
        }
        
        // Open SettingsScreen flag
        if (openSettingsScreen) {
            openSettingsScreen = false;
            net.minecraft.client.MinecraftClient.getInstance().setScreen(new SettingsScreen());
            return;
        }

        // Open anim — slide in from left, 120ms
        openAnim = Math.min(1f, openAnim + delta * 0.18f);
        float ease = openAnim * openAnim * (3f - 2f * openAnim);

        // Dark background
        ctx.fillGradient(0, 0, width, height, 0xAA000000, 0xAA000000);

        // Slide offset — GUI slides in from left
        int slideOff = (int)((1f - ease) * (GUI_W + 40));
        int ax = guiX - slideOff;
        int ay = guiY;

        // Clip to avoid drawing outside screen
        ctx.enableScissor(0, 0, width, height);

        // Main background
        ctx.fill(ax, ay, ax + GUI_W, ay + GUI_H, COL_BG);
        // Outer border
        hline(ctx, ax, ay, GUI_W, COL_BORDER);
        hline(ctx, ax, ay + GUI_H - 1, GUI_W, COL_BORDER);
        vline(ctx, ax, ay, GUI_H, COL_BORDER);
        vline(ctx, ax + GUI_W - 1, ay, GUI_H, COL_BORDER);

        // ── Left panel ────────────────────────────────────────────────────
        renderLeftPanel(ctx, mx, my, ax, ay);

        // ── Right panel ───────────────────────────────────────────────────
        renderRightPanel(ctx, mx, my, ax, ay);

        // ── Divider ───────────────────────────────────────────────────────
        vline(ctx, ax + LP_W, ay, GUI_H, COL_BORDER);

        ctx.disableScissor();

        // Config message
        if (configMsg != null) {
            long el = System.currentTimeMillis() - configMsgTime;
            if (el < 2000) {
                float al = el < 1500 ? 1f : 1f - (el - 1500) / 500f;
                int col = ((int)(al * 255) << 24) | 0x44FF88;
                ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(configMsg),
                    ax + GUI_W / 2, ay + GUI_H + 4, col);
            } else configMsg = null;
        }

        // HUD overlays
        if (settingPosition) {
            int hx = cooldownHud.getHudX(), hy = cooldownHud.getHudY();
            ctx.fill(hx - 2, hy - 2, hx + 62, hy + 32, 0xAA001133);
            ctx.fill(hx - 2, hy - 2, hx + 62, hy - 1, COL_BLUE);
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Cooldown HUD"), hx + 30, hy + 8, COL_BLUE);
        }
        if (settingProcenciarz) {
            int hx = procenciarz.getHudX(), hy = procenciarz.getHudY();
            ctx.fill(hx - 2, hy - 2, hx + 82, hy + 32, 0xAA001133);
            ctx.fill(hx - 2, hy - 2, hx + 82, hy - 1, COL_BLUE);
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Procenciarz"), hx + 40, hy + 8, COL_BLUE);
        }
        if (settingZbrojmistrz) {
            int hx = pl.durex.client.module.ZbrojmistrzModule.getHudX();
            int hy = pl.durex.client.module.ZbrojmistrzModule.getHudY();
            ctx.fill(hx - 2, hy - 2, hx + 172, hy + 122, 0xAA001133);
            ctx.fill(hx - 2, hy - 2, hx + 172, hy - 1, COL_BLUE);
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Zbrojmistrz"), hx + 85, hy + 8, COL_BLUE);
        }
    }

    private void renderLeftPanel(DrawContext ctx, int mx, int my, int offX, int offY) {
        int x = offX, y = offY;

        // Left panel background
        ctx.fill(x, y, x + LP_W, y + GUI_H, COL_LP_BG);

        // ── Logo area ─────────────────────────────────────────────────────────
        hline(ctx, x, y + LOGO_H - 1, LP_W, COL_BORDER);

        // Logo image — powiększony, wyśrodkowany w logo area, kolorowany według motywu
        net.minecraft.util.Identifier logoId = net.minecraft.util.Identifier.of("durexclient", "textures/logo.png");
        int imgSize = 78;
        int imgX = x + (LP_W - imgSize) / 2;
        int imgY = y + 8;
        
        // Ustaw kolor logo na podstawie motywu (accent color)
        float r = ((COL_ACCENT >> 16) & 0xFF) / 255f;
        float g = ((COL_ACCENT >> 8) & 0xFF) / 255f;
        float b = (COL_ACCENT & 0xFF) / 255f;
        RenderSystem.setShaderColor(r, g, b, 1.0f);
        
        ctx.drawTexture(net.minecraft.client.render.RenderLayer::getGuiTextured, logoId,
            imgX, imgY, 0, 0, imgSize, imgSize, imgSize, imgSize);
        
        // Resetuj kolor
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        // Tekst pod logo — gotycka czcionka (Unicode Fraktur)
        int textY = imgY + imgSize + 2;
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("𝔇𝔲𝔯𝔢𝔵 𝔠𝔩𝔦𝔢𝔫𝔱"), 
            x + LP_W / 2, textY, COL_ACCENT);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("𝔟𝔶 𝔉𝔞𝔠𝔥𝔬𝔴𝔦𝔢𝔠"), 
            x + LP_W / 2, textY + 10, COL_MUTED);

        // ── Category buttons ──────────────────────────────────────────────
        int cy = y + LOGO_H + 4;
        for (int i = 0; i < CATEGORIES.length; i++) {
            boolean sel = (i == selectedCat);
            boolean hov = mx >= x && mx < x + LP_W && my >= cy && my < cy + CAT_H;
            int bg = sel ? COL_CAT_SEL : (hov ? COL_CAT_HOV : 0);
            if (bg != 0) ctx.fill(x, cy, x + LP_W, cy + CAT_H, bg);
            // Left accent bar for selected
            if (sel) ctx.fill(x, cy, x + 3, cy + CAT_H, COL_BORDER);
            // Icon + label
            ctx.drawTextWithShadow(textRenderer, Text.literal(CAT_ICONS[i] + "  " + CATEGORIES[i]),
                x + 14, cy + (CAT_H - 8) / 2,
                sel ? COL_ACCENT : (hov ? COL_TEXT : COL_MUTED));
            // Separator
            hline(ctx, x, cy + CAT_H - 1, LP_W, 0x22FFFFFF);
            cy += CAT_H;
        }

        // ── Bottom buttons ────────────────────────────────────────────────
        int by = y + GUI_H - BTN_H * 2 - 2;
        // Settings
        boolean setH = mx >= x && mx < x + LP_W && my >= by && my < by + BTN_H;
        ctx.fill(x, by, x + LP_W, by + BTN_H, setH ? 0xFF0D1020 : 0xFF0A0018);
        hline(ctx, x, by, LP_W, 0x22FFFFFF);
        ctx.drawTextWithShadow(textRenderer, Text.literal("§d⚙  Settings"), x + 10, by + (BTN_H - 8) / 2, setH ? COL_ACCENT : 0xFF884499);
        by += BTN_H;
        // Close
        boolean clH = mx >= x && mx < x + LP_W && my >= by && my < by + BTN_H;
        ctx.fill(x, by, x + LP_W, by + BTN_H, clH ? 0xFF200010 : 0xFF0A0018);
        hline(ctx, x, by, LP_W, 0x22FFFFFF);
        ctx.drawTextWithShadow(textRenderer, Text.literal("§c✕  Zamknij"), x + 10, by + (BTN_H - 8) / 2, clH ? COL_OFF : 0xFFAA3344);
    }

    private void renderRightPanel(DrawContext ctx, int mx, int my, int offX, int offY) {
        int x = offX + LP_W, y = offY, w = rpW();

        // Right panel background
        ctx.fill(x, y, x + w, y + GUI_H, COL_RP_BG);

        // Category title
        ctx.fill(x, y, x + w, y + 30, 0xFF0D001E);
        hline(ctx, x, y + 30, w, COL_BORDER);
        ctx.drawTextWithShadow(textRenderer, Text.literal(CATEGORIES[selectedCat]),
            x + 14, y + 11, COL_ACCENT);

        // Scissor to right panel content area
        int contentY = y + 32;
        int contentH = GUI_H - 32;
        ctx.enableScissor(x, contentY, x + w, contentY + contentH);

        List<ModDef> mods = modsInCat(CATEGORIES[selectedCat]);
        int my2 = contentY - rpScrollY;

        for (ModDef mod : mods) {
            boolean enabled = isEnabled(mod.id());
            boolean expanded = mod.id().equals(expandedMod);
            boolean hov = mx >= x && mx < x + w && my >= my2 && my < my2 + MOD_H;

            // Module background
            ctx.fill(x, my2, x + w, my2 + MOD_H, hov ? COL_MOD_HOV : COL_MOD_BG);
            hline(ctx, x, my2 + MOD_H - 1, w, 0x22FFFFFF);

            // Left color bar
            ctx.fill(x, my2, x + 3, my2 + MOD_H, enabled ? COL_BORDER : 0x33440066);

            // Icon — item icon
            ItemStack iconStack = new ItemStack(net.minecraft.registry.Registries.ITEM.get(
                net.minecraft.util.Identifier.ofVanilla(mod.itemId())
            ));
            int iconX = x + 8;
            int iconY = my2 + (MOD_H - 16) / 2;
            ctx.drawItem(iconStack, iconX, iconY);

            // Name + desc
            ctx.drawTextWithShadow(textRenderer, Text.literal(mod.label()), x + 28, my2 + (MOD_H/2 - 9), enabled ? COL_TEXT : COL_MUTED);
            ctx.drawTextWithShadow(textRenderer, Text.literal("§8" + mod.desc()), x + 28, my2 + (MOD_H/2 + 1), 0xFF555566);

            // Toggle switch
            drawToggle(ctx, x + w - 44, my2 + (MOD_H - 12) / 2, enabled);

            // Expand arrow
            if (hasSubContent(mod.id())) {
                ctx.drawTextWithShadow(textRenderer,
                    Text.literal(expanded ? "§8▲" : "§8▼"),
                    x + w - 58, my2 + (MOD_H - 8) / 2, COL_MUTED);
            }

            my2 += MOD_H;

            // Sub-content
            if (expanded) {
                int subH = getSubHForId(mod.id());
                ctx.fill(x, my2, x + w, my2 + subH, 0xFF0C001A);
                ctx.fill(x, my2, x + 3, my2 + subH, 0x44AA44FF);
                renderModSubContent(ctx, mx, my, mod.id(), x, my2);
                my2 += subH;
            }
        }

        ctx.disableScissor();
    }

    private void drawToggle(DrawContext ctx, int x, int y, boolean on) {
        int w = 32, h = 12;
        // Track
        ctx.fill(x, y, x + w, y + h, on ? 0xFF440088 : COL_TOGGLE_OFF);
        ctx.fill(x, y, x + w, y + 1, on ? 0xFF6600CC : 0xFF444455);
        ctx.fill(x, y + h - 1, x + w, y + h, 0x33000000);
        // Knob
        int kx = on ? x + w - h : x;
        ctx.fill(kx, y, kx + h, y + h, on ? COL_BORDER : 0xFF666677);
        ctx.fill(kx + 1, y + 1, kx + h - 1, y + h - 1, on ? 0xFFAA44FF : 0xFF888899);
    }

    private boolean hasSubContent(String id) {
        return switch (id) {
            case "noPush", "fullbright" -> false;
            default -> true;
        };
    }

    private int getSubHForId(String id) {
        return switch (id) {
            case "antiKowal"     -> 4 * (SUB_H + 3) + PAD;
            case "cooldowns"     -> 2 * (SUB_H + 3) + PAD;
            case "viewModel"     -> 8 * (SUB_H + 3) + SUB_H + PAD;
            case "procenciarz"   -> 2 * (SUB_H + 3) + PAD;
            case "leverCobweb"   -> 5 * (SUB_H + 3) + PAD;
            case "autoDripstone" -> SUB_H + 3 + PAD;
            case "zbrojmistrz"   -> 2 * (SUB_H + 3) + PAD;
            case "msgBot"        -> 9 * (SUB_H + 3) + PAD;
            case "nametags"      -> 5 * (SUB_H + 3) + PAD;
            case "tracers"       -> 5 * (SUB_H + 3) + PAD;
            case "autoShieldBreak" -> 2 * (SUB_H + 3) + PAD;
            case "antiKostka"    -> (SUB_H + 3) +
                (delayExpanded ? AntiKostkaModule.DelayMode.values().length * (SUB_H + 3) : 0) +
                antiKostka.getSlots().size() * (SUB_H + 3 + HOTBAR_H + 3 + SUB_H + 6) +
                (antiKostka.getSlots().size() < AntiKostkaModule.MAX_SLOTS ? SUB_H + 3 : 0) + PAD;
            default -> 0;
        };
    }

    // ── Sub-content render ────────────────────────────────────────────────

    private void renderModSubContent(DrawContext ctx, int mx, int my, String id, int px, int sy) {
        // px here is rpX(), W is used for sub-content width = rpW()
        int rw = rpW();
        switch (id) {
            case "antiKowal" -> {
                drawSub(ctx, mx, my, px, sy, rw, "Hide Players", pl.durex.client.util.RaycastState.active ? "ON" : "OFF", pl.durex.client.util.RaycastState.active ? COL_ON : COL_OFF, false); sy += SUB_H + 3;
                drawSub(ctx, mx, my, px, sy, rw, waitingAntiKowalBind ? ">> Klawisz..." : "AntiKowal Bind", waitingAntiKowalBind ? "" : "[" + antiKowal.getBindName() + "]", COL_BIND, waitingAntiKowalBind); sy += SUB_H + 3;
                drawSub(ctx, mx, my, px, sy, rw, waitingFriendBind ? ">> Klawisz..." : "Friend Bind", waitingFriendBind ? "" : "[" + friendModule.getAddKeyName() + "]", COL_BIND, waitingFriendBind); sy += SUB_H + 3;
                drawSub(ctx, mx, my, px, sy, rw, "Clear Friends (" + friendModule.getFriends().size() + ")", "", COL_OFF, false);
            }
            case "cooldowns" -> {
                drawSub(ctx, mx, my, px, sy, rw, "Show HUD", cooldownHud.isEnabled() ? "ON" : "OFF", cooldownHud.isEnabled() ? COL_ON : COL_OFF, false); sy += SUB_H + 3;
                boolean spH = inSubR(mx, my, px, sy, rw);
                ctx.fill(px + PAD, sy, px + rw - PAD, sy + SUB_H, spH ? COL_HOVER : COL_SUB);
                ctx.fill(px + PAD, sy, px + PAD + 1, sy + SUB_H, settingPosition ? COL_BLUE : 0x664499FF);
                ctx.drawTextWithShadow(textRenderer, Text.literal("Set Position"), px + PAD + 8, sy + (SUB_H - 8) / 2, settingPosition ? COL_BLUE : 0xFFAADDFF);
            }
            case "antiKostka" -> {
                boolean dH = inSubR(mx, my, px, sy, rw);
                ctx.fill(px + PAD, sy, px + rw - PAD, sy + SUB_H, dH ? COL_HOVER : COL_SUB);
                ctx.drawTextWithShadow(textRenderer, Text.literal("Delay"), px + PAD + 6, sy + (SUB_H - 8) / 2, COL_MUTED);
                ctx.drawTextWithShadow(textRenderer, Text.literal(antiKostka.getDelayMode().label), px + PAD + 50, sy + (SUB_H - 8) / 2, COL_ACCENT);
                ctx.drawTextWithShadow(textRenderer, Text.literal(delayExpanded ? "▲" : "▼"), px + rw - PAD - 14, sy + (SUB_H - 8) / 2, COL_MUTED);
                sy += SUB_H + 3;
                if (delayExpanded) {
                    for (AntiKostkaModule.DelayMode mode : AntiKostkaModule.DelayMode.values()) {
                        boolean sel = antiKostka.getDelayMode() == mode;
                        boolean mH = inSubR(mx, my, px, sy, rw);
                        ctx.fill(px + PAD + 8, sy, px + rw - PAD, sy + SUB_H, mH ? COL_HOVER : (sel ? 0x88220044 : 0x33110022));
                        ctx.drawTextWithShadow(textRenderer, Text.literal(mode.label), px + PAD + 14, sy + (SUB_H - 8) / 2, sel ? COL_ACCENT : COL_TEXT);
                        if (sel) ctx.drawTextWithShadow(textRenderer, Text.literal("✔"), px + rw - PAD - 14, sy + (SUB_H - 8) / 2, COL_ON);
                        sy += SUB_H + 3;
                    }
                }
                for (int i = 0; i < antiKostka.getSlots().size(); i++) {
                    AntiKostkaModule.HotbarSlot slot = antiKostka.getSlots().get(i);
                    boolean waiting = waitingHotbarBind == i, editing = editingSlotName == i;
                    String label = editing ? (nameBuffer + "|") : (waiting ? ">> Klawisz..." : slot.name);
                    drawSub(ctx, mx, my, px, sy, rw, label, (editing || waiting) ? "" : "[" + slot.getLoadKeyName() + "]", editing ? COL_ACCENT : COL_BIND, waiting || editing);
                    sy += SUB_H + 3;
                    ctx.fill(px + PAD, sy, px + rw - PAD, sy + HOTBAR_H, 0x44110022);
                    if (slot.hasSaved) {
                        int iconX = px + PAD + 3, iconY = sy + (HOTBAR_H - ICON_SIZE) / 2;
                        for (int j = 0; j < 9; j++) {
                            ItemStack stack = slot.items[j];
                            if (stack != null && !stack.isEmpty()) ctx.drawItem(stack, iconX + j * (ICON_SIZE + 1), iconY);
                        }
                    } else {
                        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Brak zapisu"), px + rw / 2, sy + (HOTBAR_H - 8) / 2, 0x44FFFFFF);
                    }
                    sy += HOTBAR_H + 3;
                    int btnW = (rw - PAD * 2 - 4) / 2;
                    boolean saveH = mx >= px + PAD && mx <= px + PAD + btnW && my >= sy && my <= sy + SUB_H;
                    boolean delH  = mx >= px + PAD + btnW + 4 && mx <= px + PAD + btnW * 2 + 4 && my >= sy && my <= sy + SUB_H;
                    ctx.fill(px + PAD, sy, px + PAD + btnW, sy + SUB_H, saveH ? COL_HOVER : COL_SUB);
                    ctx.drawTextWithShadow(textRenderer, Text.literal("Save"), px + PAD + 6, sy + (SUB_H - 8) / 2, COL_ON);
                    ctx.fill(px + PAD + btnW + 4, sy, px + PAD + btnW * 2 + 4, sy + SUB_H, delH ? 0xAA400010 : 0x66200010);
                    ctx.drawTextWithShadow(textRenderer, Text.literal("Delete"), px + PAD + btnW + 10, sy + (SUB_H - 8) / 2, COL_OFF);
                    sy += SUB_H + 6;
                }
                if (antiKostka.getSlots().size() < AntiKostkaModule.MAX_SLOTS) {
                    boolean addH = inSubR(mx, my, px, sy, rw);
                    ctx.fill(px + PAD, sy, px + rw - PAD, sy + SUB_H, addH ? COL_HOVER : 0x44001133);
                    ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("+ Dodaj slot"), px + rw / 2, sy + (SUB_H - 8) / 2, COL_BLUE);
                }
            }
            case "viewModel" -> {
                net.minecraft.util.Arm mainArm = client != null && client.player != null ? client.player.getMainArm() : net.minecraft.util.Arm.RIGHT;
                boolean isMain = viewModel.getActiveHand() == ViewModelModule.Hand.RIGHT;
                String mainLabel = mainArm == net.minecraft.util.Arm.LEFT ? "Lewa (Main)" : "Prawa (Main)";
                String offLabel  = mainArm == net.minecraft.util.Arm.LEFT ? "Prawa (Off)" : "Lewa (Off)";
                boolean handH = inSubR(mx, my, px, sy, rw);
                ctx.fill(px + PAD, sy, px + rw - PAD, sy + SUB_H, handH ? COL_HOVER : COL_SUB);
                ctx.drawTextWithShadow(textRenderer, Text.literal("Reka: " + (isMain ? mainLabel : offLabel)), px + PAD + 6, sy + (SUB_H - 8) / 2, COL_BIND);
                sy += SUB_H + 3;
                String[] labels = {"Rot X","Rot Y","Rot Z","Pos X","Pos Y","Pos Z","Scale"};
                float[] values = {viewModel.getRotX(), viewModel.getRotY(), viewModel.getRotZ(), viewModel.getPosX(), viewModel.getPosY(), viewModel.getPosZ(), viewModel.getScale()};
                float[] mins = {ViewModelModule.ROT_MIN,ViewModelModule.ROT_MIN,ViewModelModule.ROT_MIN,ViewModelModule.POS_MIN,ViewModelModule.POS_MIN,ViewModelModule.POS_MIN,ViewModelModule.SCALE_MIN};
                float[] maxs = {ViewModelModule.ROT_MAX,ViewModelModule.ROT_MAX,ViewModelModule.ROT_MAX,ViewModelModule.POS_MAX,ViewModelModule.POS_MAX,ViewModelModule.POS_MAX,ViewModelModule.SCALE_MAX};
                for (int i = 0; i < labels.length; i++) { drawSliderR(ctx, mx, my, px, sy, rw, labels[i], values[i], mins[i], maxs[i]); sy += SUB_H + 3; }
                boolean rH = inSubR(mx, my, px, sy, rw);
                ctx.fill(px + PAD, sy, px + rw - PAD, sy + SUB_H, rH ? 0xAA400010 : 0x66200010);
                ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Reset " + (isMain ? mainLabel : offLabel)), px + rw / 2, sy + (SUB_H - 8) / 2, COL_OFF);
            }
            case "procenciarz" -> {
                boolean spH = inSubR(mx, my, px, sy, rw);
                ctx.fill(px + PAD, sy, px + rw - PAD, sy + SUB_H, spH ? COL_HOVER : 0x66001133);
                ctx.fill(px + PAD, sy, px + PAD + 1, sy + SUB_H, settingProcenciarz ? COL_BLUE : 0x664499FF);
                ctx.drawTextWithShadow(textRenderer, Text.literal("Set Position"), px + PAD + 8, sy + (SUB_H - 8) / 2, settingProcenciarz ? COL_BLUE : 0xFFAADDFF);
                sy += SUB_H + 3;
                boolean pb = pl.durex.client.module.ProcenciarzModule.isShowBooks();
                drawSub(ctx, mx, my, px, sy, rw, "Pokaż księgi", pb ? "ON" : "OFF", pb ? COL_ON : COL_MUTED, false);
            }
            case "leverCobweb" -> {
                drawSub(ctx, mx, my, px, sy, rw, "Szybkosc", leverCobweb.getSpeedProfile().name, COL_ACCENT, false); sy += SUB_H + 3;
                drawSub(ctx, mx, my, px, sy, rw, "Stawianie Przez Gracza", leverCobweb.isPlayerModeEnabled() ? "Wlaczone" : "Wylaczone", leverCobweb.isPlayerModeEnabled() ? COL_ON : COL_OFF, false); sy += SUB_H + 3;
                drawSub(ctx, mx, my, px, sy, rw, "Weby", leverCobweb.isWebModeEnabled() ? "Wlaczone" : "Wylaczone", leverCobweb.isWebModeEnabled() ? COL_ON : COL_OFF, false); sy += SUB_H + 3;
                drawSub(ctx, mx, my, px, sy, rw, "Do Miecza", leverCobweb.isSwitchToBestSwordEnabled() ? "Wlaczone" : "Wylaczone", leverCobweb.isSwitchToBestSwordEnabled() ? COL_ON : COL_OFF, false); sy += SUB_H + 3;
                drawSub(ctx, mx, my, px, sy, rw, "Auto Lever", leverCobweb.isLeverOnlyModeEnabled() ? "Wlaczone" : "Wylaczone", leverCobweb.isLeverOnlyModeEnabled() ? COL_ON : COL_OFF, false);
            }
            case "zbrojmistrz" -> {
                boolean sp = settingZbrojmistrz;
                ctx.fill(px + PAD, sy, px + rw - PAD, sy + SUB_H, sp ? COL_HOVER : 0x66001133);
                ctx.fill(px + PAD, sy, px + PAD + 1, sy + SUB_H, sp ? COL_BLUE : 0x664499FF);
                ctx.drawTextWithShadow(textRenderer, Text.literal("Set Position"), px + PAD + 8, sy + (SUB_H - 8) / 2, sp ? COL_BLUE : 0xFFAADDFF);
                sy += SUB_H + 3;
                boolean books = pl.durex.client.module.ZbrojmistrzModule.isShowBooks();
                drawSub(ctx, mx, my, px, sy, rw, "Pokaż księgi", books ? "ON" : "OFF", books ? COL_ON : COL_MUTED, false);
            }
            case "nametags" -> {
                boolean hp  = pl.durex.client.module.NametagsModule.isShowHp();
                boolean dst = pl.durex.client.module.NametagsModule.isShowDistance();
                boolean arm = pl.durex.client.module.NametagsModule.isShowArmor();
                boolean png = pl.durex.client.module.NametagsModule.isShowPing();
                boolean itm = pl.durex.client.module.NametagsModule.isShowItems();
                float maxD  = pl.durex.client.module.NametagsModule.getMaxDistance();
                drawSub(ctx, mx, my, px, sy, rw, "Pokaż HP",      hp  ? "ON" : "OFF", hp  ? COL_ON : COL_MUTED, false); sy += SUB_H + 3;
                drawSub(ctx, mx, my, px, sy, rw, "Pokaż dystans", dst ? "ON" : "OFF", dst ? COL_ON : COL_MUTED, false); sy += SUB_H + 3;
                drawSub(ctx, mx, my, px, sy, rw, "Pokaż zbroję",  arm ? "ON" : "OFF", arm ? COL_ON : COL_MUTED, false); sy += SUB_H + 3;
                drawSub(ctx, mx, my, px, sy, rw, "Pokaż ping",    png ? "ON" : "OFF", png ? COL_ON : COL_MUTED, false); sy += SUB_H + 3;
                drawSub(ctx, mx, my, px, sy, rw, "Pokaż itemy",   itm ? "ON" : "OFF", itm ? COL_ON : COL_MUTED, false); sy += SUB_H + 3;
                drawSliderR(ctx, mx, my, px, sy, rw, "Zasięg", maxD, 8f, 256f);
            }
            case "tracers" -> {
                int cidx = pl.durex.client.module.TracerModule.getColorIdx();
                float maxD = pl.durex.client.module.TracerModule.getMaxDistance();
                float[] col = pl.durex.client.module.TracerModule.getColor();
                int previewColor = 0xFF000000 | ((int)(col[0]*255) << 16) | ((int)(col[1]*255) << 8) | (int)(col[2]*255);
                drawSub(ctx, mx, my, px, sy, rw, "Kolor", pl.durex.client.module.TracerModule.COLOR_NAMES[cidx], previewColor, false); sy += SUB_H + 3;
                int sidx = pl.durex.client.module.TracerModule.getStyleIdx();
                int total = pl.durex.client.module.TracerModule.getTotalStyles();
                drawSub(ctx, mx, my, px, sy, rw, "Styl", (sidx+1) + "/" + total + " " + pl.durex.client.module.TracerModule.getStyleName(), COL_ACCENT, false); sy += SUB_H + 3;
                drawSliderR(ctx, mx, my, px, sy, rw, "Zasięg", maxD, 8f, 512f); sy += SUB_H + 3;
                drawSub(ctx, mx, my, px, sy, rw, "Custom Tracer", "Otwórz edytor →", COL_BLUE, false); sy += SUB_H + 3;
                if (pl.durex.client.module.TracerModule.isCustomStyle()) {
                    drawSub(ctx, mx, my, px, sy, rw, "§cUsuń Custom", "§8" + pl.durex.client.module.TracerModule.getStyleName(), COL_OFF, false);
                }
            }
            case "autoDripstone" -> {
                int spd = pl.durex.client.module.AutoDripstoneModule.getSpeed();
                drawSub(ctx, mx, my, px, sy, rw, "Szybkosc", spd + "/s  (LPM +1 | PPM -1)", COL_ACCENT, false);
            }
            case "msgBot" -> {
                boolean e1 = msgBotEditingField == 1;
                drawSub(ctx, mx, my, px, sy, rw, e1 ? ">> Wiadomosc:" : "Wiadomosc:", e1 ? (msgBotMsgInput + "|") : (msgBotMsgInput.isEmpty() ? "(puste)" : msgBotMsgInput), e1 ? COL_ACCENT : COL_BIND, e1); sy += SUB_H + 3;
                boolean e2 = msgBotEditingField == 2;
                drawSub(ctx, mx, my, px, sy, rw, e2 ? ">> Nick:" : "Nick:", e2 ? (msgBotNickInput + "|") : (msgBotNickInput.isEmpty() ? "(wszyscy)" : msgBotNickInput), e2 ? COL_ACCENT : COL_BIND, e2); sy += SUB_H + 3;
                boolean e3 = msgBotEditingField == 3;
                drawSub(ctx, mx, my, px, sy, rw, e3 ? ">> CH co (s):" : "CH co (s):", e3 ? (msgBotChDelay + "|") : msgBotChDelay, e3 ? COL_ACCENT : COL_MUTED, e3); sy += SUB_H + 3;
                boolean spam = pl.durex.client.module.MsgBotModule.isSpamming();
                drawSub(ctx, mx, my, px, sy, rw, spam ? "STOP spam" : "START spam", "Spamowanych: " + pl.durex.client.module.MsgBotModule.getTotalSpammed(), spam ? COL_OFF : COL_ON, false); sy += SUB_H + 3;
                drawSub(ctx, mx, my, px, sy, rw, "Reset listy", "", COL_MUTED, false); sy += SUB_H + 3;
                boolean afk = pl.durex.client.module.MsgBotModule.isAfk();
                drawSub(ctx, mx, my, px, sy, rw, "AFK Mode", afk ? "ON" : "OFF", afk ? COL_ON : COL_OFF, false); sy += SUB_H + 3;
                boolean ch = pl.durex.client.module.MsgBotModule.isAutoCh();
                drawSub(ctx, mx, my, px, sy, rw, "Auto CH", ch ? "ON" : "OFF", ch ? COL_ON : COL_OFF, false); sy += SUB_H + 3;
                boolean neth = pl.durex.client.module.MsgBotModule.onlyNetherite;
                drawSub(ctx, mx, my, px, sy, rw, "Only Netherite", neth ? "ON" : "OFF", neth ? COL_ON : COL_MUTED, false); sy += SUB_H + 3;
                boolean offline = pl.durex.client.module.MsgBotModule.onlyOffline;
                drawSub(ctx, mx, my, px, sy, rw, "Stiv bez Premki", offline ? "ON" : "OFF", offline ? COL_ON : COL_MUTED, false);
            }
            case "autoShieldBreak" -> {
                int delay = pl.durex.client.module.AutoShieldBreakModule.getDelayTicks();
                drawSliderR(ctx, mx, my, px, sy, rw, "Delay powrotu", delay, 1f, 20f); sy += SUB_H + 3;
                drawSub(ctx, mx, my, px, sy, rw, "Info", "~" + (delay * 50) + "ms - Auto atakuje toporem", COL_MUTED, false);
            }
        }
    }

    // ── Draw helpers ──────────────────────────────────────────────────────

    private void drawSub(DrawContext ctx, int mx, int my, int px, int sy, int rw,
            String label, String value, int color, boolean active) {
        boolean h = inSubR(mx, my, px, sy, rw);
        ctx.fill(px + PAD, sy, px + rw - PAD, sy + SUB_H, h ? COL_HOVER : COL_SUB);
        ctx.drawTextWithShadow(textRenderer, Text.literal(label), px + PAD + 6, sy + (SUB_H - 8) / 2, active ? color : COL_TEXT);
        if (!value.isEmpty()) ctx.drawTextWithShadow(textRenderer, Text.literal(value), px + rw - PAD - textRenderer.getWidth(value) - 4, sy + (SUB_H - 8) / 2, color);
    }

    private void drawSliderR(DrawContext ctx, int mx, int my, int px, int sy, int rw,
            String label, float value, float min, float max) {
        int labelW = 50; int barX = px + PAD + labelW; int barW = rw - PAD * 2 - labelW - 4;
        float t = (value - min) / (max - min);
        ctx.fill(px + PAD, sy, px + rw - PAD, sy + SUB_H, COL_SUB);
        ctx.drawTextWithShadow(textRenderer, Text.literal(label), px + PAD + 4, sy + (SUB_H - 8) / 2, COL_MUTED);
        ctx.fill(barX, sy + SUB_H / 2 - 2, barX + barW, sy + SUB_H / 2 + 2, 0x44FFFFFF);
        int fillW = (int)(t * barW);
        if (fillW > 0) ctx.fill(barX, sy + SUB_H / 2 - 2, barX + fillW, sy + SUB_H / 2 + 2, COL_ACCENT);
        int handleX = barX + fillW - 3;
        ctx.fill(handleX, sy + 3, handleX + 6, sy + SUB_H - 3, 0xFFFFFFFF);
        String valStr = String.format("%.1f", value);
        ctx.drawTextWithShadow(textRenderer, Text.literal(valStr), px + rw - PAD - textRenderer.getWidth(valStr) - 4, sy + (SUB_H - 8) / 2, COL_TEXT);
    }

    private boolean inSubR(double mx, double my, int px, int sy, int rw) {
        return mx >= px + PAD && mx <= px + rw - PAD && my >= sy && my <= sy + SUB_H;
    }

    private void hline(DrawContext ctx, int x, int y, int w, int color) {
        ctx.fill(x, y, x + w, y + 1, color);
    }
    private void vline(DrawContext ctx, int x, int y, int h, int color) {
        ctx.fill(x, y, x + 1, y + h, color);
    }

    // ── Mouse ─────────────────────────────────────────────────────────────
    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int x = guiX, y = guiY;

        // HUD drag
        if (settingPosition) {
            int hx = cooldownHud.getHudX(), hy = cooldownHud.getHudY();
            if (button == 0 && mx >= hx-2 && mx <= hx+62 && my >= hy-2 && my <= hy+32) {
                draggingHud = true; hudDragOffX = (int)mx-hx; hudDragOffY = (int)my-hy; return true;
            }
        }
        if (settingProcenciarz) {
            int hx = procenciarz.getHudX(), hy = procenciarz.getHudY();
            if (button == 0 && mx >= hx-2 && mx <= hx+82 && my >= hy-2 && my <= hy+32) {
                draggingHud = true; hudDragOffX = (int)mx-hx; hudDragOffY = (int)my-hy; return true;
            }
        }
        if (settingZbrojmistrz) {
            int hx = pl.durex.client.module.ZbrojmistrzModule.getHudX();
            int hy = pl.durex.client.module.ZbrojmistrzModule.getHudY();
            if (button == 0 && mx >= hx-2 && mx <= hx+172 && my >= hy-2 && my <= hy+122) {
                draggingHud = true; hudDragOffX = (int)mx-hx; hudDragOffY = (int)my-hy; return true;
            }
        }

        if (editingSlotName >= 0 && button == 0) { editingSlotName = -1; return true; }

        // Outside GUI — close (uwzględniając search bar)
        if (mx < guiX || mx > guiX+GUI_W || my < guiY || my > guiY+GUI_H) { close(); return true; }

        // ── Left panel ────────────────────────────────────────────────────
        if (mx >= guiX && mx < guiX + LP_W) {
            // Category buttons
            int cy = y + LOGO_H + 4;
            for (int i = 0; i < CATEGORIES.length; i++) {
                if (my >= cy && my < cy + CAT_H) {
                    if (selectedCat != i) { selectedCat = i; expandedMod = null; rpScrollY = 0; soundPop(); }
                    return true;
                }
                cy += CAT_H;
            }
            // Bottom buttons
            int by = y + GUI_H - BTN_H * 2 - 2;
            if (my >= by && my < by + BTN_H) {
                // Settings — otwórz ekran ustawień
                soundClick();
                openSettingsScreen = true;
                return true;
            }
            by += BTN_H;
            if (my >= by && my < by + BTN_H) { soundClick(); close(); return true; } // Close
            return true;
        }

        // ── Right panel ───────────────────────────────────────────────────
        if (mx >= rpX()) {
            int px = rpX(), rw = rpW();
            int contentY = y + 32;
            List<ModDef> mods = modsInCat(CATEGORIES[selectedCat]);
            int my2 = contentY - rpScrollY;

            for (ModDef mod : mods) {
                boolean expanded = mod.id().equals(expandedMod);

                // Module header row
                if (my >= my2 && my < my2 + MOD_H) {
                    // Toggle switch area
                    int toggleX = px + rw - 44;
                    int toggleY = my2 + (MOD_H - 12) / 2;
                    if (mx >= toggleX && mx <= toggleX + 32 && my >= toggleY && my <= toggleY + 12) {
                        toggle(mod.id());
                        soundToggle(isEnabled(mod.id()));
                        return true;
                    }
                    // Expand/collapse (click anywhere else on header)
                    if (hasSubContent(mod.id())) {
                        expandedMod = expanded ? null : mod.id();
                        soundPop();
                    }
                    return true;
                }
                my2 += MOD_H;

                // Sub-content clicks
                if (expanded) {
                    int subH = getSubHForId(mod.id());
                    if (my >= my2 && my < my2 + subH) {
                        handleSubClick(mod.id(), mx, my, px, my2, button, rw);
                        return true;
                    }
                    my2 += subH;
                }
            }
        }

        return super.mouseClicked(mx, my, button);
    }

    private void handleSubClick(String id, double mx, double my, int px, int sy, int button, int rw) {
        switch (id) {
            case "antiKowal" -> {
                if (inSubR(mx,my,px,sy,rw)) { if(button==0){pl.durex.client.util.RaycastState.active=!pl.durex.client.util.RaycastState.active;soundToggle(pl.durex.client.util.RaycastState.active);pl.durex.client.DurexClient.saveNow();} return; } sy+=SUB_H+3;
                if (inSubR(mx,my,px,sy,rw)) { if(button==0){waitingAntiKowalBind=true;waitingFriendBind=false;waitingHotbarBind=-1;soundClick();} return; } sy+=SUB_H+3;
                if (inSubR(mx,my,px,sy,rw)) { if(button==0){waitingFriendBind=true;waitingAntiKowalBind=false;waitingHotbarBind=-1;soundClick();} return; } sy+=SUB_H+3;
                if (inSubR(mx,my,px,sy,rw)) { if(button==0){friendModule.clearAll();soundSnap();pl.durex.client.DurexClient.saveNow();} }
            }
            case "cooldowns" -> {
                if (inSubR(mx,my,px,sy,rw)) { if(button==0){cooldownHud.setEnabled(!cooldownHud.isEnabled());soundToggle(cooldownHud.isEnabled());} return; } sy+=SUB_H+3;
                if (inSubR(mx,my,px,sy,rw)) { if(button==0){settingPosition=!settingPosition;soundClick();} }
            }
            case "antiKostka" -> {
                if (inSubR(mx,my,px,sy,rw)) { if(button==0){delayExpanded=!delayExpanded;soundClick();} return; } sy+=SUB_H+3;
                if (delayExpanded) {
                    for (AntiKostkaModule.DelayMode mode : AntiKostkaModule.DelayMode.values()) {
                        if (my>=sy && my<=sy+SUB_H && mx>=px+PAD+8 && mx<=px+rw-PAD) { if(button==0){antiKostka.setDelayMode(mode);soundClick();} return; }
                        sy+=SUB_H+3;
                    }
                }
                for (int i=0;i<antiKostka.getSlots().size();i++) {
                    if (inSubR(mx,my,px,sy,rw)) { if(button==0){waitingHotbarBind=i;waitingAntiKowalBind=false;waitingFriendBind=false;soundClick();} else if(button==1){editingSlotName=i;nameBuffer.setLength(0);nameBuffer.append(antiKostka.getSlots().get(i).name);soundClick();} return; }
                    sy+=SUB_H+3+HOTBAR_H+3;
                    int btnW2=(rw-PAD*2-4)/2;
                    if(mx>=px+PAD&&mx<=px+PAD+btnW2&&my>=sy&&my<=sy+SUB_H){if(button==0){antiKostka.saveSlot(i,client);soundSnap();}return;}
                    if(mx>=px+PAD+btnW2+4&&mx<=px+PAD+btnW2*2+4&&my>=sy&&my<=sy+SUB_H){if(button==0){antiKostka.removeSlot(i);soundSnap();}return;}
                    sy+=SUB_H+6;
                }
                if(inSubR(mx,my,px,sy,rw)&&button==0&&antiKostka.getSlots().size()<AntiKostkaModule.MAX_SLOTS){antiKostka.addSlot();soundPop();}
            }
            case "viewModel" -> {
                if(my>=sy&&my<=sy+SUB_H){if(button==0){viewModel.toggleHand();soundClick();}return;}sy+=SUB_H+3;
                String[]fields={"rotX","rotY","rotZ","posX","posY","posZ","scale"};
                for(String f:fields){if(my>=sy&&my<=sy+SUB_H){if(button==0){draggingSlider=f;sliderPx=px;applySlider(f,mx,px,rw);}return;}sy+=SUB_H+3;}
                if(my>=sy&&my<=sy+SUB_H&&mx>=px+PAD&&mx<=px+rw-PAD){if(button==0){viewModel.resetActive();soundSnap();}}
            }
            case "procenciarz" -> {
                if(inSubR(mx,my,px,sy,rw)){if(button==0){settingProcenciarz=!settingProcenciarz;soundClick();}return;}sy+=SUB_H+3;
                if(inSubR(mx,my,px,sy,rw)){if(button==0){pl.durex.client.module.ProcenciarzModule.setShowBooks(!pl.durex.client.module.ProcenciarzModule.isShowBooks());soundToggle(pl.durex.client.module.ProcenciarzModule.isShowBooks());pl.durex.client.DurexClient.saveNow();}}
            }
            case "leverCobweb" -> {
                if(inSubR(mx,my,px,sy,rw)){if(button==0){leverCobweb.setSpeedProfile(nextSpeedProfile(leverCobweb.getSpeedProfile()));soundClick();pl.durex.client.DurexClient.saveNow();}return;}sy+=SUB_H+3;
                if(inSubR(mx,my,px,sy,rw)){if(button==0){leverCobweb.setPlayerModeEnabled(!leverCobweb.isPlayerModeEnabled());soundToggle(leverCobweb.isPlayerModeEnabled());pl.durex.client.DurexClient.saveNow();}return;}sy+=SUB_H+3;
                if(inSubR(mx,my,px,sy,rw)){if(button==0){leverCobweb.setWebModeEnabled(!leverCobweb.isWebModeEnabled());soundToggle(leverCobweb.isWebModeEnabled());pl.durex.client.DurexClient.saveNow();}return;}sy+=SUB_H+3;
                if(inSubR(mx,my,px,sy,rw)){if(button==0){leverCobweb.setSwitchToBestSwordEnabled(!leverCobweb.isSwitchToBestSwordEnabled());soundToggle(leverCobweb.isSwitchToBestSwordEnabled());pl.durex.client.DurexClient.saveNow();}return;}sy+=SUB_H+3;
                if(inSubR(mx,my,px,sy,rw)){if(button==0){leverCobweb.setLeverOnlyModeEnabled(!leverCobweb.isLeverOnlyModeEnabled());soundToggle(leverCobweb.isLeverOnlyModeEnabled());pl.durex.client.DurexClient.saveNow();}}
            }
            case "zbrojmistrz" -> {
                if(inSubR(mx,my,px,sy,rw)){if(button==0){settingZbrojmistrz=!settingZbrojmistrz;soundClick();}return;}sy+=SUB_H+3;
                if(inSubR(mx,my,px,sy,rw)){if(button==0){pl.durex.client.module.ZbrojmistrzModule.setShowBooks(!pl.durex.client.module.ZbrojmistrzModule.isShowBooks());soundToggle(pl.durex.client.module.ZbrojmistrzModule.isShowBooks());pl.durex.client.DurexClient.saveNow();}}
            }
            case "nametags" -> {
                if(inSubR(mx,my,px,sy,rw)){if(button==0){pl.durex.client.module.NametagsModule.setShowHp(!pl.durex.client.module.NametagsModule.isShowHp());soundToggle(pl.durex.client.module.NametagsModule.isShowHp());pl.durex.client.DurexClient.saveNow();}return;}sy+=SUB_H+3;
                if(inSubR(mx,my,px,sy,rw)){if(button==0){pl.durex.client.module.NametagsModule.setShowDistance(!pl.durex.client.module.NametagsModule.isShowDistance());soundToggle(pl.durex.client.module.NametagsModule.isShowDistance());pl.durex.client.DurexClient.saveNow();}return;}sy+=SUB_H+3;
                if(inSubR(mx,my,px,sy,rw)){if(button==0){pl.durex.client.module.NametagsModule.setShowArmor(!pl.durex.client.module.NametagsModule.isShowArmor());soundToggle(pl.durex.client.module.NametagsModule.isShowArmor());pl.durex.client.DurexClient.saveNow();}return;}sy+=SUB_H+3;
                if(inSubR(mx,my,px,sy,rw)){if(button==0){pl.durex.client.module.NametagsModule.setShowPing(!pl.durex.client.module.NametagsModule.isShowPing());soundToggle(pl.durex.client.module.NametagsModule.isShowPing());pl.durex.client.DurexClient.saveNow();}return;}sy+=SUB_H+3;
                if(inSubR(mx,my,px,sy,rw)){if(button==0){pl.durex.client.module.NametagsModule.setShowItems(!pl.durex.client.module.NametagsModule.isShowItems());soundToggle(pl.durex.client.module.NametagsModule.isShowItems());pl.durex.client.DurexClient.saveNow();}return;}sy+=SUB_H+3;
                if(my>=sy&&my<=sy+SUB_H){if(button==0){draggingSlider="nametags_dist";sliderPx=px;applySlider("nametags_dist",mx,px,rw);}}
            }
            case "tracers" -> {
                if(inSubR(mx,my,px,sy,rw)){if(button==0){pl.durex.client.module.TracerModule.setColorIdx(pl.durex.client.module.TracerModule.getColorIdx()+1);soundClick();pl.durex.client.DurexClient.saveNow();}else if(button==1){pl.durex.client.module.TracerModule.setColorIdx(pl.durex.client.module.TracerModule.getColorIdx()-1);soundClick();pl.durex.client.DurexClient.saveNow();}return;}sy+=SUB_H+3;
                if(inSubR(mx,my,px,sy,rw)){if(button==0){pl.durex.client.module.TracerModule.setStyleIdx(pl.durex.client.module.TracerModule.getStyleIdx()+1);soundClick();pl.durex.client.DurexClient.saveNow();}else if(button==1){pl.durex.client.module.TracerModule.setStyleIdx(pl.durex.client.module.TracerModule.getStyleIdx()-1);soundClick();pl.durex.client.DurexClient.saveNow();}return;}sy+=SUB_H+3;
                if(my>=sy&&my<=sy+SUB_H){if(button==0){draggingSlider="tracers_dist";sliderPx=px;applySlider("tracers_dist",mx,px,rw);}return;}sy+=SUB_H+3;
                if(inSubR(mx,my,px,sy,rw)){if(button==0){soundClick();openTracerEditor=true;}return;}sy+=SUB_H+3;
                if(pl.durex.client.module.TracerModule.isCustomStyle()&&inSubR(mx,my,px,sy,rw)){if(button==0){pl.durex.client.module.TracerModule.removeCurrentCustomStyle();soundClick();pl.durex.client.DurexClient.saveNow();}}
            }
            case "autoDripstone" -> {
                if(inSubR(mx,my,px,sy,rw)){if(button==0){pl.durex.client.module.AutoDripstoneModule.setSpeed(pl.durex.client.module.AutoDripstoneModule.getSpeed()+1);soundClick();pl.durex.client.DurexClient.saveNow();}else if(button==1){pl.durex.client.module.AutoDripstoneModule.setSpeed(pl.durex.client.module.AutoDripstoneModule.getSpeed()-1);soundClick();pl.durex.client.DurexClient.saveNow();}}
            }
            case "autoShieldBreak" -> {
                if(my>=sy&&my<=sy+SUB_H){if(button==0){draggingSlider="shield_break_delay";sliderPx=px;applySlider("shield_break_delay",mx,px,rw);}}
            }
            case "msgBot" -> {
                if(inSubR(mx,my,px,sy,rw)){if(button==0){msgBotEditingField=1;soundClick();}return;}sy+=SUB_H+3;
                if(inSubR(mx,my,px,sy,rw)){if(button==0){msgBotEditingField=2;soundClick();}return;}sy+=SUB_H+3;
                if(inSubR(mx,my,px,sy,rw)){if(button==0){msgBotEditingField=3;soundClick();}return;}sy+=SUB_H+3;
                if(inSubR(mx,my,px,sy,rw)){if(button==0){saveMsgBotFields();if(pl.durex.client.module.MsgBotModule.isSpamming())pl.durex.client.module.MsgBotModule.stopSpam();else pl.durex.client.module.MsgBotModule.startSpam(500);soundToggle(pl.durex.client.module.MsgBotModule.isSpamming());pl.durex.client.DurexClient.saveNow();}return;}sy+=SUB_H+3;
                if(inSubR(mx,my,px,sy,rw)){if(button==0){pl.durex.client.module.MsgBotModule.resetSpammedPlayers();soundSnap();}return;}sy+=SUB_H+3;
                if(inSubR(mx,my,px,sy,rw)){if(button==0){if(pl.durex.client.module.MsgBotModule.isAfk())pl.durex.client.module.MsgBotModule.stopAfk();else pl.durex.client.module.MsgBotModule.startAfk();soundToggle(pl.durex.client.module.MsgBotModule.isAfk());}return;}sy+=SUB_H+3;
                if(inSubR(mx,my,px,sy,rw)){if(button==0){saveMsgBotFields();if(pl.durex.client.module.MsgBotModule.isAutoCh())pl.durex.client.module.MsgBotModule.stopAutoCh();else pl.durex.client.module.MsgBotModule.startAutoCh();soundToggle(pl.durex.client.module.MsgBotModule.isAutoCh());}return;}sy+=SUB_H+3;
                if(inSubR(mx,my,px,sy,rw)){if(button==0){pl.durex.client.module.MsgBotModule.onlyNetherite=!pl.durex.client.module.MsgBotModule.onlyNetherite;soundToggle(pl.durex.client.module.MsgBotModule.onlyNetherite);pl.durex.client.DurexClient.saveNow();}return;}sy+=SUB_H+3;
                if(inSubR(mx,my,px,sy,rw)){if(button==0){pl.durex.client.module.MsgBotModule.onlyOffline=!pl.durex.client.module.MsgBotModule.onlyOffline;soundToggle(pl.durex.client.module.MsgBotModule.onlyOffline);pl.durex.client.DurexClient.saveNow();}}
            }
        }
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (draggingHud) {
            if (settingProcenciarz) procenciarz.setHudPos((int)mx-hudDragOffX,(int)my-hudDragOffY);
            else if (settingZbrojmistrz) pl.durex.client.module.ZbrojmistrzModule.setHudPos((int)mx-hudDragOffX,(int)my-hudDragOffY);
            else cooldownHud.setHudPos((int)mx-hudDragOffX,(int)my-hudDragOffY);
            return true;
        }
        if (draggingSlider != null) {
            applySlider(draggingSlider, mx, sliderPx, rpW());
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        draggingHud = false;
        draggingSlider = null;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hAmount, double vAmount) {
        if (mx >= rpX()) {
            rpScrollY = Math.max(0, rpScrollY - (int)(vAmount * 12));
            return true;
        }
        return super.mouseScrolled(mx, my, hAmount, vAmount);
    }

    private void applySlider(String field, double mx, int px, int rw) {
        int labelW = 50; int barX = px + PAD + labelW; int barW = rw - PAD * 2 - labelW - 4;
        float t = (float)Math.max(0, Math.min(1, (mx - barX) / barW));
        switch (field) {
            case "rotX"          -> viewModel.setRotX(ViewModelModule.ROT_MIN + t*(ViewModelModule.ROT_MAX-ViewModelModule.ROT_MIN));
            case "rotY"          -> viewModel.setRotY(ViewModelModule.ROT_MIN + t*(ViewModelModule.ROT_MAX-ViewModelModule.ROT_MIN));
            case "rotZ"          -> viewModel.setRotZ(ViewModelModule.ROT_MIN + t*(ViewModelModule.ROT_MAX-ViewModelModule.ROT_MIN));
            case "posX"          -> viewModel.setPosX(ViewModelModule.POS_MIN + t*(ViewModelModule.POS_MAX-ViewModelModule.POS_MIN));
            case "posY"          -> viewModel.setPosY(ViewModelModule.POS_MIN + t*(ViewModelModule.POS_MAX-ViewModelModule.POS_MIN));
            case "posZ"          -> viewModel.setPosZ(ViewModelModule.POS_MIN + t*(ViewModelModule.POS_MAX-ViewModelModule.POS_MIN));
            case "scale"         -> viewModel.setScale(ViewModelModule.SCALE_MIN + t*(ViewModelModule.SCALE_MAX-ViewModelModule.SCALE_MIN));
            case "nametags_dist" -> pl.durex.client.module.NametagsModule.setMaxDistance(8f + t*(256f-8f));
            case "tracers_dist"  -> pl.durex.client.module.TracerModule.setMaxDistance(8f + t*(512f-8f));
            case "shield_break_delay" -> pl.durex.client.module.AutoShieldBreakModule.setDelayTicks((int)(1 + t*19));
        }
    }

    // ── Keyboard ──────────────────────────────────────────────────────────

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Search bar
        if (searchFocused) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                searchFocused = false;
                searchQuery = "";
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !searchQuery.isEmpty()) {
                searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                return true;
            }
            return true;
        }
        
        if (editingSlotName >= 0) {
            if (keyCode==GLFW.GLFW_KEY_ENTER||keyCode==GLFW.GLFW_KEY_KP_ENTER) {
                if (editingSlotName < antiKostka.getSlots().size())
                    antiKostka.getSlots().get(editingSlotName).name = nameBuffer.toString().isEmpty() ? "Hotbar "+(editingSlotName+1) : nameBuffer.toString();
                editingSlotName=-1; return true;
            }
            if (keyCode==GLFW.GLFW_KEY_ESCAPE){editingSlotName=-1;return true;}
            if (keyCode==GLFW.GLFW_KEY_BACKSPACE&&!nameBuffer.isEmpty()){nameBuffer.deleteCharAt(nameBuffer.length()-1);return true;}
            return true;
        }
        if (msgBotEditingField > 0) {
            if (keyCode==GLFW.GLFW_KEY_ENTER||keyCode==GLFW.GLFW_KEY_KP_ENTER||keyCode==GLFW.GLFW_KEY_ESCAPE){saveMsgBotFields();msgBotEditingField=0;return true;}
            if (keyCode==GLFW.GLFW_KEY_BACKSPACE) {
                switch(msgBotEditingField){
                    case 1->{if(!msgBotMsgInput.isEmpty())msgBotMsgInput=msgBotMsgInput.substring(0,msgBotMsgInput.length()-1);}
                    case 2->{if(!msgBotNickInput.isEmpty())msgBotNickInput=msgBotNickInput.substring(0,msgBotNickInput.length()-1);}
                    case 3->{if(!msgBotChDelay.isEmpty())msgBotChDelay=msgBotChDelay.substring(0,msgBotChDelay.length()-1);}
                }
                return true;
            }
            return true;
        }
        if (waitingAntiKowalBind) {
            waitingAntiKowalBind=false;
            antiKowal.setBind(keyCode==GLFW.GLFW_KEY_ESCAPE ? InputUtil.UNKNOWN_KEY : InputUtil.fromKeyCode(keyCode,scanCode));
            pl.durex.client.DurexClient.saveNow(); return true;
        }
        if (waitingFriendBind) {
            waitingFriendBind=false;
            friendModule.setAddKey(keyCode==GLFW.GLFW_KEY_ESCAPE ? InputUtil.UNKNOWN_KEY : InputUtil.fromKeyCode(keyCode,scanCode));
            pl.durex.client.DurexClient.saveNow(); return true;
        }
        if (waitingHotbarBind >= 0) {
            int idx=waitingHotbarBind; waitingHotbarBind=-1;
            if (keyCode!=GLFW.GLFW_KEY_ESCAPE&&idx<antiKostka.getSlots().size())
                antiKostka.getSlots().get(idx).loadKey=InputUtil.fromKeyCode(keyCode,scanCode);
            return true;
        }
        if (keyCode==GLFW.GLFW_KEY_ESCAPE) {
            if (settingPosition||settingProcenciarz||settingZbrojmistrz){settingPosition=false;settingProcenciarz=false;settingZbrojmistrz=false;return true;}
            close(); return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        // Search bar
        if (searchFocused && chr >= 32 && searchQuery.length() < 32) {
            searchQuery += chr;
            return true;
        }
        
        if (editingSlotName >= 0 && chr >= 32 && nameBuffer.length() < 24) { nameBuffer.append(chr); return true; }
        if (msgBotEditingField > 0 && chr >= 32) {
            switch(msgBotEditingField){
                case 1->{if(msgBotMsgInput.length()<80)msgBotMsgInput+=chr;}
                case 2->{if(msgBotNickInput.length()<24)msgBotNickInput+=chr;}
                case 3->{if(msgBotChDelay.length()<4&&Character.isDigit(chr))msgBotChDelay+=chr;}
            }
            return true;
        }
        return false;
    }

    // ── Misc ──────────────────────────────────────────────────────────────

    private void saveMsgBotFields() {
        pl.durex.client.module.MsgBotModule.message      = msgBotMsgInput;
        pl.durex.client.module.MsgBotModule.targetPlayer = msgBotNickInput;
        try { pl.durex.client.module.MsgBotModule.chDelaySec = Math.max(5, Integer.parseInt(msgBotChDelay)); }
        catch (NumberFormatException ignored) {}
    }

    private pl.durex.client.module.LeverCobwebModule.SpeedProfile nextSpeedProfile(pl.durex.client.module.LeverCobwebModule.SpeedProfile cur) {
        pl.durex.client.module.LeverCobwebModule.SpeedProfile[] p = pl.durex.client.module.LeverCobwebModule.SpeedProfile.values();
        for (int i=0;i<p.length;i++) if(p[i]==cur) return p[(i+1)%p.length];
        return pl.durex.client.module.LeverCobwebModule.SpeedProfile.FAST;
    }

    // ── Compat stubs (used by DurexConfig) ───────────────────────────────
    public static class ModWidget {
        public final String id; public String icon;
        public float rx=0,ry=0,tx=0,ty=0;
        public boolean expanded=false; public float expandAnim=0f;
        public CategoryWidget owner=null;
        public ModWidget(String id,String icon){this.id=id;this.icon=icon;}
    }
    public static class CategoryWidget {
        public String name;
        public final java.util.List<ModWidget> mods=new java.util.ArrayList<>();
        public float rx=0,ry=0,tx=0,ty=0;
        public boolean docked=true,expanded=true; public float expandAnim=1f;
        public java.lang.StringBuilder nameBuffer=new java.lang.StringBuilder();
        public CategoryWidget(String name){this.name=name;}
        public int totalH(){return 0;}
    }
    public static int getSubHStatic(ModWidget m){return 0;}
}
