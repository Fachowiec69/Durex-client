package pl.durex.client.module;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public final class AntiKostkaModule {

    private boolean enabled = false;
    public static final int MAX_SLOTS = 4;

    public static class HotbarSlot {
        public String name;
        public InputUtil.Key loadKey;
        public final ItemStack[] items = new ItemStack[9];
        public boolean hasSaved = false;

        public HotbarSlot(String name, int keyCode) {
            this.name = name;
            this.loadKey = InputUtil.fromKeyCode(keyCode, 0);
        }

        public String getLoadKeyName() { return loadKey.getLocalizedText().getString(); }
    }

    private final List<HotbarSlot> slots = new ArrayList<>();
    private final boolean[] wasKeyDown = new boolean[MAX_SLOTS];

    // Legit swap - kolejka operacji [targetSlot, itemId]
    private final List<int[]> swapQueue = new ArrayList<>();
    private int swapTickDelay = 0;

    public enum DelayMode {
        INSTANT("Instant", 0),
        SLOW("Slow", 8),
        FAST("Fast", 2),
        SUPER_FAST("Super Fast", 1);

        public final String label;
        public final int ticks;
        DelayMode(String label, int ticks) { this.label = label; this.ticks = ticks; }
    }

    private DelayMode delayMode = DelayMode.FAST;

    public DelayMode getDelayMode() { return delayMode; }
    public void setDelayMode(DelayMode m) { delayMode = m; }

    public AntiKostkaModule() {
        slots.add(new HotbarSlot("Hotbar 1", GLFW.GLFW_KEY_F5));
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean v) { enabled = v; }
    public List<HotbarSlot> getSlots() { return slots; }

    public void addSlot() {
        if (slots.size() < MAX_SLOTS) {
            int[] keys = {GLFW.GLFW_KEY_F5, GLFW.GLFW_KEY_F6, GLFW.GLFW_KEY_F7, GLFW.GLFW_KEY_F8};
            slots.add(new HotbarSlot("Hotbar " + (slots.size() + 1), keys[slots.size()]));
        }
    }

    public void removeSlot(int index) {
        if (slots.size() > 1 && index >= 0 && index < slots.size()) slots.remove(index);
    }

    public void saveSlot(int index, MinecraftClient client) {
        if (index < 0 || index >= slots.size() || client.player == null) return;
        HotbarSlot slot = slots.get(index);
        for (int i = 0; i < 9; i++) slot.items[i] = client.player.getInventory().getStack(i).copy();
        slot.hasSaved = true;
        client.player.sendMessage(Text.literal("§a[Durex] Zapisano: " + slot.name), true);
    }

    public void loadSlot(int index, MinecraftClient client) {
        if (index < 0 || index >= slots.size() || client.player == null) return;
        HotbarSlot slot = slots.get(index);
        if (!slot.hasSaved) {
            client.player.sendMessage(Text.literal("§c[Durex] Brak zapisu: " + slot.name), true);
            return;
        }
        // Zapisz tylko które sloty docelowe potrzebują jakich itemów
        swapQueue.clear();
        swapTickDelay = 0;
        for (int targetSlot = 0; targetSlot < 9; targetSlot++) {
            ItemStack wanted = slot.items[targetSlot];
            if (wanted == null || wanted.isEmpty()) continue;
            // [0] = targetSlot, [1] = item hash (do identyfikacji)
            swapQueue.add(new int[]{targetSlot, System.identityHashCode(wanted)});
        }
        // Zapisz referencję do slotu żeby wiedzieć czego szukać
        activeLoadSlot = slot;
        client.player.sendMessage(Text.literal("§a[Durex] Ladowanie: " + slot.name), true);
    }

    private HotbarSlot activeLoadSlot = null;

    public void tick(MinecraftClient client) {
        if (client.player == null) return;

        // Legit swap - wykonuj po jednej operacji co SWAP_DELAY ticków
        if (!swapQueue.isEmpty() && activeLoadSlot != null) {
            if (delayMode == DelayMode.INSTANT) {
                // Instant - wykonaj wszystkie operacje od razu
                while (!swapQueue.isEmpty()) {
                    int[] op = swapQueue.remove(0);
                    int targetSlot = op[0];
                    ItemStack wanted = activeLoadSlot.items[targetSlot];
                    if (wanted != null && !wanted.isEmpty()) {
                        if (!sameItem(client.player.getInventory().getStack(targetSlot), wanted)) {
                            for (int srcSlot = 0; srcSlot < 36; srcSlot++) {
                                if (srcSlot == targetSlot) continue;
                                if (sameItem(client.player.getInventory().getStack(srcSlot), wanted)) {
                                    performSwap(client, srcSlot, targetSlot);
                                    break;
                                }
                            }
                        }
                    }
                }
                activeLoadSlot = null;
            } else if (swapTickDelay <= 0) {
                int[] op = swapQueue.remove(0);
                int targetSlot = op[0];
                ItemStack wanted = activeLoadSlot.items[targetSlot];
                if (wanted != null && !wanted.isEmpty()) {
                    if (!sameItem(client.player.getInventory().getStack(targetSlot), wanted)) {
                        for (int srcSlot = 0; srcSlot < 36; srcSlot++) {
                            if (srcSlot == targetSlot) continue;
                            if (sameItem(client.player.getInventory().getStack(srcSlot), wanted)) {
                                performSwap(client, srcSlot, targetSlot);
                                break;
                            }
                        }
                    }
                }
                swapTickDelay = delayMode.ticks;
            } else {
                swapTickDelay--;
            }
        } else if (swapQueue.isEmpty()) {
            activeLoadSlot = null;
        }

        if (!enabled || client.currentScreen != null) {
            for (int i = 0; i < MAX_SLOTS; i++) wasKeyDown[i] = false;
            return;
        }
        if (!pl.durex.client.license.LicenseManager.getInstance().isValid()) return;

        long handle = client.getWindow().getHandle();
        for (int i = 0; i < slots.size(); i++) {
            int code = slots.get(i).loadKey.getCode();
            boolean isDown = code != -1 && InputUtil.isKeyPressed(handle, code);
            if (isDown && !wasKeyDown[i]) loadSlot(i, client);
            wasKeyDown[i] = isDown;
        }
    }

    /** Porównuje itemy ignorując durability - tylko typ i enchantmenty */
    private static boolean sameItem(ItemStack a, ItemStack b) {
        if (a.isEmpty() && b.isEmpty()) return true;
        if (a.isEmpty() || b.isEmpty()) return false;
        // Porównaj typ itemu
        if (a.getItem() != b.getItem()) return false;
        // Porównaj enchantmenty jeśli są (ważne dla mieczy)
        var enchA = a.getEnchantments();
        var enchB = b.getEnchantments();
        return enchA.equals(enchB);
    }

    private void performSwap(MinecraftClient client, int slotA, int slotB) {
        if (client.player == null || client.interactionManager == null) return;
        // Zawsze używaj SWAP action - bezpieczniejsze dla serwerów
        // slotB musi być hotbarem (0-8) dla SWAP action
        if (slotB < 9) {
            // src może być gdziekolwiek w inventory - konwertuj na screen slot
            int screenA = slotA < 9 ? slotA + 36 : slotA;
            client.interactionManager.clickSlot(0, screenA, slotB, SlotActionType.SWAP, client.player);
        } else if (slotA < 9) {
            // slotA jest hotbarem
            int screenB = slotB < 9 ? slotB + 36 : slotB;
            client.interactionManager.clickSlot(0, screenB, slotA, SlotActionType.SWAP, client.player);
        }
        // Jeśli oba poza hotbarem - pomiń (SWAP wymaga hotbara)
    }

    public ItemStack getSavedStack(int slotIndex, int itemIndex) {
        if (slotIndex < 0 || slotIndex >= slots.size()) return ItemStack.EMPTY;
        ItemStack s = slots.get(slotIndex).items[itemIndex];
        return s != null ? s : ItemStack.EMPTY;
    }

    public boolean hasSaved() { return slots.stream().anyMatch(s -> s.hasSaved); }

    // Legacy
    public ItemStack getSavedStack(int slot) { return getSavedStack(0, slot); }
    public void loadSavedHotbar(ItemStack[] stacks) {
        if (slots.isEmpty()) return;
        for (int i = 0; i < 9; i++) slots.get(0).items[i] = stacks[i] != null ? stacks[i] : ItemStack.EMPTY;
        slots.get(0).hasSaved = true;
    }
}
