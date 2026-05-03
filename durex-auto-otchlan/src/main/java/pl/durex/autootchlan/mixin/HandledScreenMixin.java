package pl.durex.autootchlan.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * HandledScreenMixin — placeholder.
 * Logika automatu jest w OtchlanScanner.onTick() wywoływanym z ClientTickEvents.
 * Mixin pozostawiony na wypadek przyszłych rozszerzeń (np. podświetlanie slotów).
 */
@Mixin(AbstractContainerScreen.class)
public abstract class HandledScreenMixin<T extends AbstractContainerMenu> {

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(GuiGraphics graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        // Logika jest w OtchlanScanner.onTick()
    }
}
