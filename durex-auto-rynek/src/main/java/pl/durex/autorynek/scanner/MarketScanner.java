package pl.durex.autorynek.scanner;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import pl.durex.autorynek.config.ConfigManager;
import pl.durex.autorynek.config.PriceEntry;
import pl.durex.autorynek.config.ServerProfile;
import pl.durex.autorynek.util.ChatUtil;
import pl.durex.autorynek.util.PriceParser;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * MarketScanner — logika identyczna z BK-Rynek SearchAutomationController.
 *
 * Kluczowe zasady (jak BK-Rynek):
 * 1. onScreenRender() wywolywane z mixinu przy kazdym renderze ekranu
 * 2. Threaded scanner co 5ms skanuje sloty i wywoluje onItemMatch()
 * 3. onItemMatch() -> executePurchase() -> clickSlot() + startRapidBuySpam()
 * 4. Ekran potwierdzenia (confirmSlot) klikany automatycznie
 * 5. restartScan() zamyka ekran i wysyla komende ponownie
 * 6. onChatMessage() reaguje na sukces/blad/brak kasy
 */
public class MarketScanner {

    // ── Snipe history ─────────────────────────────────────────────────────────
    public static class SnipeEntry {
        public final String name;
        public final double price;
        public final double sellPrice;
        public final long   timestamp;
        public SnipeEntry(String name, double price, double sellPrice) {
            this.name = name; this.price = price;
            this.sellPrice = sellPrice; this.timestamp = System.currentTimeMillis();
        }
    }
    private static final List<SnipeEntry> snipeHistory = new CopyOnWriteArrayList<>();
    public static List<SnipeEntry> getSnipeHistory() { return snipeHistory; }
    public static void clearHistory() { snipeHistory.clear(); }

    // ── Stan (jak BK-Rynek State) ─────────────────────────────────────────────
    private static volatile boolean active = false;
    private static volatile boolean buyingItem = false;
    private static volatile boolean purchasePending = false;
    private static volatile boolean waitingForConfirmMsg = false;
    private static volatile boolean isRestarting = false;
    private static volatile boolean isInsideConfirmScreen = false;
    private static volatile boolean sortingConfirmed = false;
    private static volatile boolean waitingForMarketOpen = false;
    private static volatile boolean isThreadedInitialClickActive = false;
    private static volatile boolean isThreadedBuyActive = false;

    private static long buyingStartTime = 0;
    private static long lastActionMs = 0;
    private static long lastClickMs = 0;
    private static long restartStartTime = 0;
    private static long screenFirstSeenTime = 0;
    private static long marketFirstSeenTime = 0;
    private static long sortingPauseUntil = 0;
    private static long refreshBlockedUntil = 0;
    private static long confirmMsgTimeout = 0;
    private static long lastSyncId = -1;
    private static long stuckInConfirmStartTime = 0;
    private static long lastOpenAttemptMs = 0;

    private static int lastSyncIdWhenBought = -1;
    private static int lastMatchedSlotId = -1;
    private static int confirmClickCount = 0;
    private static int openAttemptsLeft = 0;
    private static int extraRestartDelay = 0;

    private static String lastMatchedItemName = "";
    private static String lastMatchedPrice = "";
    private static double lastMatchedPriceDouble = 0;
    private static String startBuyTitle = "";
    private static String lastPageSignature = "";
    private static PriceEntry matchedEntry = null;

    // Cache
    private static final Map<Integer, Long> purchasedCache  = new ConcurrentHashMap<>();
    private static final Map<Integer, Long> failureCache    = new ConcurrentHashMap<>();
    private static final Map<String,  Long> tooExpensiveCache = new ConcurrentHashMap<>();
    private static final Set<Integer> currentMatches = ConcurrentHashMap.newKeySet();
    
    // Per-slot cooldown — zapobiega spamowaniu tego samego slotu (jak Autorynek)
    private static final Map<Integer, Long> slotCooldowns = new ConcurrentHashMap<>();
    private static final long SLOT_COOLDOWN_MS = 100; // 100ms cooldown per slot

    // Statystyki
    private static int    sessionBought = 0;
    private static int    sessionFailed = 0;
    private static double sessionProfit = 0;
    private static long   sessionStart  = 0;

    // Scheduler — MAX_PRIORITY, 4 watki
    private static final ScheduledExecutorService SCHEDULER =
        Executors.newScheduledThreadPool(4, r -> {
            Thread t = new Thread(r, "DurexSniper");
            t.setDaemon(true);
            t.setPriority(Thread.MAX_PRIORITY);
            return t;
        });
    private static final List<ScheduledFuture<?>> SCAN_FUTURES = new ArrayList<>();
    private static ScheduledFuture<?> SCAN_FUTURE = null;

    // ── API ───────────────────────────────────────────────────────────────────
    public static void toggle() { if (active) stop(); else start(); }

    public static void start() {
        reset();
        active = true;
        sessionBought = 0; sessionFailed = 0; sessionProfit = 0;
        sessionStart = System.currentTimeMillis();
        ChatUtil.send("&a[Durex Auto Rynek] &fSkaner START &7| F6=stop");
        // Wyslij komende otwarcia rynku
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            ServerProfile profile = ConfigManager.findProfile(getServer(mc));
            sendMarketCommand(mc, profile);
            waitingForMarketOpen = true;
            openAttemptsLeft = 3;
            lastOpenAttemptMs = System.currentTimeMillis();
        }
    }

    public static void stop() {
        active = false;
        reset();
        long elapsed = (System.currentTimeMillis() - sessionStart) / 1000;
        ChatUtil.send("&a[Durex Auto Rynek] &fSkaner STOP | Kupiono: &e" + sessionBought
            + " &7| Profit: &a" + PriceParser.format(sessionProfit) + "$ &7| Czas: &e" + elapsed + "s");
    }

    public static boolean isActive() { return active; }
    public static boolean isInsideConfirmScreen() { return isInsideConfirmScreen; }

    private static void reset() {
        buyingItem = false;
        purchasePending = false;
        waitingForConfirmMsg = false;
        isRestarting = false;
        isInsideConfirmScreen = false;
        sortingConfirmed = false;
        waitingForMarketOpen = false;
        isThreadedInitialClickActive = false;
        isThreadedBuyActive = false;
        buyingStartTime = 0; lastActionMs = 0; lastClickMs = 0;
        restartStartTime = 0; screenFirstSeenTime = 0; marketFirstSeenTime = 0;
        sortingPauseUntil = 0; refreshBlockedUntil = 0; confirmMsgTimeout = 0;
        lastSyncId = -1; stuckInConfirmStartTime = 0; lastOpenAttemptMs = 0;
        lastSyncIdWhenBought = -1; lastMatchedSlotId = -1; confirmClickCount = 0;
        openAttemptsLeft = 0; extraRestartDelay = 0;
        lastMatchedItemName = ""; lastMatchedPrice = ""; lastMatchedPriceDouble = 0;
        startBuyTitle = ""; lastPageSignature = ""; matchedEntry = null;
        purchasedCache.clear(); failureCache.clear(); currentMatches.clear();
        stopThreadedScanner();
    }

    // ── onScreenRender — wywolywane z mixinu przy kazdym renderze ─────────────
    // Identyczna logika jak BK-Rynek SearchAutomationController.onScreenRender()
    public static void onScreenRender(Component title, AbstractContainerScreen<?> screen) {
        if (!active) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        ServerProfile profile = ConfigManager.findProfile(getServer(mc));
        if (profile == null) return;

        long now = System.currentTimeMillis();
        String currentTitle = stripColors(title.getString()).toLowerCase();
        int syncId = screen.getMenu().containerId;

        // Wykryj typ ekranu
        boolean titleIsMarket = currentTitle.contains("rynek") || currentTitle.contains("aukcj")
            || currentTitle.contains("ah") || currentTitle.contains("market")
            || currentTitle.contains("auction") || currentTitle.contains("itemy")
            || (profile.marketGuiTitle != null && currentTitle.contains(profile.marketGuiTitle.toLowerCase()));

        boolean confirmKeywordsInTitle = currentTitle.contains("potwierdz")
            || currentTitle.contains("potwierdź") || currentTitle.contains("confirm")
            || currentTitle.contains("na pewno") || currentTitle.contains("sure")
            || (currentTitle.contains("zakup") && !titleIsMarket);

        // Nowy syncId = nowy ekran
        if (lastSyncId != syncId) {
            screenFirstSeenTime = now;
            lastSyncId = syncId;
            stuckInConfirmStartTime = 0;
            sortingConfirmed = false; // zawsze resetuj — każdy nowy ekran wymaga sortowania
            if (titleIsMarket) {
                startThreadedScanner(mc, screen, profile);
            } else {
                stopThreadedScanner();
            }
        }

        // Sprawdz czy ekran potwierdzenia
        boolean confirmButtonsFound = false;
        int marketItemCount = 0;
        List<Slot> slots = screen.getMenu().slots;
        for (int i = 0; i < Math.min(slots.size(), 54); i++) {
            Slot s = slots.get(i);
            if (s == null || s.getItem().isEmpty()) continue;
            String name = stripColors(s.getItem().getHoverName().getString()).toLowerCase();
            if (name.contains("potwierdz") || name.contains("potwierdź")
                || name.contains("confirm") || name.contains("zakup produkt")) {
                confirmButtonsFound = true;
            }
            // Sprawdz czy to przedmiot rynkowy — czytaj BEZPOSREDNIO z DataComponents (szybciej niz getTooltipLines)
            try {
                var loreComp = s.getItem().get(net.minecraft.core.component.DataComponents.LORE);
                if (loreComp != null) {
                    for (var line : loreComp.lines()) {
                        String l = line.getString().toLowerCase();
                        if (l.contains("wystawi") || l.contains("koszt") || l.contains("seller")) {
                            marketItemCount++;
                            break;
                        }
                    }
                }
            } catch (Exception ignored) {}
            if (marketItemCount >= 5 && confirmButtonsFound) break;
        }

        boolean confirmKeywords = confirmKeywordsInTitle || confirmButtonsFound
            || (currentTitle.contains("zakup") && marketItemCount <= 1);
        boolean isMarket = marketItemCount > 1 || (titleIsMarket && !confirmKeywords);
        isInsideConfirmScreen = confirmKeywords && !isMarket;

        // Sortowanie (anarchia.gg slot 53)
        // Jesli profil nie ma sortingSlot — od razu uznaj za posortowane
        if (profile.sortingSlot == null || profile.sortingKeyword == null) {
            sortingConfirmed = true;
        }

        if (isMarket && !buyingItem && !purchasePending && !isRestarting
                && profile.sortingSlot != null && profile.sortingKeyword != null) {
            Slot sortSlot = slots.size() > profile.sortingSlot ? slots.get(profile.sortingSlot) : null;
            if (sortSlot != null && !sortSlot.getItem().isEmpty()) {
                if (!sortingConfirmed) {
                    // Kliknij slot sortowania raz i po 150ms uznaj za posortowane
                    int sortDelay = Boolean.TRUE.equals(profile.aggressiveMode) ? 50 : 150;
                    if (now - lastClickMs > sortDelay) {
                        clickSlot(mc, screen, profile.sortingSlot, 1);
                        lastClickMs = now;
                        sortingPauseUntil = now + 150; // po 150ms uznaj za posortowane
                    }
                    if (now > sortingPauseUntil && sortingPauseUntil > 0) {
                        sortingConfirmed = true;
                    }
                    return;
                }
            } else {
                sortingConfirmed = true;
            }
        }

        // Waiting for market open
        if (waitingForMarketOpen) {
            if (isMarket) {
                waitingForMarketOpen = false;
            } else if (now - lastOpenAttemptMs > 2000) {
                if (openAttemptsLeft > 0) {
                    openAttemptsLeft--;
                    lastOpenAttemptMs = now;
                    sendMarketCommand(mc, profile);
                } else {
                    waitingForMarketOpen = false;
                    ChatUtil.send("&c[Durex] Nie udalo sie otworzyc rynku.");
                    stop();
                }
            }
        }

        // Timeout potwierdzenia
        if (waitingForConfirmMsg) {
            long timeoutLimit = isMarket ? confirmMsgTimeout - 2000 : confirmMsgTimeout;
            if (now > timeoutLimit) {
                waitingForConfirmMsg = false;
                restartScan(now, 1000);
            }
            return;
        }

        // Obsluga ekranu rynku
        if (isMarket) {
            if (buyingItem && now - buyingStartTime > 1000) {
                buyingItem = false;
            }
            if (buyingItem || purchasePending) return;

            // Ustaw czas pierwszego zobaczenia rynku
            if (marketFirstSeenTime == 0) marketFirstSeenTime = now;

            // Czekaj minimalnie po otwarciu rynku (tylko jesli sortowanie juz potwierdzone)
            // Jesli sortowanie wlasnie sie skonczylo — nie czekaj dodatkowego openDelay
            long openDelay = profile.marketOpenDelayMs != null ? profile.marketOpenDelayMs : 200;
            if (!sortingConfirmed && now - marketFirstSeenTime < openDelay) return;

            // Jesli sortowanie jeszcze nie potwierdzone — czekaj, nie restartuj
            if (!sortingConfirmed) return;

            int refreshDelay = profile.marketNextDelayMs != null ? profile.marketNextDelayMs : 200;

            // Tryb z sortowaniem (anarchia) — refresh przez klik slot 0 prawym
            if (profile.sortingSlot != null) {
                if (now - lastActionMs > refreshDelay) {
                    if (now < refreshBlockedUntil) return;
                    if (!purchasePending && !buyingItem) {
                        clickSlot(mc, screen, 0, 1);
                        lastActionMs = now;
                    }
                }
                return;
            }

            // Tryb bez sortowania — przejdz na nastepna strone
            if (now - lastActionMs < refreshDelay || purchasePending || buyingItem) return;

            if (profile.marketNextPageSlot != null && profile.marketNextPageSlot < slots.size()) {
                Slot nextSlot = slots.get(profile.marketNextPageSlot);
                if (nextSlot != null && !nextSlot.getItem().isEmpty()) {
                    String sig = buildPageSignature(screen);
                    if (!sig.isEmpty() && sig.equals(lastPageSignature)) {
                        // Ta sama niepusta strona przez za dlugo — restart
                        if (now - lastClickMs > 1500) {
                            restartScan(now, 0);
                        }
                    } else if (!sig.isEmpty()) {
                        clickSlot(mc, screen, profile.marketNextPageSlot, 0);
                        lastClickMs = now;
                        lastActionMs = now;
                        lastPageSignature = sig;
                    }
                    // jesli sig pusty — rynek sie jeszcze laduje, czekaj
                } else {
                    // Brak przycisku nastepnej strony — restart tylko jesli rynek zaladowany
                    String sig = buildPageSignature(screen);
                    if (!sig.isEmpty() && now - marketFirstSeenTime > 2000) {
                        restartScan(now, 0);
                    }
                }
            }
            // Brak konfiguracji nextPageSlot — skanuj bez przechodzenia stron

        } else if (isInsideConfirmScreen) {
            // Ekran potwierdzenia — kliknij confirmSlot
            if (buyingItem) {
                int timeout = Boolean.TRUE.equals(profile.aggressiveMode) ? 1000 : 4000;
                if (now - buyingStartTime > timeout) {
                    if (Boolean.TRUE.equals(profile.aggressiveMode) && confirmClickCount < 3) {
                        int cSlot = profile.confirmSlot != null ? profile.confirmSlot : 11;
                        clickSlot(mc, screen, cSlot, 0);
                        confirmClickCount++;
                        buyingStartTime = now;
                        return;
                    }
                    buyingItem = false;
                    restartScan(now, 0);
                    return;
                }

                int confirmDelay = profile.marketConfirmDelayMs != null ? profile.marketConfirmDelayMs : 33;
                boolean windowDelayPassed = now - screenFirstSeenTime >= confirmDelay;
                if (windowDelayPassed) {
                    if (Boolean.TRUE.equals(profile.aggressiveMode)) {
                        if (!isThreadedBuyActive) {
                            int cSlot = profile.confirmSlot != null ? profile.confirmSlot : 11;
                            startThreadedBuy(syncId, cSlot, confirmDelay);
                        }
                    } else {
                        if (confirmClickCount < (profile.marketConfirmAttempts != null ? profile.marketConfirmAttempts : 3)
                                && now - lastActionMs > confirmDelay) {
                            int cSlot = profile.confirmSlot != null ? profile.confirmSlot : 11;
                            clickSlot(mc, screen, cSlot, 0);
                            confirmClickCount++;
                            lastActionMs = now;
                        }
                    }
                }
            } else if (!waitingForConfirmMsg && !purchasePending) {
                // Utknelismy w ekranie potwierdzenia bez zakupu
                if (stuckInConfirmStartTime == 0) {
                    stuckInConfirmStartTime = now;
                } else {
                    long stuckTimeout = Boolean.TRUE.equals(profile.aggressiveMode) ? 250 : 2000;
                    if (now - stuckInConfirmStartTime > stuckTimeout) {
                        stuckInConfirmStartTime = 0;
                        restartScan(now, 0);
                    }
                }
            }
        }

        // Restart po zamknieciu
        if (isRestarting) {
            ServerProfile p = profile;
            long delay = p.marketRestartDelayMs != null ? p.marketRestartDelayMs : 500;
            delay += extraRestartDelay;
            if (now - restartStartTime > delay && now > lastActionMs) {
                isRestarting = false;
                buyingItem = false;
                waitingForConfirmMsg = false;
                purchasePending = false;
                if (active) {
                    sendMarketCommand(mc, p);
                    waitingForMarketOpen = true;
                    lastOpenAttemptMs = now;
                    openAttemptsLeft = p.marketConfirmAttempts != null ? p.marketConfirmAttempts : 3;
                }
            }
        }
    }

    // ── onItemMatch — wywolywane z mixinu gdy slot pasuje ─────────────────────
    // Wywolywane z:
    // 1. ContainerSetSlotMixin — NATYCHMIAST po otrzymaniu packetu (najszybsze)
    // 2. HandledScreenMixin.onRender — przy każdym renderze (backup)
    public static void onItemMatch(Slot slot, double price, double maxPrice, PriceEntry priceEntry) {
        if (purchasePending || buyingItem || waitingForConfirmMsg) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.containerMenu == null) return;
        
        long now = System.currentTimeMillis();
        
        // Per-slot cooldown — zapobiega spamowaniu tego samego slotu (jak Autorynek)
        Long lastAttempt = slotCooldowns.get(slot.index);
        if (lastAttempt != null && now - lastAttempt < SLOT_COOLDOWN_MS) {
            return; // Slot był już próbowany niedawno
        }
        slotCooldowns.put(slot.index, now);
        
        int syncId = mc.player.containerMenu.containerId;

        // Ustaw purchasePending NATYCHMIAST przed jakimkolwiek opóźnieniem
        purchasePending = true;
        refreshBlockedUntil = now + 10;

        // Wyślij pakiet NATYCHMIAST — jesteśmy już na głównym wątku MC
        // (ContainerSetSlotMixin i onRender są oba na głównym wątku)
        sendPacket(mc, syncId, slot.index);

        // Wykonaj pełny zakup
        executePurchase(slot, price, maxPrice, priceEntry, syncId);
    }

    private static void executePurchase(Slot slot, double price, double maxPrice,
                                         PriceEntry priceEntry, int syncId) {
        Minecraft mc = Minecraft.getInstance();
        // Nie rób mc.execute() redirect — onItemMatch już gwarantuje główny wątek
        if (slot.index > 53) { purchasePending = false; return; }
        if (!active || buyingItem || waitingForConfirmMsg) { purchasePending = false; return; }
        if (isItemPurchasedRecent(slot.index)) { purchasePending = false; return; }
        if (isItemTooExpensive(slot.getItem().getHoverName().getString())) { purchasePending = false; return; }

        long now = System.currentTimeMillis();
        lastMatchedItemName = stripColors(slot.getItem().getHoverName().getString());
        lastMatchedPriceDouble = price;
        lastMatchedPrice = PriceParser.format(price);
        lastMatchedSlotId = slot.index;
        matchedEntry = priceEntry;
        buyingItem = true;
        buyingStartTime = now;
        lastActionMs = now;
        confirmClickCount = 0;
        purchasePending = false;

        ChatUtil.send("&e[Durex] &fSNIPE: &a" + lastMatchedItemName
            + " &7za &a" + lastMatchedPrice + "$ &7(max: " + PriceParser.format(maxPrice) + "$)");

        purchasedCache.put(slot.index, now + 500L);

        // Kliknij slot przez gameMode (główny wątek)
        if (mc.player != null && mc.player.containerMenu != null) {
            clickSlotDirect(mc, mc.player.containerMenu.containerId, slot.index, 0);
        }

        refreshBlockedUntil = System.currentTimeMillis() + 10;
        lastSyncIdWhenBought = syncId;

        // Burst spam z osobnego wątku — co 2ms przez 1.5s
        sendBurst(mc, syncId, slot.index);
    }

    // ── Threaded scanner — zostawiony tylko jako backup gdy mixin nie wykryje ──
    // Główne wykrywanie jest w ContainerSetSlotMixin (przed render frame)
    // Ten scanner działa jako fallback co 5ms
    private static void startThreadedScanner(Minecraft mc, AbstractContainerScreen<?> screen,
                                              ServerProfile profile) {
        stopThreadedScanner();
        int syncId = screen.getMenu().containerId;

        // Jeden wątek co 5ms jako backup
        SCAN_FUTURES.add(SCHEDULER.scheduleWithFixedDelay(() -> {
            try {
                if (!active || buyingItem || purchasePending) return;
                if (!(mc.screen instanceof AbstractContainerScreen<?> cur)) return;
                if (cur.getMenu().containerId != syncId) return;

                List<Slot> slots = cur.getMenu().slots;
                for (int i = 0; i < Math.min(slots.size(), 45); i++) {
                    Slot slot = slots.get(i);
                    if (slot == null || slot.getItem().isEmpty()) continue;
                    if (isItemPurchasedRecent(slot.index) || isItemFailed(slot.index)) continue;

                    ScanInput input = buildScanInput(mc, slot);
                    ScanResult result = ScanEvaluator.evaluate(input, profile, false);
                    if (!result.highlight) continue;
                    if (isItemTooExpensive(input.noColorName)) continue;

                    if (buyingItem || purchasePending) return;
                    purchasePending = true;
                    refreshBlockedUntil = System.currentTimeMillis() + 10;

                    final Slot fSlot = slot;
                    final double fPrice = result.foundPrice;
                    final double fMax = result.maxPrice;
                    final PriceEntry fEntry = result.matchedEntry;

                    sendBurst(mc, syncId, slot.index);
                    mc.execute(() -> executePurchase(fSlot, fPrice, fMax, fEntry, syncId));
                    return;
                }
            } catch (Exception ignored) {}
        }, 0L, 5L, TimeUnit.MILLISECONDS));
    }

    private static void stopThreadedScanner() {
        for (ScheduledFuture<?> f : SCAN_FUTURES) f.cancel(false);
        SCAN_FUTURES.clear();
        if (SCAN_FUTURE != null) { SCAN_FUTURE.cancel(false); SCAN_FUTURE = null; }
    }

    // ── BURST — najszybszy możliwy zakup ─────────────────────────────────────
    // Faza 1: 16 pakietów BEZ sleep (0ms) — natychmiastowy burst
    // Faza 2: co 1ms przez max 1.5s
    private static void sendBurst(Minecraft mc, int syncId, int slotId) {
        SCHEDULER.submit(() -> {
            try {
                // Faza 1: burst bez sleep — 16 pakietów natychmiast
                for (int i = 0; i < 16; i++) {
                    sendPacket(mc, syncId, slotId);
                }
                // Faza 2: podtrzymanie co 1ms
                long start = System.currentTimeMillis();
                while (active && buyingItem && !isInsideConfirmScreen
                        && System.currentTimeMillis() - start < 1500) {
                    sendPacket(mc, syncId, slotId);
                    Thread.sleep(1);
                }
            } catch (InterruptedException ignored) {}
        });
    }

    // ── Rapid buy spam dla ekranu rynku (jak BK-Rynek) ───────────────────────
    private static void startRapidBuySpam(int syncId, int slotId, int intervalMs) {
        isThreadedInitialClickActive = true;
        SCHEDULER.submit(() -> {
            try {
                Minecraft mc = Minecraft.getInstance();
                // Burst bez sleep najpierw
                for (int i = 0; i < 5; i++) sendPacket(mc, syncId, slotId);
                // Potem co intervalMs
                while (active && buyingItem && !isInsideConfirmScreen) {
                    sendPacket(mc, syncId, slotId);
                    Thread.sleep(intervalMs);
                }
            } catch (InterruptedException ignored) {
            } finally {
                isThreadedInitialClickActive = false;
            }
        });
    }

    // Threaded buy dla ekranu potwierdzenia — burst + co confirmDelay
    private static void startThreadedBuy(int syncId, int slotId, int delay) {
        isThreadedBuyActive = true;
        SCHEDULER.submit(() -> {
            try {
                Minecraft mc = Minecraft.getInstance();
                // Burst najpierw
                for (int i = 0; i < 5; i++) sendPacket(mc, syncId, slotId);
                // Potem co delay
                long start = System.currentTimeMillis();
                while (active && buyingItem && System.currentTimeMillis() - start < 4000) {
                    sendPacket(mc, syncId, slotId);
                    Thread.sleep(Math.max(1, delay));
                }
            } catch (InterruptedException ignored) {
            } finally {
                isThreadedBuyActive = false;
            }
        });
    }

    // ── Restart (jak BK-Rynek restartScan) ───────────────────────────────────
    private static void restartScan(long now, int extraDelay) {
        if (isRestarting) return;
        isRestarting = true;
        restartStartTime = now;
        extraRestartDelay = extraDelay;
        lastActionMs = now + 500;
        lastPageSignature = "";
        refreshBlockedUntil = 0;
        marketFirstSeenTime = 0; // reset — po restarcie czekamy na nowe otwarcie
        sortingConfirmed = false;
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            if (mc.player != null && mc.screen != null) {
                mc.player.closeContainer();
                mc.setScreen(null);
            }
        });
    }

    // ── Chat handler ──────────────────────────────────────────────────────────
    public static void onChatMessage(String message) {
        if (!active) return;
        String lower = message.toLowerCase();

        boolean success = lower.contains("kupiles przedmiot") || lower.contains("kupiłeś przedmiot")
            || lower.contains("pomyslnie zakupiles") || lower.contains("pomyślnie zakupiłeś")
            || lower.contains("zakupiono przedmiot") || lower.contains("zakupiono produkt")
            || lower.contains("kupiles produkt") || lower.contains("kupiłeś produkt");

        boolean error = lower.contains("juz prawdopodobnie usuniety")
            || lower.contains("już prawdopodobnie usunięty")
            || lower.contains("sprzedany") || lower.contains("nie jest juz dostepny")
            || lower.contains("nie jest już dostępny")
            || lower.contains("produkt ten zostal juz") || lower.contains("produkt ten został już")
            || lower.contains("przedmiot zostal juz sprzedany")
            || lower.contains("przedmiot został już sprzedany")
            || lower.contains("prawdopodobnie usuniety") || lower.contains("prawdopodobnie usunięty")
            || lower.contains("this item was already bought");

        boolean noMoney = lower.contains("nie masz tyle") || lower.contains("brak środków")
            || lower.contains("nie posiadasz wystarczajaco") || lower.contains("nie posiadasz wystarczająco")
            || lower.contains("nie stac cie") || lower.contains("nie stać cię")
            || lower.contains("nie stac ciebie") || lower.contains("nie stać ciebie");

        long now = System.currentTimeMillis();

        if (success && (buyingItem || waitingForConfirmMsg)) {
            if (lastMatchedSlotId != -1) markPurchasedRecent(lastMatchedSlotId);
            waitingForConfirmMsg = false;
            buyingItem = false;
            purchasePending = false;
            lastActionMs = now;
            refreshBlockedUntil = now + 10;
            sessionBought++;
            if (matchedEntry != null && matchedEntry.sellPrice > lastMatchedPriceDouble) {
                sessionProfit += (matchedEntry.sellPrice - lastMatchedPriceDouble);
            }
            snipeHistory.add(0, new SnipeEntry(lastMatchedItemName, lastMatchedPriceDouble,
                matchedEntry != null ? matchedEntry.sellPrice : 0));
            if (snipeHistory.size() > 10) snipeHistory.remove(snipeHistory.size() - 1);
            ChatUtil.send("&a[Durex] &fKupiono: &e" + lastMatchedItemName);
        } else if (error) {
            sortingPauseUntil = now + 2000;
            if (buyingItem || waitingForConfirmMsg) {
                if (lastMatchedSlotId != -1) markPurchasedRecent(lastMatchedSlotId);
                waitingForConfirmMsg = false;
                buyingItem = false;
                purchasePending = false;
                lastActionMs = now + 200;
                refreshBlockedUntil = now + 10;
                sessionFailed++;
                ChatUtil.send("&c[Durex] Przedmiot juz sprzedany.");
            }
        } else if (noMoney && (buyingItem || waitingForConfirmMsg)) {
            buyingItem = false;
            waitingForConfirmMsg = false;
            purchasePending = false;
            markItemTooExpensive(lastMatchedItemName, lastMatchedPriceDouble);
            ChatUtil.send("&c[Durex] Brak kasy na: &e" + lastMatchedItemName + " &7(blok 2min)");
        }
    }

    // ── Tick — obsluga restartu gdy ekran zamkniety ───────────────────────────
    public static void onTick(Minecraft mc) {
        if (!active || mc.player == null) return;
        long now = System.currentTimeMillis();

        // Czyszczenie cache
        purchasedCache.entrySet().removeIf(e -> now > e.getValue());
        failureCache.entrySet().removeIf(e -> now > e.getValue());
        tooExpensiveCache.entrySet().removeIf(e -> now > e.getValue());

        // Restart gdy ekran zamkniety
        if (isRestarting && mc.screen == null) {
            ServerProfile profile = ConfigManager.findProfile(getServer(mc));
            long delay = profile != null && profile.marketRestartDelayMs != null
                ? profile.marketRestartDelayMs : 500;
            delay += extraRestartDelay;
            if (now - restartStartTime > delay && now > lastActionMs) {
                isRestarting = false;
                buyingItem = false;
                waitingForConfirmMsg = false;
                purchasePending = false;
                if (active) {
                    sendMarketCommand(mc, profile);
                    waitingForMarketOpen = true;
                    lastOpenAttemptMs = now;
                    openAttemptsLeft = 3;
                }
            }
        }

        // Waiting for market — ponow jesli za dlugo
        if (waitingForMarketOpen && mc.screen == null && now - lastOpenAttemptMs > 2800) {
            if (openAttemptsLeft > 0) {
                openAttemptsLeft--;
                lastOpenAttemptMs = now;
                ServerProfile profile = ConfigManager.findProfile(getServer(mc));
                sendMarketCommand(mc, profile);
            } else {
                waitingForMarketOpen = false;
                ChatUtil.send("&c[Durex] Nie udalo sie otworzyc rynku. Stop.");
                stop();
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private static ScanInput buildScanInput(Minecraft mc, Slot slot) {
        ItemStack stack = slot.getItem();
        String materialId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        List<String> loreLines = new ArrayList<>();
        try {
            List<Component> tooltip = stack.getTooltipLines(
                net.minecraft.world.item.Item.TooltipContext.of(mc.level),
                mc.player, TooltipFlag.Default.NORMAL);
            for (int i = 1; i < tooltip.size(); i++) {
                String line = stripColors(tooltip.get(i).getString()).trim();
                if (!line.isEmpty()) loreLines.add(line);
            }
        } catch (Exception ignored) {}

        StringBuilder enchsBuilder = new StringBuilder();
        try {
            ItemEnchantments enchs = stack.getEnchantments();
            enchs.entrySet().forEach(e -> {
                String name = e.getKey().value().description().getString();
                if (enchsBuilder.length() > 0) enchsBuilder.append(", ");
                enchsBuilder.append(name).append(" ").append(e.getIntValue());
            });
        } catch (Exception ignored) {}

        if (!enchsBuilder.isEmpty()) loreLines.add(enchsBuilder.toString());

        int componentCount = 0;
        try { componentCount = stack.getComponentsPatch().size(); } catch (Exception ignored) {}

        Integer customModelData = null;
        try {
            var cmd = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA);
            if (cmd != null && !cmd.floats().isEmpty()) customModelData = Math.round(cmd.floats().get(0));
        } catch (Exception ignored) {}

        return new ScanInput(
            stripColors(stack.getHoverName().getString()),
            loreLines, materialId, enchsBuilder.toString(),
            stack.getCount(), slot.index, componentCount, customModelData);
    }

    private static void clickSlot(Minecraft mc, AbstractContainerScreen<?> screen, int slotId, int button) {
        if (mc.gameMode == null || mc.player == null) return;
        mc.gameMode.handleInventoryMouseClick(
            screen.getMenu().containerId, slotId, button, ClickType.PICKUP, mc.player);
    }

    private static void clickSlotDirect(Minecraft mc, int containerId, int slotId, int button) {
        if (mc.gameMode == null || mc.player == null) return;
        mc.gameMode.handleInventoryMouseClick(containerId, slotId, button, ClickType.PICKUP, mc.player);
    }

    private static void sendPacket(Minecraft mc, int syncId, int slotId) {
        try {
            ClientPacketListener conn = mc.getConnection();
            if (conn == null || mc.player == null) return;
            var carried = mc.player.containerMenu.getCarried();
            conn.send(new ServerboundContainerClickPacket(
                syncId, 0, slotId, 0, ClickType.PICKUP, carried,
                new it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap<>()));
        } catch (Exception ignored) {}
    }

    private static void sendMarketCommand(Minecraft mc, ServerProfile profile) {
        if (mc.player == null || mc.getConnection() == null) return;
        String cmd = "ah";
        if (profile != null && profile.marketCommands != null && !profile.marketCommands.isEmpty()) {
            cmd = profile.marketCommands.get(0);
            if (cmd.startsWith("/")) cmd = cmd.substring(1);
        }
        mc.player.connection.sendChat("/" + cmd);
    }

    private static String buildPageSignature(AbstractContainerScreen<?> screen) {
        StringBuilder sb = new StringBuilder();
        List<Slot> slots = screen.getMenu().slots;
        for (int i = 0; i < Math.min(slots.size(), 45); i++) {
            ItemStack s = slots.get(i).getItem();
            if (!s.isEmpty()) sb.append(s.getHoverName().getString()).append("|");
        }
        return sb.toString();
    }

    private static String getFullTooltip(Minecraft mc, ItemStack stack) {
        try {
            List<Component> tooltip = stack.getTooltipLines(
                net.minecraft.world.item.Item.TooltipContext.of(mc.level),
                mc.player, TooltipFlag.Default.NORMAL);
            StringBuilder sb = new StringBuilder();
            for (Component c : tooltip) sb.append(c.getString()).append("\n");
            return sb.toString();
        } catch (Exception e) { return ""; }
    }

    /**
     * Sprawdza czy linia tooltipa zawierajaca keyword jest zaznaczona
     * (ma zolty/zloty kolor §e lub §6) — identycznie jak BK-Rynek isNajnowszeSelected().
     * Na screenshocie widac ze aktywna opcja ma pomaranczowa strzalke i zolty tekst.
     */
    private static boolean isKeywordSelectedYellow(Minecraft mc, ItemStack stack, String keyword) {
        try {
            List<Component> tooltip = stack.getTooltipLines(
                net.minecraft.world.item.Item.TooltipContext.of(mc.level),
                mc.player, TooltipFlag.Default.NORMAL);
            for (Component line : tooltip) {
                String raw = line.getString();
                String lower = raw.toLowerCase();
                if (!lower.contains(keyword)) continue;
                // Sprawdz czy linia zawiera zolty/zloty kod koloru
                if (raw.contains("§e") || raw.contains("§6")) return true;
                // Sprawdz kolor przez styl komponentu
                var style = line.getStyle();
                if (style.getColor() != null) {
                    String colorName = style.getColor().toString().toLowerCase();
                    if (colorName.contains("yellow") || colorName.contains("gold")
                            || colorName.contains("ffff55") || colorName.contains("ffaa00")) {
                        return true;
                    }
                }
                // Sprawdz dzieci komponentu
                for (Component sibling : line.getSiblings()) {
                    String sibText = sibling.getString().toLowerCase();
                    if (!sibText.contains(keyword) && !sibText.contains("►") && !sibText.contains(">")) continue;
                    var sibStyle = sibling.getStyle();
                    if (sibStyle.getColor() != null) {
                        String c = sibStyle.getColor().toString().toLowerCase();
                        if (c.contains("yellow") || c.contains("gold")
                                || c.contains("ffff55") || c.contains("ffaa00")) {
                            return true;
                        }
                    }
                    String sibRaw = sibling.getString();
                    if (sibRaw.contains("§e") || sibRaw.contains("§6")) return true;
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    private static boolean isItemPurchasedRecent(int slotId) {
        Long exp = purchasedCache.get(slotId);
        return exp != null && System.currentTimeMillis() < exp;
    }
    private static void markPurchasedRecent(int slotId) {
        purchasedCache.put(slotId, System.currentTimeMillis() + 3000L);
    }
    public static boolean isItemFailed(int slotId) {
        Long exp = failureCache.get(slotId);
        return exp != null && System.currentTimeMillis() < exp;
    }
    public static void markFailed(int slotId) {
        failureCache.put(slotId, System.currentTimeMillis() + 5000L);
        sessionFailed++;
    }
    private static boolean isItemTooExpensive(String name) {
        Long exp = tooExpensiveCache.get(name);
        return exp != null && System.currentTimeMillis() < exp;
    }
    private static void markItemTooExpensive(String name, double price) {
        tooExpensiveCache.put(name, System.currentTimeMillis() + 120_000L);
    }
    private static String stripColors(String s) {
        return s.replaceAll("§[0-9a-fk-or]", "").trim();
    }
    private static String getServer(Minecraft mc) {
        return mc.getCurrentServer() != null ? mc.getCurrentServer().ip : "";
    }

    // ── Status / stats ────────────────────────────────────────────────────────
    public static String getStatus() {
        if (!active) return "§7IDLE";
        if (isRestarting) return "§eRESTART...";
        if (waitingForMarketOpen) return "§eOTWIERAM RYNEK...";
        if (buyingItem) return "§6KUPUJE §f" + lastMatchedItemName;
        if (isInsideConfirmScreen) return "§6POTWIERDZAM...";
        return "§aSKANUJE §7(kupiono: §e" + sessionBought + "§7)";
    }
    public static int    getSessionBought()  { return sessionBought; }
    public static int    getSessionFailed()  { return sessionFailed; }
    public static double getSessionProfit()  { return sessionProfit; }
    public static long   getSessionStart()   { return sessionStart; }
    public static Set<Integer> getCurrentMatches() { return currentMatches; }
}
