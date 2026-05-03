package pl.durex.autorynek.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class ConfigManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path configFile;
    public static ModConfig config;

    public static void load() {
        Path configDir = FabricLoader.getInstance().getConfigDir().resolve("durex-auto-rynek");
        try {
            Files.createDirectories(configDir);
        } catch (IOException ignored) {}

        configFile = configDir.resolve("config.json");

        if (Files.exists(configFile)) {
            try (Reader r = Files.newBufferedReader(configFile)) {
                config = GSON.fromJson(r, ModConfig.class);
                if (config == null) config = createDefault();
            } catch (IOException e) {
                e.printStackTrace();
                config = createDefault();
            }
        } else {
            config = createDefault();
            save();
        }
    }

    public static void save() {
        try (Writer w = Files.newBufferedWriter(configFile)) {
            GSON.toJson(config, w);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static ServerProfile findProfile(String serverAddress) {
        if (config == null || serverAddress == null) return null;
        String serverLower = serverAddress.toLowerCase(); // Case-insensitive matching
        for (ServerProfile p : config.servers) {
            for (String domain : p.domains) {
                if (domain.equals("*") || serverLower.contains(domain.toLowerCase())) {
                    return p;
                }
            }
        }
        return config.servers.isEmpty() ? null : config.servers.get(0);
    }

    public static ServerProfile getOrCreateProfile(String serverAddress) {
        ServerProfile existing = findProfile(serverAddress);
        if (existing != null && !existing.domains.contains("*")) return existing;

        ServerProfile p = new ServerProfile();
        p.domains = new ArrayList<>(List.of(serverAddress));
        p.profileName = serverAddress;
        config.servers.add(0, p);
        save();
        return p;
    }

    private static ModConfig createDefault() {
        ModConfig cfg = new ModConfig();

        ServerProfile anarchia = new ServerProfile();
        anarchia.domains = new ArrayList<>(List.of("anarchia.gg"));
        anarchia.profileName = "anarchia_smp";
        anarchia.loreRegex = "(?i).*Koszt.*?\\$([\\d.,]+(?:mld|[km])?).*";
        anarchia.marketGuiTitle = "Rynek";
        anarchia.marketNextPageName = "Nastepna strona";
        anarchia.marketNextPageMaterial = "minecraft:lime_dye";
        anarchia.marketNextPageSlot = 50;
        anarchia.confirmSlot = 11;
        anarchia.sortingSlot = 53;
        anarchia.sortingKeyword = "najnowsz";
        anarchia.marketCommands = new ArrayList<>(List.of("/ah", "/rynek"));
        anarchia.marketOpenDelayMs = 480;
        anarchia.marketNextDelayMs = 50;
        anarchia.marketCloseDelayMs = 500;
        anarchia.marketConfirmDelayMs = 33;
        anarchia.marketActionDelayMs = 12;
        anarchia.marketConfirmAttempts = 5;
        anarchia.aggressiveMode = true;
        anarchia.autoLearnEnabled = true;
        anarchia.learnDurationMinutes = 120;
        anarchia.preClickSpamCount = 5;
        anarchia.preClickSpamDelayMs = 5;
        cfg.servers.add(anarchia);

        ServerProfile def = new ServerProfile();
        def.domains = new ArrayList<>(List.of("*"));
        def.profileName = "default";
        def.loreRegex = "(?i).*Cena\\s*:?\\s*(?:\\$\\s*)?((?:\\d{1,3}(?:[\\s.,]\\d{3})*|\\d+)(?:mld|m|k)?)(?:\\s*\\$)?.*";
        def.marketCommands = new ArrayList<>(List.of("/ah"));
        def.preClickSpamCount = 5;
        def.preClickSpamDelayMs = 5;
        cfg.servers.add(def);

        return cfg;
    }
}
