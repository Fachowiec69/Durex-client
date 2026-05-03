/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.minecraft.class_10185
 *  net.minecraft.class_1268
 *  net.minecraft.class_1269
 *  net.minecraft.class_1657
 *  net.minecraft.class_1661
 *  net.minecraft.class_1713
 *  net.minecraft.class_1747
 *  net.minecraft.class_1792
 *  net.minecraft.class_1799
 *  net.minecraft.class_1802
 *  net.minecraft.class_2246
 *  net.minecraft.class_2248
 *  net.minecraft.class_2338
 *  net.minecraft.class_2350
 *  net.minecraft.class_2350$class_2353
 *  net.minecraft.class_2374
 *  net.minecraft.class_2382
 *  net.minecraft.class_239
 *  net.minecraft.class_2401
 *  net.minecraft.class_243
 *  net.minecraft.class_2596
 *  net.minecraft.class_2680
 *  net.minecraft.class_2851
 *  net.minecraft.class_2868
 *  net.minecraft.class_310
 *  net.minecraft.class_3965
 *  net.minecraft.class_636
 *  net.minecraft.class_746
 */
package com.example;

import com.example.LeverCobwebHudManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_10185;
import net.minecraft.class_1268;
import net.minecraft.class_1269;
import net.minecraft.class_1657;
import net.minecraft.class_1661;
import net.minecraft.class_1713;
import net.minecraft.class_1747;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_2246;
import net.minecraft.class_2248;
import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_2374;
import net.minecraft.class_2382;
import net.minecraft.class_239;
import net.minecraft.class_2401;
import net.minecraft.class_243;
import net.minecraft.class_2596;
import net.minecraft.class_2680;
import net.minecraft.class_2851;
import net.minecraft.class_2868;
import net.minecraft.class_310;
import net.minecraft.class_3965;
import net.minecraft.class_636;
import net.minecraft.class_746;

@Environment(value=EnvType.CLIENT)
public final class LeverCobwebManager {
    private static final int PLACE_RETRIES = 3;
    private static final int FLUID_PLACE_ATTEMPTS = 4;
    private static final int FLUID_PICKUP_ATTEMPTS = 8;
    private static final int FAILED_RETRY_BACKOFF_TICKS = 1;
    private static final int MAX_STABILITY_WAIT_TICKS = 2;
    private static PendingSequence pendingSequence;
    private static LeverHoldState leverHoldState;
    private static boolean internalBlockInteraction;
    private static boolean leverSneakSpoofed;

    private LeverCobwebManager() {
    }

    public static boolean handleInteractBlock(class_636 interactionManager, class_746 player, class_1268 hand, class_3965 hitResult) {
        if (internalBlockInteraction || hand != class_1268.field_5808 || interactionManager == null || player == null || hitResult == null) {
            return false;
        }
        if (pendingSequence != null || leverHoldState != null) {
            return true;
        }
        class_310 client = class_310.method_1551();
        if (client.field_1687 == null || client.field_1724 != player || player.method_7325() || client.field_1755 != null) {
            return false;
        }
        class_1799 heldStack = player.method_6047();
        class_1799 offhandStack = player.method_6079();
        class_1792 class_17922 = heldStack.method_7909();
        if (class_17922 instanceof class_1747) {
            class_1747 blockItem = (class_1747)class_17922;
            PlacementPlan plan = LeverCobwebManager.resolvePlan(client, hitResult, heldStack.method_7909(), blockItem, player.method_5715());
            if (plan != null) {
                FluidSource fluidSource = LeverCobwebManager.findPreferredFluidSource(player.method_31548());
                if (fluidSource == null) {
                    return false;
                }
                int originalHotbarSlot = player.method_31548().field_7545;
                pendingSequence = new PendingSequence(plan, originalHotbarSlot, fluidSource.inventorySlot(), fluidSource.bucketItem(), fluidSource.inventorySlot() < 9 ? FluidAccessMode.HOTBAR : FluidAccessMode.SWAP_WITH_ORIGINAL_SLOT, SequenceStage.SELECT_FLUID, 0, 3, 4, 8, false, 0);
                return true;
            }
        }
        if (!offhandStack.method_31574(class_1802.field_8865) || !LeverCobwebHudManager.isHoldLeverEnabled()) {
            return false;
        }
        LeverHoldPlan leverHoldPlan = LeverCobwebManager.resolveLeverHoldPlan(client, hitResult);
        if (leverHoldPlan == null) {
            return false;
        }
        leverHoldState = new LeverHoldState(leverHoldPlan.basePos(), 0);
        return true;
    }

    public static class_3965 resolveLeverAutomationHit(class_310 client, class_3965 hitResult) {
        class_2338[] candidates;
        if (client == null || client.field_1687 == null || hitResult == null) {
            return null;
        }
        class_2338 clickedPos = hitResult.method_17777();
        class_2680 clickedState = client.field_1687.method_8320(clickedPos);
        if (clickedState.method_26204() instanceof class_2401) {
            return LeverCobwebManager.createSupportHitForLever(client, hitResult);
        }
        class_2350 preferredSide = hitResult.method_17780();
        class_2338 targetPos = clickedPos.method_10093(preferredSide);
        if (client.field_1687.method_8320(targetPos).method_26204() instanceof class_2401) {
            return LeverCobwebManager.createPlacementHit(clickedPos, preferredSide);
        }
        for (class_2338 candidate : candidates = new class_2338[]{clickedPos, clickedPos.method_10084(), clickedPos.method_10074(), targetPos, targetPos.method_10084(), targetPos.method_10074()}) {
            class_3965 supportHit = LeverCobwebManager.findAdjacentLeverSupportHit(client, candidate, preferredSide);
            if (supportHit == null) continue;
            return supportHit;
        }
        return null;
    }

    public static void clientTick(class_310 client) {
        LeverCobwebManager.handleLeverHoldTick(client);
        if (pendingSequence == null) {
            return;
        }
        if (client == null || client.field_1724 == null || client.field_1687 == null || client.field_1761 == null) {
            pendingSequence = null;
            return;
        }
        int stagesLeftThisTick = 1 + LeverCobwebHudManager.getSpeedProfile().extraInstantStagesPerTick();
        while (pendingSequence != null && stagesLeftThisTick-- > 0) {
            PendingSequence sequence = pendingSequence;
            if (sequence.delayTicks() > 0) {
                pendingSequence = sequence.withDelay(sequence.delayTicks() - 1);
                return;
            }
            if (LeverCobwebManager.shouldWaitForStableUse(client, sequence)) {
                pendingSequence = sequence.withDelay(1).withStabilityWaitedTicks(sequence.stabilityWaitedTicks() + 1);
                return;
            }
            switch (sequence.stage().ordinal()) {
                case 0: {
                    LeverCobwebManager.handleSelectFluid(client, sequence);
                    break;
                }
                case 1: {
                    LeverCobwebManager.handleUseFluid(client, sequence);
                    break;
                }
                case 2: {
                    LeverCobwebManager.handleWaitForEmptyBucket(client, sequence);
                    break;
                }
                case 3: {
                    LeverCobwebManager.handlePickupFluid(client, sequence);
                    break;
                }
                case 4: {
                    LeverCobwebManager.handleWaitForFilledBucket(client, sequence);
                    break;
                }
                case 5: {
                    LeverCobwebManager.handleRestorePlacementItem(client, sequence);
                    break;
                }
                case 6: {
                    LeverCobwebManager.handlePlaceBlock(client, sequence);
                }
            }
        }
    }

    public static void handleDisconnect() {
        pendingSequence = null;
        leverHoldState = null;
        leverSneakSpoofed = false;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static void handleLeverHoldTick(class_310 client) {
        if (leverHoldState == null) {
            return;
        }
        if (client == null || client.field_1724 == null || client.field_1687 == null || client.field_1761 == null) {
            leverHoldState = null;
            leverSneakSpoofed = false;
            return;
        }
        if (pendingSequence != null || !LeverCobwebHudManager.isHoldLeverEnabled() || client.field_1755 != null || client.field_1724.method_7325() || !client.field_1724.method_6079().method_31574(class_1802.field_8865) || !client.field_1690.field_1904.method_1434()) {
            LeverCobwebManager.stopLeverHold(client);
            return;
        }
        if (leverHoldState.delayTicks() > 0) {
            leverHoldState = leverHoldState.withDelay(leverHoldState.delayTicks() - 1);
            return;
        }
        class_239 class_2392 = client.field_1765;
        if (!(class_2392 instanceof class_3965)) {
            LeverCobwebManager.stopLeverHold(client);
            return;
        }
        class_3965 blockHitResult = (class_3965)class_2392;
        LeverHoldPlan plan = LeverCobwebManager.resolveLeverHoldPlan(client, blockHitResult);
        if (plan == null) {
            LeverCobwebManager.stopLeverHold(client);
            return;
        }
        leverHoldState = new LeverHoldState(plan.basePos(), 0);
        boolean shouldSneak = client.field_1687.method_8320(blockHitResult.method_17777()).method_26204() instanceof class_2401;
        if (shouldSneak) {
            LeverCobwebManager.ensureLeverSneak(client);
        } else {
            LeverCobwebManager.stopLeverHoldSneak(client);
        }
        try {
            internalBlockInteraction = true;
            class_1269 result = client.field_1761.method_2896(client.field_1724, class_1268.field_5810, blockHitResult);
            if (result.method_23665()) {
                client.field_1724.method_6104(class_1268.field_5810);
            }
        }
        finally {
            internalBlockInteraction = false;
        }
    }

    private static void handleSelectFluid(class_310 client, PendingSequence sequence) {
        boolean swapActive = sequence.temporarySwapActive();
        if (sequence.fluidAccessMode() == FluidAccessMode.HOTBAR) {
            LeverCobwebManager.selectHotbarSlot(client, sequence.fluidInventorySlot());
        } else {
            LeverCobwebManager.swapInventorySlotIntoHotbar(client, sequence.fluidInventorySlot(), sequence.originalHotbarSlot());
            LeverCobwebManager.selectHotbarSlot(client, sequence.originalHotbarSlot());
            swapActive = true;
        }
        pendingSequence = sequence.withTemporarySwapActive(swapActive).advanceTo(SequenceStage.USE_FLUID, LeverCobwebManager.configuredRetryDelayTicks());
    }

    private static void handleUseFluid(class_310 client, PendingSequence sequence) {
        if (!client.field_1724.method_6047().method_31574(sequence.fluidBucketItem())) {
            if (!LeverCobwebManager.reselectFluidForSequence(client, sequence)) {
                LeverCobwebManager.finishSequence(client, sequence);
                return;
            }
            pendingSequence = sequence.advanceTo(SequenceStage.USE_FLUID, LeverCobwebManager.configuredRetryBackoffTicks());
            return;
        }
        class_1269 result = LeverCobwebManager.interactStoredHit(client, sequence.plan().fluidPlaceHit(), sequence.plan().requiresSneak());
        if (!result.method_23665()) {
            result = client.field_1761.method_2919((class_1657)client.field_1724, class_1268.field_5808);
        }
        if (result.method_23665()) {
            client.field_1724.method_6104(class_1268.field_5808);
        }
        pendingSequence = sequence.withFluidPlaceAttemptsLeft(sequence.fluidPlaceAttemptsLeft() - 1).advanceTo(SequenceStage.WAIT_FOR_EMPTY_BUCKET, LeverCobwebManager.configuredSettleDelayTicks());
    }

    private static void handleWaitForEmptyBucket(class_310 client, PendingSequence sequence) {
        if (client.field_1724.method_6047().method_31574(class_1802.field_8550)) {
            pendingSequence = sequence.advanceTo(SequenceStage.PICKUP_FLUID, LeverCobwebManager.configuredSettleDelayTicks());
            return;
        }
        if (sequence.fluidPlaceAttemptsLeft() <= 0) {
            LeverCobwebManager.finishSequence(client, sequence);
            return;
        }
        pendingSequence = sequence.advanceTo(SequenceStage.USE_FLUID, LeverCobwebManager.configuredRetryBackoffTicks());
    }

    private static void handlePickupFluid(class_310 client, PendingSequence sequence) {
        if (client.field_1724.method_6047().method_31574(sequence.fluidBucketItem())) {
            pendingSequence = sequence.advanceTo(SequenceStage.RESTORE_PLACEMENT_ITEM, LeverCobwebManager.configuredPostPickupDelayTicks());
            return;
        }
        if (!client.field_1724.method_6047().method_31574(class_1802.field_8550)) {
            if (!LeverCobwebManager.reselectFluidForSequence(client, sequence)) {
                LeverCobwebManager.finishSequence(client, sequence);
                return;
            }
            pendingSequence = sequence.advanceTo(SequenceStage.PICKUP_FLUID, LeverCobwebManager.configuredRetryBackoffTicks());
            return;
        }
        class_1269 result = LeverCobwebManager.interactStoredHit(client, sequence.plan().fluidPickupHit(), sequence.plan().requiresSneak());
        if (!result.method_23665()) {
            result = client.field_1761.method_2919((class_1657)client.field_1724, class_1268.field_5808);
        }
        if (result.method_23665()) {
            client.field_1724.method_6104(class_1268.field_5808);
        }
        pendingSequence = sequence.withFluidPickupAttemptsLeft(sequence.fluidPickupAttemptsLeft() - 1).advanceTo(SequenceStage.WAIT_FOR_FILLED_BUCKET, LeverCobwebManager.configuredSettleDelayTicks());
    }

    private static void handleWaitForFilledBucket(class_310 client, PendingSequence sequence) {
        if (client.field_1724.method_6047().method_31574(sequence.fluidBucketItem())) {
            pendingSequence = sequence.advanceTo(SequenceStage.RESTORE_PLACEMENT_ITEM, LeverCobwebManager.configuredPostPickupDelayTicks());
            return;
        }
        if (sequence.fluidPickupAttemptsLeft() <= 0) {
            LeverCobwebManager.finishSequence(client, sequence);
            return;
        }
        pendingSequence = sequence.advanceTo(SequenceStage.PICKUP_FLUID, LeverCobwebManager.configuredRetryBackoffTicks());
    }

    private static void handleRestorePlacementItem(class_310 client, PendingSequence sequence) {
        PendingSequence updatedSequence = sequence;
        if (sequence.fluidAccessMode() == FluidAccessMode.SWAP_WITH_ORIGINAL_SLOT && sequence.temporarySwapActive()) {
            LeverCobwebManager.swapInventorySlotIntoHotbar(client, sequence.fluidInventorySlot(), sequence.originalHotbarSlot());
            updatedSequence = sequence.withTemporarySwapActive(false);
        }
        LeverCobwebManager.selectHotbarSlot(client, sequence.originalHotbarSlot());
        pendingSequence = updatedSequence.advanceTo(SequenceStage.PLACE_BLOCK, LeverCobwebManager.configuredPostPickupDelayTicks());
    }

    private static void handlePlaceBlock(class_310 client, PendingSequence sequence) {
        if (!sequence.plan().matchesHeldStack(client.field_1724.method_6047())) {
            if (!LeverCobwebManager.reselectPlacementItem(client, sequence)) {
                LeverCobwebManager.finishSequence(client, sequence);
                return;
            }
            pendingSequence = sequence.withDelay(LeverCobwebManager.configuredRetryBackoffTicks());
            return;
        }
        class_2680 currentState = client.field_1687.method_8320(sequence.plan().targetPos());
        if (sequence.plan().isPlacementSatisfied(currentState) && !sequence.plan().refreshExistingTarget()) {
            LeverCobwebManager.finishSequence(client, sequence);
            return;
        }
        if (!client.field_1687.method_8316(sequence.plan().targetPos()).method_15769()) {
            if (sequence.placeRetriesLeft() <= 1) {
                LeverCobwebManager.finishSequence(client, sequence);
                return;
            }
            pendingSequence = sequence.withPlaceRetriesLeft(sequence.placeRetriesLeft() - 1).withDelay(Math.max(1, LeverCobwebManager.configuredSettleDelayTicks()));
            return;
        }
        if (sequence.plan().isPlacementSatisfied(currentState)) {
            if (sequence.placeRetriesLeft() <= 1) {
                LeverCobwebManager.finishSequence(client, sequence);
                return;
            }
            pendingSequence = sequence.withPlaceRetriesLeft(sequence.placeRetriesLeft() - 1).withDelay(LeverCobwebManager.configuredRetryDelayTicks());
            return;
        }
        class_1269 result = LeverCobwebManager.interactStoredHit(client, sequence.plan().placementHit(), sequence.plan().requiresSneak());
        if (result.method_23665()) {
            client.field_1724.method_6104(class_1268.field_5808);
        }
        if (sequence.plan().isPlacementSatisfied(client.field_1687.method_8320(sequence.plan().targetPos())) || sequence.placeRetriesLeft() <= 1) {
            LeverCobwebManager.finishSequence(client, sequence);
            return;
        }
        pendingSequence = sequence.withPlaceRetriesLeft(sequence.placeRetriesLeft() - 1).withDelay(LeverCobwebManager.configuredRetryDelayTicks());
    }

    private static void finishSequence(class_310 client, PendingSequence sequence) {
        if (sequence.fluidAccessMode() == FluidAccessMode.SWAP_WITH_ORIGINAL_SLOT && sequence.temporarySwapActive()) {
            LeverCobwebManager.swapInventorySlotIntoHotbar(client, sequence.fluidInventorySlot(), sequence.originalHotbarSlot());
        }
        if (LeverCobwebHudManager.isSwitchToBestSwordEnabled()) {
            if (!LeverCobwebManager.selectBestSword(client, sequence.originalHotbarSlot())) {
                LeverCobwebManager.selectHotbarSlot(client, sequence.originalHotbarSlot());
            }
        } else {
            LeverCobwebManager.selectHotbarSlot(client, sequence.originalHotbarSlot());
        }
        pendingSequence = null;
    }

    private static PlacementPlan resolvePlan(class_310 client, class_3965 hitResult, class_1792 heldItem, class_1747 heldBlockItem, boolean requiresSneak) {
        class_2338 clickedPos = hitResult.method_17777();
        class_2680 clickedState = client.field_1687.method_8320(clickedPos);
        class_2338 directTargetPos = clickedPos.method_10093(hitResult.method_17780());
        class_2680 directTargetState = client.field_1687.method_8320(directTargetPos);
        if (clickedState.method_26204() instanceof class_2401) {
            class_3965 supportHit = LeverCobwebManager.createSupportHitForLever(client, hitResult);
            return LeverCobwebManager.createLeverPlacementPlan(client, supportHit, heldItem, heldBlockItem.method_7711(), requiresSneak);
        }
        if (clickedState.method_27852(class_2246.field_10343)) {
            PlacementPlan leverPlan;
            class_3965 leverSupportHit = LeverCobwebManager.resolveLeverAutomationHit(client, hitResult);
            if (leverSupportHit != null && (leverPlan = LeverCobwebManager.createLeverPlacementPlan(client, leverSupportHit, heldItem, heldBlockItem.method_7711(), true)) != null) {
                return leverPlan;
            }
            return null;
        }
        if (directTargetState.method_26215() || directTargetState.method_45474() || directTargetState.method_27852(class_2246.field_10343)) {
            return null;
        }
        if (directTargetState.method_26204() instanceof class_2401) {
            return LeverCobwebManager.createLeverPlacementPlan(client, LeverCobwebManager.createPlacementHit(clickedPos, hitResult.method_17780()), heldItem, heldBlockItem.method_7711(), requiresSneak);
        }
        if (directTargetState.method_27852(heldBlockItem.method_7711()) && heldBlockItem.method_7711() != class_2246.field_10343) {
            return null;
        }
        return null;
    }

    private static PlacementPlan createLeverPlacementPlan(class_310 client, class_3965 supportHit, class_1792 heldItem, class_2248 expectedBlock, boolean requiresSneak) {
        if (client == null || client.field_1687 == null || supportHit == null) {
            return null;
        }
        class_2338 targetPos = supportHit.method_17777().method_10093(supportHit.method_17780());
        class_2680 targetState = client.field_1687.method_8320(targetPos);
        if (!(targetState.method_26204() instanceof class_2401)) {
            return null;
        }
        class_3965 fluidPickupHit = LeverCobwebManager.createFluidPickupHit(targetPos);
        return new PlacementPlan(targetPos, supportHit, fluidPickupHit, supportHit, heldItem, expectedBlock, false, requiresSneak);
    }

    private static LeverHoldPlan resolveLeverHoldPlan(class_310 client, class_3965 hitResult) {
        class_2338 baseBelowLever;
        class_2338 clickedPos = hitResult.method_17777();
        class_2680 clickedState = client.field_1687.method_8320(clickedPos);
        if (clickedState.method_26204() instanceof class_2401 && LeverCobwebManager.isValidLeverHoldBase(client, baseBelowLever = clickedPos.method_10074())) {
            return new LeverHoldPlan(baseBelowLever);
        }
        if (LeverCobwebManager.isValidLeverHoldBase(client, clickedPos)) {
            return new LeverHoldPlan(clickedPos);
        }
        return null;
    }

    private static boolean isValidLeverHoldBase(class_310 client, class_2338 basePos) {
        if (!LeverCobwebManager.hasLeverSupport(client.field_1687.method_8320(basePos))) {
            return false;
        }
        if (!LeverCobwebManager.hasLeverSupport(client.field_1687.method_8320(basePos.method_10086(2)))) {
            return false;
        }
        class_2680 leverSlotState = client.field_1687.method_8320(basePos.method_10084());
        if (!(leverSlotState.method_26215() || leverSlotState.method_45474() || leverSlotState.method_26204() instanceof class_2401)) {
            return false;
        }
        for (class_2350 direction : class_2350.class_2353.field_11062) {
            if (!LeverCobwebManager.hasLeverSupport(client.field_1687.method_8320(basePos.method_10093(direction).method_10084()))) continue;
            return true;
        }
        return false;
    }

    private static boolean hasLeverSupport(class_2680 state) {
        return !state.method_26215() && !state.method_45474();
    }

    private static class_3965 createSupportHitForLever(class_310 client, class_3965 leverHit) {
        class_2350 supportFace;
        class_2338 leverPos = leverHit.method_17777();
        class_2338 supportPos = leverPos.method_10093((supportFace = leverHit.method_17780()).method_10153());
        if (LeverCobwebManager.hasLeverSupport(client.field_1687.method_8320(supportPos))) {
            return LeverCobwebManager.createPlacementHit(supportPos, supportFace);
        }
        for (class_2350 direction : class_2350.values()) {
            class_2338 fallbackSupportPos = leverPos.method_10093(direction);
            if (!LeverCobwebManager.hasLeverSupport(client.field_1687.method_8320(fallbackSupportPos))) continue;
            return LeverCobwebManager.createPlacementHit(fallbackSupportPos, direction.method_10153());
        }
        return null;
    }

    private static class_3965 findAdjacentLeverSupportHit(class_310 client, class_2338 supportPos, class_2350 preferredSide) {
        if (!LeverCobwebManager.hasLeverSupport(client.field_1687.method_8320(supportPos))) {
            return null;
        }
        if (preferredSide != null && client.field_1687.method_8320(supportPos.method_10093(preferredSide)).method_26204() instanceof class_2401) {
            return LeverCobwebManager.createPlacementHit(supportPos, preferredSide);
        }
        for (class_2350 direction : class_2350.values()) {
            if (direction == preferredSide || !(client.field_1687.method_8320(supportPos.method_10093(direction)).method_26204() instanceof class_2401)) continue;
            return LeverCobwebManager.createPlacementHit(supportPos, direction);
        }
        return null;
    }

    private static class_3965 createPlacementHit(class_2338 supportPos, class_2350 supportFace) {
        class_243 hitPos = class_243.method_24953((class_2382)supportPos).method_1031((double)supportFace.method_10148() * 0.5, (double)supportFace.method_10164() * 0.5, (double)supportFace.method_10165() * 0.5);
        return new class_3965(hitPos, supportFace, supportPos, false);
    }

    private static class_3965 createFluidPickupHit(class_2338 targetPos) {
        return new class_3965(class_243.method_24953((class_2382)targetPos), class_2350.field_11036, targetPos, false);
    }

    private static boolean isInteractionStage(SequenceStage stage) {
        return stage == SequenceStage.PLACE_BLOCK;
    }

    private static boolean shouldWaitForStableUse(class_310 client, PendingSequence sequence) {
        if (client == null || client.field_1724 == null) {
            return false;
        }
        if (!LeverCobwebManager.isInteractionStage(sequence.stage())) {
            return false;
        }
        if (sequence.stabilityWaitedTicks() >= 2) {
            return false;
        }
        class_243 velocity = client.field_1724.method_18798();
        double horizontalSpeedSq = velocity.field_1352 * velocity.field_1352 + velocity.field_1350 * velocity.field_1350;
        if (LeverCobwebManager.isPlayerInCobweb(client)) {
            return Math.abs(velocity.field_1351) > 0.3 || horizontalSpeedSq > 0.18;
        }
        boolean airborne = !client.field_1724.method_24828();
        boolean verticalUnstable = Math.abs(velocity.field_1351) > 0.28;
        boolean horizontalUnstable = horizontalSpeedSq > 0.16;
        return airborne && (verticalUnstable || horizontalUnstable);
    }

    private static boolean isPlayerInCobweb(class_310 client) {
        if (client.field_1687 == null) {
            return false;
        }
        class_2338 feet = client.field_1724.method_24515();
        if (client.field_1687.method_8320(feet).method_27852(class_2246.field_10343)) {
            return true;
        }
        class_2338 eye = class_2338.method_49638((class_2374)client.field_1724.method_33571());
        return client.field_1687.method_8320(eye).method_27852(class_2246.field_10343);
    }

    private static int configuredRetryDelayTicks() {
        return LeverCobwebHudManager.getSpeedProfile().retryDelayTicks();
    }

    private static int configuredSettleDelayTicks() {
        return LeverCobwebHudManager.getSpeedProfile().settleDelayTicks();
    }

    private static int configuredPostPickupDelayTicks() {
        return LeverCobwebHudManager.getSpeedProfile().postPickupDelayTicks();
    }

    private static int configuredRetryBackoffTicks() {
        return 1;
    }

    private static int findInventorySlot(class_1661 inventory, class_1792 item) {
        for (int slot = 0; slot < 36; ++slot) {
            if (!inventory.method_5438(slot).method_31574(item)) continue;
            return slot;
        }
        return -1;
    }

    private static FluidSource findPreferredFluidSource(class_1661 inventory) {
        int waterSlot = LeverCobwebManager.findInventorySlot(inventory, class_1802.field_8705);
        if (waterSlot >= 0) {
            return new FluidSource(waterSlot, class_1802.field_8705);
        }
        int lavaSlot = LeverCobwebManager.findInventorySlot(inventory, class_1802.field_8187);
        if (lavaSlot >= 0) {
            return new FluidSource(lavaSlot, class_1802.field_8187);
        }
        return null;
    }

    private static boolean selectBestSword(class_310 client, int fallbackHotbarSlot) {
        class_1661 inventory = client.field_1724.method_31548();
        int bestSwordSlot = -1;
        float bestSwordScore = Float.NEGATIVE_INFINITY;
        for (int slot = 0; slot < 36; ++slot) {
            class_1799 stack = inventory.method_5438(slot);
            float swordScore = LeverCobwebManager.swordScore(stack);
            if (swordScore <= bestSwordScore) continue;
            bestSwordScore = swordScore;
            bestSwordSlot = slot;
        }
        if (bestSwordSlot < 0) {
            return false;
        }
        if (bestSwordSlot < 9) {
            LeverCobwebManager.selectHotbarSlot(client, bestSwordSlot);
            return true;
        }
        LeverCobwebManager.swapInventorySlotIntoHotbar(client, bestSwordSlot, fallbackHotbarSlot);
        LeverCobwebManager.selectHotbarSlot(client, fallbackHotbarSlot);
        return true;
    }

    private static float swordScore(class_1799 stack) {
        if (stack.method_31574(class_1802.field_22022)) {
            return 7.0f;
        }
        if (stack.method_31574(class_1802.field_8802)) {
            return 6.0f;
        }
        if (stack.method_31574(class_1802.field_8371)) {
            return 5.0f;
        }
        if (stack.method_31574(class_1802.field_8528)) {
            return 4.0f;
        }
        if (stack.method_31574(class_1802.field_8845)) {
            return 3.0f;
        }
        if (stack.method_31574(class_1802.field_8091)) {
            return 2.0f;
        }
        return Float.NEGATIVE_INFINITY;
    }

    private static void selectHotbarSlot(class_310 client, int hotbarSlot) {
        if (!class_1661.method_7380((int)hotbarSlot)) {
            return;
        }
        client.field_1724.method_31548().method_61496(hotbarSlot);
        if (client.method_1562() != null) {
            client.method_1562().method_52787((class_2596)new class_2868(hotbarSlot));
        }
    }

    private static void swapInventorySlotIntoHotbar(class_310 client, int inventorySlot, int hotbarSlot) {
        int screenSlot = LeverCobwebManager.inventoryIndexToScreenSlot(inventorySlot);
        if (screenSlot < 0 || !class_1661.method_7380((int)hotbarSlot)) {
            return;
        }
        client.field_1761.method_2906(client.field_1724.field_7498.field_7763, screenSlot, hotbarSlot, class_1713.field_7791, (class_1657)client.field_1724);
    }

    private static int inventoryIndexToScreenSlot(int inventorySlot) {
        if (inventorySlot >= 0 && inventorySlot < 9) {
            return 36 + inventorySlot;
        }
        if (inventorySlot >= 9 && inventorySlot < 36) {
            return inventorySlot;
        }
        return -1;
    }

    private static void ensureLeverSneak(class_310 client) {
        if (leverSneakSpoofed || client.field_1724.method_5715()) {
            return;
        }
        client.field_1724.method_5660(true);
        if (client.method_1562() != null) {
            client.method_1562().method_52787((class_2596)new class_2851(new class_10185(false, false, false, false, false, true, false)));
        }
        leverSneakSpoofed = true;
    }

    private static void stopLeverHoldSneak(class_310 client) {
        if (!leverSneakSpoofed || client == null || client.field_1724 == null) {
            leverSneakSpoofed = false;
            return;
        }
        client.field_1724.method_5660(false);
        if (client.method_1562() != null) {
            client.method_1562().method_52787((class_2596)new class_2851(new class_10185(false, false, false, false, false, false, false)));
        }
        leverSneakSpoofed = false;
    }

    private static void stopLeverHold(class_310 client) {
        leverHoldState = null;
        LeverCobwebManager.stopLeverHoldSneak(client);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static class_1269 interactStoredHit(class_310 client, class_3965 hitResult, boolean forceSneak) {
        boolean spoofSneak;
        if (client == null || client.field_1724 == null || client.field_1761 == null || hitResult == null) {
            return class_1269.field_5811;
        }
        boolean bl = spoofSneak = forceSneak && !client.field_1724.method_5715();
        if (spoofSneak && client.field_1724.field_3944 != null) {
            client.field_1724.method_5660(true);
            client.field_1724.field_3944.method_52787((class_2596)new class_2851(new class_10185(false, false, false, false, false, true, false)));
        }
        try {
            internalBlockInteraction = true;
            class_1269 class_12692 = client.field_1761.method_2896(client.field_1724, class_1268.field_5808, hitResult);
            return class_12692;
        }
        finally {
            internalBlockInteraction = false;
            if (spoofSneak && client.field_1724.field_3944 != null) {
                client.field_1724.method_5660(false);
                client.field_1724.field_3944.method_52787((class_2596)new class_2851(new class_10185(false, false, false, false, false, false, false)));
            }
        }
    }

    private static boolean reselectFluidForSequence(class_310 client, PendingSequence sequence) {
        if (sequence.fluidAccessMode() == FluidAccessMode.HOTBAR) {
            if (!class_1661.method_7380((int)sequence.fluidInventorySlot())) {
                return false;
            }
            LeverCobwebManager.selectHotbarSlot(client, sequence.fluidInventorySlot());
            return true;
        }
        if (!sequence.temporarySwapActive()) {
            LeverCobwebManager.swapInventorySlotIntoHotbar(client, sequence.fluidInventorySlot(), sequence.originalHotbarSlot());
        }
        LeverCobwebManager.selectHotbarSlot(client, sequence.originalHotbarSlot());
        return true;
    }

    private static boolean reselectPlacementItem(class_310 client, PendingSequence sequence) {
        if (client == null || client.field_1724 == null) {
            return false;
        }
        class_1799 originalStack = client.field_1724.method_31548().method_5438(sequence.originalHotbarSlot());
        if (!sequence.plan().matchesHeldStack(originalStack)) {
            return false;
        }
        LeverCobwebManager.selectHotbarSlot(client, sequence.originalHotbarSlot());
        return true;
    }

    @Environment(value=EnvType.CLIENT)
    private record PendingSequence(PlacementPlan plan, int originalHotbarSlot, int fluidInventorySlot, class_1792 fluidBucketItem, FluidAccessMode fluidAccessMode, SequenceStage stage, int delayTicks, int placeRetriesLeft, int fluidPlaceAttemptsLeft, int fluidPickupAttemptsLeft, boolean temporarySwapActive, int stabilityWaitedTicks) {
        private PendingSequence advanceTo(SequenceStage nextStage, int nextDelayTicks) {
            return new PendingSequence(this.plan, this.originalHotbarSlot, this.fluidInventorySlot, this.fluidBucketItem, this.fluidAccessMode, nextStage, nextDelayTicks, this.placeRetriesLeft, this.fluidPlaceAttemptsLeft, this.fluidPickupAttemptsLeft, this.temporarySwapActive, 0);
        }

        private PendingSequence withDelay(int nextDelayTicks) {
            return new PendingSequence(this.plan, this.originalHotbarSlot, this.fluidInventorySlot, this.fluidBucketItem, this.fluidAccessMode, this.stage, nextDelayTicks, this.placeRetriesLeft, this.fluidPlaceAttemptsLeft, this.fluidPickupAttemptsLeft, this.temporarySwapActive, this.stabilityWaitedTicks);
        }

        private PendingSequence withPlaceRetriesLeft(int nextPlaceRetriesLeft) {
            return new PendingSequence(this.plan, this.originalHotbarSlot, this.fluidInventorySlot, this.fluidBucketItem, this.fluidAccessMode, this.stage, this.delayTicks, nextPlaceRetriesLeft, this.fluidPlaceAttemptsLeft, this.fluidPickupAttemptsLeft, this.temporarySwapActive, this.stabilityWaitedTicks);
        }

        private PendingSequence withFluidPlaceAttemptsLeft(int nextFluidPlaceAttemptsLeft) {
            return new PendingSequence(this.plan, this.originalHotbarSlot, this.fluidInventorySlot, this.fluidBucketItem, this.fluidAccessMode, this.stage, this.delayTicks, this.placeRetriesLeft, nextFluidPlaceAttemptsLeft, this.fluidPickupAttemptsLeft, this.temporarySwapActive, this.stabilityWaitedTicks);
        }

        private PendingSequence withFluidPickupAttemptsLeft(int nextFluidPickupAttemptsLeft) {
            return new PendingSequence(this.plan, this.originalHotbarSlot, this.fluidInventorySlot, this.fluidBucketItem, this.fluidAccessMode, this.stage, this.delayTicks, this.placeRetriesLeft, this.fluidPlaceAttemptsLeft, nextFluidPickupAttemptsLeft, this.temporarySwapActive, this.stabilityWaitedTicks);
        }

        private PendingSequence withTemporarySwapActive(boolean nextTemporarySwapActive) {
            return new PendingSequence(this.plan, this.originalHotbarSlot, this.fluidInventorySlot, this.fluidBucketItem, this.fluidAccessMode, this.stage, this.delayTicks, this.placeRetriesLeft, this.fluidPlaceAttemptsLeft, this.fluidPickupAttemptsLeft, nextTemporarySwapActive, this.stabilityWaitedTicks);
        }

        private PendingSequence withStabilityWaitedTicks(int ticks) {
            return new PendingSequence(this.plan, this.originalHotbarSlot, this.fluidInventorySlot, this.fluidBucketItem, this.fluidAccessMode, this.stage, this.delayTicks, this.placeRetriesLeft, this.fluidPlaceAttemptsLeft, this.fluidPickupAttemptsLeft, this.temporarySwapActive, ticks);
        }
    }

    @Environment(value=EnvType.CLIENT)
    private record LeverHoldState(class_2338 basePos, int delayTicks) {
        private LeverHoldState withDelay(int nextDelayTicks) {
            return new LeverHoldState(this.basePos, nextDelayTicks);
        }
    }

    @Environment(value=EnvType.CLIENT)
    private record PlacementPlan(class_2338 targetPos, class_3965 fluidPlaceHit, class_3965 fluidPickupHit, class_3965 placementHit, class_1792 placementItem, class_2248 expectedBlock, boolean refreshExistingTarget, boolean requiresSneak) {
        private boolean matchesHeldStack(class_1799 stack) {
            return stack.method_31574(this.placementItem);
        }

        private boolean isPlacementSatisfied(class_2680 state) {
            return state.method_27852(this.expectedBlock);
        }
    }

    @Environment(value=EnvType.CLIENT)
    private record FluidSource(int inventorySlot, class_1792 bucketItem) {
    }

    @Environment(value=EnvType.CLIENT)
    private static enum FluidAccessMode {
        HOTBAR,
        SWAP_WITH_ORIGINAL_SLOT;

    }

    @Environment(value=EnvType.CLIENT)
    private static enum SequenceStage {
        SELECT_FLUID,
        USE_FLUID,
        WAIT_FOR_EMPTY_BUCKET,
        PICKUP_FLUID,
        WAIT_FOR_FILLED_BUCKET,
        RESTORE_PLACEMENT_ITEM,
        PLACE_BLOCK;

    }

    @Environment(value=EnvType.CLIENT)
    private record LeverHoldPlan(class_2338 basePos) {
    }
}

