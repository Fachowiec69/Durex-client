package pl.durex.client.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import pl.durex.client.DurexClient;
import pl.durex.client.gui.render.GuiRenderUtils;
import pl.durex.client.license.LicenseManager;
import pl.durex.client.module.AntiKostkaModule;
import pl.durex.client.module.AntiKowalModule;
import pl.durex.client.module.CooldownHudModule;
import pl.durex.client.module.FriendModule;
import pl.durex.client.module.ViewModelModule;

import java.util.ArrayList;
import java.util.List;

public final class DurexClickGuiScreen extends Screen {

    // ── Modules ───────────────────────────────────────────────────────────
    private final AntiKowalModule antiKowal = DurexClient.getAntiKowalModule();
    private final FriendModule friendModule = DurexClient.getFriendModule();
    private final CooldownHudModule cooldownHud = DurexClient.getCooldownHudModule();
    private final AntiKostkaModule antiKostka = DurexClient.getAntiKostkaModule();
    private final ViewModelModule viewModel = DurexClient.getViewModelModule();
    private final pl.durex.client.module.ProcenciarzModule procenciarz = DurexClient.getProcenciarzModule();
    private final pl.durex.client.module.LeverCobwebModule leverCobweb = DurexClient.getLeverCobwebModule();
    private final pl.durex.client.module.AutoDripstoneModule autoDripstone = DurexClient.getAutoDripstoneModule();
    private final pl.durex.client.module.NoPushModule noPush = DurexClient.getNoPushModule();
    // MsgBot — pola tekstowe dla sub-contentu
    private String msgBotMsgInput   = pl.durex.client.module.MsgBotModule.message;
    private String msgBotNickInput  = pl.durex.client.module.MsgBotModule.targetPlayer;
    private String msgBotChDelay    = String.valueOf(pl.durex.client.module.MsgBotModule.chDelaySec);
    // 0=none, 1=msg, 2=nick, 3=chDelay
    private int msgBotEditingField  = 0;
    // ── Constants ─────────────────────────────────────────────────────────
    private static final int W        = 220;
    private static final int CAT_H    = 28;
    private static final int MOD_H    = 22;
    private static final int SUB_H    = 20;
    private static final int HOTBAR_H = 28;
    private static final int ICON_SIZE = 16;
    private static final int PAD      = 8;
    private static final int FOOTER_H = 18;
    private static final int SNAP_DIST = 90;
    private static final int ANIM_LERP_SPEED = 5; // ms per frame approx

    private static final int COL_BG      = 0xEE080015;
    private static final int COL_HEADER  = 0xFF120028;
    private static final int COL_BORDER  = 0xFF8800EE;
    private static final int COL_CAT_BG  = 0xCC1A0035;
    private static final int COL_MOD_BG  = 0x881A0040;
    private static final int COL_HOVER   = 0xAA2A0060;
    private static final int COL_SUB     = 0x661A0035;
    private static final int COL_ACCENT  = 0xFFCC77FF;
    private static final int COL_TEXT    = 0xFFEEDDFF;
    private static final int COL_MUTED   = 0xFF9966BB;
    private static final int COL_ON      = 0xFF44FF88;
    private static final int COL_OFF     = 0xFFFF4455;
    private static final int COL_BIND    = 0xFFFFCC44;
    private static final int COL_BLUE    = 0xFF4499FF;

    // ── Category & Module widgets ─────────────────────────────────────────

    public static class ModWidget {
        public final String id;
    public     String icon;
        // position when floating
    public     float rx, ry, tx, ty;
    public     float scale = 1f, targetScale = 1f;
    public     boolean expanded = false;
    public     float expandAnim = 0f;
    public     boolean dragging = false;
    public     float dragOffX, dragOffY;
    public     long pressTime = -1;
    public     boolean dragStarted = false;
        // which category owns this mod (null = floating free)
    public     CategoryWidget owner = null;

        public ModWidget(String id, String icon) { this.id = id; this.icon = icon; }

        void tick() {
            // Podczas dragu - pozycja natychmiastowa (bez lerp)
            if (dragging && dragStarted) {
                rx = tx; ry = ty;
            } else {
                rx += (tx - rx) * 0.35f; if (Math.abs(rx - tx) < 0.5f) rx = tx;
                ry += (ty - ry) * 0.35f; if (Math.abs(ry - ty) < 0.5f) ry = ty;
            }
            scale += (targetScale - scale) * 0.25f; if (Math.abs(scale - targetScale) < 0.005f) scale = targetScale;
            float et = expanded ? 1f : 0f;
            expandAnim += (et - expandAnim) * 0.35f; if (Math.abs(expandAnim - et) < 0.005f) expandAnim = et;
        }
    }

    public static class CategoryWidget {
    public     String name;
        public final List<ModWidget> mods = new ArrayList<>();
    public     float rx, ry, tx, ty;
    public     float scale = 1f, targetScale = 1f;
    public     boolean expanded = true;
    public     float expandAnim = 1f;
    public     boolean dragging = false;
    public     float dragOffX, dragOffY;
    public     long pressTime = -1;
    public     boolean dragStarted = false;
    public     boolean docked = true;
    public     boolean editingName = false;
    public     StringBuilder nameBuffer = new StringBuilder();

        public CategoryWidget(String name) { this.name = name; }

        void tick() {
            // Podczas dragu - pozycja natychmiastowa
            if (dragging && dragStarted) {
                rx = tx; ry = ty;
            } else {
                rx += (tx - rx) * 0.35f; if (Math.abs(rx - tx) < 0.5f) rx = tx;
                ry += (ty - ry) * 0.35f; if (Math.abs(ry - ty) < 0.5f) ry = ty;
            }
            scale += (targetScale - scale) * 0.25f; if (Math.abs(scale - targetScale) < 0.005f) scale = targetScale;
            float et = expanded ? 1f : 0f;
            expandAnim += (et - expandAnim) * 0.18f; if (Math.abs(expandAnim - et) < 0.005f) expandAnim = et;
            for (ModWidget m : mods) m.tick();
        }

        public int contentH() {
            if (expandAnim < 0.01f) return 0;
            int h = 0;
            for (ModWidget m : mods) {
                h += MOD_H + 2;
                if (m.expandAnim > 0.01f) h += getSubHStatic(m);
            }
            return (int)(expandAnim * (h + PAD));
        }

        public int totalH() { return CAT_H + contentH(); }
    }

    // ── State ─────────────────────────────────────────────────────────────
    // Statyczne - przetrwają między otwarciem GUI
    public static final List<CategoryWidget> categories = new ArrayList<>();
    public static final List<ModWidget> freeMods = new ArrayList<>();
    public static int savedPanelX = -1, savedPanelY = -1;

    // Main panel
    private int panelX, panelY;
    private boolean panelDrag;
    private int panelDragOffX, panelDragOffY;

    // Drag state
    private CategoryWidget activeCatDrag = null;
    private ModWidget activeModDrag = null;

    // Bind/edit state
    private boolean waitingAntiKowalBind = false;
    private boolean waitingFriendBind = false;
    private int waitingHotbarBind = -1;
    private int editingSlotName = -1;
    private final StringBuilder nameBuffer = new StringBuilder();
    private boolean settingPosition = false;
    private boolean settingProcenciarz = false;
    private boolean settingZbrojmistrz = false;
    private boolean delayExpanded = false;
    private boolean draggingHud = false;
    private int hudDragOffX, hudDragOffY;

    // Drop target highlight
    private CategoryWidget dropTargetCat = null;
    // Nowa kategoria - wpisywanie nazwy
    private boolean namingNewCat = false;
    private final StringBuilder newCatNameBuffer = new StringBuilder();
    // Intro animation
    private static boolean firstOpen = true;
    private float introAnim = 0f;
    // Panel scroll
    private int panelScrollY = 0;
    // Tutorial (pierwszy raz)
    private boolean showTutorial = false;
    private long tutorialStartMs = 0;
    private static final long TUTORIAL_DURATION_MS = 10_000;

    public DurexClickGuiScreen() {
        super(Text.literal("Durex Client"));
    }

    // ── Init ──────────────────────────────────────────────────────────────

    @Override
    protected void init() {
        panelX = savedPanelX >= 0 ? savedPanelX : 20;
        panelY = savedPanelY >= 0 ? savedPanelY : (height - 300) / 2;

        if (categories.isEmpty() && freeMods.isEmpty()) {
            // Pierwszy raz — pusta lista, pokaż tutorial
            showTutorial = true;
            tutorialStartMs = System.currentTimeMillis();
        }

        // Wymuś pełną animację od razu
        introAnim = 1f;
        firstOpen = false;

        updateDockedPositions(true);
    }

    private int calcPanelH() {
        int h = CAT_H + PAD * 2 + FOOTER_H; // header + padding + footer
        for (CategoryWidget cat : categories) {
            if (cat.docked) h += cat.totalH() + 4;
        }
        h += CAT_H + 4; // przycisk + kategoria
        return Math.max(h, 100);
    }

    private void updateDockedPositions(boolean instant) {
        int y = panelY + CAT_H + PAD - panelScrollY;
        for (CategoryWidget cat : categories) {
            if (cat.docked) {
                cat.tx = panelX; cat.ty = y;
                if (instant) { cat.rx = cat.tx; cat.ry = cat.ty; }
                y += cat.totalH() + 4;
            }
            // Zawsze aktualizuj pozycje modów (docked i floating)
            updateCatModPositions(cat, instant);
        }
    }

    // ── Sound ─────────────────────────────────────────────────────────────
    private void sound(String name, float pitch) {
        if (client == null) return;
        var se = net.minecraft.registry.Registries.SOUND_EVENT.get(net.minecraft.util.Identifier.of("minecraft", name));
        if (se != null) client.getSoundManager().play(PositionedSoundInstance.master(se, 1f, pitch));
    }
    private void soundPop()   { sound("block.copper_bulb.turn_on",  1.2f); }
    private void soundSnap()  { sound("block.copper_bulb.turn_off", 0.9f); }
    private void soundToggle(boolean on) { sound(on ? "block.copper.hit" : "block.copper_bulb.turn_off", on ? 1.5f : 0.7f); }
    private void soundClick() { sound("block.copper.hit", 1.0f); }

    // ── Mouse ─────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        // Biblioteka modułów — obsłuż kliknięcia najpierw (overlay na wierzchu)
        if (libOpen && handleLibraryClick(mx, my, button)) return true;

        // Okno nazwy nowej kategorii
        if (namingNewCat) {
            // Kliknięcie poza oknem = anuluj
            int ox = width / 2 - 100, oy = height / 2 - 30;
            if (!(mx >= ox && mx <= ox + 200 && my >= oy && my <= oy + 60)) {
                namingNewCat = false; newCatNameBuffer.setLength(0);
            }
            return true;
        }

        // HUD drag
        if (settingPosition) {
            int hx = cooldownHud.getHudX(), hy = cooldownHud.getHudY();
            if (button == 0 && mx >= hx - 2 && mx <= hx + 60 && my >= hy - 2 && my <= hy + 80) {
                draggingHud = true; hudDragOffX = (int)mx - hx; hudDragOffY = (int)my - hy; return true;
            }
        }
        if (settingProcenciarz) {
            int hx = procenciarz.getHudX(), hy = procenciarz.getHudY();
            if (button == 0 && mx >= hx - 2 && mx <= hx + 150 && my >= hy - 2 && my <= hy + 80) {
                draggingHud = true; hudDragOffX = (int)mx - hx; hudDragOffY = (int)my - hy; return true;
            }
        }
        if (settingZbrojmistrz) {
            int hx = pl.durex.client.module.ZbrojmistrzModule.getHudX();
            int hy = pl.durex.client.module.ZbrojmistrzModule.getHudY();
            if (button == 0 && mx >= hx - 2 && mx <= hx + 170 && my >= hy - 2 && my <= hy + 120) {
                draggingHud = true; hudDragOffX = (int)mx - hx; hudDragOffY = (int)my - hy; return true;
            }
        }

        if (editingSlotName >= 0 && button == 0) { editingSlotName = -1; return true; }

        // Panel header drag + reset button
        if (button == 0 && mx >= panelX && mx <= panelX + W && my >= panelY && my <= panelY + CAT_H) {
            // Reset button
            if (mx >= panelX + W - 18 && mx <= panelX + W - 6) {
                pl.durex.client.config.DurexConfig.resetLayout();
                soundSnap();
                return true;
            }
            panelDrag = true; panelDragOffX = (int)mx - panelX; panelDragOffY = (int)my - panelY; return true;
        }

        // Floating mods (free) - najpierw sub-content, potem header
        for (ModWidget m : new java.util.ArrayList<>(freeMods)) {
            if (m.expanded && m.expandAnim > 0.01f) {
                if (handleModSubClick(m, mx, my, (int)m.rx, (int)m.ry + MOD_H + 2, button)) return true;
            }
            if (hitMod(m, mx, my)) {
                if (button == 0) startModDrag(m, mx, my);
                else if (button == 1) { m.expanded = !m.expanded; soundPop(); }
                return true;
            }
        }

        // Kategorie - najpierw moduły w środku, potem header
        for (CategoryWidget cat : new java.util.ArrayList<>(categories)) {
            // X button
            int xBtnX = (int)cat.rx + W - 18;
            int xBtnY = (int)cat.ry + (CAT_H - 12) / 2;
            if (button == 0 && mx >= xBtnX && mx <= xBtnX + 12 && my >= xBtnY && my <= xBtnY + 12) {
                for (ModWidget m : new java.util.ArrayList<>(cat.mods)) { m.owner = null; m.rx = cat.rx + 10; m.ry = cat.ry; freeMods.add(m); }
                categories.remove(cat);
                if (cat.docked) updateDockedPositions(false);
                soundSnap(); return true;
            }
            // Moduły w kategorii - PRZED headerem
            if (cat.expanded && cat.expandAnim > 0.01f) {
                for (ModWidget m : new java.util.ArrayList<>(cat.mods)) {
                    if (m.expanded && m.expandAnim > 0.01f) {
                        if (handleModSubClick(m, mx, my, (int)m.rx, (int)m.ry + MOD_H + 2, button)) return true;
                    }
                    if (hitMod(m, mx, my)) {
                        if (button == 0) startModDrag(m, mx, my);
                        else if (button == 1) { m.expanded = !m.expanded; soundPop(); }
                        return true;
                    }
                }
            }
            // Header kategorii
            if (hitCatHeader(cat, mx, my)) {
                if (button == 0) startCatDrag(cat, mx, my);  // LPM = drag
                else if (button == 1) {                        // PPM = toggle expand
                    cat.expanded = !cat.expanded;
                    soundPop();
                }
                return true;
            }
        }

        // Przyciski na dole: [+ Kategoria] i [⊞ Moduły]
        int addBtnY = panelY + calcPanelH() - FOOTER_H - CAT_H - 4;
        int halfBtnW = (W - PAD * 2 - 4) / 2;
        // + Kategoria
        if (button == 0 && mx >= panelX + PAD && mx <= panelX + PAD + halfBtnW && my >= addBtnY && my <= addBtnY + CAT_H) {
            namingNewCat = true;
            newCatNameBuffer.setLength(0);
            soundPop(); return true;
        }
        // ⊞ Moduły
        int libBtnX2 = panelX + PAD + halfBtnW + 4;
        if (button == 0 && mx >= libBtnX2 && mx <= libBtnX2 + halfBtnW && my >= addBtnY && my <= addBtnY + CAT_H) {
            libOpen = !libOpen;
            libSearchFocused = false;
            showTutorial = false; // zamknij tutorial gdy kliknie
            soundPop(); return true;
        }

        return super.mouseClicked(mx, my, button);
    }

    private void startCatDrag(CategoryWidget cat, double mx, double my) {
        cat.pressTime = System.currentTimeMillis();
        cat.dragStarted = false;
        cat.dragging = true;
        cat.dragOffX = (float)(mx - cat.rx);
        cat.dragOffY = (float)(my - cat.ry);
        activeCatDrag = cat;
    }

    private void startModDrag(ModWidget m, double mx, double my) {
        m.pressTime = System.currentTimeMillis();
        m.dragStarted = false;
        m.dragging = true;
        m.dragOffX = (float)(mx - m.rx);
        m.dragOffY = (float)(my - m.ry);
        activeModDrag = m;
    }

    private boolean hitCat(CategoryWidget cat, double mx, double my) {
        return mx >= cat.rx && mx <= cat.rx + W && my >= cat.ry && my <= cat.ry + cat.totalH();
    }

    private boolean hitCatHeader(CategoryWidget cat, double mx, double my) {
        return mx >= cat.rx && mx <= cat.rx + W && my >= cat.ry && my <= cat.ry + CAT_H;
    }

    private boolean hitMod(ModWidget m, double mx, double my) {
        // Tylko header modułu - nie sub-content (żeby kliknięcia w sub-content działały)
        return mx >= m.rx && mx <= m.rx + W - 10 && my >= m.ry && my <= m.ry + MOD_H;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (button == 0) {
            panelDrag = false;
            draggingHud = false;
            draggingSlider = null;
            libDragging = false;

            if (activeCatDrag != null) {
                CategoryWidget cat = activeCatDrag;
                activeCatDrag = null;
                cat.dragging = false;
                if (!cat.dragStarted) {
                    // Krótki LPM bez ruchu = toggle (fallback)
                    cat.expanded = !cat.expanded;
                    soundPop();
                } else {
                    float dist = distToPanel(cat.rx, cat.ry);
                    if (dist < SNAP_DIST) {
                        cat.docked = true;
                        soundSnap();
                        updateDockedPositions(false);
                    }
                }
                cat.targetScale = 1f;
                cat.pressTime = -1;
                cat.dragStarted = false;
            }

            if (activeModDrag != null) {
                ModWidget m = activeModDrag;
                activeModDrag = null;
                m.dragging = false;
                dropTargetCat = null;

                if (!m.dragStarted) {
                    toggleMod(m);
                    soundToggle(isModEnabled(m));
                } else {
                    CategoryWidget target = findCatAt(mx, my);
                    if (target != null && target != m.owner) {
                        if (m.owner != null) m.owner.mods.remove(m);
                        else freeMods.remove(m);
                        target.mods.add(m);
                        m.owner = target;
                        soundSnap();
                        // Natychmiast przelicz i ustaw pozycje
                        updateDockedPositions(false);
                        updateCatModPositions(target, true); // instant dla tej kategorii
                    } else if (target == null) {
                        if (m.owner != null) { m.owner.mods.remove(m); m.owner = null; updateDockedPositions(false); }
                        if (!freeMods.contains(m)) freeMods.add(m);
                        // Zostaw mod na aktualnej pozycji kursora
                        m.rx = (float)mx - m.dragOffX;
                        m.ry = (float)my - m.dragOffY;
                        m.tx = m.rx; m.ty = m.ry;
                    }
                }
                m.targetScale = 1f;
                m.pressTime = -1;
                m.dragStarted = false;
            }
        }

        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hAmount, double vAmount) {
        // Scroll w panelu
        if (mx >= panelX && mx <= panelX + W && my >= panelY && my <= panelY + calcPanelH()) {
            int maxScroll = Math.max(0, calcTotalContentH() - (height - 100));
            panelScrollY = (int)Math.max(0, Math.min(maxScroll, panelScrollY - vAmount * 15));
            updateDockedPositions(false);
            return true;
        }
        return super.mouseScrolled(mx, my, hAmount, vAmount);
    }

    private int calcTotalContentH() {
        int h = 0;
        for (CategoryWidget cat : categories) if (cat.docked) h += cat.totalH() + 4;
        return h + CAT_H + 4;
    }

    private CategoryWidget findCatAt(double mx, double my) {
        for (CategoryWidget cat : categories) {
            // Zawsze można upuścić na header kategorii
            if (mx >= cat.rx && mx <= cat.rx + W && my >= cat.ry && my <= cat.ry + Math.max(CAT_H, cat.totalH())) return cat;
        }
        return null;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (libDragging) {
            List<ModDef> mods = libShowList() ? getFilteredMods() : List.of();
            int lh = libH(mods);
            libPosX = Math.max(0, Math.min(width - LIB_W, (int)mx - libDragOffX));
            libPosY = Math.max(0, Math.min(height - lh, (int)my - libDragOffY));
            return true;
        }
        if (draggingHud) {
            if (settingProcenciarz) procenciarz.setHudPos((int)mx - hudDragOffX, (int)my - hudDragOffY);
            else if (settingZbrojmistrz) pl.durex.client.module.ZbrojmistrzModule.setHudPos((int)mx - hudDragOffX, (int)my - hudDragOffY);
            else cooldownHud.setHudPos((int)mx - hudDragOffX, (int)my - hudDragOffY);
            return true;
        }
        if (draggingSlider != null) {
            applySlider(draggingSlider, mx, sliderPx);
            return true;
        }
        if (panelDrag) {
            int ph = calcPanelH();
            panelX = Math.max(0, Math.min(width - W, (int)mx - panelDragOffX));
            panelY = Math.max(0, Math.min(height - ph, (int)my - panelDragOffY));
            updateDockedPositions(false);
            return true;
        }
        if (activeCatDrag != null) {
            CategoryWidget cat = activeCatDrag;
            double moved = Math.sqrt(dx*dx + dy*dy);
            if (!cat.dragStarted && moved > 2) {
                cat.dragStarted = true;
                if (cat.docked) {
                    cat.docked = false;
                    cat.tx = cat.rx;
                    cat.ty = cat.ry;
                    cat.dragOffX = (float)(mx - cat.rx);
                    cat.dragOffY = (float)(my - cat.ry);
                    cat.targetScale = 1.04f;
                    soundPop();
                    updateDockedPositions(false);
                }
            }
            if (cat.dragStarted && !cat.docked) {
                float nx = (float)(mx - cat.dragOffX);
                float ny = (float)(my - cat.dragOffY);
                cat.tx = Math.max(0, Math.min(width - W, nx));
                cat.ty = Math.max(0, Math.min(height - cat.totalH(), ny));
                cat.rx = cat.tx; cat.ry = cat.ty;
            }
            return true;
        }
        if (activeModDrag != null) {
            ModWidget m = activeModDrag;
            double moved = Math.sqrt(dx*dx + dy*dy);
            if (!m.dragStarted && moved > 2) {
                m.dragStarted = true;
                m.targetScale = 1.06f;
                soundPop();
            }
            if (m.dragStarted) {
                float nx = (float)(mx - m.dragOffX);
                float ny = (float)(my - m.dragOffY);
                int mh = MOD_H + (m.expandAnim > 0.01f ? getSubHStatic(m) : 0);
                m.tx = Math.max(0, Math.min(width - (W - 10), nx));
                m.ty = Math.max(0, Math.min(height - mh, ny));
                m.rx = m.tx; m.ry = m.ty;
                dropTargetCat = findCatAt(mx, my);
            }
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    private void updateCatModPositions(CategoryWidget cat, boolean instant) {
        int my = (int)cat.ty + CAT_H + 2;
        for (ModWidget m : cat.mods) {
            m.tx = cat.tx + 10; m.ty = my;
            if (instant) { m.rx = m.tx; m.ry = m.ty; }
            my += MOD_H + 2 + getSubHStatic(m);
        }
    }

    private float distToPanel(float fx, float fy) {
        float cx = fx + W / 2f, cy = fy + CAT_H / 2f;
        float pcx = panelX + W / 2f, pcy = panelY + calcPanelH() / 2f;
        float ddx = cx - pcx, ddy = cy - pcy;
        return (float)Math.sqrt(ddx * ddx + ddy * ddy);
    }

    // ── Key input ─────────────────────────────────────────────────────────

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Wyszukiwarka biblioteki
        if (libSearchFocused) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) { libSearchFocused = false; return true; }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !libSearch.isEmpty()) {
                libSearch = libSearch.substring(0, libSearch.length() - 1); return true;
            }
            return true;
        }
        // Okno nazwy nowej kategorii
        if (namingNewCat) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                String name = newCatNameBuffer.toString().trim().isEmpty() ? "Kategoria " + (categories.size() + 1) : newCatNameBuffer.toString();
                CategoryWidget newCat = new CategoryWidget(name);
                newCat.docked = true;
                categories.add(newCat);
                updateDockedPositions(false);
                namingNewCat = false; newCatNameBuffer.setLength(0);
                soundPop(); return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) { namingNewCat = false; newCatNameBuffer.setLength(0); return true; }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !newCatNameBuffer.isEmpty()) { newCatNameBuffer.deleteCharAt(newCatNameBuffer.length() - 1); return true; }
            return true;
        }
        // Category name editing
        for (CategoryWidget cat : categories) {
            if (cat.editingName) {
                if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                    cat.name = cat.nameBuffer.isEmpty() ? "Kategoria" : cat.nameBuffer.toString();
                    cat.editingName = false; return true;
                }
                if (keyCode == GLFW.GLFW_KEY_ESCAPE) { cat.editingName = false; return true; }
                if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !cat.nameBuffer.isEmpty()) { cat.nameBuffer.deleteCharAt(cat.nameBuffer.length() - 1); return true; }
                return true;
            }
        }
        if (editingSlotName >= 0) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                if (editingSlotName < antiKostka.getSlots().size())
                    antiKostka.getSlots().get(editingSlotName).name = nameBuffer.toString().isEmpty() ? "Hotbar " + (editingSlotName + 1) : nameBuffer.toString();
                editingSlotName = -1; return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) { editingSlotName = -1; return true; }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !nameBuffer.isEmpty()) { nameBuffer.deleteCharAt(nameBuffer.length() - 1); return true; }
            return true;
        }
        // MsgBot field editing
        if (msgBotEditingField > 0) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER || keyCode == GLFW.GLFW_KEY_ESCAPE) {
                saveMsgBotFields(); msgBotEditingField = 0; return true;
            }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                switch (msgBotEditingField) {
                    case 1 -> { if (!msgBotMsgInput.isEmpty())  msgBotMsgInput  = msgBotMsgInput.substring(0, msgBotMsgInput.length()-1); }
                    case 2 -> { if (!msgBotNickInput.isEmpty()) msgBotNickInput = msgBotNickInput.substring(0, msgBotNickInput.length()-1); }
                    case 3 -> { if (!msgBotChDelay.isEmpty())   msgBotChDelay   = msgBotChDelay.substring(0, msgBotChDelay.length()-1); }
                }
                return true;
            }
            return true;
        }
        if (waitingAntiKowalBind) { waitingAntiKowalBind = false; antiKowal.setBind(keyCode == GLFW.GLFW_KEY_ESCAPE ? InputUtil.UNKNOWN_KEY : InputUtil.fromKeyCode(keyCode, scanCode)); pl.durex.client.DurexClient.saveNow(); return true; }        if (waitingFriendBind) { waitingFriendBind = false; friendModule.setAddKey(keyCode == GLFW.GLFW_KEY_ESCAPE ? InputUtil.UNKNOWN_KEY : InputUtil.fromKeyCode(keyCode, scanCode)); pl.durex.client.DurexClient.saveNow(); return true; }
        if (waitingHotbarBind >= 0) {
            int idx = waitingHotbarBind; waitingHotbarBind = -1;
            if (keyCode != GLFW.GLFW_KEY_ESCAPE && idx < antiKostka.getSlots().size())
                antiKostka.getSlots().get(idx).loadKey = InputUtil.fromKeyCode(keyCode, scanCode);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) { if (settingPosition || settingProcenciarz || settingZbrojmistrz) { settingPosition = false; settingProcenciarz = false; settingZbrojmistrz = false; return true; } close(); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        // Wyszukiwarka biblioteki
        if (libSearchFocused && chr >= 32 && libSearch.length() < 30) { libSearch += chr; return true; }
        if (namingNewCat && chr >= 32 && newCatNameBuffer.length() < 24) { newCatNameBuffer.append(chr); return true; }
        for (CategoryWidget cat : categories) {
            if (cat.editingName && chr >= 32 && cat.nameBuffer.length() < 20) { cat.nameBuffer.append(chr); return true; }
        }
        if (editingSlotName >= 0 && nameBuffer.length() < 20 && chr >= 32) { nameBuffer.append(chr); return true; }
        // MsgBot fields
        if (msgBotEditingField > 0 && chr >= 32) {
            switch (msgBotEditingField) {
                case 1 -> { if (msgBotMsgInput.length()  < 200) msgBotMsgInput  += chr; }
                case 2 -> { if (msgBotNickInput.length() < 40)  msgBotNickInput += chr; }
                case 3 -> { if (Character.isDigit(chr) && msgBotChDelay.length() < 4) msgBotChDelay += chr; }
            }
            return true;
        }
        return false;
    }

    // ── Module logic ──────────────────────────────────────────────────────

    private void toggleMod(ModWidget m) {
        switch (m.id) {
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
            case "zbrojmistrz"   -> pl.durex.client.module.ZbrojmistrzModule.setEnabled(
                    !pl.durex.client.module.ZbrojmistrzModule.isEnabled());
            case "nametags"      -> pl.durex.client.module.NametagsModule.setEnabled(
                    !pl.durex.client.module.NametagsModule.isEnabled());
            case "tracers"       -> pl.durex.client.module.TracerModule.setEnabled(
                    !pl.durex.client.module.TracerModule.isEnabled());
        }
        pl.durex.client.DurexClient.saveNow();
    }

    private boolean isModEnabled(ModWidget m) {
        return switch (m.id) {
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
            default -> false;
        };
    }

    private String getModLabel(ModWidget m) {
        return switch (m.id) {
            case "antiKowal"     -> "AntiKowal";
            case "cooldowns"     -> "Cooldowns";
            case "antiKostka"    -> "AntiKostka";
            case "viewModel"     -> "ViewModel";
            case "procenciarz"   -> "Procenciarz";
            case "leverCobweb"   -> "Nemos Helper";
            case "autoDripstone" -> "Auto Dripstone";
            case "noPush"        -> "No Push";
            case "msgBot"        -> "Msg Bot";
            case "zbrojmistrz"   -> "Zbrojmistrz";
            case "nametags"      -> "Nametags";
            case "tracers"       -> "Tracers";
            default -> m.id;
        };
    }

    // Static version for use in inner class contentH()
    static int getSubHStatic(ModWidget m) {
        float a = m.expandAnim;
        return switch (m.id) {
            case "antiKowal"     -> (int)(a * (4 * (SUB_H + 3) + PAD));
            case "cooldowns"     -> (int)(a * (2 * (SUB_H + 3) + PAD));
            case "viewModel"     -> (int)(a * (8 * (SUB_H + 3) + SUB_H + PAD));
            case "procenciarz"   -> (int)(a * (2 * (SUB_H + 3) + PAD));
            case "leverCobweb"   -> (int)(a * (5 * (SUB_H + 3) + PAD));
            case "autoDripstone" -> (int)(a * (SUB_H + 3 + PAD));
            case "noPush"        -> 0;
            case "zbrojmistrz"   -> (int)(a * (2 * (SUB_H + 3) + PAD));
            case "msgBot"        -> (int)(a * (9 * (SUB_H + 3) + PAD));
            case "nametags"      -> (int)(a * (5 * (SUB_H + 3) + PAD));
            case "tracers"       -> (int)(a * (2 * (SUB_H + 3) + PAD));
            case "antiKostka"    -> (int)(a * (
                (SUB_H + 3) + // delay row
                DurexClient.getAntiKostkaModule().getSlots().size() * (SUB_H + 3 + HOTBAR_H + 3 + SUB_H + 6) +
                (DurexClient.getAntiKostkaModule().getSlots().size() < AntiKostkaModule.MAX_SLOTS ? SUB_H + 3 : 0) + PAD));
            default -> 0;
        };
    }

    private int getSubH(ModWidget m) {
        float a = m.expandAnim;
        return switch (m.id) {
            case "antiKowal"     -> (int)(a * (4 * (SUB_H + 3) + PAD));
            case "cooldowns"     -> (int)(a * (2 * (SUB_H + 3) + PAD));
            case "antiKostka"    -> (int)(a * (
                (SUB_H + 3) +
                (delayExpanded ? AntiKostkaModule.DelayMode.values().length * (SUB_H + 3) : 0) +
                antiKostka.getSlots().size() * (SUB_H + 3 + HOTBAR_H + 3 + SUB_H + 6) +
                (antiKostka.getSlots().size() < AntiKostkaModule.MAX_SLOTS ? SUB_H + 3 : 0) + PAD));
            case "viewModel"     -> (int)(a * (8 * (SUB_H + 3) + SUB_H + PAD));
            case "procenciarz"   -> (int)(a * (2 * (SUB_H + 3) + PAD));
            case "leverCobweb"   -> (int)(a * (5 * (SUB_H + 3) + PAD));
            case "autoDripstone" -> (int)(a * (SUB_H + 3 + PAD));
            case "noPush"        -> 0;
            case "zbrojmistrz"   -> (int)(a * (2 * (SUB_H + 3) + PAD));
            case "msgBot"        -> (int)(a * (9 * (SUB_H + 3) + PAD));
            case "nametags"      -> (int)(a * (5 * (SUB_H + 3) + PAD));
            case "tracers"       -> (int)(a * (2 * (SUB_H + 3) + PAD));
            default -> 0;
        };
    }

    // ── Module Library ────────────────────────────────────────────────────

    /** Definicja modułu w bibliotece */
    public record ModDef(String id, String icon, String label, String category, String desc) {}

    /** Wszystkie dostępne moduły z kategoriami */
    private static final ModDef[] ALL_MOD_DEFS = {
        new ModDef("antiKowal",     "⚔",  "AntiKowal",      "Utility", "Blokuje naprawy u kowala"),
        new ModDef("cooldowns",     "⏱",  "Cooldowns",      "Utility", "HUD z cooldownami itemów"),
        new ModDef("antiKostka",    "🎒",  "AntiKostka",     "Utility", "Zapisuje i ładuje hotbar"),
        new ModDef("viewModel",     "👁",  "ViewModel",      "Utility", "Edytor pozycji ręki/broni"),
        new ModDef("leverCobweb",   "🕸",  "Nemos Helper",   "Utility", "Auto dźwignia i pajęczyna"),
        new ModDef("autoDripstone", "💧",  "Auto Dripstone", "Utility", "Automatyczne stalagmity"),
        new ModDef("noPush",        "🛡",  "No Push",        "Utility", "Brak odpychania przez graczy"),
        new ModDef("msgBot",        "✉",  "Msg Bot",        "Utility", "Automatyczne wiadomości"),
        new ModDef("procenciarz",   "%",   "Procenciarz",    "HUD",     "Procent DMG broni celu"),
        new ModDef("zbrojmistrz",   "⚔",  "Zbrojmistrz",    "HUD",     "Zbroja i enchanty celu"),
        new ModDef("nametags",      "🏷",  "Nametags",       "Visual",  "Tagi nad głowami graczy"),
        new ModDef("tracers",       "→",   "Tracers",        "Visual",  "Linie do graczy przez ściany"),
    };

    private static final String[] LIB_TABS = {"Wszystkie", "HUD", "Utility", "Visual"};

    // Stan biblioteki
    private boolean libOpen = false;
    private int libTabIdx = 0;
    private String libSearch = "";
    private boolean libSearchFocused = false;
    // Drag biblioteki
    private int libPosX = -1, libPosY = -1;
    private boolean libDragging = false;
    private int libDragOffX, libDragOffY;
    // Recent searches
    private final java.util.ArrayDeque<String> recentSearches = new java.util.ArrayDeque<>();
    private static final int MAX_RECENT = 5;
    // Animacja dodawania modułu
    private record AddAnim(String id, float startX, float startY, float endX, float endY, long startMs) {}
    private final List<AddAnim> addAnims = new ArrayList<>();
    private static final long ADD_ANIM_MS = 400;

    private static final int LIB_W        = 270;
    private static final int LIB_ITEM_H   = 28;
    private static final int LIB_TAB_H    = 22;
    private static final int LIB_SEARCH_H = 26;
    private static final int LIB_PAD      = 8;
    private static final int LIB_RECENT_H = 18;

    private int libX() { return libPosX >= 0 ? libPosX : panelX + W + 10; }
    private int libY() { return libPosY >= 0 ? libPosY : panelY; }

    private boolean libShowList() { return !libSearch.isEmpty() || libTabIdx > 0; }

    private List<ModDef> getFilteredMods() {
        String tab = LIB_TABS[libTabIdx];
        String q = libSearch.toLowerCase();
        List<ModDef> result = new ArrayList<>();
        for (ModDef def : ALL_MOD_DEFS) {
            if (!tab.equals("Wszystkie") && !def.category().equals(tab)) continue;
            if (!q.isEmpty() && !def.label().toLowerCase().contains(q)
                    && !def.id().toLowerCase().contains(q)
                    && !def.desc().toLowerCase().contains(q)) continue;
            result.add(def);
        }
        return result;
    }

    private boolean isModInGui(String id) {
        for (ModWidget m : freeMods) if (m.id.equals(id)) return true;
        for (CategoryWidget cat : categories) for (ModWidget m : cat.mods) if (m.id.equals(id)) return true;
        return false;
    }

    private void removeModFromGui(String id) {
        freeMods.removeIf(m -> m.id.equals(id));
        for (CategoryWidget cat : categories) cat.mods.removeIf(m -> m.id.equals(id));
        updateDockedPositions(false);
    }

    private void addRecentSearch(String q) {
        if (q.isBlank()) return;
        recentSearches.remove(q);
        recentSearches.addFirst(q);
        while (recentSearches.size() > MAX_RECENT) recentSearches.removeLast();
    }

    private int libH(List<ModDef> mods) {
        int base = CAT_H + 4 + LIB_SEARCH_H + 4 + LIB_TAB_H + 4 + LIB_PAD;
        // Recent searches (tylko gdy pole puste i są wyniki)
        if (libSearch.isEmpty() && !recentSearches.isEmpty() && libTabIdx == 0) {
            base += 14 + recentSearches.size() * (LIB_RECENT_H + 2) + 4;
        }
        if (libShowList()) base += mods.size() * (LIB_ITEM_H + 3) + LIB_PAD;
        return base;
    }

    private void renderLibrary(DrawContext ctx, int mx, int my) {
        List<ModDef> mods = libShowList() ? getFilteredMods() : List.of();
        int lx = libX(), ly = libY();
        int lw = LIB_W, lh = libH(mods);

        // Cień + tło
        GuiRenderUtils.drawShadow(ctx, lx, ly, lw, lh, 10, 0xCC6600CC, 14);
        GuiRenderUtils.drawRoundedRect(ctx, lx, ly, lw, lh, 10, 0xF0060012);
        GuiRenderUtils.drawRoundedOutline(ctx, lx, ly, lw, lh, 10, 1, COL_BORDER);

        // Header gradient
        boolean hdrH = mx >= lx && mx <= lx + lw && my >= ly && my <= ly + CAT_H;
        ctx.fillGradient(lx, ly, lx + lw, ly + CAT_H, 0xFF1A0038, 0xFF0D0020);
        GuiRenderUtils.drawRoundedRect(ctx, lx, ly, lw, CAT_H, 10, hdrH ? 0x22FFFFFF : 0);
        ctx.drawTextWithShadow(textRenderer, Text.literal("§5⊞ §dBiblioteka modułów"),
            lx + 10, ly + (CAT_H - 8) / 2, 0xFFFFFFFF);
        // Licznik modułów
        int total = ALL_MOD_DEFS.length;
        int added = (int) java.util.Arrays.stream(ALL_MOD_DEFS).filter(d -> isModInGui(d.id())).count();
        String counter = added + "/" + total;
        int cw = textRenderer.getWidth(counter) + 8;
        GuiRenderUtils.drawRoundedRect(ctx, lx + lw - cw - 22, ly + (CAT_H - 14) / 2, cw, 14, 4, 0x44AA44FF);
        ctx.drawTextWithShadow(textRenderer, Text.literal("§a" + counter),
            lx + lw - cw - 19, ly + (CAT_H - 8) / 2, 0xFFFFFFFF);
        // X zamknij
        boolean xH = mx >= lx + lw - 18 && mx <= lx + lw - 6 && my >= ly + 4 && my <= ly + CAT_H - 4;
        GuiRenderUtils.drawRoundedRect(ctx, lx + lw - 18, ly + 4, 12, CAT_H - 8, 3, xH ? 0xAA440010 : 0x33220010);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("✕"),
            lx + lw - 12, ly + (CAT_H - 8) / 2, xH ? COL_OFF : 0x66FF4455);

        int cy = ly + CAT_H + 4;

        // Wyszukiwarka
        boolean sfH = mx >= lx + LIB_PAD && mx <= lx + lw - LIB_PAD && my >= cy && my <= cy + LIB_SEARCH_H;
        int searchBg = libSearchFocused ? 0xAA1A0040 : (sfH ? 0x661A0040 : 0x441A0040);
        GuiRenderUtils.drawRoundedRect(ctx, lx + LIB_PAD, cy, lw - LIB_PAD * 2, LIB_SEARCH_H, 5, searchBg);
        GuiRenderUtils.drawRoundedOutline(ctx, lx + LIB_PAD, cy, lw - LIB_PAD * 2, LIB_SEARCH_H, 5, 1,
            libSearchFocused ? COL_ACCENT : (sfH ? 0x88AA44FF : 0x33AA44FF));
        // Ikona lupy
        ctx.drawTextWithShadow(textRenderer, Text.literal("§8🔍"), lx + LIB_PAD + 5, cy + (LIB_SEARCH_H - 8) / 2, 0xFFFFFFFF);
        String searchTxt = libSearch.isEmpty() && !libSearchFocused
            ? "§8Szukaj modułu..."
            : "§f" + libSearch + (libSearchFocused ? "§7|" : "");
        ctx.drawTextWithShadow(textRenderer, Text.literal(searchTxt), lx + LIB_PAD + 18, cy + (LIB_SEARCH_H - 8) / 2, 0xFFFFFFFF);
        // Clear button gdy jest tekst
        if (!libSearch.isEmpty()) {
            boolean clH = mx >= lx + lw - LIB_PAD - 14 && mx <= lx + lw - LIB_PAD && my >= cy && my <= cy + LIB_SEARCH_H;
            ctx.drawTextWithShadow(textRenderer, Text.literal(clH ? "§c✕" : "§8✕"),
                lx + lw - LIB_PAD - 10, cy + (LIB_SEARCH_H - 8) / 2, 0xFFFFFFFF);
        }
        cy += LIB_SEARCH_H + 4;

        // Zakładki
        int tabW = (lw - LIB_PAD * 2) / LIB_TABS.length;
        for (int i = 0; i < LIB_TABS.length; i++) {
            int tx = lx + LIB_PAD + i * tabW;
            boolean sel = libTabIdx == i;
            boolean tH = mx >= tx && mx <= tx + tabW - 2 && my >= cy && my <= cy + LIB_TAB_H;
            if (sel) {
                ctx.fillGradient(tx, cy, tx + tabW - 2, cy + LIB_TAB_H, 0xCC220044, 0xAA110033);
                GuiRenderUtils.drawRoundedOutline(ctx, tx, cy, tabW - 2, LIB_TAB_H, 4, 1, COL_BORDER);
            } else {
                GuiRenderUtils.drawRoundedRect(ctx, tx, cy, tabW - 2, LIB_TAB_H, 4, tH ? 0x44220044 : 0x22110022);
            }
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(LIB_TABS[i]),
                tx + tabW / 2 - 1, cy + (LIB_TAB_H - 8) / 2, sel ? COL_ACCENT : (tH ? COL_TEXT : COL_MUTED));
        }
        cy += LIB_TAB_H + 4;

        // Recent searches (gdy pole puste i zakładka "Wszystkie")
        if (libSearch.isEmpty() && !recentSearches.isEmpty() && libTabIdx == 0) {
            ctx.drawTextWithShadow(textRenderer, Text.literal("§8Ostatnie wyszukiwania"),
                lx + LIB_PAD + 2, cy + 2, 0xFFFFFFFF);
            cy += 14;
            for (String recent : recentSearches) {
                boolean rH = mx >= lx + LIB_PAD && mx <= lx + lw - LIB_PAD - 20 && my >= cy && my <= cy + LIB_RECENT_H;
                GuiRenderUtils.drawRoundedRect(ctx, lx + LIB_PAD, cy, lw - LIB_PAD * 2 - 20, LIB_RECENT_H, 3,
                    rH ? 0x44220044 : 0x22110022);
                ctx.drawTextWithShadow(textRenderer, Text.literal("§8🕐 §7" + recent),
                    lx + LIB_PAD + 5, cy + (LIB_RECENT_H - 8) / 2, 0xFFFFFFFF);
                cy += LIB_RECENT_H + 2;
            }
            cy += 4;
        }

        // Lista modułów
        if (libShowList()) {
            if (mods.isEmpty()) {
                ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§8Brak wyników dla \"" + libSearch + "\""),
                    lx + lw / 2, cy + 6, 0xFFFFFFFF);
            }
            for (ModDef def : mods) {
                boolean inGui = isModInGui(def.id());
                boolean itemH = mx >= lx + LIB_PAD && mx <= lx + lw - LIB_PAD && my >= cy && my <= cy + LIB_ITEM_H;

                // Tło itemu z gradientem
                if (itemH) {
                    ctx.fillGradient(lx + LIB_PAD, cy, lx + lw - LIB_PAD, cy + LIB_ITEM_H, 0x662A0060, 0x441A0040);
                    GuiRenderUtils.drawRoundedOutline(ctx, lx + LIB_PAD, cy, lw - LIB_PAD * 2, LIB_ITEM_H, 4, 1, COL_BORDER);
                } else {
                    GuiRenderUtils.drawRoundedRect(ctx, lx + LIB_PAD, cy, lw - LIB_PAD * 2, LIB_ITEM_H, 4,
                        inGui ? 0x33110022 : 0x441A0040);
                    GuiRenderUtils.drawRoundedOutline(ctx, lx + LIB_PAD, cy, lw - LIB_PAD * 2, LIB_ITEM_H, 4, 1,
                        inGui ? 0x33440066 : 0x22660099);
                }

                // Ikona w kółku
                int iconBg = inGui ? 0x44440066 : 0x44006600;
                GuiRenderUtils.drawRoundedRect(ctx, lx + LIB_PAD + 4, cy + (LIB_ITEM_H - 16) / 2, 16, 16, 4, iconBg);
                ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(def.icon()),
                    lx + LIB_PAD + 12, cy + (LIB_ITEM_H - 8) / 2, inGui ? 0xFF886699 : COL_ON);

                // Nazwa + opis
                ctx.drawTextWithShadow(textRenderer, Text.literal(inGui ? "§7" + def.label() : "§f" + def.label()),
                    lx + LIB_PAD + 24, cy + 4, 0xFFFFFFFF);
                ctx.drawTextWithShadow(textRenderer,
                    Text.literal("§8" + def.desc()),
                    lx + LIB_PAD + 24, cy + 14, 0xFFFFFFFF);

                // Przycisk +/X po prawej
                int btnX = lx + lw - LIB_PAD - 18;
                int btnY = cy + (LIB_ITEM_H - 16) / 2;
                boolean btnH = mx >= btnX && mx <= btnX + 18 && my >= btnY && my <= btnY + 16;
                if (inGui) {
                    GuiRenderUtils.drawRoundedRect(ctx, btnX, btnY, 18, 16, 4, btnH ? 0xAA440010 : 0x55220010);
                    ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§c✕"), btnX + 9, btnY + 4, 0xFFFFFFFF);
                } else {
                    GuiRenderUtils.drawRoundedRect(ctx, btnX, btnY, 18, 16, 4, btnH ? 0xAA004400 : 0x55002200);
                    ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§a+"), btnX + 9, btnY + 4, 0xFFFFFFFF);
                }
                cy += LIB_ITEM_H + 3;
            }
        }

        // Animacje dodawania
        long now = System.currentTimeMillis();
        addAnims.removeIf(a -> now - a.startMs() > ADD_ANIM_MS);
        for (AddAnim anim : addAnims) {
            float t = (float)(now - anim.startMs()) / ADD_ANIM_MS;
            // ease out cubic
            float ease = 1f - (1f - t) * (1f - t) * (1f - t);
            float ax = anim.startX() + (anim.endX() - anim.startX()) * ease;
            float ay = anim.startY() + (anim.endY() - anim.startY()) * ease;
            float scale = 1.3f - 0.3f * ease;
            int alpha = (int)((1f - t) * 220);
            // Rysuj "duszka" modułu lecącego
            ctx.getMatrices().push();
            ctx.getMatrices().translate(ax, ay, 0);
            ctx.getMatrices().scale(scale, scale, 1f);
            GuiRenderUtils.drawRoundedRect(ctx, -(W - 10) / 2, -MOD_H / 2, W - 10, MOD_H, 4,
                (alpha << 24) | 0x220044);
            GuiRenderUtils.drawRoundedOutline(ctx, -(W - 10) / 2, -MOD_H / 2, W - 10, MOD_H, 4, 1,
                (alpha << 24) | 0x8800EE);
            ctx.getMatrices().pop();
        }
    }

    private boolean handleLibraryClick(double mx, double my, int button) {
        if (!libOpen) return false;
        List<ModDef> mods = libShowList() ? getFilteredMods() : List.of();
        int lx = libX(), ly = libY();
        int lw = LIB_W, lh = libH(mods);

        if (!(mx >= lx && mx <= lx + lw && my >= ly && my <= ly + lh)) {
            libOpen = false; libSearchFocused = false; return false;
        }

        // X zamknij
        if (mx >= lx + lw - 18 && mx <= lx + lw - 6 && my >= ly + 4 && my <= ly + CAT_H - 4) {
            libOpen = false; libSearchFocused = false; soundSnap(); return true;
        }

        // Header drag
        if (button == 0 && my >= ly && my <= ly + CAT_H) {
            libDragging = true; libDragOffX = (int)mx - lx; libDragOffY = (int)my - ly; return true;
        }

        int cy = ly + CAT_H + 4;

        // Wyszukiwarka
        if (mx >= lx + LIB_PAD && mx <= lx + lw - LIB_PAD && my >= cy && my <= cy + LIB_SEARCH_H) {
            // Clear button
            if (!libSearch.isEmpty() && mx >= lx + lw - LIB_PAD - 14) {
                libSearch = ""; soundClick(); return true;
            }
            libSearchFocused = true; soundClick(); return true;
        }
        libSearchFocused = false;
        cy += LIB_SEARCH_H + 4;

        // Zakładki
        int tabW = (lw - LIB_PAD * 2) / LIB_TABS.length;
        if (my >= cy && my <= cy + LIB_TAB_H) {
            for (int i = 0; i < LIB_TABS.length; i++) {
                int tx = lx + LIB_PAD + i * tabW;
                if (mx >= tx && mx <= tx + tabW - 2) {
                    if (!libSearch.isEmpty()) addRecentSearch(libSearch);
                    libTabIdx = i; soundClick(); return true;
                }
            }
        }
        cy += LIB_TAB_H + 4;

        // Recent searches
        if (libSearch.isEmpty() && !recentSearches.isEmpty() && libTabIdx == 0) {
            cy += 14;
            for (String recent : new java.util.ArrayList<>(recentSearches)) {
                if (mx >= lx + LIB_PAD && mx <= lx + lw - LIB_PAD - 20 && my >= cy && my <= cy + LIB_RECENT_H) {
                    libSearch = recent; libSearchFocused = false; soundClick(); return true;
                }
                cy += LIB_RECENT_H + 2;
            }
            cy += 4;
        }

        // Moduły
        for (ModDef def : mods) {
            if (mx >= lx + LIB_PAD && mx <= lx + lw - LIB_PAD && my >= cy && my <= cy + LIB_ITEM_H) {
                boolean inGui = isModInGui(def.id());
                int btnX = lx + lw - LIB_PAD - 18;
                int btnY = cy + (LIB_ITEM_H - 16) / 2;
                if (mx >= btnX) {
                    if (inGui) {
                        removeModFromGui(def.id());
                        soundSnap();
                        pl.durex.client.DurexClient.saveNow();
                    } else {
                        // Oblicz pozycję docelową
                        float destX = 20, destY = 60;
                        for (ModWidget existing : freeMods) destY = Math.max(destY, existing.ry + MOD_H + 4);
                        // Dodaj moduł
                        ModWidget m = new ModWidget(def.id(), def.icon());
                        m.rx = destX; m.ry = destY; m.tx = destX; m.ty = destY;
                        freeMods.add(m);
                        // Animacja: start = środek przycisku, end = środek modułu
                        float startX = (float)(lx + lw - LIB_PAD - 9);
                        float startY = (float)(btnY + 8);
                        float endX = destX + (W - 10) / 2f;
                        float endY = destY + MOD_H / 2f;
                        addAnims.add(new AddAnim(def.id(), startX, startY, endX, endY, System.currentTimeMillis()));
                        if (!libSearch.isEmpty()) addRecentSearch(libSearch);
                        soundPop();
                        pl.durex.client.DurexClient.saveNow();
                    }
                }
                return true;
            }
            cy += LIB_ITEM_H + 3;
        }
        return true;
    }

    /** Odpycha floating moduły i kategorie od siebie i od krawędzi ekranu */
    private void resolveCollisions() {
        final int MARGIN = 4;
        final int GAP    = 3;
        final float PUSH = 1.5f;

        // Zbierz pozycje i rozmiary wszystkich floating widgetów
        int n = 0;
        for (ModWidget m : freeMods) if (m != activeModDrag) n++;
        for (CategoryWidget cat : categories) if (!cat.docked && cat != activeCatDrag) n++;

        float[] xs = new float[n], ys = new float[n];
        int[]   ws = new int[n],   hs = new int[n];
        Object[] refs = new Object[n];
        int idx = 0;
        for (ModWidget m : freeMods) {
            if (m == activeModDrag) continue;
            xs[idx] = m.tx; ys[idx] = m.ty;
            ws[idx] = W - 10;
            hs[idx] = MOD_H + (m.expandAnim > 0.01f ? getSubHStatic(m) : 0);
            refs[idx] = m; idx++;
        }
        for (CategoryWidget cat : categories) {
            if (cat.docked || cat == activeCatDrag) continue;
            xs[idx] = cat.tx; ys[idx] = cat.ty;
            ws[idx] = W; hs[idx] = cat.totalH();
            refs[idx] = cat; idx++;
        }

        // Odpychanie od krawędzi
        for (int i = 0; i < n; i++) {
            if (xs[i] < MARGIN)                    xs[i] += PUSH;
            if (ys[i] < MARGIN)                    ys[i] += PUSH;
            if (xs[i] + ws[i] > width - MARGIN)   xs[i] -= PUSH;
            if (ys[i] + hs[i] > height - MARGIN)  ys[i] -= PUSH;
        }

        // Odpychanie od siebie
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                float ox1 = (xs[i] + ws[i] + GAP) - xs[j];
                float ox2 = (xs[j] + ws[j] + GAP) - xs[i];
                float oy1 = (ys[i] + hs[i] + GAP) - ys[j];
                float oy2 = (ys[j] + hs[j] + GAP) - ys[i];
                if (ox1 <= 0 || ox2 <= 0 || oy1 <= 0 || oy2 <= 0) continue;

                float minOvX = Math.min(ox1, ox2);
                float minOvY = Math.min(oy1, oy2);
                float push = PUSH * 0.5f;
                if (minOvX < minOvY) {
                    if (xs[i] < xs[j]) { xs[i] -= push; xs[j] += push; }
                    else               { xs[i] += push; xs[j] -= push; }
                } else {
                    if (ys[i] < ys[j]) { ys[i] -= push; ys[j] += push; }
                    else               { ys[i] += push; ys[j] -= push; }
                }
            }
        }

        // Zastosuj nowe pozycje
        for (int i = 0; i < n; i++) {
            if (refs[i] instanceof ModWidget m) { m.tx = xs[i]; m.ty = ys[i]; }
            else if (refs[i] instanceof CategoryWidget cat) { cat.tx = xs[i]; cat.ty = ys[i]; }
        }
    }

    private void renderTutorial(DrawContext ctx, long elapsedMs) {
        // Fade out w ostatnich 1.5 sekundy
        float alpha = 1f;
        long fadeStart = TUTORIAL_DURATION_MS - 1500;
        if (elapsedMs > fadeStart) {
            alpha = 1f - (float)(elapsedMs - fadeStart) / 1500f;
        }
        int a = (int)(alpha * 255);
        if (a <= 0) return;

        // Pozycja przycisku ⊞ Moduły
        int ph = calcPanelH();
        int addY = panelY + ph - FOOTER_H - CAT_H - 4;
        int halfBtnW = (W - PAD * 2 - 4) / 2;
        int libBtnX = panelX + PAD + halfBtnW + 4;
        int libBtnCX = libBtnX + halfBtnW / 2;

        // Subtelny puls — tylko cienka ramka wokół przycisku
        float pulse = (float)(Math.sin(elapsedMs / 400.0) * 0.5 + 0.5);
        int pulseColor = (a << 24) | (int)(0xCC77FF * pulse + 0x884499 * (1 - pulse));
        GuiRenderUtils.drawRoundedOutline(ctx, libBtnX - 2, addY - 2, halfBtnW + 4, CAT_H + 4, 5, 2,
            ((int)(alpha * (0x88 + (int)(pulse * 0x77))) << 24) | 0xCC77FF);

        // Mały tooltip po prawej stronie panelu
        int ttW = 160;
        int ttH = 36;
        int ttX = panelX + W + 6;
        int ttY = addY + (CAT_H - ttH) / 2;
        // Clamp do ekranu
        if (ttX + ttW > width - 4) ttX = panelX - ttW - 6;
        ttY = Math.max(4, Math.min(height - ttH - 4, ttY));

        int bgA   = (int)(alpha * 0xDD);
        int brdA  = (int)(alpha * 0xFF);

        GuiRenderUtils.drawShadow(ctx, ttX, ttY, ttW, ttH, 5, (bgA / 2) << 24 | 0x8800EE, 6);
        ctx.fill(ttX, ttY, ttX + ttW, ttY + ttH, (bgA << 24) | 0x0D0020);
        GuiRenderUtils.drawRoundedOutline(ctx, ttX, ttY, ttW, ttH, 5, 1, (brdA << 24) | 0x8800EE);

        // Strzałka wskazująca na przycisk (lewa strona tooltipa)
        int arrowMidY = ttY + ttH / 2;
        ctx.fill(ttX - 5, arrowMidY - 1, ttX, arrowMidY + 1, (a << 24) | 0xCC77FF);
        ctx.fill(ttX - 8, arrowMidY - 3, ttX - 5, arrowMidY + 3, (a << 24) | 0xCC77FF);

        // Tekst
        int secsLeft = (int)Math.ceil((TUTORIAL_DURATION_MS - elapsedMs) / 1000.0);
        ctx.drawCenteredTextWithShadow(textRenderer,
            Text.literal("§dKliknij §f⊞ Moduły"),
            ttX + ttW / 2, ttY + 6, (a << 24) | 0xFFFFFF);
        ctx.drawCenteredTextWithShadow(textRenderer,
            Text.literal("§7aby dodać moduł"),
            ttX + ttW / 2, ttY + 17, (a << 24) | 0xFFFFFF);
        ctx.drawCenteredTextWithShadow(textRenderer,
            Text.literal("§8" + secsLeft + "s"),
            ttX + ttW / 2, ttY + 27, (a << 24) | 0xFFFFFF);
    }

    private boolean handleModSubClick(ModWidget m, double mx, double my, int px, int sy, int button) {
        return switch (m.id) {
            case "antiKowal"     -> handleAntiKowalSub(mx, my, px, sy, button);
            case "cooldowns"     -> handleCooldownsSub(mx, my, px, sy, button);
            case "antiKostka"    -> handleAntiKostkaSub(mx, my, px, sy, button);
            case "viewModel"     -> handleViewModelSub(mx, my, px, sy, button);
            case "procenciarz"   -> handleProcenciarzSub(mx, my, px, sy, button);
            case "leverCobweb"   -> handleLeverCobwebSub(mx, my, px, sy, button);
            case "autoDripstone" -> handleAutoDripstoneSub(mx, my, px, sy, button);
            case "msgBot"        -> handleMsgBotSub(mx, my, px, sy, button);
            case "zbrojmistrz"   -> handleZbrojmistrzSub(mx, my, px, sy, button);
            case "nametags"      -> handleNametagsSub(mx, my, px, sy, button);
            case "tracers"       -> handleTracersSub(mx, my, px, sy, button);
            default -> false;
        };
    }

    private boolean inSub(double mx, double my, int px, int sy) {
        return mx >= px + PAD && mx <= px + W - PAD - 10 && my >= sy && my <= sy + SUB_H;
    }

    private boolean handleAntiKowalSub(double mx, double my, int px, int sy, int button) {
        if (inSub(mx, my, px, sy)) { if (button == 0) { pl.durex.client.util.RaycastState.active = !pl.durex.client.util.RaycastState.active; soundToggle(pl.durex.client.util.RaycastState.active); pl.durex.client.DurexClient.saveNow(); } return true; } sy += SUB_H + 3;
        if (inSub(mx, my, px, sy)) { if (button == 0) { waitingAntiKowalBind = true; waitingFriendBind = false; waitingHotbarBind = -1; soundClick(); } return true; } sy += SUB_H + 3;
        if (inSub(mx, my, px, sy)) { if (button == 0) { waitingFriendBind = true; waitingAntiKowalBind = false; waitingHotbarBind = -1; soundClick(); } return true; } sy += SUB_H + 3;
        if (inSub(mx, my, px, sy)) { if (button == 0) { friendModule.clearAll(); soundSnap(); pl.durex.client.DurexClient.saveNow(); } return true; }
        return false;
    }

    private boolean handleCooldownsSub(double mx, double my, int px, int sy, int button) {
        if (inSub(mx, my, px, sy)) { if (button == 0) { cooldownHud.setEnabled(!cooldownHud.isEnabled()); soundToggle(cooldownHud.isEnabled()); } return true; } sy += SUB_H + 3;
        if (inSub(mx, my, px, sy)) { if (button == 0) { settingPosition = !settingPosition; soundClick(); } return true; }
        return false;
    }

    private boolean handleAntiKostkaSub(double mx, double my, int px, int sy, int button) {
        if (inSub(mx, my, px, sy)) { if (button == 0) { delayExpanded = !delayExpanded; soundClick(); } return true; } sy += SUB_H + 3;
        if (delayExpanded) {
            for (AntiKostkaModule.DelayMode mode : AntiKostkaModule.DelayMode.values()) {
                if (my >= sy && my <= sy + SUB_H && mx >= px + PAD + 8 && mx <= px + W - PAD) {
                    if (button == 0) { antiKostka.setDelayMode(mode); soundClick(); } return true;
                }
                sy += SUB_H + 3;
            }
        }
        for (int i = 0; i < antiKostka.getSlots().size(); i++) {
            if (inSub(mx, my, px, sy)) {
                if (button == 0) { waitingHotbarBind = i; waitingAntiKowalBind = false; waitingFriendBind = false; soundClick(); }
                else if (button == 1) { editingSlotName = i; nameBuffer.setLength(0); nameBuffer.append(antiKostka.getSlots().get(i).name); soundClick(); }
                return true;
            }
            sy += SUB_H + 3 + HOTBAR_H + 3;
            int btnW2 = (W - PAD * 2 - 4) / 2;
            if (mx >= px + PAD && mx <= px + PAD + btnW2 && my >= sy && my <= sy + SUB_H) { if (button == 0) { antiKostka.saveSlot(i, client); soundSnap(); } return true; }
            if (mx >= px + PAD + btnW2 + 4 && mx <= px + PAD + btnW2 * 2 + 4 && my >= sy && my <= sy + SUB_H) { if (button == 0) { antiKostka.removeSlot(i); soundSnap(); } return true; }
            sy += SUB_H + 6;
        }
        if (inSub(mx, my, px, sy) && button == 0 && antiKostka.getSlots().size() < AntiKostkaModule.MAX_SLOTS) { antiKostka.addSlot(); soundPop(); return true; }
        return false;
    }

    private String draggingSlider = null;
    private int sliderPx = 0;

    private boolean handleViewModelSub(double mx, double my, int px, int sy, int button) {
        if (my >= sy && my <= sy + SUB_H) { if (button == 0) { viewModel.toggleHand(); soundClick(); } return true; } sy += SUB_H + 3;
        String[] fields = {"rotX","rotY","rotZ","posX","posY","posZ","scale"};
        for (String f : fields) {
            if (my >= sy && my <= sy + SUB_H) { if (button == 0) { draggingSlider = f; sliderPx = px; applySlider(f, mx, px); } return true; }
            sy += SUB_H + 3;
        }
        if (my >= sy && my <= sy + SUB_H && mx >= px + PAD && mx <= px + W - PAD) { if (button == 0) { viewModel.resetActive(); soundSnap(); } return true; }
        return false;
    }

    private void applySlider(String field, double mx, int px) {
        int barX = px + PAD + 50; int barW = W - PAD * 2 - 50;
        float t = (float)Math.max(0, Math.min(1, (mx - barX) / barW));
        switch (field) {
            case "rotX" -> viewModel.setRotX(ViewModelModule.ROT_MIN + t * (ViewModelModule.ROT_MAX - ViewModelModule.ROT_MIN));
            case "rotY" -> viewModel.setRotY(ViewModelModule.ROT_MIN + t * (ViewModelModule.ROT_MAX - ViewModelModule.ROT_MIN));
            case "rotZ" -> viewModel.setRotZ(ViewModelModule.ROT_MIN + t * (ViewModelModule.ROT_MAX - ViewModelModule.ROT_MIN));
            case "posX" -> viewModel.setPosX(ViewModelModule.POS_MIN + t * (ViewModelModule.POS_MAX - ViewModelModule.POS_MIN));
            case "posY" -> viewModel.setPosY(ViewModelModule.POS_MIN + t * (ViewModelModule.POS_MAX - ViewModelModule.POS_MIN));
            case "posZ" -> viewModel.setPosZ(ViewModelModule.POS_MIN + t * (ViewModelModule.POS_MAX - ViewModelModule.POS_MIN));
            case "scale" -> viewModel.setScale(ViewModelModule.SCALE_MIN + t * (ViewModelModule.SCALE_MAX - ViewModelModule.SCALE_MIN));
            case "nametags_dist" -> pl.durex.client.module.NametagsModule.setMaxDistance(8f + t * (256f - 8f));
            case "tracers_dist"  -> pl.durex.client.module.TracerModule.setMaxDistance(8f + t * (512f - 8f));
        }
    }


    private boolean handleProcenciarzSub(double mx, double my, int px, int sy, int button) {
        if (my >= sy && my <= sy + SUB_H) { if (button == 0) { settingProcenciarz = !settingProcenciarz; soundClick(); } return true; } sy += SUB_H + 3;
        if (inSub(mx, my, px, sy)) {
            if (button == 0) {
                pl.durex.client.module.ProcenciarzModule.setShowBooks(!pl.durex.client.module.ProcenciarzModule.isShowBooks());
                soundToggle(pl.durex.client.module.ProcenciarzModule.isShowBooks());
                pl.durex.client.DurexClient.saveNow();
            }
            return true;
        }
        return false;
    }

    private boolean handleLeverCobwebSub(double mx, double my, int px, int sy, int button) {
        if (inSub(mx, my, px, sy)) { if (button == 0) { leverCobweb.setSpeedProfile(getNextSpeedProfile(leverCobweb.getSpeedProfile())); soundClick(); pl.durex.client.DurexClient.saveNow(); } return true; } sy += SUB_H + 3;
        if (inSub(mx, my, px, sy)) { if (button == 0) { leverCobweb.setPlayerModeEnabled(!leverCobweb.isPlayerModeEnabled()); soundToggle(leverCobweb.isPlayerModeEnabled()); pl.durex.client.DurexClient.saveNow(); } return true; } sy += SUB_H + 3;
        if (inSub(mx, my, px, sy)) { if (button == 0) { leverCobweb.setWebModeEnabled(!leverCobweb.isWebModeEnabled()); soundToggle(leverCobweb.isWebModeEnabled()); pl.durex.client.DurexClient.saveNow(); } return true; } sy += SUB_H + 3;
        if (inSub(mx, my, px, sy)) { if (button == 0) { leverCobweb.setSwitchToBestSwordEnabled(!leverCobweb.isSwitchToBestSwordEnabled()); soundToggle(leverCobweb.isSwitchToBestSwordEnabled()); pl.durex.client.DurexClient.saveNow(); } return true; } sy += SUB_H + 3;
        if (inSub(mx, my, px, sy)) { if (button == 0) { leverCobweb.setLeverOnlyModeEnabled(!leverCobweb.isLeverOnlyModeEnabled()); soundToggle(leverCobweb.isLeverOnlyModeEnabled()); pl.durex.client.DurexClient.saveNow(); } return true; }
        return false;
    }
    
    private boolean handleMsgBotSub(double mx, double my, int px, int sy, int button) {
        // Wiersz 1: wiadomość (klik = zacznij edytować)
        if (inSub(mx, my, px, sy)) {
            if (button == 0) { msgBotEditingField = 1; soundClick(); }
            return true;
        } sy += SUB_H + 3;
        // Wiersz 2: nick
        if (inSub(mx, my, px, sy)) {
            if (button == 0) { msgBotEditingField = 2; soundClick(); }
            return true;
        } sy += SUB_H + 3;
        // Wiersz 3: ch delay
        if (inSub(mx, my, px, sy)) {
            if (button == 0) { msgBotEditingField = 3; soundClick(); }
            return true;
        } sy += SUB_H + 3;
        // Wiersz 4: START
        if (inSub(mx, my, px, sy)) {
            if (button == 0) {
                saveMsgBotFields();
                if (pl.durex.client.module.MsgBotModule.isSpamming())
                    pl.durex.client.module.MsgBotModule.stopSpam();
                else
                    pl.durex.client.module.MsgBotModule.startSpam(500);
                soundToggle(pl.durex.client.module.MsgBotModule.isSpamming());
                pl.durex.client.DurexClient.saveNow();
            }
            return true;
        } sy += SUB_H + 3;
        // Wiersz 5: Reset listy
        if (inSub(mx, my, px, sy)) {
            if (button == 0) { pl.durex.client.module.MsgBotModule.resetSpammedPlayers(); soundSnap(); }
            return true;
        } sy += SUB_H + 3;
        // Wiersz 6: AFK Mode ON/OFF
        if (inSub(mx, my, px, sy)) {
            if (button == 0) {
                if (pl.durex.client.module.MsgBotModule.isAfk())
                    pl.durex.client.module.MsgBotModule.stopAfk();
                else
                    pl.durex.client.module.MsgBotModule.startAfk();
                soundToggle(pl.durex.client.module.MsgBotModule.isAfk());
            }
            return true;
        } sy += SUB_H + 3;
        // Wiersz 7: CH ON/OFF
        if (inSub(mx, my, px, sy)) {
            if (button == 0) {
                saveMsgBotFields();
                if (pl.durex.client.module.MsgBotModule.isAutoCh())
                    pl.durex.client.module.MsgBotModule.stopAutoCh();
                else
                    pl.durex.client.module.MsgBotModule.startAutoCh();
                soundToggle(pl.durex.client.module.MsgBotModule.isAutoCh());
            }
            return true;
        } sy += SUB_H + 3;
        // Wiersz 8: Only Netherite
        if (inSub(mx, my, px, sy)) {
            if (button == 0) {
                pl.durex.client.module.MsgBotModule.onlyNetherite = !pl.durex.client.module.MsgBotModule.onlyNetherite;
                soundToggle(pl.durex.client.module.MsgBotModule.onlyNetherite);
                pl.durex.client.DurexClient.saveNow();
            }
            return true;
        } sy += SUB_H + 3;
        // Wiersz 9: Stiv bez Premki
        if (inSub(mx, my, px, sy)) {
            if (button == 0) {
                pl.durex.client.module.MsgBotModule.onlyOffline = !pl.durex.client.module.MsgBotModule.onlyOffline;
                soundToggle(pl.durex.client.module.MsgBotModule.onlyOffline);
                pl.durex.client.DurexClient.saveNow();
            }
            return true;
        }
        return false;
    }

    private void saveMsgBotFields() {
        pl.durex.client.module.MsgBotModule.message      = msgBotMsgInput;
        pl.durex.client.module.MsgBotModule.targetPlayer = msgBotNickInput;
        try { pl.durex.client.module.MsgBotModule.chDelaySec = Math.max(5, Integer.parseInt(msgBotChDelay)); }
        catch (NumberFormatException ignored) {}
    }

    private boolean handleZbrojmistrzSub(double mx, double my, int px, int sy, int button) {
        // Set Position
        if (inSub(mx, my, px, sy)) {
            if (button == 0) { settingZbrojmistrz = !settingZbrojmistrz; soundClick(); }
            return true;
        } sy += SUB_H + 3;
        // Pokaż księgi
        if (inSub(mx, my, px, sy)) {
            if (button == 0) {
                pl.durex.client.module.ZbrojmistrzModule.setShowBooks(
                    !pl.durex.client.module.ZbrojmistrzModule.isShowBooks());
                soundToggle(pl.durex.client.module.ZbrojmistrzModule.isShowBooks());
                pl.durex.client.DurexClient.saveNow();
            }
            return true;
        }
        return false;
    }

    private boolean handleNametagsSub(double mx, double my, int px, int sy, int button) {
        // HP
        if (inSub(mx, my, px, sy)) {
            if (button == 0) { pl.durex.client.module.NametagsModule.setShowHp(!pl.durex.client.module.NametagsModule.isShowHp()); soundToggle(pl.durex.client.module.NametagsModule.isShowHp()); pl.durex.client.DurexClient.saveNow(); }
            return true;
        } sy += SUB_H + 3;
        // Dystans
        if (inSub(mx, my, px, sy)) {
            if (button == 0) { pl.durex.client.module.NametagsModule.setShowDistance(!pl.durex.client.module.NametagsModule.isShowDistance()); soundToggle(pl.durex.client.module.NametagsModule.isShowDistance()); pl.durex.client.DurexClient.saveNow(); }
            return true;
        } sy += SUB_H + 3;
        // Zbroja
        if (inSub(mx, my, px, sy)) {
            if (button == 0) { pl.durex.client.module.NametagsModule.setShowArmor(!pl.durex.client.module.NametagsModule.isShowArmor()); soundToggle(pl.durex.client.module.NametagsModule.isShowArmor()); pl.durex.client.DurexClient.saveNow(); }
            return true;
        } sy += SUB_H + 3;
        // Kolor nicku
        if (inSub(mx, my, px, sy)) {
            if (button == 0) { pl.durex.client.module.NametagsModule.setNickColorIdx(pl.durex.client.module.NametagsModule.getNickColorIdx() + 1); soundClick(); pl.durex.client.DurexClient.saveNow(); }
            else if (button == 1) { pl.durex.client.module.NametagsModule.setNickColorIdx(pl.durex.client.module.NametagsModule.getNickColorIdx() - 1); soundClick(); pl.durex.client.DurexClient.saveNow(); }
            return true;
        } sy += SUB_H + 3;
        // Max dystans — suwak
        if (my >= sy && my <= sy + SUB_H) {
            if (button == 0) { draggingSlider = "nametags_dist"; sliderPx = px; applySlider("nametags_dist", mx, px); }
            return true;
        }
        return false;
    }

    private boolean handleTracersSub(double mx, double my, int px, int sy, int button) {
        // Kolor
        if (inSub(mx, my, px, sy)) {
            if (button == 0) { pl.durex.client.module.TracerModule.setColorIdx(pl.durex.client.module.TracerModule.getColorIdx() + 1); soundClick(); pl.durex.client.DurexClient.saveNow(); }
            else if (button == 1) { pl.durex.client.module.TracerModule.setColorIdx(pl.durex.client.module.TracerModule.getColorIdx() - 1); soundClick(); pl.durex.client.DurexClient.saveNow(); }
            return true;
        } sy += SUB_H + 3;
        // Max dystans — suwak
        if (my >= sy && my <= sy + SUB_H) {
            if (button == 0) { draggingSlider = "tracers_dist"; sliderPx = px; applySlider("tracers_dist", mx, px); }
            return true;
        }
        return false;
    }

    private boolean handleAutoDripstoneSub(double mx, double my, int px, int sy, int button) {
        // Suwak prędkości: LPM = +1, PPM = -1
        if (inSub(mx, my, px, sy)) {
            if (button == 0) {
                pl.durex.client.module.AutoDripstoneModule.setSpeed(
                    pl.durex.client.module.AutoDripstoneModule.getSpeed() + 1);
                soundClick(); pl.durex.client.DurexClient.saveNow();
            } else if (button == 1) {
                pl.durex.client.module.AutoDripstoneModule.setSpeed(
                    pl.durex.client.module.AutoDripstoneModule.getSpeed() - 1);
                soundClick(); pl.durex.client.DurexClient.saveNow();
            }
            return true;
        }
        return false;
    }

    private pl.durex.client.module.LeverCobwebModule.SpeedProfile getNextSpeedProfile(pl.durex.client.module.LeverCobwebModule.SpeedProfile current) {
        pl.durex.client.module.LeverCobwebModule.SpeedProfile[] profiles = pl.durex.client.module.LeverCobwebModule.SpeedProfile.values();
        for (int i = 0; i < profiles.length; i++) {
            if (profiles[i] == current) {
                return profiles[(i + 1) % profiles.length];
            }
        }
        return pl.durex.client.module.LeverCobwebModule.SpeedProfile.FAST;
    }

    // ── Render ────────────────────────────────────────────────────────────

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        // Tick + aktualizuj pozycje modów w kategoriach (pomijaj dragowany mod)
        for (CategoryWidget cat : categories) {
            cat.tick();
            int modY = (int)cat.ty + CAT_H + 2;
            for (ModWidget m : cat.mods) {
                // Nie nadpisuj pozycji modułu który jest właśnie dragowany
                if (m != activeModDrag) {
                    m.tx = cat.tx + 10; m.ty = modY;
                }
                modY += MOD_H + 2 + getSubHStatic(m);
            }
        }
        for (ModWidget m : freeMods) m.tick();

        // Collision avoidance — odpychanie modułów i kategorii od siebie i od krawędzi
        if (activeModDrag == null && activeCatDrag == null) {
            resolveCollisions();
        }

        // Intro animation
        if (firstOpen) {
            introAnim += 0.08f;
            if (introAnim >= 1f) { introAnim = 1f; firstOpen = false; }
        } else {
            introAnim = 1f;
        }

        // Fade background - lżejsze tło
        int bgAlpha = (int)(0x66 * introAnim);
        ctx.fillGradient(0, 0, width, height, (bgAlpha << 24), (bgAlpha << 24));

        // HUD overlays
        if (settingPosition) {
            int hx = cooldownHud.getHudX(), hy = cooldownHud.getHudY();
            GuiRenderUtils.drawRoundedRect(ctx, hx - 2, hy - 2, 64, 30, 4, 0xAA001133);
            GuiRenderUtils.drawRoundedOutline(ctx, hx - 2, hy - 2, 64, 30, 4, 1, COL_BLUE);
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Cooldown HUD"), hx + 30, hy + 8, COL_BLUE);
        }
        if (settingProcenciarz) {
            int hx = procenciarz.getHudX(), hy = procenciarz.getHudY();
            GuiRenderUtils.drawRoundedRect(ctx, hx - 2, hy - 2, 80, 30, 4, 0xAA001133);
            GuiRenderUtils.drawRoundedOutline(ctx, hx - 2, hy - 2, 80, 30, 4, 1, COL_BLUE);
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Procenciarz"), hx + 38, hy + 8, COL_BLUE);
        }

        // Main panel
        int ph = calcPanelH();
        GuiRenderUtils.drawShadow(ctx, panelX, panelY, W, ph, 8, 0xBB8800EE, 12);
        GuiRenderUtils.drawRoundedRect(ctx, panelX, panelY, W, ph, 8, COL_BG);
        GuiRenderUtils.drawRoundedOutline(ctx, panelX, panelY, W, ph, 8, 1, COL_BORDER);
        // Header
        GuiRenderUtils.drawRoundedRect(ctx, panelX, panelY, W, CAT_H, 8, COL_HEADER);
        ctx.drawTextWithShadow(textRenderer, Text.literal("✦ Durex Client"), panelX + 10, panelY + (CAT_H - 8) / 2, COL_ACCENT);
        String expiry = LicenseManager.getInstance().getDaysLeftText();
        ctx.drawTextWithShadow(textRenderer, Text.literal(expiry), panelX + W - textRenderer.getWidth(expiry) - 28, panelY + (CAT_H - 8) / 2, COL_MUTED);
        // Reset button
        boolean resetHov = mx >= panelX + W - 18 && mx <= panelX + W - 6 && my >= panelY + 4 && my <= panelY + CAT_H - 4;
        GuiRenderUtils.drawRoundedRect(ctx, panelX + W - 18, panelY + 4, 12, CAT_H - 8, 3, resetHov ? 0xAA440010 : 0x33220010);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("⟳"), panelX + W - 12, panelY + (CAT_H - 8) / 2, resetHov ? 0xFFFF6677 : 0x88FF4455);

        // Docked categories
        for (CategoryWidget cat : categories) {
            if (!cat.docked) continue;
            renderCategory(ctx, mx, my, cat);
        }

        // Przyciski na dole panelu: [+ Kategoria] [⊞ Moduły]
        int addY = panelY + ph - FOOTER_H - CAT_H - 4;
        int halfW = (W - PAD * 2 - 4) / 2;
        // + Kategoria
        boolean addCatH = mx >= panelX + PAD && mx <= panelX + PAD + halfW && my >= addY && my <= addY + CAT_H;
        GuiRenderUtils.drawRoundedRect(ctx, panelX + PAD, addY, halfW, CAT_H, 4, addCatH ? 0x44220044 : 0x22110022);
        GuiRenderUtils.drawRoundedOutline(ctx, panelX + PAD, addY, halfW, CAT_H, 4, 1, 0x44AA44FF);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("+ Kategoria"), panelX + PAD + halfW / 2, addY + (CAT_H - 8) / 2, 0xFF88AAFF);
        // ⊞ Moduły
        int libBtnX = panelX + PAD + halfW + 4;
        boolean libBtnH = mx >= libBtnX && mx <= libBtnX + halfW && my >= addY && my <= addY + CAT_H;
        GuiRenderUtils.drawRoundedRect(ctx, libBtnX, addY, halfW, CAT_H, 4, (libOpen || libBtnH) ? 0x44220044 : 0x22110022);
        GuiRenderUtils.drawRoundedOutline(ctx, libBtnX, addY, halfW, CAT_H, 4, 1, libOpen ? COL_BORDER : 0x44AA44FF);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("⊞ Moduły"), libBtnX + halfW / 2, addY + (CAT_H - 8) / 2, libOpen ? COL_ACCENT : 0xFF88AAFF);

        // Footer
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("LPM toggle | PPM opcje | Przeciagnij = wyciagnij"),
                panelX + W / 2, panelY + ph - FOOTER_H + 3, COL_MUTED);

        // Floating categories
        for (CategoryWidget cat : categories) {
            if (cat.docked) continue;
            renderCategory(ctx, mx, my, cat);
        }

        // Free floating mods
        for (ModWidget m : freeMods) renderMod(ctx, mx, my, m, (int)m.rx, (int)m.ry, true);

        // Dragged mod on top
        if (activeModDrag != null && activeModDrag.dragStarted) {
            renderMod(ctx, mx, my, activeModDrag, (int)activeModDrag.rx, (int)activeModDrag.ry, true);
        }

        // Biblioteka modułów (overlay)
        if (libOpen) renderLibrary(ctx, mx, my);

        // Tutorial overlay
        if (showTutorial) {
            long elapsed = System.currentTimeMillis() - tutorialStartMs;
            if (elapsed >= TUTORIAL_DURATION_MS) {
                showTutorial = false;
            } else {
                renderTutorial(ctx, elapsed);
            }
        }

        // Fioletowa ramka ekranu
        int brd = 2;
        ctx.fill(0, 0, width, brd, 0x668800EE);           // góra
        ctx.fill(0, height - brd, width, height, 0x668800EE); // dół
        ctx.fill(0, 0, brd, height, 0x668800EE);           // lewo
        ctx.fill(width - brd, 0, width, height, 0x668800EE); // prawo
        // Narożniki jaśniejsze
        ctx.fill(0, 0, 6, 6, 0xAA8800EE);
        ctx.fill(width - 6, 0, width, 6, 0xAA8800EE);
        ctx.fill(0, height - 6, 6, height, 0xAA8800EE);
        ctx.fill(width - 6, height - 6, width, height, 0xAA8800EE);

        super.render(ctx, mx, my, delta);

        // Okno nazwy nowej kategorii - na samym wierzchu
        if (namingNewCat) {
            int ox = width / 2 - 100, oy = height / 2 - 30;
            ctx.fillGradient(0, 0, width, height, 0x88000000, 0x88000000);
            GuiRenderUtils.drawShadow(ctx, ox, oy, 200, 60, 6, 0xBB8800EE, 10);
            GuiRenderUtils.drawRoundedRect(ctx, ox, oy, 200, 60, 6, 0xEE0D0020);
            GuiRenderUtils.drawRoundedOutline(ctx, ox, oy, 200, 60, 6, 1, COL_BORDER);
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Nazwa kategorii"), ox + 100, oy + 8, COL_ACCENT);
            // Input field
            String input = newCatNameBuffer.toString() + "|";
            GuiRenderUtils.drawRoundedRect(ctx, ox + 10, oy + 22, 180, 18, 3, 0x661A0040);
            GuiRenderUtils.drawRoundedOutline(ctx, ox + 10, oy + 22, 180, 18, 3, 1, COL_ACCENT);
            ctx.drawTextWithShadow(textRenderer, Text.literal(input), ox + 14, oy + 27, COL_TEXT);
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Enter = zatwierdź  |  ESC = anuluj"), ox + 100, oy + 46, COL_MUTED);
        }
    }

    private void renderCategory(DrawContext ctx, int mx, int my, CategoryWidget cat) {
        int cx = (int)cat.rx, cy = (int)cat.ry;
        int ch = cat.totalH();
        boolean isDropTarget = cat == dropTargetCat;

        // Shadow + bg
        GuiRenderUtils.drawShadow(ctx, cx, cy, W, ch, 6, 0x998800EE, 8);
        GuiRenderUtils.drawRoundedRect(ctx, cx, cy, W, ch, 6, COL_BG);
        GuiRenderUtils.drawRoundedOutline(ctx, cx, cy, W, ch, 6, isDropTarget ? 2 : 1, isDropTarget ? 0xFF44FF88 : COL_BORDER);

        // Header
        boolean hov = mx >= cx && mx <= cx + W && my >= cy && my <= cy + CAT_H;
        GuiRenderUtils.drawRoundedRect(ctx, cx, cy, W, CAT_H, 6, hov ? 0xFF1E0040 : COL_CAT_BG);

        // Expand arrow
        ctx.drawTextWithShadow(textRenderer, Text.literal(cat.expanded ? "▼" : "▶"), cx + 8, cy + (CAT_H - 8) / 2, COL_MUTED);

        // Name (editable)
        String catName = cat.editingName ? (cat.nameBuffer + "|") : cat.name;
        ctx.drawTextWithShadow(textRenderer, Text.literal(catName), cx + 22, cy + (CAT_H - 8) / 2, cat.editingName ? COL_ACCENT : COL_TEXT);

        // Mod count badge
        String badge = String.valueOf(cat.mods.size());
        int bw = textRenderer.getWidth(badge) + 8;
        GuiRenderUtils.drawRoundedRect(ctx, cx + W - bw - 6, cy + (CAT_H - 12) / 2, bw, 12, 3, 0x44AA44FF);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(badge), cx + W - bw / 2 - 6, cy + (CAT_H - 8) / 2, COL_ACCENT);

        // X button (delete category)
        int xBtnX = cx + W - 18;
        int xBtnY = cy + (CAT_H - 12) / 2;
        boolean xHov = mx >= xBtnX && mx <= xBtnX + 12 && my >= xBtnY && my <= xBtnY + 12;
        GuiRenderUtils.drawRoundedRect(ctx, xBtnX, xBtnY, 12, 12, 3, xHov ? 0xAA440010 : 0x44220010);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("✕"), xBtnX + 6, xBtnY + 2, xHov ? COL_OFF : 0x88FF4455);

        // Mods inside - scissor do aktualnej wysokości kategorii
        if (cat.expandAnim > 0.01f) {
            int clipBottom = cy + ch;
            ctx.enableScissor(cx, cy + CAT_H, cx + W + 50, clipBottom);
            for (ModWidget m : cat.mods) {
                if (m != activeModDrag) renderMod(ctx, mx, my, m, (int)m.rx, (int)m.ry, false);
            }
            ctx.disableScissor();
        }
    }

    private void renderMod(DrawContext ctx, int mx, int my, ModWidget m, int x, int y, boolean floating) {
        boolean enabled = isModEnabled(m);
        boolean hov = mx >= x && mx <= x + W - 10 && my >= y && my <= y + MOD_H;
        int bg = floating ? 0xEE0D0020 : (hov ? COL_HOVER : COL_MOD_BG);
        int border = floating ? (m.dragging ? 0xFFAA44FF : COL_BORDER) : (hov ? 0x44AA44FF : 0x22660099);

        if (floating) GuiRenderUtils.drawShadow(ctx, x, y, W - 10, MOD_H, 4, 0x998800EE, 6);
        GuiRenderUtils.drawRoundedRect(ctx, x, y, W - 10, MOD_H, 4, bg);
        GuiRenderUtils.drawRoundedOutline(ctx, x, y, W - 10, MOD_H, 4, 1, border);

        // Icon
        ctx.drawTextWithShadow(textRenderer, Text.literal(m.icon), x + 6, y + (MOD_H - 8) / 2, enabled ? COL_ON : COL_MUTED);

        // Name
        ctx.drawTextWithShadow(textRenderer, Text.literal(getModLabel(m)), x + 20, y + (MOD_H - 8) / 2, COL_TEXT);

        // ON/OFF badge
        String st = enabled ? "ON" : "OFF";
        int stColor = enabled ? COL_ON : COL_OFF;
        int stW = textRenderer.getWidth(st) + 6;
        GuiRenderUtils.drawRoundedRect(ctx, x + W - 10 - stW - 4, y + (MOD_H - 12) / 2, stW, 12, 3, enabled ? 0x3344FF88 : 0x33FF4455);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(st), x + W - 10 - stW / 2 - 4, y + (MOD_H - 8) / 2, stColor);

        // Expand arrow
        if (getSubH(m) > 0 || m.id.equals("antiKowal") || m.id.equals("cooldowns") || m.id.equals("antiKostka") || m.id.equals("viewModel") || m.id.equals("procenciarz") || m.id.equals("leverCobweb") || m.id.equals("autoDripstone") || m.id.equals("msgBot")) {
            ctx.drawTextWithShadow(textRenderer, Text.literal(m.expanded ? "▲" : "▼"), x + W - 10 - stW - 14, y + (MOD_H - 8) / 2, COL_MUTED);
        }

        // Sub-content
        if (m.expandAnim > 0.01f && m.expanded) {
            int sy = y + MOD_H + 2;
            renderModSub(ctx, mx, my, m, x, sy);
        }
    }

    private void renderModSub(DrawContext ctx, int mx, int my, ModWidget m, int px, int sy) {
        switch (m.id) {
            case "antiKowal" -> {
                drawSub(ctx, mx, my, px, sy, "Hide Players", pl.durex.client.util.RaycastState.active ? "ON" : "OFF", pl.durex.client.util.RaycastState.active ? COL_ON : COL_OFF, false); sy += SUB_H + 3;
                drawSub(ctx, mx, my, px, sy, waitingAntiKowalBind ? ">> Klawisz..." : "AntiKowal Bind", waitingAntiKowalBind ? "" : "[" + antiKowal.getBindName() + "]", COL_BIND, waitingAntiKowalBind); sy += SUB_H + 3;
                drawSub(ctx, mx, my, px, sy, waitingFriendBind ? ">> Klawisz..." : "Friend Bind", waitingFriendBind ? "" : "[" + friendModule.getAddKeyName() + "]", COL_BIND, waitingFriendBind); sy += SUB_H + 3;
                drawSub(ctx, mx, my, px, sy, "Clear Friends (" + friendModule.getFriends().size() + ")", "", COL_OFF, false);
            }
            case "cooldowns" -> {
                drawSub(ctx, mx, my, px, sy, "Show HUD", cooldownHud.isEnabled() ? "ON" : "OFF", cooldownHud.isEnabled() ? COL_ON : COL_OFF, false); sy += SUB_H + 3;
                boolean spH = inSub(mx, my, px, sy);
                GuiRenderUtils.drawRoundedRect(ctx, px + PAD, sy, W - PAD * 2 - 10, SUB_H, 3, spH ? COL_HOVER : COL_SUB);
                GuiRenderUtils.drawRoundedOutline(ctx, px + PAD, sy, W - PAD * 2 - 10, SUB_H, 3, 1, settingPosition ? COL_BLUE : 0x664499FF);
                ctx.drawTextWithShadow(textRenderer, Text.literal("Set Position"), px + PAD + 8, sy + (SUB_H - 8) / 2, settingPosition ? COL_BLUE : 0xFFAADDFF);
            }
            case "antiKostka" -> {
                // Delay
                boolean dH = inSub(mx, my, px, sy);
                GuiRenderUtils.drawRoundedRect(ctx, px + PAD, sy, W - PAD * 2 - 10, SUB_H, 3, dH ? COL_HOVER : COL_SUB);
                GuiRenderUtils.drawRoundedOutline(ctx, px + PAD, sy, W - PAD * 2 - 10, SUB_H, 3, 1, COL_ACCENT);
                ctx.drawTextWithShadow(textRenderer, Text.literal("Delay"), px + PAD + 6, sy + (SUB_H - 8) / 2, COL_MUTED);
                ctx.drawTextWithShadow(textRenderer, Text.literal(antiKostka.getDelayMode().label), px + PAD + 50, sy + (SUB_H - 8) / 2, COL_ACCENT);
                ctx.drawTextWithShadow(textRenderer, Text.literal(delayExpanded ? "▲" : "▼"), px + W - PAD - 18, sy + (SUB_H - 8) / 2, COL_MUTED);
                sy += SUB_H + 3;
                if (delayExpanded) {
                    for (AntiKostkaModule.DelayMode mode : AntiKostkaModule.DelayMode.values()) {
                        boolean sel = antiKostka.getDelayMode() == mode;
                        boolean mH = inSub(mx, my, px, sy);
                        GuiRenderUtils.drawRoundedRect(ctx, px + PAD + 8, sy, W - PAD * 2 - 18, SUB_H, 3, mH ? COL_HOVER : (sel ? 0x88220044 : 0x33110022));
                        if (sel) GuiRenderUtils.drawRoundedOutline(ctx, px + PAD + 8, sy, W - PAD * 2 - 18, SUB_H, 3, 1, COL_ACCENT);
                        ctx.drawTextWithShadow(textRenderer, Text.literal(mode.label), px + PAD + 14, sy + (SUB_H - 8) / 2, sel ? COL_ACCENT : COL_TEXT);
                        if (sel) ctx.drawTextWithShadow(textRenderer, Text.literal("✔"), px + W - PAD - 18, sy + (SUB_H - 8) / 2, COL_ON);
                        sy += SUB_H + 3;
                    }
                }
                // Slots
                for (int i = 0; i < antiKostka.getSlots().size(); i++) {
                    AntiKostkaModule.HotbarSlot slot = antiKostka.getSlots().get(i);
                    boolean waiting = waitingHotbarBind == i, editing = editingSlotName == i;
                    String label = editing ? (nameBuffer + "|") : (waiting ? ">> Klawisz..." : slot.name);
                    drawSub(ctx, mx, my, px, sy, label, (editing || waiting) ? "" : "[" + slot.getLoadKeyName() + "]", editing ? COL_ACCENT : COL_BIND, waiting || editing);
                    sy += SUB_H + 3;
                    GuiRenderUtils.drawRoundedRect(ctx, px + PAD, sy, W - PAD * 2 - 10, HOTBAR_H, 3, 0x44110022);
                    if (slot.hasSaved) {
                        int iconX = px + PAD + 3, iconY = sy + (HOTBAR_H - ICON_SIZE) / 2;
                        for (int j = 0; j < 9; j++) {
                            ItemStack stack = slot.items[j];
                            if (stack != null && !stack.isEmpty()) ctx.drawItem(stack, iconX + j * (ICON_SIZE + 1), iconY);
                        }
                    } else {
                        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Brak zapisu"), px + W / 2, sy + (HOTBAR_H - 8) / 2, 0x44FFFFFF);
                    }
                    sy += HOTBAR_H + 3;
                    int btnW = (W - PAD * 2 - 10 - 4) / 2;
                    boolean saveH = mx >= px + PAD && mx <= px + PAD + btnW && my >= sy && my <= sy + SUB_H;
                    boolean delH  = mx >= px + PAD + btnW + 4 && mx <= px + PAD + btnW * 2 + 4 && my >= sy && my <= sy + SUB_H;
                    GuiRenderUtils.drawRoundedRect(ctx, px + PAD, sy, btnW, SUB_H, 3, saveH ? COL_HOVER : COL_SUB);
                    ctx.drawTextWithShadow(textRenderer, Text.literal("Save"), px + PAD + 6, sy + (SUB_H - 8) / 2, COL_ON);
                    GuiRenderUtils.drawRoundedRect(ctx, px + PAD + btnW + 4, sy, btnW, SUB_H, 3, delH ? 0xAA400010 : 0x66200010);
                    ctx.drawTextWithShadow(textRenderer, Text.literal("Delete"), px + PAD + btnW + 10, sy + (SUB_H - 8) / 2, COL_OFF);
                    sy += SUB_H + 6;
                }
                if (antiKostka.getSlots().size() < AntiKostkaModule.MAX_SLOTS) {
                    boolean addH = inSub(mx, my, px, sy);
                    GuiRenderUtils.drawRoundedRect(ctx, px + PAD, sy, W - PAD * 2 - 10, SUB_H, 3, addH ? COL_HOVER : 0x44001133);
                    ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("+ Dodaj slot"), px + W / 2, sy + (SUB_H - 8) / 2, COL_BLUE);
                }
            }
            case "viewModel" -> {
                net.minecraft.util.Arm mainArm = client != null && client.player != null ? client.player.getMainArm() : net.minecraft.util.Arm.RIGHT;
                boolean isMain = viewModel.getActiveHand() == ViewModelModule.Hand.RIGHT;
                String mainLabel = mainArm == net.minecraft.util.Arm.LEFT ? "Lewa (Main)" : "Prawa (Main)";
                String offLabel  = mainArm == net.minecraft.util.Arm.LEFT ? "Prawa (Off)" : "Lewa (Off)";
                boolean handH = inSub(mx, my, px, sy);
                GuiRenderUtils.drawRoundedRect(ctx, px + PAD, sy, W - PAD * 2 - 10, SUB_H, 3, handH ? COL_HOVER : COL_SUB);
                ctx.drawTextWithShadow(textRenderer, Text.literal("Reka: " + (isMain ? mainLabel : offLabel)), px + PAD + 6, sy + (SUB_H - 8) / 2, COL_BIND);
                sy += SUB_H + 3;
                String[] labels = {"Rot X","Rot Y","Rot Z","Pos X","Pos Y","Pos Z","Scale"};
                float[] values = {viewModel.getRotX(), viewModel.getRotY(), viewModel.getRotZ(), viewModel.getPosX(), viewModel.getPosY(), viewModel.getPosZ(), viewModel.getScale()};
                float[] mins = {ViewModelModule.ROT_MIN,ViewModelModule.ROT_MIN,ViewModelModule.ROT_MIN,ViewModelModule.POS_MIN,ViewModelModule.POS_MIN,ViewModelModule.POS_MIN,ViewModelModule.SCALE_MIN};
                float[] maxs = {ViewModelModule.ROT_MAX,ViewModelModule.ROT_MAX,ViewModelModule.ROT_MAX,ViewModelModule.POS_MAX,ViewModelModule.POS_MAX,ViewModelModule.POS_MAX,ViewModelModule.SCALE_MAX};
                for (int i = 0; i < labels.length; i++) { drawSlider(ctx, mx, my, px, sy, labels[i], values[i], mins[i], maxs[i]); sy += SUB_H + 3; }
                boolean rH = mx >= px + PAD && mx <= px + W - PAD - 10 && my >= sy && my <= sy + SUB_H;
                GuiRenderUtils.drawRoundedRect(ctx, px + PAD, sy, W - PAD * 2 - 10, SUB_H, 3, rH ? 0xAA400010 : 0x66200010);
                ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Reset " + (isMain ? mainLabel : offLabel)), px + W / 2, sy + (SUB_H - 8) / 2, COL_OFF);
            }
            case "procenciarz" -> {
                boolean spH = inSub(mx, my, px, sy);
                GuiRenderUtils.drawRoundedRect(ctx, px + PAD, sy, W - PAD * 2 - 10, SUB_H, 3, spH ? COL_HOVER : 0x66001133);
                GuiRenderUtils.drawRoundedOutline(ctx, px + PAD, sy, W - PAD * 2 - 10, SUB_H, 3, 1, settingProcenciarz ? COL_BLUE : 0x664499FF);
                ctx.drawTextWithShadow(textRenderer, Text.literal("Set Position"), px + PAD + 6, sy + (SUB_H - 8) / 2, settingProcenciarz ? COL_BLUE : 0xFFAADDFF);
                sy += SUB_H + 3;
                boolean pb = pl.durex.client.module.ProcenciarzModule.isShowBooks();
                drawSub(ctx, mx, my, px, sy, "Pokaż księgi",
                    pb ? "ON" : "OFF", pb ? COL_ON : COL_MUTED, false);
            }
            case "leverCobweb" -> {
                drawSub(ctx, mx, my, px, sy, "Szybkosc", leverCobweb.getSpeedProfile().name, COL_ACCENT, false); sy += SUB_H + 3;
                drawSub(ctx, mx, my, px, sy, "Stawianie Przez Gracza", leverCobweb.isPlayerModeEnabled() ? "Wlaczone" : "Wylaczone", leverCobweb.isPlayerModeEnabled() ? COL_ON : COL_OFF, false); sy += SUB_H + 3;
                drawSub(ctx, mx, my, px, sy, "Weby", leverCobweb.isWebModeEnabled() ? "Wlaczone" : "Wylaczone", leverCobweb.isWebModeEnabled() ? COL_ON : COL_OFF, false); sy += SUB_H + 3;
                drawSub(ctx, mx, my, px, sy, "Do Miecza", leverCobweb.isSwitchToBestSwordEnabled() ? "Wlaczone" : "Wylaczone", leverCobweb.isSwitchToBestSwordEnabled() ? COL_ON : COL_OFF, false); sy += SUB_H + 3;
                drawSub(ctx, mx, my, px, sy, "Auto Lever", leverCobweb.isLeverOnlyModeEnabled() ? "Wlaczone" : "Wylaczone", leverCobweb.isLeverOnlyModeEnabled() ? COL_ON : COL_OFF, false);
            }
            case "zbrojmistrz" -> {
                boolean sp = settingZbrojmistrz;
                GuiRenderUtils.drawRoundedRect(ctx, px + PAD, sy, W - PAD * 2 - 10, SUB_H, 3, sp ? COL_HOVER : 0x66001133);
                GuiRenderUtils.drawRoundedOutline(ctx, px + PAD, sy, W - PAD * 2 - 10, SUB_H, 3, 1, sp ? COL_BLUE : 0x664499FF);
                ctx.drawTextWithShadow(textRenderer, Text.literal("Set Position"), px + PAD + 6, sy + (SUB_H - 8) / 2, sp ? COL_BLUE : 0xFFAADDFF);
                sy += SUB_H + 3;
                boolean books = pl.durex.client.module.ZbrojmistrzModule.isShowBooks();
                drawSub(ctx, mx, my, px, sy, "Pokaż księgi",
                    books ? "ON" : "OFF", books ? COL_ON : COL_MUTED, false);
            }
            case "nametags" -> {
                boolean hp  = pl.durex.client.module.NametagsModule.isShowHp();
                boolean dst = pl.durex.client.module.NametagsModule.isShowDistance();
                boolean arm = pl.durex.client.module.NametagsModule.isShowArmor();
                int cidx    = pl.durex.client.module.NametagsModule.getNickColorIdx();
                float maxD  = pl.durex.client.module.NametagsModule.getMaxDistance();
                String nickPreview = pl.durex.client.module.NametagsModule.getNickColor() + "Nick";
                drawSub(ctx, mx, my, px, sy, "Pokaż HP",      hp  ? "ON" : "OFF", hp  ? COL_ON : COL_MUTED, false); sy += SUB_H + 3;
                drawSub(ctx, mx, my, px, sy, "Pokaż dystans", dst ? "ON" : "OFF", dst ? COL_ON : COL_MUTED, false); sy += SUB_H + 3;
                drawSub(ctx, mx, my, px, sy, "Pokaż zbroję",  arm ? "ON" : "OFF", arm ? COL_ON : COL_MUTED, false); sy += SUB_H + 3;
                drawSub(ctx, mx, my, px, sy, "Kolor nicku",
                    pl.durex.client.module.NametagsModule.NICK_COLOR_NAMES[cidx] + " " + nickPreview,
                    COL_ACCENT, false); sy += SUB_H + 3;
                drawSlider(ctx, mx, my, px, sy, "Zasięg", maxD, 8f, 256f);
            }
            case "tracers" -> {
                int cidx = pl.durex.client.module.TracerModule.getColorIdx();
                float maxD = pl.durex.client.module.TracerModule.getMaxDistance();
                float[] col = pl.durex.client.module.TracerModule.getColor();
                int previewColor = 0xFF000000 | ((int)(col[0]*255) << 16) | ((int)(col[1]*255) << 8) | (int)(col[2]*255);
                drawSub(ctx, mx, my, px, sy, "Kolor",
                    pl.durex.client.module.TracerModule.COLOR_NAMES[cidx],
                    previewColor, false); sy += SUB_H + 3;
                drawSlider(ctx, mx, my, px, sy, "Zasięg", maxD, 8f, 512f);
            }
            case "autoDripstone" -> {
                int spd = pl.durex.client.module.AutoDripstoneModule.getSpeed();                String spdLabel = spd + "/s  (LPM +1 | PPM -1)";
                drawSub(ctx, mx, my, px, sy, "Szybkosc", spdLabel, COL_ACCENT, false);
            }
            case "msgBot" -> {
                // Wiersz 1: wiadomość
                boolean e1 = msgBotEditingField == 1;
                drawSub(ctx, mx, my, px, sy,
                    e1 ? ">> Wiadomosc:" : "Wiadomosc:",
                    e1 ? (msgBotMsgInput + "|") : (msgBotMsgInput.isEmpty() ? "(puste)" : msgBotMsgInput),
                    e1 ? COL_ACCENT : COL_BIND, e1); sy += SUB_H + 3;
                // Wiersz 2: nick
                boolean e2 = msgBotEditingField == 2;
                drawSub(ctx, mx, my, px, sy,
                    e2 ? ">> Nick:" : "Nick:",
                    e2 ? (msgBotNickInput + "|") : (msgBotNickInput.isEmpty() ? "(wszyscy)" : msgBotNickInput),
                    e2 ? COL_ACCENT : COL_BIND, e2); sy += SUB_H + 3;
                // Wiersz 3: ch delay
                boolean e3 = msgBotEditingField == 3;
                drawSub(ctx, mx, my, px, sy,
                    e3 ? ">> CH co (s):" : "CH co (s):",
                    e3 ? (msgBotChDelay + "|") : msgBotChDelay,
                    e3 ? COL_ACCENT : COL_MUTED, e3); sy += SUB_H + 3;
                // Wiersz 4: START/STOP
                boolean spam = pl.durex.client.module.MsgBotModule.isSpamming();
                drawSub(ctx, mx, my, px, sy,
                    spam ? "STOP spam" : "START spam",
                    "Spamowanych: " + pl.durex.client.module.MsgBotModule.getTotalSpammed(),
                    spam ? COL_OFF : COL_ON, false); sy += SUB_H + 3;
                // Wiersz 5: Reset listy
                drawSub(ctx, mx, my, px, sy, "Reset listy", "", COL_MUTED, false); sy += SUB_H + 3;
                // Wiersz 6: AFK Mode
                boolean afk = pl.durex.client.module.MsgBotModule.isAfk();
                drawSub(ctx, mx, my, px, sy, "AFK Mode",
                    afk ? "ON" : "OFF", afk ? COL_ON : COL_OFF, false); sy += SUB_H + 3;
                // Wiersz 7: CH
                boolean ch = pl.durex.client.module.MsgBotModule.isAutoCh();
                drawSub(ctx, mx, my, px, sy, "Auto CH",
                    ch ? "ON" : "OFF", ch ? COL_ON : COL_OFF, false); sy += SUB_H + 3;
                // Wiersz 8: Only Netherite
                boolean neth = pl.durex.client.module.MsgBotModule.onlyNetherite;
                drawSub(ctx, mx, my, px, sy, "Only Netherite",
                    neth ? "ON" : "OFF", neth ? COL_ON : COL_MUTED, false); sy += SUB_H + 3;
                // Wiersz 9: Stiv bez Premki
                boolean offline = pl.durex.client.module.MsgBotModule.onlyOffline;
                drawSub(ctx, mx, my, px, sy, "🤡 Stiv bez Premki",
                    offline ? "ON" : "OFF", offline ? COL_ON : COL_MUTED, false);
            }
        }
    }

    private void drawSub(DrawContext ctx, int mx, int my, int px, int sy, String label, String value, int color, boolean active) {
        boolean h = inSub(mx, my, px, sy);
        GuiRenderUtils.drawRoundedRect(ctx, px + PAD, sy, W - PAD * 2 - 10, SUB_H, 3, h ? COL_HOVER : COL_SUB);
        ctx.drawTextWithShadow(textRenderer, Text.literal(label), px + PAD + 6, sy + (SUB_H - 8) / 2, active ? color : COL_TEXT);
        if (!value.isEmpty()) ctx.drawTextWithShadow(textRenderer, Text.literal(value), px + W - PAD - 10 - textRenderer.getWidth(value), sy + (SUB_H - 8) / 2, color);
    }

    private void drawSlider(DrawContext ctx, int mx, int my, int px, int sy, String label, float value, float min, float max) {
        int labelW = 50; int barX = px + PAD + labelW; int barW = W - PAD * 2 - 10 - labelW;
        float t = (value - min) / (max - min);
        GuiRenderUtils.drawRoundedRect(ctx, px + PAD, sy, W - PAD * 2 - 10, SUB_H, 3, COL_SUB);
        ctx.drawTextWithShadow(textRenderer, Text.literal(label), px + PAD + 4, sy + (SUB_H - 8) / 2, COL_MUTED);
        GuiRenderUtils.drawRoundedRect(ctx, barX, sy + SUB_H / 2 - 2, barW, 4, 2, 0x44FFFFFF);
        int fillW = (int)(t * barW);
        if (fillW > 0) GuiRenderUtils.drawRoundedRect(ctx, barX, sy + SUB_H / 2 - 2, fillW, 4, 2, COL_ACCENT);
        int handleX = barX + fillW - 3;
        ctx.fill(handleX, sy + 3, handleX + 6, sy + SUB_H - 3, 0xFFFFFFFF);
        String valStr = String.format("%.1f", value);
        ctx.drawTextWithShadow(textRenderer, Text.literal(valStr), px + W - PAD - 10 - textRenderer.getWidth(valStr), sy + (SUB_H - 8) / 2, COL_TEXT);
    }

    @Override public boolean shouldPause() { return false; }

    @Override
    public void close() {
        savedPanelX = panelX;
        savedPanelY = panelY;
        pl.durex.client.config.DurexConfig.save();
        super.close();
    }
}
