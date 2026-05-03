package pl.durex.client.mixin;

import net.minecraft.client.render.OutlineVertexConsumerProvider;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.durex.client.DurexClient;
import pl.durex.client.module.FriendModule;

@Mixin(EntityRenderDispatcher.class)
abstract class EntityGlowMixin {

    @Inject(
            method = "render",
            at = @At("HEAD")
    )
    private <E extends Entity> void durex$applyGlow(
            E entity,
            double x, double y, double z,
            float tickDelta,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            CallbackInfo ci
    ) {
        FriendModule tracker = DurexClient.getFriendModule();
        if (!tracker.isTracked(entity)) return;
        if (!(vertexConsumers instanceof OutlineVertexConsumerProvider outline)) return;

        outline.setColor(255, 105, 180, 255);
    }
}
