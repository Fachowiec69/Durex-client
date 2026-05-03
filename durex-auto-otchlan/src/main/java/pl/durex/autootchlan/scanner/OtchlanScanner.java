package pl.durex.autootchlan.scanner;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import pl.durex.autootchlan.config.ConfigManager;
import pl.durex.autootchlan.config.ModConfig;
import pl.durex.autootchlan.config.PriorityEntry;
import pl.durex.autootchlan.util.ChatUtil;

import java.util.*;
import java.util.concurrent.*;

/**
 * OtchlanScanner
 *
 * GUI otchłani ma 9×6 = 54 sloty:
 *   - Kolumna 0 i 8 (sloty %9==0 i %9==8) = zielone = nawigacja
 *   - Rząd 0 (sloty 0-8) i rząd 5 (sloty 45-53) = nawigacja
 *   - Itemy są w slotach 10-16, 19-25, 28-34, 37-43 (rzędy 1-4, kolumny 1-7)
 *
 * Mechanizm pobierania:
 *   QUICK_MOVE (shift+click) = przenosi item do ekwipunku gracza
 *   Wysyłamy pakiety dla wszystkich itemów naraz (burst).
 *
 * Następna strona:
 *   Slot nextPageSlot (domyślnie 53 = prawy dolny róg) = zielony barwnik
 *   Klik lewym = przejście na następną stronę
 */
public class OtchlanScanner {

    private enum State { IDLE, GRABBING, DUMPING_INV, WAITING_PAGE, GOING_NEXT }

    private static volatile State   state         = State.IDLE;
    private static volatile boolean active        = false;
    private static volatile int     currentPage   = 0;
    private static volatile int     lastSyncId    = -1;
    private static volatile long    stateEnteredAt = 0;
    private static volatile long    pageClickedAt  = 0;

    private static final Set<Integer> grabbedSlots = ConcurrentHashMap.newKeySet();

    private static final ScheduledExecutorService SCHEDULER =
        Executors.newScheduledThreadPool(1, r -> {
            Thread t = new Thread(r, "DurexOtchlanSpam");
            t.setDaemon(true);
            t.setPriority(Thread.MAX_PRIORITY);
            return t;
        });
    private static ScheduledFuture<?> spamFuture = null;

    private static volatile int  sessionGrabbed = 0;
    private static volatile int  sessionPages   = 0;
    private static volatile long sessionStart   = 0;

    // ── API ───────────────────────────────────────────────────────────────────
    public static void toggle() { if (active) stop(); else start(); }

    public static void start() {
        active        = true;
        state         = State.IDLE;
        sessionGrabbed = 0;
        sessionPages   = 0;
        sessionStart   = System.currentTimeMillis();
        currentPage    = 0;
        lastSyncId     = -1;
        grabbedSlots.clear();
        ChatUtil.send("&a[DAO] &fAktywny! Czekam na otchlan... &7(HOME=stop)");
    }

    public static void stop() {
        active = false;
        state  = State.IDLE;
        cancelSpam();
        grabbedSlots.clear();
        long elapsed = (System.currentTimeMillis() - sessionStart) / 1000;
        ChatUtil.send("&a[DAO] &fStop | Zebrano: &e" + sessionGrabbed
            + " &7z &e" + sessionPages + " &7stron | &e" + elapsed + "s");
    }

    public static void printStatus() {
        if (!active) { ChatUtil.send("&7[DAO] Nieaktywny. /dao start"); return; }
        ChatUtil.send("&a[DAO] &fStan: &e" + state + " &7Str: &f#" + currentPage
            + " &7Zebrano: &f" + sessionGrabbed + " &7Stron: &f" + sessionPages);
    }

    public static boolean isActive()       { return active; }
    public static int  getSessionThrown()  { return sessionGrabbed; }
    public static int  getSessionPages()   { return sessionPages; }
    public static int  getCurrentPage()    { return currentPage; }
    public static long getSessionStart()   { return sessionStart; }

    // ── Chat callbacks ────────────────────────────────────────────────────────
    public static void onCountdown(int seconds) {
        if (!active) return;
        ChatUtil.send("&e[DAO] &7Odliczanie: &f" + seconds + "s");
        if (seconds <= 1) startCommandSpam();
    }

    public static void onOtchlanOpened() {
        if (!active) return;
        currentPage = 0;
        grabbedSlots.clear();
        startCommandSpam();
    }

    // ── Spam komendy ──────────────────────────────────────────────────────────
    private static void startCommandSpam() {
        cancelSpam();
        ModConfig cfg = ConfigManager.get();
        final int[] count = {0};
        spamFuture = SCHEDULER.scheduleWithFixedDelay(() -> {
            try {
                if (!active || count[0] >= cfg.spamCount) { cancelSpam(); return; }
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null) {
                    mc.execute(() -> {
                        if (mc.player == null) return;
                        String cmd = cfg.command.startsWith("/")
                            ? cfg.command.substring(1) : cfg.command;
                        mc.player.connection.sendCommand(cmd);
                    });
                }
                count[0]++;
            } catch (Exception ignored) {}
        }, 0L, cfg.spamIntervalMs, TimeUnit.MILLISECONDS);
    }

    private static void cancelSpam() {
        if (spamFuture != null) { spamFuture.cancel(false); spamFuture = null; }
    }

    // ── GŁÓWNA PĘTLA — co tick ────────────────────────────────────────────────
    public static void onTick(Minecraft mc) {
        if (!active || mc.player == null) return;
        if (!(mc.screen instanceof AbstractContainerScreen<?> screen)) return;

        String title = stripColors(screen.getTitle().getString());
        if (!isOtchlanTitle(title.toLowerCase())) return;

        int syncId = screen.getMenu().containerId;
        long now   = System.currentTimeMillis();
        ModConfig cfg = ConfigManager.get();

        // Nowy syncId = nowa strona otwarta
        if (syncId != lastSyncId) {
            lastSyncId     = syncId;
            stateEnteredAt = now;
            grabbedSlots.clear();
            int page = parsePageNumber(title);
            if (page > 0) currentPage = page;
            state = State.GRABBING;
            ChatUtil.send("&a[DAO] &7Strona &f#" + currentPage + " &7— grabuje...");
            return; // jeden tick przerwy żeby sloty się załadowały
        }

        switch (state) {

            case GRABBING -> {
                // Czekaj openDelayMs żeby serwer załadował sloty
                if (now - stateEnteredAt < cfg.openDelayMs) return;

                // Sprawdź wolne sloty — jeśli 0, wyrzuć ekwipunek najpierw
                int freeSlots = countFreeInventorySlots(mc);
                if (freeSlots == 0) {
                    ChatUtil.send("&e[DAO] &7Eq pelne — wyrzucam wszystko na ziemie...");
                    dumpInventory(mc);
                    state          = State.DUMPING_INV;
                    stateEnteredAt = now;
                    return;
                }

                int grabbed = doQuickMoveAll(mc, screen, cfg, syncId);
                ChatUtil.send("&7[DAO] Zebrano: &f" + grabbed + " &7itemow");

                state          = State.WAITING_PAGE;
                stateEnteredAt = now;
            }

            case DUMPING_INV -> {
                // Czekaj 500ms żeby pakiety wyrzucenia dotarły do serwera
                if (now - stateEnteredAt < 500) return;
                // Ponownie otwórz otchłań
                startCommandSpam();
                state          = State.IDLE; // onTick wykryje nowe GUI przez syncId
                stateEnteredAt = now;
            }

            case WAITING_PAGE -> {
                // Czekaj pageDelayMs żeby serwer przetworzył kliknięcia
                if (now - stateEnteredAt < cfg.pageDelayMs) return;

                // Resetuj lastSyncId PRZED kliknięciem — żeby nowe GUI (nawet z tym samym syncId)
                // zostało wykryte jako "nowa strona"
                lastSyncId = -1;

                boolean went = clickNextPage(mc, screen, cfg, syncId);
                if (went) {
                    state         = State.GOING_NEXT;
                    pageClickedAt = now;
                } else {
                    onFinished();
                }
            }

            case GOING_NEXT -> {
                // Czekaj na nowy syncId (nowa strona) — max 5s
                // Nowe GUI wykryte przez syncId != lastSyncId na początku onTick
                if (now - pageClickedAt > 5000) {
                    // Timeout — spróbuj jeszcze raz kliknąć lub zakończ
                    ChatUtil.send("&c[DAO] Timeout strony — koniec.");
                    onFinished();
                }
            }

            default -> {}
        }
    }

    // ── QUICK_MOVE (shift+click) — pobiera item do ekwipunku ─────────────────
    private static int doQuickMoveAll(Minecraft mc, AbstractContainerScreen<?> screen,
                                       ModConfig cfg, int syncId) {
        var menu = mc.player.containerMenu;
        if (menu == null || menu.containerId != syncId) return 0;

        ClientPacketListener conn = mc.player.connection;
        if (conn == null) return 0;

        List<Slot> slots = menu.slots;

        // Zbierz sloty do pobrania — sortuj priorytety pierwsze
        List<Slot> toGrab = new ArrayList<>();
        for (int i = 0; i < Math.min(slots.size(), 54); i++) {
            Slot slot = slots.get(i);
            if (slot == null || slot.getItem().isEmpty()) continue;
            if (grabbedSlots.contains(i)) continue;
            if (isNavSlot(i, cfg)) continue;
            toGrab.add(slot);
        }

        if (toGrab.isEmpty()) return 0;

        // Sortuj — priorytety pierwsze
        toGrab.sort((a, b) -> Integer.compare(
            getPriorityLevel(b.getItem()),
            getPriorityLevel(a.getItem())
        ));

        // Ogranicz do wolnych slotów
        int freeSlots = countFreeInventorySlots(mc);
        if (freeSlots == 0) return 0;
        if (toGrab.size() > freeSlots) {
            toGrab = toGrab.subList(0, freeSlots);
        }

        // Wyślij QUICK_MOVE przez gameMode — najbardziej kompatybilny sposób
        int count = 0;
        for (Slot slot : toGrab) {
            if (slot.getItem().isEmpty()) continue;
            grabbedSlots.add(slot.index);

            mc.gameMode.handleInventoryMouseClick(
                syncId,
                slot.index,
                0,
                ClickType.QUICK_MOVE,
                mc.player
            );

            count++;
            sessionGrabbed++;
        }
        return count;
    }

    /**
     * Wyrzuca cały ekwipunek gracza na ziemię.
     *
     * Gdy GUI otchłani jest otwarte, nie można modyfikować player inventory
     * przez containerId=0 — serwer to ignoruje.
     * Rozwiązanie: zamknij GUI → wyrzuć eq → poczekaj → otwórz otchłań ponownie.
     */
    private static void dumpInventory(Minecraft mc) {
        if (mc.player == null) return;

        // 1. Zamknij GUI otchłani
        mc.player.closeContainer();
        mc.setScreen(null);

        // 2. Wyrzuć wszystkie itemy z ekwipunku (sloty 0-35)
        //    Używamy ServerboundPlayerActionPacket z DROP_ALL_ITEMS
        //    lub po prostu wysyłamy komendy drop przez chat — ale najprościej:
        //    Symulujemy naciśnięcie Q na każdym slocie przez gameMode.handleInventoryMouseClick
        var inv = mc.player.getInventory();
        ClientPacketListener conn = mc.player.connection;
        if (conn == null) return;

        // Player inventory containerId = 0, ale tylko gdy GUI jest ZAMKNIĘTE
        // Po closeContainer() containerMenu = player's own inventory (containerId=0)
        var menu = mc.player.containerMenu;
        int stateId = menu.getStateId();

        for (int i = 0; i < 36; i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) continue;

            var changed = new Int2ObjectOpenHashMap<ItemStack>();
            changed.put(i, ItemStack.EMPTY);

            conn.send(new ServerboundContainerClickPacket(
                0,              // containerId=0 = player inventory
                stateId,
                i,
                1,              // button=1 = cały stack (Ctrl+Q)
                ClickType.THROW,
                ItemStack.EMPTY,
                changed
            ));
        }

        ChatUtil.send("&a[DAO] &7Wyrzucono eq. Ponownie otwieram otchlan...");

        // 3. Zresetuj stan — po ponownym otwarciu GUI wykryje nowy syncId
        lastSyncId = -1;
        grabbedSlots.clear();
        state = State.IDLE;
    }

    /**
     * Liczy wolne sloty w ekwipunku gracza (sloty 0-35).
     */
    private static int countFreeInventorySlots(Minecraft mc) {
        if (mc.player == null) return 0;
        var inv = mc.player.getInventory();
        int free = 0;
        for (int i = 0; i < 36; i++) {
            if (inv.getItem(i).isEmpty()) free++;
        }
        return free;
    }

    // ── Klik następnej strony ─────────────────────────────────────────────────
    private static boolean clickNextPage(Minecraft mc, AbstractContainerScreen<?> screen,
                                          ModConfig cfg, int syncId) {
        var menu = mc.player.containerMenu;
        if (menu == null || menu.containerId != syncId) return false;

        List<Slot> slots = menu.slots;
        int nextSlot = cfg.nextPageSlot;

        if (nextSlot < 0 || nextSlot >= slots.size()) return false;

        Slot slot = slots.get(nextSlot);
        if (slot == null || slot.getItem().isEmpty()) return false;

        // Użyj gameMode.handleInventoryMouseClick — dokładnie jak vanilla klik
        // To jest najbardziej kompatybilny sposób klikania slotów w custom GUI
        mc.gameMode.handleInventoryMouseClick(
            syncId,      // containerId
            nextSlot,    // slotId
            0,           // mouseButton (0 = lewy)
            ClickType.PICKUP,
            mc.player
        );

        sessionPages++;
        ChatUtil.send("&7[DAO] → Strona &f#" + (currentPage + 1));
        return true;
    }

    // ── Koniec ────────────────────────────────────────────────────────────────
    private static void onFinished() {
        state = State.IDLE;
        ChatUtil.send("&a[DAO] &fGotowe! Zebrano &e" + sessionGrabbed
            + " &fitemow z &e" + sessionPages + " &fstron. Czekam na kolejna...");
        sessionGrabbed = 0;
        sessionPages   = 0;
        currentPage    = 0;
        lastSyncId     = -1;
        grabbedSlots.clear();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Wykrywa sloty nawigacyjne na podstawie pozycji w siatce 9×6.
     *
     * Z screenshota:
     *   - Kolumna 0 (slot % 9 == 0) = zielona = nawigacja
     *   - Kolumna 8 (slot % 9 == 8) = zielona = nawigacja
     *   - Rząd 0 (slot < 9)         = górny rząd = nawigacja
     *   - Rząd 5 (slot >= 45)       = dolny rząd = nawigacja/następna strona
     *
     * Itemy są TYLKO w rzędach 1-4, kolumnach 1-7 (sloty 10-16, 19-25, 28-34, 37-43)
     */
    private static boolean isNavSlot(int slotIndex, ModConfig cfg) {
        // Zawsze pomijaj skonfigurowany slot następnej/poprzedniej strony
        if (slotIndex == cfg.nextPageSlot || slotIndex == cfg.prevPageSlot) return true;

        // Pozycja w siatce 9×6
        int col = slotIndex % 9;
        int row = slotIndex / 9;

        // Kolumna 0 lub 8 = zielone boki
        if (col == 0 || col == 8) return true;

        // Rząd 0 (górny) lub rząd 5 (dolny) = nawigacja
        if (row == 0 || row >= 5) return true;

        return false;
    }

    private static boolean isOtchlanTitle(String lower) {
        return lower.contains("otch");
    }

    private static int parsePageNumber(String title) {
        int idx = title.lastIndexOf('#');
        if (idx >= 0 && idx < title.length() - 1) {
            try {
                String num = title.substring(idx + 1).replaceAll("[^0-9]", "");
                if (!num.isEmpty()) return Integer.parseInt(num);
            } catch (NumberFormatException ignored) {}
        }
        return 1;
    }

    private static int getPriorityLevel(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        String name = stripColors(stack.getHoverName().getString()).toLowerCase();
        for (PriorityEntry e : ConfigManager.getPriorities()) {
            if (e.keyword != null && name.contains(e.keyword.toLowerCase())) return e.priority;
        }
        return 0;
    }

    public static String stripColors(String s) {
        return s.replaceAll("§[0-9a-fk-orA-FK-OR]", "").trim();
    }
}
