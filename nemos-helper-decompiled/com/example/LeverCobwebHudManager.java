/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
 *  net.minecraft.class_304
 *  net.minecraft.class_310
 *  net.minecraft.class_3675$class_307
 *  net.minecraft.class_437
 */
package com.example;

import com.example.LeverCobwebConfig;
import com.example.LeverCobwebScreen;
import com.example.LeverCobwebSpeedProfile;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.class_304;
import net.minecraft.class_310;
import net.minecraft.class_3675;
import net.minecraft.class_437;

@Environment(value=EnvType.CLIENT)
public final class LeverCobwebHudManager {
    private static final String KEY_CATEGORY = "key.categories.mini_hud";
    private static final String OPEN_KEY = "key.template_mod.open_lever_cobweb_menu";
    private static LeverCobwebConfig config = new LeverCobwebConfig();
    private static class_304 openMenuKeyBinding;

    private LeverCobwebHudManager() {
    }

    public static void initialize() {
        config = LeverCobwebConfig.load();
        openMenuKeyBinding = KeyBindingHelper.registerKeyBinding((class_304)new class_304(OPEN_KEY, class_3675.class_307.field_1668, 293, KEY_CATEGORY));
    }

    public static void onClientTick(class_310 client) {
        if (openMenuKeyBinding == null) {
            return;
        }
        while (openMenuKeyBinding.method_1436()) {
            class_437 class_4372 = client.field_1755;
            if (class_4372 instanceof LeverCobwebScreen) {
                LeverCobwebScreen leverCobwebScreen = (LeverCobwebScreen)class_4372;
                leverCobwebScreen.method_25419();
                continue;
            }
            client.method_1507((class_437)new LeverCobwebScreen(client.field_1755));
        }
    }

    public static LeverCobwebSpeedProfile getSpeedProfile() {
        return config.getSpeedProfile();
    }

    public static void cycleSpeedProfile() {
        LeverCobwebHudManager.setSpeedProfile(LeverCobwebHudManager.getSpeedProfile().next());
    }

    public static void setSpeedProfile(LeverCobwebSpeedProfile profile) {
        config.setSpeedProfile(profile);
        config.save();
    }

    public static boolean isPlaceThroughPlayersEnabled() {
        return config.isPlaceThroughPlayersEnabled();
    }

    public static void setPlaceThroughPlayersEnabled(boolean enabled) {
        config.setPlaceThroughPlayersEnabled(enabled);
        config.save();
    }

    public static boolean isPlaceThroughCobwebsEnabled() {
        return config.isPlaceThroughCobwebsEnabled();
    }

    public static void setPlaceThroughCobwebsEnabled(boolean enabled) {
        config.setPlaceThroughCobwebsEnabled(enabled);
        config.save();
    }

    public static boolean isSwitchToBestSwordEnabled() {
        return config.isSwitchToBestSwordEnabled();
    }

    public static void setSwitchToBestSwordEnabled(boolean enabled) {
        config.setSwitchToBestSwordEnabled(enabled);
        config.save();
    }

    public static boolean isHoldLeverEnabled() {
        return config.isHoldLeverEnabled();
    }

    public static void setHoldLeverEnabled(boolean enabled) {
        config.setHoldLeverEnabled(enabled);
        config.save();
    }
}

