package pl.durex.autorynek.scanner;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import pl.durex.autorynek.config.ConfigManager;
import pl.durex.autorynek.config.PriceEntry;
import pl.durex.autorynek.config.ServerProfile;
import pl.durex.autorynek.util.ChatUtil;
import pl.durex.autorynek.util.PriceParser;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PriceLearnController {

    private static boolean active = false;
    private static long startTime = 0;
    private static int pagesScanned = 0;
    private static int offersCollected = 0;
    private static String lastPageSig = "";
    private static final Map<String, List<Double>> collected = new HashMap<>();

    public static void toggle() {
        if (active) stop(true);
        else start();
    }

    public static void start() {
        active = true;
        startTime = System.currentTimeMillis();
        pagesScanned = 0;
        offersCollected = 0;
        lastPageSig = "";
        collected.clear();
        ServerProfile p = ConfigManager.findProfile(getServer());
        int mins = (p != null && p.learnDurationMinutes != null) ? p.learnDurationMinutes : 120;
        ChatUtil.send("&a[Durex Auto Rynek] &fRozpoczeto nauke cen przez &e" + mins + " minut&f!");
        ChatUtil.send("&7Przegladaj rynek - mod zbiera ceny w tle i zaktualizuje config.");
    }

    public static void stop(boolean apply) {
        active = false;
        ChatUtil.send("&a[Durex Auto Rynek] &fNauka zakonczona. Zebrano &e"
            + offersCollected + " &fofert z &e" + pagesScanned + " &fstron.");
        if (apply && !collected.isEmpty()) applyPrices();
    }

    public static boolean isActive() { return active; }

    public static void onTick(Minecraft client) {
        if (!active || client.player == null) return;
        ServerProfile profile = ConfigManager.findProfile(getServer());
        if (profile == null) return;

        long elapsed = System.currentTimeMillis() - startTime;
        long duration = (long)(profile.learnDurationMinutes != null ? profile.learnDurationMinutes : 120) * 60_000;
        if (elapsed >= duration) { stop(true); return; }

        if (!(client.screen instanceof AbstractContainerScreen<?> screen)) return;
        String title = screen.getTitle().getString();
        if (profile.marketGuiTitle == null || !title.contains(profile.marketGuiTitle)) return;

        scanPage(client, screen, profile);
    }

    private static void scanPage(Minecraft client, AbstractContainerScreen<?> screen, ServerProfile profile) {
        List<Slot> slots = screen.getMenu().slots;

        StringBuilder sig = new StringBuilder();
        for (int i = 0; i < Math.min(slots.size(), 45); i++) {
            ItemStack s = slots.get(i).getItem();
            if (!s.isEmpty()) sig.append(s.getHoverName().getString()).append("|");
        }
        String signature = sig.toString();
        if (signature.isEmpty() || signature.equals(lastPageSig)) return;
        lastPageSig = signature;
        pagesScanned++;

        Pattern pattern;
        try { pattern = Pattern.compile(profile.loreRegex); }
        catch (Exception e) { return; }

        for (int i = 0; i < Math.min(slots.size(), 45); i++) {
            ItemStack stack = slots.get(i).getItem();
            if (stack.isEmpty()) continue;
            String name = stripColors(stack.getHoverName().getString());
            if (name.isEmpty()) continue;
            double price = extractPrice(client, stack, pattern);
            if (price > 0) {
                collected.computeIfAbsent(name, k -> new ArrayList<>()).add(price);
                offersCollected++;
            }
        }
    }

    private static void applyPrices() {
        ServerProfile profile = ConfigManager.findProfile(getServer());
        if (profile == null) return;

        int updated = 0, added = 0;
        int minSamples = profile.learnMinSamples != null ? profile.learnMinSamples : 5;
        double threshold = profile.learnOutlierThreshold != null ? profile.learnOutlierThreshold : 0.3;
        double buyMargin = profile.learnBuyMargin != null ? profile.learnBuyMargin : 0.95;
        double sellMargin = profile.learnSellMargin != null ? profile.learnSellMargin : 1.05;

        for (Map.Entry<String, List<Double>> entry : collected.entrySet()) {
            String name = entry.getKey();
            List<Double> prices = entry.getValue();
            if (prices.size() < minSamples) continue;

            double median = median(prices);
            List<Double> filtered = prices.stream()
                .filter(p -> p >= median * (1 - threshold) && p <= median * (1 + threshold))
                .toList();
            if (filtered.size() < 3) continue;

            double cleanMedian = median(filtered);
            double newMax = Math.floor(cleanMedian * buyMargin);
            double newSell = Math.floor(cleanMedian * sellMargin);

            PriceEntry existing = findEntry(profile, name);
            if (existing != null) {
                double change = Math.abs(existing.maxPrice - newMax) / Math.max(existing.maxPrice, 1);
                if (change > 0.05) {
                    existing.maxPrice = newMax;
                    if (Boolean.TRUE.equals(existing.sellEnabled)) existing.sellPrice = newSell;
                    updated++;
                }
            } else if (newSell > newMax * 1.05) {
                PriceEntry e = new PriceEntry();
                e.name = name;
                e.maxPrice = newMax;
                e.sellPrice = newSell;
                e.buyEnabled = true;
                e.sellEnabled = true;
                e.requiredCount = 1;
                e.sellCount = 1;
                e.componentCount = 0;
                profile.prices.add(e);
                added++;
            }
        }

        ConfigManager.save();
        ChatUtil.send("&a[Durex Auto Rynek] &fZaktualizowano &e" + updated
            + " &fcen, dodano &e" + added + " &fnowych przedmiotow do configu.");
    }

    private static double extractPrice(Minecraft client, ItemStack stack, Pattern pattern) {
        List<Component> tooltip = stack.getTooltipLines(
            net.minecraft.world.item.Item.TooltipContext.of(client.level),
            client.player,
            TooltipFlag.Default.NORMAL
        );
        for (Component line : tooltip) {
            Matcher m = pattern.matcher(line.getString());
            if (m.find()) {
                try { return PriceParser.parse(m.group(1)); } catch (Exception ignored) {}
            }
        }
        return -1;
    }

    private static PriceEntry findEntry(ServerProfile profile, String name) {
        for (PriceEntry e : profile.prices) {
            if (e.name != null && stripColors(e.name).equalsIgnoreCase(name)) return e;
        }
        return null;
    }

    private static double median(List<Double> vals) {
        List<Double> s = new ArrayList<>(vals);
        Collections.sort(s);
        int n = s.size();
        return n % 2 == 0 ? (s.get(n/2-1) + s.get(n/2)) / 2.0 : s.get(n/2);
    }

    private static String stripColors(String s) {
        return s.replaceAll("§[0-9a-fk-or]", "").trim();
    }

    private static String getServer() {
        Minecraft c = Minecraft.getInstance();
        return c.getCurrentServer() != null ? c.getCurrentServer().ip : "";
    }

    public static String getStatus() {
        if (!active) return "§7IDLE";
        long mins = (System.currentTimeMillis() - startTime) / 60_000;
        return "§aUCZY SIE §7(" + mins + "min, " + offersCollected + " ofert)";
    }
}
