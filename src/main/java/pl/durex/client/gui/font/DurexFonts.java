package pl.durex.client.gui.font;

import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class DurexFonts {
    public static final Identifier INTER = Identifier.of("durexclient", "inter");
    public static final Identifier INTER_DISPLAY = Identifier.of("durexclient", "inter_display");

    private DurexFonts() {
    }

    public static MutableText inter(String text) {
        return styled(text, INTER);
    }

    public static MutableText display(String text) {
        return styled(text, INTER_DISPLAY);
    }

    private static MutableText styled(String text, Identifier font) {
        return Text.literal(text).setStyle(Style.EMPTY.withFont(font));
    }
}
