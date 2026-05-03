package pl.durex.client.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.CommandExecutionC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.Vec3d;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MsgBot — port z msg-bot mod.
 * Wysyła /msg do graczy w promieniu 50 bloków.
 * Obsługuje Auto-CH (zmiana kanału) i Auto-Jump.
 */
public class MsgBotModule {

    private static final int SEARCH_RADIUS = 50;

    // ── Stan ──────────────────────────────────────────────────────────────
    private static volatile boolean enabled    = false;
    private static volatile boolean spamming   = false;
    private static volatile boolean autoCh     = false;

    private static Thread spamThread = null;
    private static Thread afkThread  = null;
    private static Thread chThread   = null;

    public static final Set<String> spammedPlayers = ConcurrentHashMap.newKeySet();
    private static volatile int totalSpammed = 0;
    private static volatile int nearbyCount  = 0;
    private static volatile String lastTarget = "";
    public  static volatile boolean sectorCooldown    = false;
    public  static volatile boolean waitingForConfirm = false;
    public  static final Object confirmLock = new Object();

    // ── Config ────────────────────────────────────────────────────────────
    public static String  message       = "";
    public static String  targetPlayer  = "";
    public static int     chDelaySec    = 30;
    public static boolean onlyNetherite = false;
    public static boolean onlyOffline   = false; // Stiv bez Premki — tylko cracked (UUID v3)

    // ── Gettery ───────────────────────────────────────────────────────────
    public static boolean isEnabled()  { return enabled; }
    public static void    setEnabled(boolean v) { enabled = v; }
    public static boolean isSpamming()    { return spamming; }
    public static boolean isAfk()         { return afkThread != null; }
    public static boolean isAutoCh()      { return autoCh; }
    public static int     getTotalSpammed(){ return totalSpammed; }
    public static int     getNearbyCount() { return nearbyCount; }
    public static String  getLastTarget()  { return lastTarget; }
    public static boolean isSectorCooldown(){ return sectorCooldown; }

    // ── Spam ──────────────────────────────────────────────────────────────

    public static void startSpam(int delayMs) {
        if (spamming || message.isEmpty()) return;
        spamming = true;

        final String msg    = message;
        final String target = targetPlayer;

        spamThread = new Thread(() -> {
            while (spamming) {
                try {
                    if (sectorCooldown) { Thread.sleep(500); continue; }

                    List<String> targets;
                    if (!target.isEmpty()) {
                        targets = Collections.singletonList(target);
                    } else {
                        targets = getNearbyPlayerNames();
                        nearbyCount = targets.size();
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
                            Thread.sleep(delayMs);
                        } else {
                            if (spammedPlayers.contains(name)) continue;
                            spammedPlayers.add(name);
                            lastTarget = name;
                            totalSpammed++;
                            final String cmd = "msg " + name + " " + msg;
                            waitingForConfirm = true;
                            MinecraftClient.getInstance().execute(() -> sendCommand(cmd));
                            synchronized (confirmLock) {
                                if (waitingForConfirm) confirmLock.wait(3000);
                            }
                            waitingForConfirm = false;
                        }
                    }

                    if (target.isEmpty()) Thread.sleep(500);

                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    System.err.println("[MsgBot] Błąd: " + e.getMessage());
                }
            }
            spamming = false;
        }, "DurexMsgBot-Spam");
        spamThread.setDaemon(true);
        spamThread.start();
    }

    public static void stopSpam() {
        spamming = false;
        if (spamThread != null) { spamThread.interrupt(); spamThread = null; }
    }

    public static void resetSpammedPlayers() {
        spammedPlayers.clear();
        totalSpammed = 0;
    }

    // ── AFK Mode ──────────────────────────────────────────────────────────
    // Co kilka sekund obraca głowę o losowy mały kąt i wysyła pakiet ruchu.
    // Serwer widzi aktywność i nie wyrzuca za bezczynność.

    public static void startAfk() {
        if (afkThread != null) return;
        java.util.Random rng = new java.util.Random();
        afkThread = new Thread(() -> {
            while (afkThread != null && !Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(3000 + rng.nextInt(2000)); // co 3-5s
                    MinecraftClient.getInstance().execute(() -> {
                        MinecraftClient mc = MinecraftClient.getInstance();
                        if (mc.player == null || mc.getNetworkHandler() == null) return;

                        // Losowy mały obrót głowy (±5 stopni)
                        float dyaw   = (rng.nextFloat() - 0.5f) * 10f;
                        float dpitch = (rng.nextFloat() - 0.5f) * 4f;
                        float newYaw   = mc.player.getYaw()   + dyaw;
                        float newPitch = Math.max(-90f, Math.min(90f, mc.player.getPitch() + dpitch));

                        mc.player.setYaw(newYaw);
                        mc.player.setPitch(newPitch);

                        // Wyślij pakiet ruchu — serwer widzi zmianę kąta
                        mc.getNetworkHandler().sendPacket(
                            new net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.LookAndOnGround(
                                newYaw, newPitch, mc.player.isOnGround(), false
                            )
                        );
                    });
                } catch (InterruptedException e) { break; }
            }
        }, "DurexMsgBot-AFK");
        afkThread.setDaemon(true);
        afkThread.start();
    }

    public static void stopAfk() {
        if (afkThread != null) { afkThread.interrupt(); afkThread = null; }
    }

    // ── Auto CH ───────────────────────────────────────────────────────────

    public static void startAutoCh() {
        if (autoCh) return;
        autoCh = true;
        AutoChHandler.enabled = true;
        AutoChHandler.reset();

        chThread = new Thread(() -> {
            while (autoCh) {
                try {
                    Thread.sleep(chDelaySec * 1000L);
                    if (!autoCh) break;
                    MinecraftClient.getInstance().execute(() -> {
                        MinecraftClient mc = MinecraftClient.getInstance();
                        if (mc.player != null)
                            mc.player.networkHandler.sendPacket(new CommandExecutionC2SPacket("ch"));
                    });
                } catch (InterruptedException e) { break; }
            }
            autoCh = false;
            AutoChHandler.enabled = false;
        }, "DurexMsgBot-CH");
        chThread.setDaemon(true);
        chThread.start();
    }

    public static void stopAutoCh() {
        autoCh = false;
        AutoChHandler.enabled = false;
        if (chThread != null) { chThread.interrupt(); chThread = null; }
    }

    // ── Sektor ────────────────────────────────────────────────────────────

    public static void onSectorChanged() {
        sectorCooldown = true;
        Thread t = new Thread(() -> {
            try {
                Thread.sleep(3000);
                sectorCooldown = false;
            } catch (InterruptedException ignored) {}
        });
        t.setDaemon(true);
        t.start();
    }

    // ── Chat confirm ──────────────────────────────────────────────────────

    public static void onChatReceived(String msg) {
        if (!waitingForConfirm) return;
        if (msg.contains(" -> ") && msg.contains(":")) {
            waitingForConfirm = false;
            synchronized (confirmLock) { confirmLock.notifyAll(); }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private static void sendCommand(String command) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        try {
            mc.player.networkHandler.sendPacket(new CommandExecutionC2SPacket(command));
        } catch (Exception e) {
            System.err.println("[MsgBot] sendCommand error: " + e.getMessage());
        }
    }

    private static List<String> getNearbyPlayerNames() {
        List<String> names = new ArrayList<>();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || mc.getNetworkHandler() == null) return names;

        Vec3d myPos = mc.player.getPos();
        for (AbstractClientPlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;
            if (player.getPos().distanceTo(myPos) > SEARCH_RADIUS) continue;
            if (onlyNetherite && !hasFullNetheriteArmor(player)) continue;
            if (onlyOffline   && !isCrackedAccount(player)) continue;

            String name = extractCleanName(mc, player);
            if (name != null && !name.isEmpty()) names.add(name);
        }
        return names;
    }

    /**
     * Cracked (offline) gracze mają UUID wersja 3 (MD5 hash od "OfflinePlayer:nick").
     * Premium gracze mają UUID wersja 4 (losowy Mojang).
     */
    private static boolean isCrackedAccount(AbstractClientPlayerEntity player) {
        return player.getUuid().version() == 3;
    }

    private static boolean hasFullNetheriteArmor(AbstractClientPlayerEntity player) {
        return player.getEquippedStack(EquipmentSlot.HEAD).isOf(Items.NETHERITE_HELMET)
            && player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.NETHERITE_CHESTPLATE)
            && player.getEquippedStack(EquipmentSlot.LEGS).isOf(Items.NETHERITE_LEGGINGS)
            && player.getEquippedStack(EquipmentSlot.FEET).isOf(Items.NETHERITE_BOOTS);
    }

    private static String extractCleanName(MinecraftClient mc, AbstractClientPlayerEntity player) {
        PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(player.getUuid());
        if (entry != null) {
            String n = entry.getProfile().getName();
            if (n != null && !n.contains("§")) return n;
        }
        String n = player.getGameProfile().getName();
        if (n != null && !n.contains("§")) return n;
        return player.getName().getString().replaceAll("§[0-9a-fk-or]", "").trim();
    }
}
