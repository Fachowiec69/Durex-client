package pl.durex.autootchlan.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

/**
 * ConfigManager — auto-save przy każdej zmianie.
 * Plik: .minecraft/config/durex-auto-otchlan.json
 */
public class ConfigManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
        .getConfigDir().resolve("durex-auto-otchlan.json");

    private static ModConfig config = new ModConfig();

    // ── Load / Save ───────────────────────────────────────────────────────────
    public static void load() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                try (Reader r = Files.newBufferedReader(CONFIG_PATH)) {
                    ModConfig loaded = GSON.fromJson(r, ModConfig.class);
                    if (loaded != null) {
                        config = loaded;
                        // Upewnij się że lista nie jest null po deserializacji
                        if (config.priorities == null) config.priorities = new ArrayList<>();
                    }
                }
                System.out.println("[DAO] Konfiguracja zaladowana z: " + CONFIG_PATH);
            } else {
                // Pierwszy raz — zapisz domyślną konfigurację
                save();
                System.out.println("[DAO] Utworzono domyslna konfiguracje: " + CONFIG_PATH);
            }
        } catch (Exception e) {
            System.err.println("[DAO] Blad ladowania konfiguracji: " + e.getMessage());
            config = new ModConfig(); // fallback do domyślnych
        }
    }

    /** Zapisuje konfigurację do pliku. Wywołuj po każdej zmianie. */
    public static void save() {
        try {
            // Upewnij się że katalog istnieje
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer w = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(config, w);
            }
        } catch (Exception e) {
            System.err.println("[DAO] Blad zapisu konfiguracji: " + e.getMessage());
        }
    }

    public static ModConfig get() { return config; }

    // ── Priorytety ────────────────────────────────────────────────────────────
    public static List<PriorityEntry> getPriorities() {
        if (config.priorities == null) config.priorities = new ArrayList<>();
        return config.priorities;
    }

    public static void addPriority(PriorityEntry entry) {
        getPriorities().add(entry);
        save(); // auto-save
    }

    public static void removePriority(int index) {
        List<PriorityEntry> list = getPriorities();
        if (index >= 0 && index < list.size()) {
            list.remove(index);
            save(); // auto-save
        }
    }

    public static void updatePriority(int index, PriorityEntry entry) {
        List<PriorityEntry> list = getPriorities();
        if (index >= 0 && index < list.size()) {
            list.set(index, entry);
            save(); // auto-save
        }
    }

    public static boolean isPriority(String itemName) {
        if (itemName == null) return false;
        String lower = itemName.toLowerCase();
        for (PriorityEntry e : getPriorities()) {
            if (e.keyword != null && lower.contains(e.keyword.toLowerCase())) return true;
        }
        return false;
    }

    // ── Settery z auto-save ───────────────────────────────────────────────────
    public static void setCommand(String cmd) {
        config.command = cmd;
        save();
    }

    public static void setNextPageSlot(int slot) {
        config.nextPageSlot = slot;
        save();
    }

    public static void setOpenDelayMs(int ms) {
        config.openDelayMs = ms;
        save();
    }

    public static void setPageDelayMs(int ms) {
        config.pageDelayMs = ms;
        save();
    }

    public static void setSpamCount(int count) {
        config.spamCount = count;
        save();
    }

    public static void setSpamIntervalMs(int ms) {
        config.spamIntervalMs = ms;
        save();
    }

    public static void setGrabAll(boolean grabAll) {
        config.grabAll = grabAll;
        save();
    }
}
