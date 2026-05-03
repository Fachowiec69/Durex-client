package pl.durex.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.durex.client.DurexClient;
import pl.durex.client.module.AntiKowalModule;
import pl.durex.client.util.RaycastHelper;
import pl.durex.client.util.RaycastState;

@Mixin(GameRenderer.class)
abstract class GameRendererMixin {
    @Shadow
    @Final
    private MinecraftClient client;














































    @Inject(method = "updateCrosshairTarget", at = @At("TAIL"))
    private void durex$retargetBlacksmith(float tickDelta, CallbackInfo ci) {
        AntiKowalModule antiKowal = DurexClient.getAntiKowalModule();
        if (!antiKowal.isEnabled() || client.player == null || client.world == null || !client.options.useKey.isPressed()) {
            return;
        }

        HitResult current = client.crosshairTarget;
        if (current instanceof EntityHitResult entityHitResult && !antiKowal.shouldIgnoreForTargeting(entityHitResult.getEntity())) {
            return;
        }

        Entity preferredTarget = antiKowal.findPreferredTarget(client);
        if (preferredTarget == null) {
            return;
        }

        EntityHitResult replacement = new EntityHitResult(preferredTarget);
        client.crosshairTarget = replacement;
        ((MinecraftClientAccessor) client).durex$setTargetedEntity(preferredTarget);
    }

    @Inject(method = "updateCrosshairTarget", at = @At("TAIL"))
    private void durex$noPlayerClipRedirect(float tickDelta, CallbackInfo ci) {
        if (!RaycastState.active || client.player == null || client.world == null) return;

        HitResult current = client.crosshairTarget;
        if (!(current instanceof EntityHitResult entityHit)) return;
        if (!(entityHit.getEntity() instanceof OtherClientPlayerEntity)) return;
        if (DurexClient.getFriendModule().isTracked(entityHit.getEntity())) return;

        HitResult blockBehind = RaycastHelper.raycastThrough(client);
        if (blockBehind != null && blockBehind.getType() != HitResult.Type.MISS) {
            client.crosshairTarget = blockBehind;
            ((MinecraftClientAccessor) client).durex$setTargetedEntity(null);
        }
    }
}
