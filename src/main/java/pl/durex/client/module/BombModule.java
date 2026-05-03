package pl.durex.client.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.Random;

public class BombModule {
    
    private static final Random RANDOM = new Random();
    private static long lastMcSearchUse = 0;
    private static long lastBombaUse = 0;
    private static long lastScanUse = 0;
    private static long lastBotnetUse = 0;
    
    private static final long COOLDOWN_MS = 60000; // 60 sekund
    
    public static boolean fakeDDoS(String target, int duration) {
        long now = System.currentTimeMillis();
        if (now - lastBombaUse < COOLDOWN_MS) {
            long remaining = (COOLDOWN_MS - (now - lastBombaUse)) / 1000;
            MinecraftClient client = MinecraftClient.getInstance();
            sendMessage(client, "§c[Cooldown] Musisz poczekać " + remaining + " sekund");
            return false;
        }
        lastBombaUse = now;
        
        new Thread(() -> {
            try {
                MinecraftClient client = MinecraftClient.getInstance();
                
             
                sendMessage(client, "§c[DDoS] Inicjalizacja ataku...");
                Thread.sleep(500);
                
                int bots = 1000 + RANDOM.nextInt(500);
                sendMessage(client, "§c[DDoS] Ładowanie botnetu... (" + bots + " botów)");
                Thread.sleep(800);
                
                sendMessage(client, "§c[DDoS] Wysyłanie pakietów na " + target);
                Thread.sleep(300);
                
                // Symulacja wysyłania pakietów
                int totalPackets = 0;
                for (int i = 0; i < 5; i++) {
                    totalPackets += 5000 + RANDOM.nextInt(10000);
                    sendMessage(client, "§c[DDoS] Wysłano: " + totalPackets + " pakietów");
                    Thread.sleep(600);
                }
                
                sendMessage(client, "§c[DDoS] Atak zakończony po " + duration + "s");
                Thread.sleep(300);
                sendMessage(client, "§c[DDoS] Cel prawdopodobnie offline");
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
        return true;
    }
    
    public static boolean fakeScan(String target) {
        long now = System.currentTimeMillis();
        if (now - lastScanUse < COOLDOWN_MS) {
            long remaining = (COOLDOWN_MS - (now - lastScanUse)) / 1000;
            MinecraftClient client = MinecraftClient.getInstance();
            sendMessage(client, "§c[Cooldown] Musisz poczekać " + remaining + " sekund");
            return false;
        }
        lastScanUse = now;
        
        new Thread(() -> {
            try {
                MinecraftClient client = MinecraftClient.getInstance();
                
                sendMessage(client, "§a[Scan] Skanowanie " + target + "...");
                Thread.sleep(500);
                
                
                String[] ports = {"21", "22", "25", "80", "443", "3306", "8080", "25565"};
                String[] status = {"OPEN", "CLOSED", "FILTERED"};
                
                for (String port : ports) {
                    String portStatus = status[RANDOM.nextInt(status.length)];
                    String color = portStatus.equals("OPEN") ? "§a" : "§c";
                    sendMessage(client, "§a[Scan] Port " + port + ": " + color + portStatus);
                    Thread.sleep(300);
                }
                
                sendMessage(client, "§a[Scan] Skanowanie zakończone");
                sendMessage(client, "§a[Scan] Znaleziono " + (2 + RANDOM.nextInt(4)) + " otwartych portów");
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
        return true;
    }
    
    public static boolean fakeBotnet() {
        long now = System.currentTimeMillis();
        if (now - lastBotnetUse < COOLDOWN_MS) {
            long remaining = (COOLDOWN_MS - (now - lastBotnetUse)) / 1000;
            MinecraftClient client = MinecraftClient.getInstance();
            sendMessage(client, "§c[Cooldown] Musisz poczekać " + remaining + " sekund");
            return false;
        }
        lastBotnetUse = now;
        
        MinecraftClient client = MinecraftClient.getInstance();
        
        int totalBots = 1000 + RANDOM.nextInt(500);
        int onlineBots = (int)(totalBots * (0.7 + RANDOM.nextDouble() * 0.2));
        int bandwidth = 500 + RANDOM.nextInt(1500);
        
        sendMessage(client, "§e[Botnet] ═══════════════════════════");
        sendMessage(client, "§e[Botnet] Status: §aONLINE");
        sendMessage(client, "§e[Botnet] Boty online: §a" + onlineBots + "§e/§7" + totalBots);
        sendMessage(client, "§e[Botnet] Przepustowość: §a" + bandwidth + " Gbps");
        sendMessage(client, "§e[Botnet] Ostatni atak: §72 minuty temu");
        sendMessage(client, "§e[Botnet] ═══════════════════════════");
        return true;
    }
    
    public static boolean fakeMcSearch(String target) {
        long now = System.currentTimeMillis();
        if (now - lastMcSearchUse < COOLDOWN_MS) {
            long remaining = (COOLDOWN_MS - (now - lastMcSearchUse)) / 1000;
            MinecraftClient client = MinecraftClient.getInstance();
            sendMessage(client, "§c[Cooldown] Musisz poczekać " + remaining + " sekund");
            return false;
        }
        lastMcSearchUse = now;
        return true;
    }
    
    private static void sendMessage(MinecraftClient client, String message) {
        if (client.player != null) {
            client.player.sendMessage(Text.literal(message), false);
        }
    }
}
