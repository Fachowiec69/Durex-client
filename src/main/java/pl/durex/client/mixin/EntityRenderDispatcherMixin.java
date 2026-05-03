package pl.durex.client.mixin;

import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pl.durex.client.DurexClient;

@Mixin(EntityRenderDispatcher.class)
abstract class EntityRenderDispatcherMixin {
    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void durex$hideOtherPlayers(Entity entity, Frustum frustum, double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
        // Chowaj graczy gdy AntiKowal włączony, ALE nie tych którzy są w FriendModule
        if (DurexClient.getAntiKowalModule().isEnabled()
                && entity instanceof OtherClientPlayerEntity
                && !DurexClient.getFriendModule().isTracked(entity)) {
            cir.setReturnValue(false);
        }
    }
}
