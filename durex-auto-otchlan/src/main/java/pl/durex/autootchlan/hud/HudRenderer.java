package pl.durex.autootchlan.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import pl.durex.autootchlan.scanner.OtchlanScanner;

public class HudRenderer {

    public static void render(GuiGraphics graphics) {
        if (!OtchlanScanner.isActive()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        int x = 4;
        int y = 4;

        // Tło
        graphics.fill(x - 2, y - 2, x + 160, y + 30, 0x88000000);

        // Tytuł
        graphics.drawString(mc.font,
            "§a[Auto Otchlan] §fAktywny",
            x, y, 0xFFFFFF, false);

        // Statystyki
        graphics.drawString(mc.font,
            "§7Zebrano: §f" + OtchlanScanner.getSessionThrown()
            + " §7| Strony: §f" + OtchlanScanner.getSessionPages()
            + " §7| Strona: §f#" + OtchlanScanner.getCurrentPage(),
            x, y + 10, 0xAAAAAA, false);

        // Hint
        graphics.drawString(mc.font,
            "§7F8=GUI | F9=toggle | /dao",
            x, y + 20, 0x555555, false);
    }
}
