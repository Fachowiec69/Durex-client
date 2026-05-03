/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  com.google.gson.JsonParseException
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.fabricmc.loader.api.FabricLoader
 */
package com.example;

import com.example.LeverCobwebSpeedProfile;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;

@Environment(value=EnvType.CLIENT)
public final class LeverCobwebConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("lever-cobweb.json");
    private String speedProfile = LeverCobwebSpeedProfile.FAST.name();
    private Boolean placeThroughPlayersEnabled = Boolean.TRUE;
    private Boolean placeThroughCobwebsEnabled = Boolean.TRUE;
    private Boolean switchToBestSwordEnabled = Boolean.TRUE;
    private Boolean holdLeverEnabled = Boolean.FALSE;

    public static LeverCobwebConfig load() {
        LeverCobwebConfig leverCobwebConfig;
        block10: {
            if (!Files.exists(CONFIG_PATH, new LinkOption[0])) {
                LeverCobwebConfig config = new LeverCobwebConfig();
                config.save();
                return config;
            }
            BufferedReader reader = Files.newBufferedReader(CONFIG_PATH, StandardCharsets.UTF_8);
            try {
                LeverCobwebConfig config = (LeverCobwebConfig)GSON.fromJson((Reader)reader, LeverCobwebConfig.class);
                if (config == null) {
                    config = new LeverCobwebConfig();
                }
                leverCobwebConfig = config;
                if (reader == null) break block10;
            }
            catch (Throwable config) {
                try {
                    if (reader != null) {
                        try {
                            ((Reader)reader).close();
                        }
                        catch (Throwable throwable) {
                            config.addSuppressed(throwable);
                        }
                    }
                    throw config;
                }
                catch (JsonParseException | IOException exception) {
                    System.out.println("[Lever Cobweb] Failed to read config, using defaults: " + exception.getMessage());
                    LeverCobwebConfig config2 = new LeverCobwebConfig();
                    config2.save();
                    return config2;
                }
            }
            ((Reader)reader).close();
        }
        return leverCobwebConfig;
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent(), new FileAttribute[0]);
            try (BufferedWriter writer = Files.newBufferedWriter(CONFIG_PATH, StandardCharsets.UTF_8, new OpenOption[0]);){
                GSON.toJson((Object)this, (Appendable)writer);
            }
        }
        catch (IOException exception) {
            System.out.println("[Lever Cobweb] Failed to save config: " + exception.getMessage());
        }
    }

    public LeverCobwebSpeedProfile getSpeedProfile() {
        return LeverCobwebSpeedProfile.fromStoredValue(this.speedProfile);
    }

    public void setSpeedProfile(LeverCobwebSpeedProfile profile) {
        this.speedProfile = profile.name();
    }

    public boolean isPlaceThroughPlayersEnabled() {
        return this.placeThroughPlayersEnabled == null || this.placeThroughPlayersEnabled != false;
    }

    public void setPlaceThroughPlayersEnabled(boolean placeThroughPlayersEnabled) {
        this.placeThroughPlayersEnabled = placeThroughPlayersEnabled;
    }

    public boolean isPlaceThroughCobwebsEnabled() {
        return this.placeThroughCobwebsEnabled == null || this.placeThroughCobwebsEnabled != false;
    }

    public void setPlaceThroughCobwebsEnabled(boolean placeThroughCobwebsEnabled) {
        this.placeThroughCobwebsEnabled = placeThroughCobwebsEnabled;
    }

    public boolean isSwitchToBestSwordEnabled() {
        return this.switchToBestSwordEnabled == null || this.switchToBestSwordEnabled != false;
    }

    public void setSwitchToBestSwordEnabled(boolean switchToBestSwordEnabled) {
        this.switchToBestSwordEnabled = switchToBestSwordEnabled;
    }

    public boolean isHoldLeverEnabled() {
        return this.holdLeverEnabled != null && this.holdLeverEnabled != false;
    }

    public void setHoldLeverEnabled(boolean holdLeverEnabled) {
        this.holdLeverEnabled = holdLeverEnabled;
    }
}

