package pl.durex.client.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import pl.durex.client.mixin.ItemCooldownAccessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CooldownHudModule {

    private boolean enabled = false;
    private int hudX = 10;
    private int hudY = 100;

    // Zapisane całkowite czasy cooldownów (item -> ticki)
    private static final Map<Item, Integer> cooldownDurations = new HashMap<>();

    public static void recordCooldown(Item item, int duration) {
        if (duration > 0) cooldownDurations.put(item, duration);
        else cooldownDurations.remove(item);
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean v) { enabled = v; }
    public int getHudX() { return hudX; }
    public int getHudY() { return hudY; }
    public void setHudPos(int x, int y) { hudX = x; hudY = y; }

    public List<CooldownEntry> getCooldowns(MinecraftClient client) {
        List<CooldownEntry> list = new ArrayList<>();
        if (client.player == null) return list;

        ItemCooldownManager mgr = client.player.getItemCooldownManager();

        try {
            Map<Item, ?> entries = ((ItemCooldownAccessor) mgr).durex$getEntries();
            if (entries == null || entries.isEmpty()) return list;

            for (Map.Entry<Item, ?> e : new HashMap<>(entries).entrySet()) {
                Item item = e.getKey();
                float progress = mgr.getCooldownProgress(new ItemStack(item), 0f);
                if (progress <= 0f) continue;

                int totalTicks = cooldownDurations.getOrDefault(item, 0);
                int remainingTicks = totalTicks > 0
                        ? (int)(progress * totalTicks)
                        : (int)(progress * 400);

                list.add(new CooldownEntry(new ItemStack(item), progress, remainingTicks));
            }
        } catch (Exception ignored) {}

        list.sort((a, b) -> b.remainingTicks() - a.remainingTicks());
        return list;
    }

    public record CooldownEntry(ItemStack stack, float progress, int remainingTicks) {
        public String timeText() {
            int totalSeconds = remainingTicks / 20;
            int minutes = totalSeconds / 60;
            int seconds = totalSeconds % 60;
            int tenths = (remainingTicks % 20) * 10 / 20;
            if (minutes > 0) return minutes + "m " + seconds + "s";
            if (seconds >= 10) return seconds + "s";
            return seconds + "." + tenths + "s";
        }
    }
}
