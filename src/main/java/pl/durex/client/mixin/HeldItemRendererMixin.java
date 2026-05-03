package pl.durex.client.mixin;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.durex.client.DurexClient;
import pl.durex.client.module.ViewModelModule;

@Mixin(HeldItemRenderer.class)
public class HeldItemRendererMixin {

    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"))
    private void durex$applyViewModel(AbstractClientPlayerEntity player, float tickDelta, float pitch,
            Hand hand, float swingProgress, ItemStack item, float equipProgress,
            MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {

        ViewModelModule vm = DurexClient.getViewModelModule();
        if (!vm.isEnabled()) return;

        boolean isMain = hand == Hand.MAIN_HAND;

        float rotX = isMain ? vm.rightRotX : vm.leftRotX;
        float rotY = isMain ? vm.rightRotY : vm.leftRotY;
        float rotZ = isMain ? vm.rightRotZ : vm.leftRotZ;
        float posX = isMain ? vm.rightPosX : vm.leftPosX;
        float posY = isMain ? vm.rightPosY : vm.leftPosY;
        float posZ = isMain ? vm.rightPosZ : vm.leftPosZ;
        float scale = isMain ? vm.rightScale : vm.leftScale;

        if (rotX == 0 && rotY == 0 && rotZ == 0 && posX == 0 && posY == 0 && posZ == 0 && scale == 1f) return;

        // Każde wywołanie renderFirstPersonItem dostaje WŁASNĄ kopię MatrixStack
        // więc transformacje są już izolowane - nie trzeba push/pop
        matrices.translate(posX, posY, posZ);
        matrices.translate(0.5, 0.5, 0.5);
        matrices.scale(scale, scale, scale);
        matrices.translate(-0.5, -0.5, -0.5);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(rotX));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotY));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotZ));
    }
}
