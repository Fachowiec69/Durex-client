package pl.durex.autorynek.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.durex.autorynek.config.ConfigManager;
import pl.durex.autorynek.config.ServerProfile;
import pl.durex.autorynek.scanner.MarketScanner;
import pl.durex.autorynek.scanner.ScanEvaluator;
import pl.durex.autorynek.scanner.ScanInput;
import pl.durex.autorynek.scanner.ScanResult;

import java.util.ArrayList;
import java.util.List;

/**
 * KLUCZOWY MIXIN — przechwytuje packet SetContainerSlot PRZED render frame.
 *
 * Gdy serwer wysyła nowy przedmiot na rynek, ten mixin wykrywa go
 * natychmiast po otrzymaniu packetu sieciowego — nie czekamy na render.
 * To daje nam przewagę ~0-16ms nad BK-Rynek który wykrywa dopiero w render().
 *
 * Kolejność zdarzeń:
 * 1. Serwer wysyła ClientboundContainerSetSlotPacket
 * 2. TEN MIXIN wykrywa okazję i wysyła pakiet zakupu NATYCHMIAST
 * 3. BK-Rynek wykrywa dopiero przy następnym render() (do 16ms później)
 */
@Mixin(ClientPacketListener.class)
public class ContainerSetSlotMixin {

    @Inject(
        method = "handleContainerSetSlot",
        at = @At("TAIL")
    )
    private void onSetSlot(ClientboundContainerSetSlotPacket packet, CallbackInfo ci) {
        if (!MarketScanner.isActive()) return;
        if (MarketScanner.isInsideConfirmScreen()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // Sprawdź czy to aktualnie otwarty ekran
        AbstractContainerMenu menu = mc.player.containerMenu;
        if (menu == null) return;
        if (packet.getContainerId() != menu.containerId) return;

        int slotIndex = packet.getSlot();
        if (slotIndex < 0 || slotIndex >= menu.slots.size()) return;

        Slot slot = menu.slots.get(slotIndex);
        if (slot == null) return;

        ItemStack stack = slot.getItem();
        if (stack.isEmpty()) return;

        // Pomiń jeśli slot już był kupiony/nieudany
        if (MarketScanner.isItemFailed(slotIndex)) return;

        // Pobierz profil
        String server = mc.getCurrentServer() != null ? mc.getCurrentServer().ip : "";
        ServerProfile profile = ConfigManager.findProfile(server);
        if (profile == null) return;

        // Oceń slot
        ScanResult result = evaluateStack(mc, slot, stack, profile);
        if (result == null || !result.highlight) return;

        // OKAZJA — wywołaj zakup NATYCHMIAST (jesteśmy na głównym wątku MC)
        MarketScanner.onItemMatch(slot, result.foundPrice, result.maxPrice, result.matchedEntry);
    }

    private ScanResult evaluateStack(Minecraft mc, Slot slot, ItemStack stack, ServerProfile profile) {
        try {
            String materialId = net.minecraft.core.registries.BuiltInRegistries.ITEM
                .getKey(stack.getItem()).toString();

            // Czytaj lore BEZPOSREDNIO z komponentu — szybciej niz getTooltipLines()
            List<String> loreLines = new ArrayList<>();

            // Linia 0 = nazwa przedmiotu (potrzebna do ceny)
            loreLines.add(stack.getHoverName().getString().replaceAll("§[0-9a-fk-or]", "").trim());

            // Lore z komponentu DataComponents.LORE
            try {
                var loreComp = stack.get(net.minecraft.core.component.DataComponents.LORE);
                if (loreComp != null) {
                    for (var line : loreComp.lines()) {
                        String l = line.getString().replaceAll("§[0-9a-fk-or]", "").trim();
                        if (!l.isEmpty()) loreLines.add(l);
                    }
                }
            } catch (Exception ignored) {}

            // Enchantments
            StringBuilder enchsBuilder = new StringBuilder();
            try {
                net.minecraft.world.item.enchantment.ItemEnchantments enchs = stack.getEnchantments();
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
                if (cmd != null && !cmd.floats().isEmpty())
                    customModelData = Math.round(cmd.floats().get(0));
            } catch (Exception ignored) {}

            String noColorName = stack.getHoverName().getString()
                .replaceAll("§[0-9a-fk-or]", "").trim();

            ScanInput input = new ScanInput(noColorName, loreLines, materialId,
                enchsBuilder.toString(), stack.getCount(), slot.index,
                componentCount, customModelData);

            return ScanEvaluator.evaluate(input, profile, false);
        } catch (Exception e) {
            return null;
        }
    }
}
