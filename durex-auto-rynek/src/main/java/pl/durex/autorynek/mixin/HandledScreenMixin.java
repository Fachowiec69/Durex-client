package pl.durex.autorynek.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.durex.autorynek.config.ConfigManager;
import pl.durex.autorynek.config.PriceEntry;
import pl.durex.autorynek.hud.HudRenderer;
import pl.durex.autorynek.scanner.MarketScanner;
import pl.durex.autorynek.scanner.ScanEvaluator;
import pl.durex.autorynek.scanner.ScanInput;
import pl.durex.autorynek.scanner.ScanResult;

import java.util.ArrayList;
import java.util.List;

@Mixin(AbstractContainerScreen.class)
public abstract class HandledScreenMixin<T extends AbstractContainerMenu> {

    @Shadow protected int leftPos;
    @Shadow protected int topPos;

    // Cache per-ekran (jak BK-Rynek)
    private ItemStack[] lastStacks = null;
    private ScanResult[] lastResults = null;
    private int lastScreenSyncIdCache = -1;

    /**
     * Inject na render() — identycznie jak BK-Rynek HandledScreenMixin.
     * Mixin jest JEDYNYM miejscem wykrywania okazji (ma cache stacków).
     * Threaded scanner tylko wysyła pakiety gdy purchasePending = true.
     */
    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(GuiGraphics graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        Component title = self.getTitle();

        // Logika automatu (sortowanie, refresh, restart)
        MarketScanner.onScreenRender(title, self);

        if (MarketScanner.isInsideConfirmScreen()) return;
        if (!MarketScanner.isActive()) {
            HudRenderer.renderOnScreen(graphics, self);
            return;
        }

        List<Slot> slots = self.getMenu().slots;
        int syncId = self.getMenu().containerId;

        // Reset cache jesli nowy ekran
        if (lastStacks == null || syncId != lastScreenSyncIdCache) {
            lastStacks = new ItemStack[slots.size()];
            lastResults = new ScanResult[slots.size()];
            lastScreenSyncIdCache = syncId;
        }

        pl.durex.autorynek.config.ServerProfile profile =
            ConfigManager.findProfile(mc.getCurrentServer() != null ? mc.getCurrentServer().ip : "");

        for (Slot slot : slots) {
            if (slot == null) continue;
            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) continue;
            if (MarketScanner.isItemFailed(slot.index)) continue;

            // Cache — parsuj tooltip tylko gdy stack się zmienił
            ScanResult result = null;
            if (slot.index >= 0 && slot.index < lastStacks.length
                    && lastStacks[slot.index] != null
                    && ItemStack.isSameItemSameComponents(lastStacks[slot.index], stack)) {
                result = lastResults[slot.index];
            }

            if (result == null) {
                result = evaluateSlot(mc, slot, profile);
                if (slot.index >= 0 && slot.index < lastStacks.length) {
                    lastStacks[slot.index] = stack.copy();
                    lastResults[slot.index] = result;
                }
            }

            if (result != null && result.highlight) {
                // Podswietl slot
                int realX = leftPos + slot.x;
                int realY = topPos + slot.y;
                HudRenderer.drawRoundedRect(graphics, realX, realY, 16, 16, 3, 0x3555FF55);
                HudRenderer.drawRoundedRectOutline(graphics, realX, realY, 16, 16, 3, 0xCC55FF55);

                // Wywolaj onItemMatch — to triggeruje zakup
                MarketScanner.onItemMatch(slot, result.foundPrice, result.maxPrice, result.matchedEntry);
            }
        }

        HudRenderer.renderOnScreen(graphics, self);
    }

    @Inject(method = "removed", at = @At("TAIL"))
    private void onRemoved(CallbackInfo ci) {
        // Reset cache przy zamknieciu ekranu
        lastStacks = null;
        lastResults = null;
        lastScreenSyncIdCache = -1;
    }

    private ScanResult evaluateSlot(Minecraft mc, Slot slot, pl.durex.autorynek.config.ServerProfile profile) {
        if (profile == null) return ScanResult.noHighlight();
        ItemStack stack = slot.getItem();

        String materialId = net.minecraft.core.registries.BuiltInRegistries.ITEM
            .getKey(stack.getItem()).toString();

        // Czytaj lore BEZPOSREDNIO z komponentu — szybciej niz getTooltipLines()
        List<String> loreLines = new ArrayList<>();
        try {
            var loreComp = stack.get(net.minecraft.core.component.DataComponents.LORE);
            if (loreComp != null) {
                for (var line : loreComp.lines()) {
                    String l = line.getString().replaceAll("§[0-9a-fk-or]", "").trim();
                    if (!l.isEmpty()) loreLines.add(l);
                }
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

        String noColorName = stack.getHoverName().getString()
            .replaceAll("§[0-9a-fk-or]", "").trim();

        ScanInput input = new ScanInput(noColorName, loreLines, materialId,
            enchsBuilder.toString(), stack.getCount(), slot.index, componentCount, customModelData);

        return ScanEvaluator.evaluate(input, profile, false);
    }
}
