package pl.durex.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.durex.client.module.NoPushModule;

@Environment(EnvType.CLIENT)
@Mixin(ClientPlayerEntity.class)
public class NoPushMixin {

    @Inject(
        method = "pushOutOfBlocks",
        at = @At("HEAD"),
        cancellable = true
    )
    private void cancelPushOutOfBlocks(double x, double z, CallbackInfo ci) {
        if (NoPushModule.isEnabled()) {
            ci.cancel();
        }
    }
}
