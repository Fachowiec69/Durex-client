/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.minecraft.class_10185
 *  net.minecraft.class_1268
 *  net.minecraft.class_1269
 *  net.minecraft.class_1297
 *  net.minecraft.class_1657
 *  net.minecraft.class_1747
 *  net.minecraft.class_2246
 *  net.minecraft.class_2338
 *  net.minecraft.class_2374
 *  net.minecraft.class_2382
 *  net.minecraft.class_239
 *  net.minecraft.class_239$class_240
 *  net.minecraft.class_243
 *  net.minecraft.class_2596
 *  net.minecraft.class_2851
 *  net.minecraft.class_310
 *  net.minecraft.class_3959
 *  net.minecraft.class_3959$class_242
 *  net.minecraft.class_3959$class_3960
 *  net.minecraft.class_3965
 *  net.minecraft.class_3966
 *  net.minecraft.class_636
 *  net.minecraft.class_746
 *  org.jetbrains.annotations.Nullable
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package com.example.mixin.client;

import com.example.LeverCobwebHudManager;
import com.example.LeverCobwebManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_10185;
import net.minecraft.class_1268;
import net.minecraft.class_1269;
import net.minecraft.class_1297;
import net.minecraft.class_1657;
import net.minecraft.class_1747;
import net.minecraft.class_2246;
import net.minecraft.class_2338;
import net.minecraft.class_2374;
import net.minecraft.class_2382;
import net.minecraft.class_239;
import net.minecraft.class_243;
import net.minecraft.class_2596;
import net.minecraft.class_2851;
import net.minecraft.class_310;
import net.minecraft.class_3959;
import net.minecraft.class_3965;
import net.minecraft.class_3966;
import net.minecraft.class_636;
import net.minecraft.class_746;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(value=EnvType.CLIENT)
@Mixin(value={class_310.class})
public abstract class MinecraftClientMixin {
    @Shadow
    @Nullable
    public class_239 field_1765;
    @Shadow
    @Nullable
    public class_746 field_1724;
    @Shadow
    @Nullable
    public class_636 field_1761;

    @Inject(method={"method_1583"}, at={@At(value="HEAD")}, cancellable=true)
    private void templateMod$allowBlockUseThroughPlayers(CallbackInfo ci) {
        class_243 end;
        class_3966 entityHitResult;
        class_239 behindHit;
        class_239 class_2392;
        boolean throughCobwebPlacement;
        if (this.field_1765 == null || this.field_1724 == null || this.field_1761 == null || !(this.field_1724.method_6047().method_7909() instanceof class_1747)) {
            return;
        }
        boolean bl = throughCobwebPlacement = LeverCobwebHudManager.isPlaceThroughCobwebsEnabled() && this.field_1724.method_5715() && this.isPlayerInsideCobweb();
        if (throughCobwebPlacement && (class_2392 = this.field_1765) instanceof class_3965) {
            class_3965 cobwebHit = (class_3965)class_2392;
            if (this.field_1724.method_37908().method_8320(cobwebHit.method_17777()).method_27852(class_2246.field_10343)) {
                class_3965 effectiveHit;
                behindHit = this.findBlockBehindCobweb(cobwebHit);
                class_3965 class_39652 = effectiveHit = behindHit == null ? null : LeverCobwebManager.resolveLeverAutomationHit((class_310)this, (class_3965)behindHit);
                if (effectiveHit == null) {
                    effectiveHit = behindHit;
                }
                if (effectiveHit != null && this.useBlockWithOptionalSneak(effectiveHit, false)) {
                    ci.cancel();
                    return;
                }
            }
        }
        if (!(LeverCobwebHudManager.isPlaceThroughPlayersEnabled() && (behindHit = this.field_1765) instanceof class_3966 && (entityHitResult = (class_3966)behindHit).method_17782() instanceof class_1657)) {
            return;
        }
        class_243 start = this.field_1724.method_5836(1.0f);
        class_3965 blockHitResult = this.raycastPastCobwebs(start, end = start.method_1019(this.field_1724.method_5828(1.0f).method_1021(4.5)), null);
        if (blockHitResult == null) {
            return;
        }
        class_3965 resolvedHit = LeverCobwebManager.resolveLeverAutomationHit((class_310)this, blockHitResult);
        if (this.useBlockWithOptionalSneak(resolvedHit != null ? resolvedHit : blockHitResult, false)) {
            ci.cancel();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private boolean useBlockWithOptionalSneak(class_3965 blockHitResult, boolean forceSneak) {
        boolean spoofSneak;
        boolean bl = spoofSneak = forceSneak && !this.field_1724.method_5715();
        if (spoofSneak && this.field_1724.field_3944 != null) {
            this.field_1724.method_5660(true);
            this.field_1724.field_3944.method_52787((class_2596)new class_2851(new class_10185(false, false, false, false, false, true, false)));
        }
        try {
            class_1269 result = this.field_1761.method_2896(this.field_1724, class_1268.field_5808, blockHitResult);
            if (!result.method_23665()) {
                boolean bl2 = false;
                return bl2;
            }
            this.field_1724.method_6104(class_1268.field_5808);
            boolean bl3 = true;
            return bl3;
        }
        finally {
            if (spoofSneak && this.field_1724.field_3944 != null) {
                this.field_1724.method_5660(false);
                this.field_1724.field_3944.method_52787((class_2596)new class_2851(new class_10185(false, false, false, false, false, false, false)));
            }
        }
    }

    private boolean isPlayerInsideCobweb() {
        return this.isCobwebAt(this.field_1724.method_24515()) || this.isCobwebAt(class_2338.method_49637((double)this.field_1724.method_23317(), (double)(this.field_1724.method_23318() + 0.7), (double)this.field_1724.method_23321())) || this.isCobwebAt(class_2338.method_49638((class_2374)this.field_1724.method_33571()));
    }

    private boolean isCobwebAt(class_2338 pos) {
        return this.field_1724.method_37908().method_8320(pos).method_27852(class_2246.field_10343);
    }

    private class_3965 findBlockBehindCobweb(class_3965 cobwebHit) {
        class_243 faceSkip = class_243.method_24954((class_2382)cobwebHit.method_17780().method_62675()).method_1021(-0.55);
        class_243 start = cobwebHit.method_17784().method_1019(faceSkip);
        class_243 end = this.field_1724.method_5836(1.0f).method_1019(this.field_1724.method_5828(1.0f).method_1021(4.5));
        return this.raycastPastCobwebs(start, end, cobwebHit.method_17777());
    }

    private class_3965 raycastPastCobwebs(class_243 start, class_243 end, @Nullable class_2338 ignoredPos) {
        if (start.method_1025(end) < 0.01) {
            return null;
        }
        class_243 currentStart = start;
        class_2338 skippedPos = ignoredPos;
        for (int attempt = 0; attempt < 5; ++attempt) {
            class_3965 hit = this.field_1724.method_37908().method_17742(new class_3959(currentStart, end, class_3959.class_3960.field_17559, class_3959.class_242.field_1348, (class_1297)this.field_1724));
            if (hit.method_17783() != class_239.class_240.field_1332) {
                return null;
            }
            class_2338 hitPos = hit.method_17777();
            if (!(this.field_1724.method_37908().method_8320(hitPos).method_27852(class_2246.field_10343) || skippedPos != null && hitPos.equals((Object)skippedPos))) {
                return hit;
            }
            class_243 remaining = end.method_1020(currentStart);
            if (remaining.method_1027() < 1.0E-4) {
                return null;
            }
            currentStart = hit.method_17784().method_1019(remaining.method_1029().method_1021(0.08));
            skippedPos = hitPos;
        }
        return null;
    }
}

