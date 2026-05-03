/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.minecraft.class_2561
 */
package com.example;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_2561;

@Environment(value=EnvType.CLIENT)
public enum LeverCobwebSpeedProfile {
    SAFE("screen.template_mod.lever_cobweb.speed.safe", 1, 1, 2, 0, 0.17f, -8745554),
    STABLE("screen.template_mod.lever_cobweb.speed.stable", 1, 1, 1, 0, 0.33f, -6576706),
    BALANCED("screen.template_mod.lever_cobweb.speed.balanced", 0, 1, 1, 0, 0.5f, -6246185),
    FAST("screen.template_mod.lever_cobweb.speed.fast", 0, 0, 1, 1, 0.67f, -2047377),
    TURBO("screen.template_mod.lever_cobweb.speed.turbo", 0, 0, 1, 2, 0.83f, -996751),
    LIGHTNING("screen.template_mod.lever_cobweb.speed.lightning", 0, 0, 0, 4, 1.0f, -4487428);

    private final String translationKey;
    private final int retryDelayTicks;
    private final int settleDelayTicks;
    private final int postPickupDelayTicks;
    private final int extraInstantStagesPerTick;
    private final float indicatorProgress;
    private final int displayColor;

    private LeverCobwebSpeedProfile(String translationKey, int retryDelayTicks, int settleDelayTicks, int postPickupDelayTicks, int extraInstantStagesPerTick, float indicatorProgress, int displayColor) {
        this.translationKey = translationKey;
        this.retryDelayTicks = retryDelayTicks;
        this.settleDelayTicks = settleDelayTicks;
        this.postPickupDelayTicks = postPickupDelayTicks;
        this.extraInstantStagesPerTick = extraInstantStagesPerTick;
        this.indicatorProgress = indicatorProgress;
        this.displayColor = displayColor;
    }

    public class_2561 label() {
        return class_2561.method_43471((String)this.translationKey);
    }

    public int retryDelayTicks() {
        return this.retryDelayTicks;
    }

    public int settleDelayTicks() {
        return this.settleDelayTicks;
    }

    public int postPickupDelayTicks() {
        return this.postPickupDelayTicks;
    }

    public int extraInstantStagesPerTick() {
        return this.extraInstantStagesPerTick;
    }

    public float indicatorProgress() {
        return this.indicatorProgress;
    }

    public int displayColor() {
        return this.displayColor;
    }

    public boolean isLightning() {
        return this == LIGHTNING;
    }

    public LeverCobwebSpeedProfile next() {
        LeverCobwebSpeedProfile[] values = LeverCobwebSpeedProfile.values();
        return values[(this.ordinal() + 1) % values.length];
    }

    public static LeverCobwebSpeedProfile fromStoredValue(String value) {
        if (value == null || value.isBlank()) {
            return FAST;
        }
        for (LeverCobwebSpeedProfile profile : LeverCobwebSpeedProfile.values()) {
            if (!profile.name().equalsIgnoreCase(value)) continue;
            return profile;
        }
        return FAST;
    }
}

