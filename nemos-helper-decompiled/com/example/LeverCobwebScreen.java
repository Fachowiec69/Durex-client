/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.minecraft.class_156
 *  net.minecraft.class_1799
 *  net.minecraft.class_1802
 *  net.minecraft.class_1935
 *  net.minecraft.class_2561
 *  net.minecraft.class_310
 *  net.minecraft.class_332
 *  net.minecraft.class_3532
 *  net.minecraft.class_437
 *  net.minecraft.class_5250
 *  net.minecraft.class_5348
 */
package com.example;

import com.example.LeverCobwebHudManager;
import com.example.LeverCobwebSpeedProfile;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_156;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_1935;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_3532;
import net.minecraft.class_437;
import net.minecraft.class_5250;
import net.minecraft.class_5348;

@Environment(value=EnvType.CLIENT)
public final class LeverCobwebScreen
extends class_437 {
    private static final int PANEL_WIDTH = 404;
    private static final int HEADER_HEIGHT = 66;
    private static final int ROW_HEIGHT = 50;
    private static final int ROW_SPACING = 9;
    private static final int PANEL_SIDE_PADDING = 14;
    private static final int ACCENT_WIDTH = 3;
    private static final int STATUS_RESERVED_WIDTH = 102;
    private static final int PARTICLES_PER_CHANGE = 10;
    private static final float OPEN_CLOSE_SPEED = 8.2f;
    private static final int BACKGROUND_TOP = -300804576;
    private static final int BACKGROUND_BOTTOM = -301331180;
    private static final int PANEL_FILL = -300870368;
    private static final int PANEL_BORDER = -13617334;
    private static final int HEADER_FILL = -15328215;
    private static final int CARD_FILL = -14275524;
    private static final int CARD_FILL_HOVERED = -13946556;
    private static final int CARD_BORDER = -12959144;
    private static final int CARD_BORDER_HOVERED = -10919042;
    private static final int ACCENT_INFO = -10063476;
    private static final int TITLE_COLOR = -788997;
    private static final int SUBTITLE_COLOR = -6708550;
    private static final int DESCRIPTION_COLOR = -8616545;
    private static final int INFO_STATUS_COLOR = -5261362;
    private static final int STATUS_ENABLED = -1585014;
    private static final int STATUS_DISABLED = -6511167;
    private static final int ICON_BACKGROUND = -14538438;
    private static final int PARTICLE_GLOW = 1728046247;
    private static final int HEADER_GLOW_PRIMARY = 1073141897;
    private static final int HEADER_GLOW_SOFT = 552653178;
    private static final int HEADER_GLOW_TRAIL = 352317874;
    private final class_437 parent;
    private final List<AccentParticle> particles = new ArrayList<AccentParticle>();
    private final Random random = new Random();
    private long lastRenderTimeMs = -1L;
    private float popupProgress = 0.0f;
    private boolean closing = false;
    private boolean closeCompleted = false;
    private float speedBarProgress;
    private float speedBarTarget;
    private float throughPlayersProgress;
    private float throughPlayersTarget;
    private float throughCobwebsProgress;
    private float throughCobwebsTarget;
    private float switchSwordProgress;
    private float switchSwordTarget;
    private float holdLeverProgress;
    private float holdLeverTarget;

    public LeverCobwebScreen(class_437 parent) {
        super((class_2561)class_2561.method_43471((String)"screen.template_mod.lever_cobweb.title"));
        this.parent = parent;
        this.speedBarTarget = this.speedBarProgress = LeverCobwebHudManager.getSpeedProfile().indicatorProgress();
        this.throughPlayersTarget = this.throughPlayersProgress = LeverCobwebHudManager.isPlaceThroughPlayersEnabled() ? 1.0f : 0.0f;
        this.throughCobwebsTarget = this.throughCobwebsProgress = LeverCobwebHudManager.isPlaceThroughCobwebsEnabled() ? 1.0f : 0.0f;
        this.switchSwordTarget = this.switchSwordProgress = LeverCobwebHudManager.isSwitchToBestSwordEnabled() ? 1.0f : 0.0f;
        this.holdLeverTarget = this.holdLeverProgress = LeverCobwebHudManager.isHoldLeverEnabled() ? 1.0f : 0.0f;
    }

    public void method_25394(class_332 context, int mouseX, int mouseY, float delta) {
        float frameDelta = this.frameDeltaSeconds();
        this.updatePopupAnimation(frameDelta);
        if (this.closing && this.popupProgress <= 0.0f) {
            this.finishClose();
            return;
        }
        this.updateAnimations(frameDelta);
        this.updateParticles(frameDelta);
        PopupVisualState popup = this.popupVisualState();
        context.method_25296(0, 0, this.field_22789, this.field_22790, LeverCobwebScreen.withMultipliedAlpha(-300804576, popup.overlayAlpha()), LeverCobwebScreen.withMultipliedAlpha(-301331180, popup.overlayAlpha()));
        PanelLayout layout = this.layout(popup.panelScale(), popup.slideOffset());
        this.renderPanel(context, layout);
        this.renderHeader(context, layout);
        for (MenuRow row : this.layoutRows(layout)) {
            this.renderRow(context, row, mouseX, mouseY);
        }
        this.renderParticles(context);
        super.method_25394(context, mouseX, mouseY, delta);
    }

    public boolean method_25402(double mouseX, double mouseY, int button) {
        if (this.closing || this.popupProgress < 0.92f) {
            return true;
        }
        if (button != 0) {
            return super.method_25402(mouseX, mouseY, button);
        }
        for (MenuRow row : this.layoutRows(this.layout(this.popupVisualState().panelScale(), this.popupVisualState().slideOffset()))) {
            if (!row.clickable() || !row.contains(mouseX, mouseY)) continue;
            switch (row.id().ordinal()) {
                case 0: {
                    LeverCobwebHudManager.cycleSpeedProfile();
                    LeverCobwebSpeedProfile profile = LeverCobwebHudManager.getSpeedProfile();
                    this.speedBarTarget = profile.indicatorProgress();
                    this.spawnParticles(row, profile.displayColor());
                    break;
                }
                case 1: {
                    boolean enabled = !LeverCobwebHudManager.isPlaceThroughPlayersEnabled();
                    LeverCobwebHudManager.setPlaceThroughPlayersEnabled(enabled);
                    this.throughPlayersTarget = enabled ? 1.0f : 0.0f;
                    this.spawnParticles(row, enabled ? -2570884 : -7695702);
                    break;
                }
                case 2: {
                    boolean enabled = !LeverCobwebHudManager.isPlaceThroughCobwebsEnabled();
                    LeverCobwebHudManager.setPlaceThroughCobwebsEnabled(enabled);
                    this.throughCobwebsTarget = enabled ? 1.0f : 0.0f;
                    this.spawnParticles(row, enabled ? -2570884 : -7695702);
                    break;
                }
                case 3: {
                    boolean enabled = !LeverCobwebHudManager.isSwitchToBestSwordEnabled();
                    LeverCobwebHudManager.setSwitchToBestSwordEnabled(enabled);
                    this.switchSwordTarget = enabled ? 1.0f : 0.0f;
                    this.spawnParticles(row, enabled ? -2570884 : -7695702);
                    break;
                }
                case 4: {
                    boolean enabled = !LeverCobwebHudManager.isHoldLeverEnabled();
                    LeverCobwebHudManager.setHoldLeverEnabled(enabled);
                    this.holdLeverTarget = enabled ? 1.0f : 0.0f;
                    this.spawnParticles(row, enabled ? -2570884 : -7695702);
                    break;
                }
                case 5: {
                    return true;
                }
            }
            return true;
        }
        return super.method_25402(mouseX, mouseY, button);
    }

    public void method_25419() {
        if (!this.closing) {
            this.closing = true;
        }
    }

    public boolean method_25421() {
        return false;
    }

    private void renderPanel(class_332 context, PanelLayout layout) {
        context.method_25294(layout.left(), layout.top(), layout.right(), layout.bottom(), this.popupColor(-300870368));
        context.method_49601(layout.left(), layout.top(), layout.width(), layout.height(), this.popupColor(-13617334));
    }

    private void renderHeader(class_332 context, PanelLayout layout) {
        int headerLeft = layout.left() + 14;
        int headerTop = layout.top() + 14;
        int headerWidth = layout.width() - 28;
        this.renderHeaderGlow(context, headerLeft, headerTop, headerWidth);
        context.method_25294(headerLeft, headerTop, headerLeft + headerWidth, headerTop + 66, this.popupColor(-15328215));
        context.method_49601(headerLeft, headerTop, headerWidth, 66, this.popupColor(-12959144));
        int iconLeft = headerLeft + 13;
        int iconTop = headerTop + 17;
        context.method_25294(iconLeft, iconTop, iconLeft + 32, iconTop + 32, this.popupColor(-14538438));
        context.method_49601(iconLeft, iconTop, 32, 32, this.popupColor(-10919042));
        if (this.popupProgress > 0.18f) {
            context.method_51427(new class_1799((class_1935)class_1802.field_8786), iconLeft + 4, iconTop + 9);
            context.method_51427(new class_1799((class_1935)class_1802.field_8865), iconLeft + 16, iconTop + 2);
        }
        int textLeft = headerLeft + 58;
        int textRight = headerLeft + headerWidth - 16;
        int textMaxWidth = textRight - textLeft;
        this.drawClippedText(context, (class_2561)class_2561.method_43471((String)"screen.template_mod.lever_cobweb.title"), textLeft, headerTop + 12, textMaxWidth, this.popupColor(-788997), true);
        this.drawClippedText(context, (class_2561)class_2561.method_43471((String)"screen.template_mod.lever_cobweb.subtitle"), textLeft, headerTop + 33, textMaxWidth, this.popupColor(-6708550), false);
    }

    private void renderRow(class_332 context, MenuRow row, int mouseX, int mouseY) {
        float progress;
        int statusColor;
        int accentColor;
        boolean hovered = row.clickable() && row.contains(mouseX, mouseY);
        LeverCobwebSpeedProfile profile = LeverCobwebHudManager.getSpeedProfile();
        boolean placeThroughPlayersEnabled = LeverCobwebHudManager.isPlaceThroughPlayersEnabled();
        boolean placeThroughCobwebsEnabled = LeverCobwebHudManager.isPlaceThroughCobwebsEnabled();
        boolean switchToBestSwordEnabled = LeverCobwebHudManager.isSwitchToBestSwordEnabled();
        boolean holdLeverEnabled = LeverCobwebHudManager.isHoldLeverEnabled();
        int fillColor = this.popupColor(hovered ? -13946556 : -14275524);
        int borderColor = this.popupColor(hovered ? -10919042 : -12959144);
        context.method_25294(row.left(), row.top(), row.right(), row.bottom(), fillColor);
        context.method_49601(row.left(), row.top(), row.width(), row.height(), borderColor);
        class_5250 status = switch (row.id().ordinal()) {
            case 0 -> {
                accentColor = this.popupColor(profile.displayColor());
                statusColor = this.popupColor(profile.displayColor());
                progress = this.speedBarProgress;
                yield profile.label();
            }
            case 1 -> {
                accentColor = this.popupColor(placeThroughPlayersEnabled ? -2968742 : -10722181);
                statusColor = this.popupColor(placeThroughPlayersEnabled ? -1585014 : -6511167);
                progress = this.throughPlayersProgress;
                yield class_2561.method_43471((String)(placeThroughPlayersEnabled ? "screen.template_mod.lever_cobweb.status.enabled" : "screen.template_mod.lever_cobweb.status.disabled"));
            }
            case 2 -> {
                accentColor = this.popupColor(placeThroughCobwebsEnabled ? -2968742 : -10722181);
                statusColor = this.popupColor(placeThroughCobwebsEnabled ? -1585014 : -6511167);
                progress = this.throughCobwebsProgress;
                yield class_2561.method_43471((String)(placeThroughCobwebsEnabled ? "screen.template_mod.lever_cobweb.status.enabled" : "screen.template_mod.lever_cobweb.status.disabled"));
            }
            case 3 -> {
                accentColor = this.popupColor(switchToBestSwordEnabled ? -2968742 : -10722181);
                statusColor = this.popupColor(switchToBestSwordEnabled ? -1585014 : -6511167);
                progress = this.switchSwordProgress;
                yield class_2561.method_43471((String)(switchToBestSwordEnabled ? "screen.template_mod.lever_cobweb.status.enabled" : "screen.template_mod.lever_cobweb.status.disabled"));
            }
            case 4 -> {
                accentColor = this.popupColor(holdLeverEnabled ? -2968742 : -10722181);
                statusColor = this.popupColor(holdLeverEnabled ? -1585014 : -6511167);
                progress = this.holdLeverProgress;
                yield class_2561.method_43471((String)(holdLeverEnabled ? "screen.template_mod.lever_cobweb.status.enabled" : "screen.template_mod.lever_cobweb.status.disabled"));
            }
            case 5 -> {
                accentColor = this.popupColor(-10063476);
                statusColor = this.popupColor(-5261362);
                progress = 0.58f;
                yield class_2561.method_43471((String)"screen.template_mod.lever_cobweb.mode.auto");
            }
            default -> {
                accentColor = this.popupColor(-10063476);
                statusColor = this.popupColor(-5261362);
                progress = 0.0f;
                yield class_2561.method_43473();
            }
        };
        if (row.id() == RowId.SPEED && LeverCobwebHudManager.getSpeedProfile().isLightning()) {
            this.renderAccentBarLightning(context, row, progress);
        } else {
            this.renderAccentBar(context, row, accentColor, progress, row.id() != RowId.MODE_INFO);
        }
        int textLeft = row.left() + 26;
        int statusRight = row.right() - 18;
        int textRight = statusRight - 102;
        context.method_27535(this.field_22793, row.title(), textLeft, row.top() + 8, this.popupColor(-788997));
        this.drawClippedText(context, row.subtitle(), textLeft, row.top() + 23, textRight - textLeft, this.popupColor(-8616545), false);
        int statusWidth = this.field_22793.method_27525((class_5348)status);
        context.method_27535(this.field_22793, (class_2561)status, statusRight - statusWidth, row.top() + 16, statusColor);
    }

    private List<MenuRow> layoutRows(PanelLayout layout) {
        ArrayList<MenuRow> rows = new ArrayList<MenuRow>(6);
        int rowLeft = layout.left() + 14;
        int rowWidth = layout.width() - 28;
        int top = layout.top() + 14 + 66 + 12;
        rows.add(new MenuRow(RowId.SPEED, rowLeft, top, rowWidth, 50, true, (class_2561)class_2561.method_43471((String)"screen.template_mod.lever_cobweb.row.speed"), (class_2561)class_2561.method_43471((String)"screen.template_mod.lever_cobweb.row.speed.description")));
        rows.add(new MenuRow(RowId.THROUGH_PLAYERS, rowLeft, top + 50 + 9, rowWidth, 50, true, (class_2561)class_2561.method_43471((String)"screen.template_mod.lever_cobweb.row.through_players"), (class_2561)class_2561.method_43471((String)"screen.template_mod.lever_cobweb.row.through_players.description")));
        rows.add(new MenuRow(RowId.THROUGH_COBWEBS, rowLeft, top + 118, rowWidth, 50, true, (class_2561)class_2561.method_43471((String)"screen.template_mod.lever_cobweb.row.through_cobwebs"), (class_2561)class_2561.method_43471((String)"screen.template_mod.lever_cobweb.row.through_cobwebs.description")));
        rows.add(new MenuRow(RowId.SWITCH_SWORD, rowLeft, top + 177, rowWidth, 50, true, (class_2561)class_2561.method_43471((String)"screen.template_mod.lever_cobweb.row.switch_sword"), (class_2561)class_2561.method_43471((String)"screen.template_mod.lever_cobweb.row.switch_sword.description")));
        rows.add(new MenuRow(RowId.HOLD_LEVER, rowLeft, top + 236, rowWidth, 50, true, (class_2561)class_2561.method_43471((String)"screen.template_mod.lever_cobweb.row.hold_lever"), (class_2561)class_2561.method_43471((String)"screen.template_mod.lever_cobweb.row.hold_lever.description")));
        rows.add(new MenuRow(RowId.MODE_INFO, rowLeft, top + 295, rowWidth, 50, false, (class_2561)class_2561.method_43471((String)"screen.template_mod.lever_cobweb.row.mode"), (class_2561)class_2561.method_43471((String)"screen.template_mod.lever_cobweb.row.mode.description")));
        return rows;
    }

    private PanelLayout layout(float panelScale, int slideOffset) {
        int basePanelWidth = Math.min(404, this.field_22789 - 32);
        int basePanelHeight = 451;
        int panelWidth = Math.max(1, Math.round((float)basePanelWidth * panelScale));
        int panelHeight = Math.max(1, Math.round((float)basePanelHeight * panelScale));
        int left = (this.field_22789 - panelWidth) / 2;
        int top = Math.max(26, (this.field_22790 - panelHeight) / 2) + slideOffset;
        return new PanelLayout(left, top, panelWidth, panelHeight);
    }

    private void renderAccentBarLightning(class_332 context, MenuRow row, float progress) {
        int barLeft = row.left() + 14;
        int barTop = row.top() + 10;
        int barBottom = row.bottom() - 10;
        int barHeight = barBottom - barTop;
        context.method_25294(barLeft, barTop, barLeft + 3, barBottom, this.popupColor(-11840921));
        if (progress <= 0.0f) {
            return;
        }
        int litHeight = Math.max(1, Math.round((float)barHeight * progress));
        int litTop = barBottom - litHeight;
        float time = (float)(class_156.method_658() % 1600L) / 1600.0f;
        float pulse = 0.5f + class_3532.method_15374((float)(time * ((float)Math.PI * 2) * 2.0f)) * 0.5f;
        int glowColor = LeverCobwebScreen.withAlpha(-2839809, pulse * 0.55f);
        context.method_25294(barLeft - 2, litTop - 3, barLeft + 3 + 2, barBottom + 2, this.popupColor(glowColor));
        context.method_25294(barLeft, litTop, barLeft + 3, barBottom, this.popupColor(-4487428));
        int tipColor = LeverCobwebScreen.withAlpha(-1122817, 0.75f + pulse * 0.25f);
        context.method_25294(barLeft, litTop, barLeft + 3, Math.min(litTop + 2, barBottom), this.popupColor(tipColor));
    }

    private void renderAccentBar(class_332 context, MenuRow row, int accentColor, float progress, boolean shimmer) {
        int barLeft = row.left() + 14;
        int barTop = row.top() + 10;
        int barBottom = row.bottom() - 10;
        int barHeight = barBottom - barTop;
        context.method_25294(barLeft, barTop, barLeft + 3, barBottom, this.popupColor(-11840921));
        if (progress <= 0.0f) {
            return;
        }
        int litHeight = Math.max(1, Math.round((float)barHeight * progress));
        int litTop = barBottom - litHeight;
        context.method_25294(barLeft, litTop, barLeft + 3, barBottom, accentColor);
        if (shimmer && progress < 1.0f) {
            int shimmerTop = Math.max(barTop, litTop - 4);
            int shimmerBottom = Math.min(barBottom, litTop + 6);
            context.method_25294(barLeft - 1, shimmerTop, barLeft + 3 + 1, shimmerBottom, this.popupColor(1728049328));
        }
    }

    private void spawnParticles(MenuRow row, int color) {
        double originX = (double)row.left() + 15.5;
        double top = (double)row.top() + 11.0;
        double height = (double)row.height() - 22.0;
        for (int i = 0; i < 10; ++i) {
            double offsetY = top + height * ((double)i / (double)Math.max(1, 9));
            double velocityX = 16.0 + this.random.nextDouble() * 24.0;
            double velocityY = (this.random.nextDouble() - 0.5) * 22.0;
            float size = 1.4f + this.random.nextFloat() * 1.8f;
            float lifetime = 0.32f + this.random.nextFloat() * 0.24f;
            this.particles.add(new AccentParticle(originX, offsetY + (this.random.nextDouble() - 0.5) * 8.0, velocityX, velocityY, size, lifetime, color));
        }
    }

    private void updateAnimations(float frameDelta) {
        this.speedBarProgress = class_3532.method_16439((float)Math.min(1.0f, frameDelta * 12.5f), (float)this.speedBarProgress, (float)this.speedBarTarget);
        if (Math.abs(this.speedBarProgress - this.speedBarTarget) < 0.01f) {
            this.speedBarProgress = this.speedBarTarget;
        }
        this.throughPlayersProgress = class_3532.method_16439((float)Math.min(1.0f, frameDelta * 13.5f), (float)this.throughPlayersProgress, (float)this.throughPlayersTarget);
        if (Math.abs(this.throughPlayersProgress - this.throughPlayersTarget) < 0.01f) {
            this.throughPlayersProgress = this.throughPlayersTarget;
        }
        this.throughCobwebsProgress = class_3532.method_16439((float)Math.min(1.0f, frameDelta * 13.5f), (float)this.throughCobwebsProgress, (float)this.throughCobwebsTarget);
        if (Math.abs(this.throughCobwebsProgress - this.throughCobwebsTarget) < 0.01f) {
            this.throughCobwebsProgress = this.throughCobwebsTarget;
        }
        this.switchSwordProgress = class_3532.method_16439((float)Math.min(1.0f, frameDelta * 13.5f), (float)this.switchSwordProgress, (float)this.switchSwordTarget);
        if (Math.abs(this.switchSwordProgress - this.switchSwordTarget) < 0.01f) {
            this.switchSwordProgress = this.switchSwordTarget;
        }
        this.holdLeverProgress = class_3532.method_16439((float)Math.min(1.0f, frameDelta * 13.5f), (float)this.holdLeverProgress, (float)this.holdLeverTarget);
        if (Math.abs(this.holdLeverProgress - this.holdLeverTarget) < 0.01f) {
            this.holdLeverProgress = this.holdLeverTarget;
        }
    }

    private void updateParticles(float frameDelta) {
        Iterator<AccentParticle> iterator = this.particles.iterator();
        while (iterator.hasNext()) {
            AccentParticle particle = iterator.next();
            if (particle.update(frameDelta)) continue;
            iterator.remove();
        }
    }

    private void renderParticles(class_332 context) {
        float contentAlpha = this.popupVisualState().contentAlpha();
        for (AccentParticle particle : this.particles) {
            float alpha = particle.alpha() * contentAlpha;
            int glowColor = LeverCobwebScreen.withAlpha(1728046247, alpha * 0.45f);
            int particleColor = LeverCobwebScreen.withAlpha(particle.color(), alpha);
            int x = Math.round((float)particle.x());
            int y = Math.round((float)particle.y());
            int glowRadius = Math.max(2, Math.round(particle.size() + 1.5f));
            int size = Math.max(1, Math.round(particle.size()));
            context.method_25294(x - glowRadius, y - glowRadius, x + glowRadius, y + glowRadius, glowColor);
            context.method_25294(x, y, x + size, y + size, particleColor);
        }
    }

    private float frameDeltaSeconds() {
        long now = class_156.method_658();
        if (this.lastRenderTimeMs < 0L) {
            this.lastRenderTimeMs = now;
            return 0.016f;
        }
        float delta = (float)(now - this.lastRenderTimeMs) / 1000.0f;
        this.lastRenderTimeMs = now;
        return class_3532.method_15363((float)delta, (float)0.0f, (float)0.05f);
    }

    private void updatePopupAnimation(float frameDelta) {
        float target = this.closing ? 0.0f : 1.0f;
        this.popupProgress = class_3532.method_16439((float)Math.min(1.0f, frameDelta * 8.2f), (float)this.popupProgress, (float)target);
        if (Math.abs(this.popupProgress - target) < 0.01f) {
            this.popupProgress = target;
        }
    }

    private static int withAlpha(int color, float alpha) {
        int clampedAlpha = class_3532.method_15340((int)((int)(alpha * 255.0f)), (int)0, (int)255);
        return color & 0xFFFFFF | clampedAlpha << 24;
    }

    private int popupColor(int color) {
        return LeverCobwebScreen.withMultipliedAlpha(color, this.popupVisualState().contentAlpha());
    }

    private static int withMultipliedAlpha(int color, float multiplier) {
        int baseAlpha = color >>> 24 & 0xFF;
        int multipliedAlpha = class_3532.method_15340((int)((int)((float)baseAlpha * multiplier)), (int)0, (int)255);
        return color & 0xFFFFFF | multipliedAlpha << 24;
    }

    private void drawClippedText(class_332 context, class_2561 text, int x, int y, int maxWidth, int color, boolean shadow) {
        if (maxWidth <= 0) {
            return;
        }
        if (this.field_22793.method_27525((class_5348)text) <= maxWidth) {
            if (shadow) {
                context.method_27535(this.field_22793, text, x, y, color);
            } else {
                context.method_51439(this.field_22793, text, x, y, color, false);
            }
            return;
        }
        String raw = text.getString();
        String ellipsis = "...";
        int ellipsisWidth = this.field_22793.method_1727(ellipsis);
        int availableWidth = Math.max(0, maxWidth - ellipsisWidth);
        String clipped = this.field_22793.method_27523(raw, availableWidth);
        if (shadow) {
            context.method_25303(this.field_22793, clipped + ellipsis, x, y, color);
        } else {
            context.method_51433(this.field_22793, clipped + ellipsis, x, y, color, false);
        }
    }

    private void renderHeaderGlow(class_332 context, int headerLeft, int headerTop, int headerWidth) {
        float time = (float)(class_156.method_658() % 3200L) / 3200.0f;
        float pulse = 0.72f + class_3532.method_15374((float)(time * ((float)Math.PI * 2))) * 0.18f;
        int iconGlow = LeverCobwebScreen.withMultipliedAlpha(1073141897, this.popupVisualState().contentAlpha() * pulse);
        int iconGlowSoft = LeverCobwebScreen.withMultipliedAlpha(552653178, this.popupVisualState().contentAlpha() * (0.85f + pulse * 0.15f));
        int trailGlow = LeverCobwebScreen.withMultipliedAlpha(352317874, this.popupVisualState().contentAlpha() * (0.75f + pulse * 0.2f));
        context.method_25294(headerLeft + 7, headerTop + 12, headerLeft + 60, headerTop + 66 - 12, iconGlowSoft);
        context.method_25294(headerLeft + 17, headerTop + 18, headerLeft + 49, headerTop + 66 - 18, iconGlow);
        context.method_25294(headerLeft + headerWidth - 118, headerTop + 14, headerLeft + headerWidth - 24, headerTop + 66 - 14, trailGlow);
    }

    private PopupVisualState popupVisualState() {
        float presented = this.closing ? LeverCobwebScreen.easeOutQuad(this.popupProgress) : LeverCobwebScreen.easeOutBack(this.popupProgress);
        float overlayAlpha = this.closing ? LeverCobwebScreen.easeOutQuad(this.popupProgress) * 0.9f : LeverCobwebScreen.easeOutCubic(this.popupProgress) * 0.9f;
        float contentAlpha = this.closing ? LeverCobwebScreen.easeOutQuad(this.popupProgress) : LeverCobwebScreen.easeOutCubic(this.popupProgress);
        float panelScale = class_3532.method_16439((float)presented, (float)0.94f, (float)1.0f);
        int slideOffset = Math.round((1.0f - presented) * 16.0f);
        return new PopupVisualState(panelScale, slideOffset, overlayAlpha, contentAlpha);
    }

    private void finishClose() {
        if (this.closeCompleted) {
            return;
        }
        this.closeCompleted = true;
        class_310 client = this.field_22787;
        if (client != null) {
            client.method_1507(this.parent);
        }
    }

    private static float easeOutCubic(float value) {
        float t = 1.0f - class_3532.method_15363((float)value, (float)0.0f, (float)1.0f);
        return 1.0f - t * t * t;
    }

    private static float easeOutQuad(float value) {
        float t = class_3532.method_15363((float)value, (float)0.0f, (float)1.0f);
        return 1.0f - (1.0f - t) * (1.0f - t);
    }

    private static float easeOutBack(float value) {
        float t = class_3532.method_15363((float)value, (float)0.0f, (float)1.0f);
        float c1 = 1.70158f;
        float c3 = c1 + 1.0f;
        float p = t - 1.0f;
        return 1.0f + c3 * p * p * p + c1 * p * p;
    }

    @Environment(value=EnvType.CLIENT)
    private record PopupVisualState(float panelScale, int slideOffset, float overlayAlpha, float contentAlpha) {
    }

    @Environment(value=EnvType.CLIENT)
    private record PanelLayout(int left, int top, int width, int height) {
        int right() {
            return this.left + this.width;
        }

        int bottom() {
            return this.top + this.height;
        }
    }

    @Environment(value=EnvType.CLIENT)
    private record MenuRow(RowId id, int left, int top, int width, int height, boolean clickable, class_2561 title, class_2561 subtitle) {
        int right() {
            return this.left + this.width;
        }

        int bottom() {
            return this.top + this.height;
        }

        boolean contains(double mouseX, double mouseY) {
            return mouseX >= (double)this.left && mouseX <= (double)this.right() && mouseY >= (double)this.top && mouseY <= (double)this.bottom();
        }
    }

    @Environment(value=EnvType.CLIENT)
    private static enum RowId {
        SPEED,
        THROUGH_PLAYERS,
        THROUGH_COBWEBS,
        SWITCH_SWORD,
        HOLD_LEVER,
        MODE_INFO;

    }

    @Environment(value=EnvType.CLIENT)
    private static final class AccentParticle {
        private double x;
        private double y;
        private final double velocityX;
        private final double velocityY;
        private final float size;
        private final float lifetime;
        private final int color;
        private float age;

        private AccentParticle(double x, double y, double velocityX, double velocityY, float size, float lifetime, int color) {
            this.x = x;
            this.y = y;
            this.velocityX = velocityX;
            this.velocityY = velocityY;
            this.size = size;
            this.lifetime = lifetime;
            this.color = color;
        }

        private boolean update(float frameDelta) {
            this.age += frameDelta;
            this.x += this.velocityX * (double)frameDelta;
            this.y += this.velocityY * (double)frameDelta;
            return this.age < this.lifetime;
        }

        private float alpha() {
            return 1.0f - class_3532.method_15363((float)(this.age / this.lifetime), (float)0.0f, (float)1.0f);
        }

        private double x() {
            return this.x;
        }

        private double y() {
            return this.y;
        }

        private float size() {
            return this.size;
        }

        private int color() {
            return this.color;
        }
    }
}

