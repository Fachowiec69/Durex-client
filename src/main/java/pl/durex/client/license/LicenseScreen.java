package pl.durex.client.license;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * Dummy LicenseScreen - nigdy nie powinien się pokazać
 * Prawdziwy LicenseScreen jest w LicenseModule (ładowany przez stage loader)
 */
public class LicenseScreen extends Screen {
    
    protected LicenseScreen() {
        super(Text.literal("Licencja"));
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        
        context.drawCenteredTextWithShadow(this.textRenderer, "Durex Client", centerX, centerY - 20, 0x00FF00);
        context.drawCenteredTextWithShadow(this.textRenderer, "Licencja aktywna", centerX, centerY, 0xFFFFFF);
    }
}
