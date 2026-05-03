package pl.durex.client.mixin;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pl.durex.client.module.LeverCobwebModule;

/**
 * Przechwytuje interactBlock na poziomie ClientPlayerInteractionManager.
 *
 * Dla Auto Lever (leverOnlyModeEnabled):
 *   - spoofi sneak przed wywołaniem handleInteractBlock
 *   - dzięki temu kliknięcie PPM na blok z GUI (skrzynka, piec itp.)
 *     stawia dźwignię z offhand zamiast otwierać GUI
 *   - sneak jest zdejmowany po zakończeniu
 */
@Mixin(ClientPlayerInteractionManager.class)
public class LeverCobwebMixin {

    @Inject(method = "interactBlock", at = @At("HEAD"), cancellable = true)
    private void durex$handleLeverCobweb(ClientPlayerEntity player, Hand hand,
                                          BlockHitResult hitResult,
                                          CallbackInfoReturnable<ActionResult> cir) {
        boolean sneakSpoofed = false;

        // Spoof sneak tylko dla MAIN_HAND — to jest kliknięcie gracza
        // OFF_HAND jest wywoływany wewnętrznie przez handleInteractBlock (internalBlockInteraction=true)
        // więc nie spoofiujemy go ponownie
        if (hand == Hand.MAIN_HAND
                && LeverCobwebModule.isEnabled()
                && LeverCobwebModule.isLeverOnlyModeEnabled()
                && !player.isSneaking()) {
            player.setSneaking(true);
            sneakSpoofed = true;
        }

        try {
            if (LeverCobwebModule.handleInteractBlock(
                    (ClientPlayerInteractionManager)(Object)this, player, hand, hitResult)) {
                cir.setReturnValue(ActionResult.SUCCESS);
            }
        } finally {
            if (sneakSpoofed) {
                player.setSneaking(false);
            }
        }
    }
}
