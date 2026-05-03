package com.example.msgbot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class ModConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir().resolve("msg-bot.json");

    // === POLA KONFIGURACJI ===
    public String message = "";
    public String targetPlayer = "";
    public String jumpDelay = "3";
    public String chDelay = "30";
    public boolean onlyNetherite = false;
    public boolean onlyRank = false;
    public boolean onlyOffline = true; // domyslnie true - tylko cracked
    public String rankPrefix = ""; // prefix rangi do filtrowania
    public Set<String> spammedPlayers = new HashSet<>();
    public int totalSpammed = 0;

    // === SINGLETON ===
    private static ModConfig instance;

    public static ModConfig get() {
        if (instance == null) load();
        return instance;
    }

    public static void load() {
        try {
            if (CONFIG_PATH.toFile().exists()) {
                try (Reader r = new FileReader(CONFIG_PATH.toFile())) {
                    instance = GSON.fromJson(r, ModConfig.class);
                    if (instance == null) instance = new ModConfig();
                    if (instance.spammedPlayers == null) instance.spammedPlayers = new HashSet<>();
                    MsgBot.LOGGER.info("[MsgBot] Config zaladowany z {}", CONFIG_PATH);
                }
            } else {
                instance = new ModConfig();
                save();
                MsgBot.LOGGER.info("[MsgBot] Utworzono nowy config: {}", CONFIG_PATH);
            }
        } catch (Exception e) {
            MsgBot.LOGGER.error("[MsgBot] Blad ladowania configu: {}", e.getMessage());
            instance = new ModConfig();
        }
    }

    public static void save() {
        try (Writer w = new FileWriter(CONFIG_PATH.toFile())) {
            GSON.toJson(instance, w);
        } catch (Exception e) {
            MsgBot.LOGGER.error("[MsgBot] Blad zapisu configu: {}", e.getMessage());
        }
    }
}
