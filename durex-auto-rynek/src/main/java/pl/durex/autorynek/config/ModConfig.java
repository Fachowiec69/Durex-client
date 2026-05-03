package pl.durex.autorynek.config;

import java.util.ArrayList;
import java.util.List;

public class ModConfig {
    public String defaultProfile = "default";
    public String discordWebhookUrl = "";
    public Boolean webhookBuyEnabled = true;
    public Boolean webhookSellEnabled = true;
    public Boolean soundsEnabled = true;
    public Boolean hudEnabled = true;
    public int hudX = 4;
    public int hudY = 4;
    public int logX = 0;
    public int logY = 0;
    public List<ServerProfile> servers = new ArrayList<>();
}
