package pl.durex.autorynek.config;

import java.util.ArrayList;
import java.util.List;

public class ServerProfile {
    public List<String> domains = new ArrayList<>();
    public String profileName = "default";
    public String loreRegex = "(?i).*Koszt.*?\\$([\\d.,]+(?:mld|[km])?).*";
    public String marketGuiTitle = "Rynek";
    public String marketNextPageName = "Nastepna strona";
    public String marketNextPageMaterial = "minecraft:lime_dye";
    public Integer marketNextPageSlot = 50;
    public Integer sortingSlot = 53;
    public String sortingKeyword = "najnowsz";
    public Integer marketOpenDelayMs = 500;
    public Integer marketNextDelayMs = 200;
    public Integer marketCloseDelayMs = 500;
    public Integer marketConfirmDelayMs = 100;
    public Integer marketActionDelayMs = 50;
    public Integer marketRestartDelayMs = 500;
    public Integer marketConfirmAttempts = 3;
    public Integer confirmSlot = null;
    public List<String> marketCommands = new ArrayList<>();
    public List<String> successMessages = new ArrayList<>();
    public List<String> errorMessages = new ArrayList<>();
    public Boolean aggressiveMode = false;
    public String loginCommand = "";
    public Boolean autoReconnect = true;
    public List<PriceEntry> prices = new ArrayList<>();

    // Pre-click spam settings
    public Integer preClickSpamCount = 5;
    public Integer preClickSpamDelayMs = 5;

    // Nauka cen
    public Boolean autoLearnEnabled = true;
    public Integer learnDurationMinutes = 120;
    public Double learnOutlierThreshold = 0.3;
    public Double learnBuyMargin = 0.95;
    public Double learnSellMargin = 1.05;
    public Integer learnMinSamples = 5;
}
