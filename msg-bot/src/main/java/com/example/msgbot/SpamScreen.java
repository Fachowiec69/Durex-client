package com.example.msgbot;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.CommandExecutionC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SpamScreen extends Screen {

    // === STAN GLOBALNY ===
    private static volatile boolean spamming = false;
    private static volatile boolean jumping = false;
    private static volatile boolean autoCh = false;
    private static Thread spamThread = null;
    private static Thread jumpThread = null;
    private static Thread chThread = null;
    // Globalna lista - przezywa zmiany sektorow
    public static final Set<String> spammedPlayers = ConcurrentHashMap.newKeySet();
    private static volatile int totalSpammed = 0;
    private static volatile int nearbyCount = 0;
    private static volatile String lastTarget = "";
    // Delay po zmianie sektora
    public static volatile boolean sectorCooldown = false;
    // Synchronizacja chat confirm
    public static volatile boolean waitingForConfirm = false;
    public static final Object confirmLock = new Object();

    private static final int SEARCH_RADIUS = 50;

    // === GETTERY DLA HUD ===
    public static boolean isSpamming() { return spamming; }
    public static boolean isJumping() { return jumping; }
    public static boolean isAutoCh() { return autoCh; }
    public static int getTotalSpammed() { return totalSpammed; }
    public static int getNearbyCount() { return nearbyCount; }
    public static String getLastTarget() { return lastTarget; }
    public static boolean isSectorCooldown() { return sectorCooldown; }

    // Widgety GUI
    private TextFieldWidget messageField;
    private TextFieldWidget playerField;
    private TextFieldWidget jumpDelayField;
    private TextFieldWidget chDelayField;
    private ButtonWidget startBtn500;
    private ButtonWidget startBtn100;
    private ButtonWidget stopBtn;
    private ButtonWidget jumpStartBtn;
    private ButtonWidget jumpStopBtn;
    private ButtonWidget netheriteBtn;
    private ButtonWidget chStartBtn;
    private ButtonWidget chStopBtn;

    public SpamScreen() {
        super(Text.literal("Msg Spammer"));
        MsgBot.LOGGER.info("[MsgBot] GUI otwarte");
    }

    @Override
    protected void init() {
        ModConfig cfg = ModConfig.get();
        int cx = this.width / 2;
        int sy = this.height / 2 - 100;

        // Wiadomosc
        messageField = new TextFieldWidget(this.textRenderer, cx - 150, sy, 300, 20, Text.literal("Wiadomosc"));
        messageField.setMaxLength(256);
        messageField.setPlaceholder(Text.literal("Wpisz wiadomosc..."));
        messageField.setText(cfg.message);
        this.addDrawableChild(messageField);

        // Nick gracza
        playerField = new TextFieldWidget(this.textRenderer, cx - 150, sy + 30, 300, 20, Text.literal("Nick"));
        playerField.setMaxLength(64);
        playerField.setPlaceholder(Text.literal("Nick gracza (puste = wszyscy w zasiegu)"));
        playerField.setText(cfg.targetPlayer);
        this.addDrawableChild(playerField);

        // START / STOP spam
        startBtn500 = ButtonWidget.builder(Text.literal("START 500ms"), btn -> startSpam(500))
                .dimensions(cx - 155, sy + 60, 150, 20).build();
        this.addDrawableChild(startBtn500);

        startBtn100 = ButtonWidget.builder(Text.literal("START 100ms"), btn -> startSpam(100))
                .dimensions(cx + 5, sy + 60, 150, 20).build();
        this.addDrawableChild(startBtn100);

        stopBtn = ButtonWidget.builder(Text.literal("STOP"), btn -> stopSpam())
                .dimensions(cx - 50, sy + 90, 100, 20).build();
        this.addDrawableChild(stopBtn);

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Reset listy"), btn -> {
            resetSpammedPlayers();
        }).dimensions(cx - 50, sy + 115, 100, 20).build());

        // Filtry
        netheriteBtn = ButtonWidget.builder(Text.literal("Only Netherite: OFF"), btn -> {
            cfg.onlyNetherite = !cfg.onlyNetherite;
            ModConfig.save();
            updateButtonColors();
        }).dimensions(cx - 155, sy + 145, 310, 20).build();
        this.addDrawableChild(netheriteBtn);

        // Jump
        jumpDelayField = new TextFieldWidget(this.textRenderer, cx - 150, sy + 175, 80, 20, Text.literal("Jump delay"));
        jumpDelayField.setMaxLength(5);
        jumpDelayField.setText(cfg.jumpDelay);
        this.addDrawableChild(jumpDelayField);

        jumpStartBtn = ButtonWidget.builder(Text.literal("JUMP ON"), btn -> startJump())
                .dimensions(cx - 60, sy + 175, 60, 20).build();
        this.addDrawableChild(jumpStartBtn);

        jumpStopBtn = ButtonWidget.builder(Text.literal("JUMP OFF"), btn -> stopJump())
                .dimensions(cx + 5, sy + 175, 70, 20).build();
        this.addDrawableChild(jumpStopBtn);

        // Auto CH
        chDelayField = new TextFieldWidget(this.textRenderer, cx - 150, sy + 205, 80, 20, Text.literal("CH delay"));
        chDelayField.setMaxLength(5);
        chDelayField.setText(cfg.chDelay);
        this.addDrawableChild(chDelayField);

        chStartBtn = ButtonWidget.builder(Text.literal("CH ON"), btn -> startAutoCh())
                .dimensions(cx - 60, sy + 205, 60, 20).build();
        this.addDrawableChild(chStartBtn);

        chStopBtn = ButtonWidget.builder(Text.literal("CH OFF"), btn -> stopAutoCh())
                .dimensions(cx + 5, sy + 205, 70, 20).build();
        this.addDrawableChild(chStopBtn);

        updateButtonColors();
    }

    private void updateButtonColors() {
        ModConfig cfg = ModConfig.get();
        if (startBtn500 == null) return;
        startBtn500.setMessage(Text.literal(spamming ? "§a§lSTART 500ms" : "§7START 500ms"));
        startBtn100.setMessage(Text.literal(spamming ? "§a§lSTART 100ms" : "§7START 100ms"));
        stopBtn.setMessage(Text.literal(spamming ? "§c§lSTOP" : "§7STOP"));
        if (jumpStartBtn != null) jumpStartBtn.setMessage(Text.literal(jumping ? "§a§lJUMP ON" : "§7JUMP ON"));
        if (jumpStopBtn != null) jumpStopBtn.setMessage(Text.literal(jumping ? "§c§lJUMP OFF" : "§7JUMP OFF"));
        if (netheriteBtn != null) netheriteBtn.setMessage(Text.literal(cfg.onlyNetherite ? "§5§lOnly Netherite: ON" : "§7Only Netherite: OFF"));
        if (chStartBtn != null) chStartBtn.setMessage(Text.literal(autoCh ? "§a§lCH ON" : "§7CH ON"));
        if (chStopBtn != null) chStopBtn.setMessage(Text.literal(autoCh ? "§c§lCH OFF" : "§7CH OFF"));
    }

    // Wywolywane przez AutoChHandler po zmianie sektora
    public static void onSectorChanged() {
        sectorCooldown = true;
        MsgBot.LOGGER.info("[MsgBot] Zmiana sektora - cooldown 3s, zapisuje liste spamowanych");
        // Zapisz liste do configu
        ModConfig.get().spammedPlayers = new HashSet<>(spammedPlayers);
        ModConfig.get().totalSpammed = totalSpammed;
        ModConfig.save();

        Thread t = new Thread(() -> {
            try {
                Thread.sleep(3000);
                sectorCooldown = false;
                MsgBot.LOGGER.info("[MsgBot] Cooldown po zmianie sektora minął - wznawiamy spam");
            } catch (InterruptedException ignored) {}
        });
        t.setDaemon(true);
        t.start();
    }

    public static void resetSpammedPlayers() {
        spammedPlayers.clear();
        totalSpammed = 0;
        ModConfig.get().spammedPlayers.clear();
        ModConfig.get().totalSpammed = 0;
        ModConfig.save();
        MsgBot.LOGGER.info("[MsgBot] Lista spamowanych zresetowana i zapisana");
    }

    public static void onChatReceived(String msg) {
        if (!waitingForConfirm) return;
        if (msg.contains(" -> ") && msg.contains(":")) {
            MsgBot.LOGGER.info("[MsgBot] Potwierdzenie z chatu: '{}'", msg);
            waitingForConfirm = false;
            synchronized (confirmLock) {
                confirmLock.notifyAll();
            }
        }
    }

    private void sendCommand(String command) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) {
            MsgBot.LOGGER.warn("[MsgBot] sendCommand: player == null, pomijam: {}", command);
            return;
        }
        try {
            mc.player.networkHandler.sendPacket(new CommandExecutionC2SPacket(command));
            MsgBot.LOGGER.info("[MsgBot] Wyslano: /{}", command);
        } catch (Exception e) {
            MsgBot.LOGGER.error("[MsgBot] BLAD: {}", e.getMessage(), e);
        }
    }

    private void startSpam(int delayMs) {
        if (spamming) return;

        ModConfig cfg = ModConfig.get();
        cfg.message = messageField.getText().trim();
        cfg.targetPlayer = playerField.getText().trim();
        ModConfig.save();

        if (cfg.message.isEmpty()) return;

        // Wczytaj zapisana liste spamowanych z configu
        spammedPlayers.addAll(cfg.spammedPlayers);
        totalSpammed = cfg.totalSpammed;

        spamming = true;
        updateButtonColors();
        MsgBot.LOGGER.info("[MsgBot] START SPAM | delay={}ms | msg='{}' | cel='{}'",
                delayMs, cfg.message, cfg.targetPlayer.isEmpty() ? "(wszyscy)" : cfg.targetPlayer);

        final String msg = cfg.message;
        final String target = cfg.targetPlayer;

        spamThread = new Thread(() -> {
            while (spamming) {
                try {
                    // Czekaj jesli jest cooldown po zmianie sektora
                    if (sectorCooldown) {
                        MsgBot.LOGGER.info("[MsgBot] Czekam na cooldown sektora...");
                        Thread.sleep(500);
                        continue;
                    }

                    List<String> targets;
                    if (!target.isEmpty()) {
                        targets = Collections.singletonList(target);
                    } else {
                        targets = getNearbyPlayerNames();
                        nearbyCount = targets.size();
                        MsgBot.LOGGER.info("[MsgBot] Skan: {} graczy: {}", targets.size(), targets);
                    }

                    for (String name : targets) {
                        if (!spamming || sectorCooldown) break;

                        if (!target.isEmpty()) {
                            lastTarget = name;
                            final String cmd = "msg " + name + " " + msg;
                            waitingForConfirm = true;
                            MinecraftClient.getInstance().execute(() -> sendCommand(cmd));
                            synchronized (confirmLock) {
                                if (waitingForConfirm) confirmLock.wait(3000);
                            }
                            waitingForConfirm = false;
                        } else {
                            if (spammedPlayers.contains(name)) continue;
                            spammedPlayers.add(name);
                            lastTarget = name;
                            final String cmd = "msg " + name + " " + msg;
                            waitingForConfirm = true;
                            MinecraftClient.getInstance().execute(() -> sendCommand(cmd));
                            synchronized (confirmLock) {
                                if (waitingForConfirm) confirmLock.wait(3000);
                            }
                            waitingForConfirm = false;
                            totalSpammed++;
                            // Zapisz liste po kazdym nowym graczu
                            ModConfig.get().spammedPlayers = new HashSet<>(spammedPlayers);
                            ModConfig.get().totalSpammed = totalSpammed;
                            ModConfig.save();
                        }
                    }

                    if (target.isEmpty()) Thread.sleep(500);

                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    MsgBot.LOGGER.error("[MsgBot] BLAD w watku: {}", e.getMessage(), e);
                }
            }
            spamming = false;
            MsgBot.LOGGER.info("[MsgBot] Watek spamu zakonczony");
        });
        spamThread.setDaemon(true);
        spamThread.setName("MsgBot-SpamThread");
        spamThread.start();
    }

    private void stopSpam() {
        spamming = false;
        if (spamThread != null) { spamThread.interrupt(); spamThread = null; }
        updateButtonColors();
    }

    private void startJump() {
        if (jumping) return;
        ModConfig cfg = ModConfig.get();
        if (jumpDelayField != null) cfg.jumpDelay = jumpDelayField.getText().trim();
        ModConfig.save();

        int delaySec;
        try { delaySec = Math.max(1, Integer.parseInt(cfg.jumpDelay)); }
        catch (NumberFormatException e) { delaySec = 3; }

        jumping = true;
        updateButtonColors();
        final int fd = delaySec;
        jumpThread = new Thread(() -> {
            while (jumping) {
                try {
                    Thread.sleep(fd * 1000L);
                    MinecraftClient.getInstance().execute(() -> {
                        MinecraftClient mc = MinecraftClient.getInstance();
                        if (mc.player != null) { mc.player.jump(); MsgBot.LOGGER.info("[MsgBot] JUMP!"); }
                    });
                } catch (InterruptedException e) { break; }
            }
            jumping = false;
        });
        jumpThread.setDaemon(true);
        jumpThread.setName("MsgBot-JumpThread");
        jumpThread.start();
    }

    private void stopJump() {
        jumping = false;
        if (jumpThread != null) { jumpThread.interrupt(); jumpThread = null; }
        updateButtonColors();
    }

    private void startAutoCh() {
        if (autoCh) return;
        ModConfig cfg = ModConfig.get();
        if (chDelayField != null) cfg.chDelay = chDelayField.getText().trim();
        ModConfig.save();

        int delaySec;
        try { delaySec = Math.max(5, Integer.parseInt(cfg.chDelay)); }
        catch (NumberFormatException e) { delaySec = 30; }

        autoCh = true;
        AutoChHandler.enabled = true;
        AutoChHandler.reset();
        updateButtonColors();

        final int fd = delaySec;
        chThread = new Thread(() -> {
            while (autoCh) {
                try {
                    Thread.sleep(fd * 1000L);
                    if (!autoCh) break;
                    MinecraftClient.getInstance().execute(() -> {
                        MinecraftClient mc = MinecraftClient.getInstance();
                        if (mc.player != null) {
                            MsgBot.LOGGER.info("[MsgBot] AUTO-CH: otwieram /ch");
                            mc.player.networkHandler.sendPacket(new CommandExecutionC2SPacket("ch"));
                        }
                    });
                } catch (InterruptedException e) { break; }
            }
            autoCh = false;
            AutoChHandler.enabled = false;
        });
        chThread.setDaemon(true);
        chThread.setName("MsgBot-ChThread");
        chThread.start();
    }

    private void stopAutoCh() {
        autoCh = false;
        AutoChHandler.enabled = false;
        if (chThread != null) { chThread.interrupt(); chThread = null; }
        updateButtonColors();
    }

    private boolean isOfflinePlayer(AbstractClientPlayerEntity player) {
        // Premium (online) gracze maja UUID wersja 4 (losowy Mojang)
        // Cracked (offline) gracze maja UUID wersja 3 (MD5 hash od "OfflinePlayer:nick")
        // UUID.version() zwraca 4 dla premium, 3 dla offline
        java.util.UUID uuid = player.getUuid();
        int version = uuid.version();
        MsgBot.LOGGER.info("[MsgBot] Gracz {} UUID={} wersja={}", 
                player.getGameProfile().getName(), uuid, version);
        return version == 3; // 3 = offline/cracked
    }

    private boolean hasFullNetheriteArmor(AbstractClientPlayerEntity player) {
        return player.getEquippedStack(EquipmentSlot.HEAD).isOf(Items.NETHERITE_HELMET)
            && player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.NETHERITE_CHESTPLATE)
            && player.getEquippedStack(EquipmentSlot.LEGS).isOf(Items.NETHERITE_LEGGINGS)
            && player.getEquippedStack(EquipmentSlot.FEET).isOf(Items.NETHERITE_BOOTS);
    }

    private boolean hasRank(AbstractClientPlayerEntity player) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getNetworkHandler() == null) return false;

        String rankPrefix = ModConfig.get().rankPrefix.trim();

        // Sprawdz display name gracza - ranga to tekst PRZED nickiem
        String displayName = player.getDisplayName().getString();
        String cleanNick = player.getGameProfile().getName();

        // Jesli display name rozni sie od samego nicku - gracz ma prefix (range)
        boolean hasAnyRank = !displayName.equals(cleanNick) && displayName.contains(cleanNick);

        if (!hasAnyRank) return false;

        // Jesli podano konkretny prefix - sprawdz czy go ma
        if (!rankPrefix.isEmpty()) {
            return displayName.contains(rankPrefix);
        }

        return true;
    }

    private List<String> getNearbyPlayerNames() {
        List<String> names = new ArrayList<>();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null || client.getNetworkHandler() == null) return names;

        ModConfig cfg = ModConfig.get();
        Vec3d myPos = client.player.getPos();

        for (AbstractClientPlayerEntity player : client.world.getPlayers()) {
            if (player == client.player) continue;
            double dist = player.getPos().distanceTo(myPos);
            if (dist > SEARCH_RADIUS) continue;

            // Filtr netherite
            if (cfg.onlyNetherite && !hasFullNetheriteArmor(player)) continue;

            // Czysty nick
            String cleanName = extractCleanName(client, player);
            if (cleanName != null && !cleanName.isEmpty()) {
                names.add(cleanName);
                MsgBot.LOGGER.info("[MsgBot] Gracz: '{}' dist={}", cleanName, (int)dist);
            }
        }
        return names;
    }

    private String extractCleanName(MinecraftClient client, AbstractClientPlayerEntity player) {
        String cleanName = null;
        PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(player.getUuid());
        if (entry != null) {
            String n = entry.getProfile().getName();
            if (n != null && !n.contains("§")) cleanName = n;
        }
        if (cleanName == null) {
            String n = player.getGameProfile().getName();
            if (n != null && !n.contains("§")) cleanName = n;
        }
        if (cleanName == null) {
            cleanName = player.getName().getString().replaceAll("§[0-9a-fk-or]", "").trim();
        }
        return cleanName;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        this.renderBackground(ctx, mouseX, mouseY, delta);
        super.render(ctx, mouseX, mouseY, delta);
        updateButtonColors();

        int cx = this.width / 2;
        int sy = this.height / 2 - 100;

        ctx.drawCenteredTextWithShadow(this.textRenderer, this.title, cx, sy - 12, 0xFFFFFF);
        ctx.drawTextWithShadow(this.textRenderer, Text.literal("§7Wiadomosc:"), cx - 150, sy - 10, 0xAAAAAA);
        ctx.drawTextWithShadow(this.textRenderer, Text.literal("§7Nick (opcjonalny):"), cx - 150, sy + 20, 0xAAAAAA);
        ctx.drawTextWithShadow(this.textRenderer, Text.literal("§7Jump co (s):"), cx - 150, sy + 165, 0xAAAAAA);
        ctx.drawTextWithShadow(this.textRenderer, Text.literal("§7CH co (s):"), cx - 150, sy + 195, 0xAAAAAA);

        String spamStatus = spamming ? (sectorCooldown ? "§eCooldown sektora..." : "§aSpamuje...") : "§7Stop";
        ctx.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("Spam: " + spamStatus + "  §7Spamowanych: §a" + totalSpammed),
                cx, sy + 232, 0xFFFFFF);
        ctx.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("§7W zasiegu: §e" + nearbyCount + (lastTarget.isEmpty() ? "" : "  §7Cel: §b" + lastTarget)),
                cx, sy + 244, 0xFFFFFF);

        String jumpStatus = jumping ? "§aON" : "§7OFF";
        String chStatus = autoCh ? "§aON" : "§7OFF";
        ctx.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("Jump: " + jumpStatus + "  CH: " + chStatus),
                cx, sy + 256, 0xFFFFFF);
    }

    @Override
    public void close() {
        ModConfig cfg = ModConfig.get();
        if (messageField != null) cfg.message = messageField.getText().trim();
        if (playerField != null) cfg.targetPlayer = playerField.getText().trim();
        if (jumpDelayField != null) cfg.jumpDelay = jumpDelayField.getText().trim();
        if (chDelayField != null) cfg.chDelay = chDelayField.getText().trim();
        cfg.spammedPlayers = new HashSet<>(spammedPlayers);
        cfg.totalSpammed = totalSpammed;
        ModConfig.save();
        MsgBot.LOGGER.info("[MsgBot] GUI zamkniete, config zapisany");
        super.close();
    }

    @Override
    public boolean shouldPause() { return false; }
}
