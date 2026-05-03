package pl.durex.client.module;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeverBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.*;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.block.BlockState;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;

/**
 * Moduł automatyzacji Lever + Cobweb
 * Automatycznie wykonuje sekwencję: płyn → blok → zabranie płynu
 */
public class LeverCobwebModule {
    
    public LeverCobwebModule() {
    }
    
    private static PendingSequence pendingSequence = null;
    private static LeverHoldState leverHoldState = null;
    private static boolean internalBlockInteraction = false;
    private static boolean leverSneakSpoofed = false;
    
    // Konfiguracja podstawowa
    private static final int PLACE_RETRIES = 3;
    private static final int FLUID_PLACE_ATTEMPTS = 4;
    private static final int FLUID_PICKUP_ATTEMPTS = 8;
    private static final int MAX_STABILITY_WAIT_TICKS = 2;
    
    // Opcje modułu (jak w oryginalnym NemosHelper)
    private static boolean enabled = false;
    private static boolean holdLeverEnabled = true;
    private static boolean switchToBestSwordEnabled = true;
    private static boolean playerModeEnabled = true;        // "Stawianie Przez Gracza"
    private static boolean webModeEnabled = true;           // "Weby" 
    private static boolean leverOnlyModeEnabled = false;    // "Auto Lever"

    
    // Profile prędkości (jak w oryginalnym)
    private static SpeedProfile speedProfile = SpeedProfile.FAST;
    
    public enum SpeedProfile {
        SLOW("Slow", 4, 3, 2, 1),
        NORMAL("Normal", 2, 2, 1, 2), 
        FAST("Fast", 1, 1, 1, 2),
        INSTANT("Instant", 0, 0, 0, 3),
        ERROR_KILLER("Error Killer", 0, 0, 0, 4);
        
        public final String name;
        public final int retryDelayTicks;
        public final int settleDelayTicks;
        public final int postPickupDelayTicks;
        public final int stagesPerTick;
        
        SpeedProfile(String name, int retryDelay, int settleDelay, int postPickupDelay, int stagesPerTick) {
            this.name = name;
            this.retryDelayTicks = retryDelay;
            this.settleDelayTicks = settleDelay;
            this.postPickupDelayTicks = postPickupDelay;
            this.stagesPerTick = stagesPerTick;
        }
    }
    
    public static boolean isEnabled() { return enabled; }
    public static void setEnabled(boolean enabled) { LeverCobwebModule.enabled = enabled; }
    
    public static boolean isHoldLeverEnabled() { return holdLeverEnabled; }
    public static void setHoldLeverEnabled(boolean enabled) { LeverCobwebModule.holdLeverEnabled = enabled; }
    
    public static boolean isSwitchToBestSwordEnabled() { return switchToBestSwordEnabled; }
    public static void setSwitchToBestSwordEnabled(boolean enabled) { LeverCobwebModule.switchToBestSwordEnabled = enabled; }
    
    public static boolean isPlayerModeEnabled() { return playerModeEnabled; }
    public static void setPlayerModeEnabled(boolean enabled) { LeverCobwebModule.playerModeEnabled = enabled; }
    
    public static boolean isWebModeEnabled() { return webModeEnabled; }
    public static void setWebModeEnabled(boolean enabled) { LeverCobwebModule.webModeEnabled = enabled; }
    
    public static boolean isLeverOnlyModeEnabled() { return leverOnlyModeEnabled; }
    public static void setLeverOnlyModeEnabled(boolean enabled) { LeverCobwebModule.leverOnlyModeEnabled = enabled; }

    public static void setInternalBlockInteraction(boolean val) { internalBlockInteraction = val; }
    
    public static SpeedProfile getSpeedProfile() { return speedProfile; }
    public static void setSpeedProfile(SpeedProfile profile) { LeverCobwebModule.speedProfile = profile; }
    
    /**
     * Główna metoda obsługująca interakcję z blokami
     */
    public static boolean handleInteractBlock(ClientPlayerInteractionManager interactionManager, 
                                            ClientPlayerEntity player, Hand hand, BlockHitResult hitResult) {
        if (!enabled || internalBlockInteraction || hand != Hand.MAIN_HAND || 
            interactionManager == null || player == null || hitResult == null) {
            return false;
        }
        
        if (pendingSequence != null || leverHoldState != null) {
            return true;
        }
        
        MinecraftClient client = MinecraftClient.getInstance();
        // Dla leverOnlyMode sneak jest spoofiowany przez mixin — nie blokuj
        boolean sneakBlock = player.isSneaking() && !leverOnlyModeEnabled;
        if (client.world == null || client.player != player || sneakBlock || client.currentScreen != null) {
            return false;
        }
        
        ItemStack heldStack = player.getMainHandStack();
        ItemStack offhandStack = player.getOffHandStack();
        Item heldItem = heldStack.getItem();
        
        // Sprawdź czy trzyma blok w głównej ręce (tylko jeśli Player Mode włączony)
        if (heldItem instanceof BlockItem && playerModeEnabled) {
            BlockItem blockItem = (BlockItem) heldItem;
            PlacementPlan plan = resolvePlan(client, hitResult, heldItem, blockItem, player.isSneaking());
            if (plan != null) {
                FluidSource fluidSource = findPreferredFluidSource(player.getInventory());
                if (fluidSource == null) {
                    return false;
                }
                
                int originalHotbarSlot = player.getInventory().selectedSlot;
                FluidAccessMode accessMode = fluidSource.inventorySlot() < 9 ? 
                    FluidAccessMode.HOTBAR : FluidAccessMode.SWAP_WITH_ORIGINAL_SLOT;
                
                pendingSequence = new PendingSequence(
                    plan, originalHotbarSlot, fluidSource.inventorySlot(), 
                    fluidSource.bucketItem(), accessMode, SequenceStage.SELECT_FLUID, 
                    0, PLACE_RETRIES, FLUID_PLACE_ATTEMPTS, FLUID_PICKUP_ATTEMPTS, 
                    false, 0
                );
                return true;
            }
        }
        
        // Sprawdź lever hold (dźwignia w offhand) - tylko jeśli Hold Lever włączony i Auto Lever wyłączony
        if (offhandStack.isOf(Items.LEVER) && holdLeverEnabled && !leverOnlyModeEnabled) {
            LeverHoldPlan leverHoldPlan = resolveLeverHoldPlan(client, hitResult);
            if (leverHoldPlan != null) {
                leverHoldState = new LeverHoldState(leverHoldPlan.basePos(), 0);
                return true;
            }
        }
        
        // Tryb "Auto Lever" - stawia dźwignię z offhand na klikniętym bloku bez shifta.
        // Sneak jest już spoofiowany przez LeverCobwebMixin przed wywołaniem tej metody.
        if (leverOnlyModeEnabled && offhandStack.isOf(Items.LEVER)) {
            // Postaw dźwignię z offhand na klikniętym bloku
            try {
                internalBlockInteraction = true;
                ActionResult result = interactionManager.interactBlock(player, Hand.OFF_HAND, hitResult);
                if (result.isAccepted()) {
                    player.swingHand(Hand.OFF_HAND);
                }
            } finally {
                internalBlockInteraction = false;
            }
            return true;
        }
        
        return false;
    }
    
    /**
     * Tick główny - wywoływany co tick
     */
    public static void clientTick(MinecraftClient client) {
        handleLeverHoldTick(client);
        
        if (pendingSequence == null) {
            return;
        }
        
        if (client == null || client.player == null || client.world == null || 
            client.interactionManager == null) {
            pendingSequence = null;
            return;
        }
        
        // Wykonaj maksymalnie X etapów na tick (zależy od profilu prędkości)
        int stagesLeftThisTick = getStagesPerTick();
        while (pendingSequence != null && stagesLeftThisTick-- > 0) {
            PendingSequence sequence = pendingSequence;
            
            if (sequence.delayTicks() > 0) {
                pendingSequence = sequence.withDelay(sequence.delayTicks() - 1);
                return;
            }
            
            if (shouldWaitForStableUse(client, sequence)) {
                pendingSequence = sequence.withDelay(1).withStabilityWaitedTicks(sequence.stabilityWaitedTicks() + 1);
                return;
            }
            
            switch (sequence.stage()) {
                case SELECT_FLUID -> handleSelectFluid(client, sequence);
                case USE_FLUID -> handleUseFluid(client, sequence);
                case WAIT_FOR_EMPTY_BUCKET -> handleWaitForEmptyBucket(client, sequence);
                case PICKUP_FLUID -> handlePickupFluid(client, sequence);
                case WAIT_FOR_FILLED_BUCKET -> handleWaitForFilledBucket(client, sequence);
                case RESTORE_PLACEMENT_ITEM -> handleRestorePlacementItem(client, sequence);
                case PLACE_BLOCK -> handlePlaceBlock(client, sequence);
            }
        }
    }
    
    /**
     * Obsługa disconnectu
     */
    public static void handleDisconnect() {
        pendingSequence = null;
        leverHoldState = null;
        leverSneakSpoofed = false;
    }
    
    // === ETAPY SEKWENCJI ===
    
    private static void handleSelectFluid(MinecraftClient client, PendingSequence sequence) {
        boolean swapActive = sequence.temporarySwapActive();
        
        if (sequence.fluidAccessMode() == FluidAccessMode.HOTBAR) {
            selectHotbarSlot(client, sequence.fluidInventorySlot());
        } else {
            swapInventorySlotIntoHotbar(client, sequence.fluidInventorySlot(), sequence.originalHotbarSlot());
            selectHotbarSlot(client, sequence.originalHotbarSlot());
            swapActive = true;
        }
        
        pendingSequence = sequence.withTemporarySwapActive(swapActive)
            .advanceTo(SequenceStage.USE_FLUID, configuredRetryDelayTicks());
    }
    
    private static void handleUseFluid(MinecraftClient client, PendingSequence sequence) {
        if (!client.player.getMainHandStack().isOf(sequence.fluidBucketItem())) {
            if (!reselectFluidForSequence(client, sequence)) {
                finishSequence(client, sequence);
                return;
            }
            // Error Killer pomija delay przy reselect
            int delay = speedProfile == SpeedProfile.ERROR_KILLER ? 0 : 1;
            pendingSequence = sequence.advanceTo(SequenceStage.USE_FLUID, delay);
            return;
        }
        
        ActionResult result = interactStoredHit(client, sequence.plan().fluidPlaceHit(), sequence.plan().requiresSneak());
        if (!result.isAccepted()) {
            result = client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
        }
        
        if (result.isAccepted()) {
            client.player.swingHand(Hand.MAIN_HAND);
        }
        
        pendingSequence = sequence.withFluidPlaceAttemptsLeft(sequence.fluidPlaceAttemptsLeft() - 1)
            .advanceTo(SequenceStage.WAIT_FOR_EMPTY_BUCKET, configuredSettleDelayTicks());
    }
    
    private static void handleWaitForEmptyBucket(MinecraftClient client, PendingSequence sequence) {
        if (client.player.getMainHandStack().isOf(Items.BUCKET)) {
            pendingSequence = sequence.advanceTo(SequenceStage.PICKUP_FLUID, configuredSettleDelayTicks());
            return;
        }
        
        if (sequence.fluidPlaceAttemptsLeft() <= 0) {
            finishSequence(client, sequence);
            return;
        }
        
        pendingSequence = sequence.advanceTo(SequenceStage.USE_FLUID, 1);
    }
    
    private static void handlePickupFluid(MinecraftClient client, PendingSequence sequence) {
        if (client.player.getMainHandStack().isOf(sequence.fluidBucketItem())) {
            pendingSequence = sequence.advanceTo(SequenceStage.RESTORE_PLACEMENT_ITEM, configuredPostPickupDelayTicks());
            return;
        }
        
        if (!client.player.getMainHandStack().isOf(Items.BUCKET)) {
            if (!reselectFluidForSequence(client, sequence)) {
                finishSequence(client, sequence);
                return;
            }
            // Error Killer pomija delay przy reselect
            int delay = speedProfile == SpeedProfile.ERROR_KILLER ? 0 : 1;
            pendingSequence = sequence.advanceTo(SequenceStage.PICKUP_FLUID, delay);
            return;
        }
        
        ActionResult result = interactStoredHit(client, sequence.plan().fluidPickupHit(), sequence.plan().requiresSneak());
        if (!result.isAccepted()) {
            result = client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
        }
        
        if (result.isAccepted()) {
            client.player.swingHand(Hand.MAIN_HAND);
        }
        
        pendingSequence = sequence.withFluidPickupAttemptsLeft(sequence.fluidPickupAttemptsLeft() - 1)
            .advanceTo(SequenceStage.WAIT_FOR_FILLED_BUCKET, configuredSettleDelayTicks());
    }
    
    private static void handleWaitForFilledBucket(MinecraftClient client, PendingSequence sequence) {
        if (client.player.getMainHandStack().isOf(sequence.fluidBucketItem())) {
            pendingSequence = sequence.advanceTo(SequenceStage.RESTORE_PLACEMENT_ITEM, configuredPostPickupDelayTicks());
            return;
        }
        
        if (sequence.fluidPickupAttemptsLeft() <= 0) {
            finishSequence(client, sequence);
            return;
        }
        
        pendingSequence = sequence.advanceTo(SequenceStage.PICKUP_FLUID, 1);
    }
    
    private static void handleRestorePlacementItem(MinecraftClient client, PendingSequence sequence) {
        PendingSequence updatedSequence = sequence;
        
        if (sequence.fluidAccessMode() == FluidAccessMode.SWAP_WITH_ORIGINAL_SLOT && sequence.temporarySwapActive()) {
            swapInventorySlotIntoHotbar(client, sequence.fluidInventorySlot(), sequence.originalHotbarSlot());
            updatedSequence = sequence.withTemporarySwapActive(false);
        }
        
        selectHotbarSlot(client, sequence.originalHotbarSlot());
        pendingSequence = updatedSequence.advanceTo(SequenceStage.PLACE_BLOCK, configuredPostPickupDelayTicks());
    }
    
    private static void handlePlaceBlock(MinecraftClient client, PendingSequence sequence) {
        if (!sequence.plan().matchesHeldStack(client.player.getMainHandStack())) {
            if (!reselectPlacementItem(client, sequence)) {
                finishSequence(client, sequence);
                return;
            }
            // Error Killer pomija delay przy reselect
            int delay = speedProfile == SpeedProfile.ERROR_KILLER ? 0 : 1;
            pendingSequence = sequence.withDelay(delay);
            return;
        }
        
        BlockState currentState = client.world.getBlockState(sequence.plan().targetPos());
        if (sequence.plan().isPlacementSatisfied(currentState) && !sequence.plan().refreshExistingTarget()) {
            finishSequence(client, sequence);
            return;
        }
        
        if (!client.world.getFluidState(sequence.plan().targetPos()).isEmpty()) {
            if (sequence.placeRetriesLeft() <= 1) {
                finishSequence(client, sequence);
                return;
            }
            // Error Killer ma mniejszy delay przy retry
            int delay = speedProfile == SpeedProfile.ERROR_KILLER ? 0 : Math.max(1, configuredSettleDelayTicks());
            pendingSequence = sequence.withPlaceRetriesLeft(sequence.placeRetriesLeft() - 1)
                .withDelay(delay);
            return;
        }
        
        if (sequence.plan().isPlacementSatisfied(currentState)) {
            if (sequence.placeRetriesLeft() <= 1) {
                finishSequence(client, sequence);
                return;
            }
            pendingSequence = sequence.withPlaceRetriesLeft(sequence.placeRetriesLeft() - 1)
                .withDelay(configuredRetryDelayTicks());
            return;
        }
        
        ActionResult result = interactStoredHit(client, sequence.plan().placementHit(), sequence.plan().requiresSneak());
        if (result.isAccepted()) {
            client.player.swingHand(Hand.MAIN_HAND);
        }
        
        if (sequence.plan().isPlacementSatisfied(client.world.getBlockState(sequence.plan().targetPos())) || 
            sequence.placeRetriesLeft() <= 1) {
            finishSequence(client, sequence);
            return;
        }
        
        pendingSequence = sequence.withPlaceRetriesLeft(sequence.placeRetriesLeft() - 1)
            .withDelay(configuredRetryDelayTicks());
    }
    
    private static void finishSequence(MinecraftClient client, PendingSequence sequence) {
        if (sequence.fluidAccessMode() == FluidAccessMode.SWAP_WITH_ORIGINAL_SLOT && sequence.temporarySwapActive()) {
            swapInventorySlotIntoHotbar(client, sequence.fluidInventorySlot(), sequence.originalHotbarSlot());
        }
        
        if (switchToBestSwordEnabled) {
            if (!selectBestSword(client, sequence.originalHotbarSlot())) {
                selectHotbarSlot(client, sequence.originalHotbarSlot());
            }
        } else {
            selectHotbarSlot(client, sequence.originalHotbarSlot());
        }
        
        pendingSequence = null;
    }
    
    // === LEVER HOLD LOGIC ===
    
    private static void handleLeverHoldTick(MinecraftClient client) {
        if (leverHoldState == null) {
            return;
        }
        
        if (client == null || client.player == null || client.world == null || 
            client.interactionManager == null) {
            leverHoldState = null;
            leverSneakSpoofed = false;
            return;
        }
        
        if (pendingSequence != null || (!holdLeverEnabled && !leverOnlyModeEnabled) || 
            client.currentScreen != null || client.player.isSneaking() || 
            !client.player.getOffHandStack().isOf(Items.LEVER) || 
            !client.options.useKey.isPressed()) {
            stopLeverHold(client);
            return;
        }
        
        if (leverHoldState.delayTicks() > 0) {
            leverHoldState = leverHoldState.withDelay(leverHoldState.delayTicks() - 1);
            return;
        }
        
        if (!(client.crosshairTarget instanceof BlockHitResult blockHitResult)) {
            stopLeverHold(client);
            return;
        }
        
        LeverHoldPlan plan = resolveLeverHoldPlan(client, blockHitResult);
        if (plan == null) {
            stopLeverHold(client);
            return;
        }
        
        leverHoldState = new LeverHoldState(plan.basePos(), 0);
        
        boolean shouldSneak = client.world.getBlockState(blockHitResult.getBlockPos()).getBlock() instanceof LeverBlock;
        if (shouldSneak) {
            ensureLeverSneak(client);
        } else {
            stopLeverHoldSneak(client);
        }
        
        try {
            internalBlockInteraction = true;
            ActionResult result = client.interactionManager.interactBlock(client.player, Hand.OFF_HAND, blockHitResult);
            if (result.isAccepted()) {
                client.player.swingHand(Hand.OFF_HAND);
            }
        } finally {
            internalBlockInteraction = false;
        }
    }
    
    // === HELPER METHODS ===
    
    private static PlacementPlan resolvePlan(MinecraftClient client, BlockHitResult hitResult, 
                                           Item heldItem, BlockItem heldBlockItem, boolean requiresSneak) {
        BlockPos clickedPos = hitResult.getBlockPos();
        BlockState clickedState = client.world.getBlockState(clickedPos);
        BlockPos directTargetPos = clickedPos.offset(hitResult.getSide());
        BlockState directTargetState = client.world.getBlockState(directTargetPos);
        
        // Sprawdź czy kliknięto na dźwignię
        if (clickedState.getBlock() instanceof LeverBlock) {
            BlockHitResult supportHit = createSupportHitForLever(client, hitResult);
            return createLeverPlacementPlan(client, supportHit, heldItem, heldBlockItem.getBlock(), requiresSneak);
        }
        
        // Sprawdź czy kliknięto na pajęczynę (automatyczna dźwignia)
        if (clickedState.isOf(Blocks.COBWEB)) {
            BlockHitResult leverSupportHit = resolveLeverAutomationHit(client, hitResult);
            if (leverSupportHit != null) {
                PlacementPlan leverPlan = createLeverPlacementPlan(client, leverSupportHit, heldItem, heldBlockItem.getBlock(), true);
                if (leverPlan != null) {
                    return leverPlan;
                }
            }
            return null;
        }
        
        // Sprawdź czy można postawić blok w docelowej pozycji
        if (directTargetState.isAir() || directTargetState.isLiquid() || directTargetState.isOf(Blocks.COBWEB)) {
            return null;
        }
        
        // Sprawdź czy w docelowej pozycji jest dźwignia
        if (directTargetState.getBlock() instanceof LeverBlock) {
            return createLeverPlacementPlan(client, createPlacementHit(clickedPos, hitResult.getSide()), 
                heldItem, heldBlockItem.getBlock(), requiresSneak);
        }
        
        // Sprawdź czy próbuje zastąpić ten sam blok
        if (directTargetState.isOf(heldBlockItem.getBlock()) && heldBlockItem.getBlock() != Blocks.COBWEB) {
            return null;
        }
        
        return null;
    }
    
    private static PlacementPlan createLeverPlacementPlan(MinecraftClient client, BlockHitResult supportHit, 
                                                        Item heldItem, Block expectedBlock, boolean requiresSneak) {
        if (client == null || client.world == null || supportHit == null) {
            return null;
        }
        
        BlockPos targetPos = supportHit.getBlockPos().offset(supportHit.getSide());
        BlockState targetState = client.world.getBlockState(targetPos);
        
        if (!(targetState.getBlock() instanceof LeverBlock)) {
            return null;
        }
        
        // Dla pajęczyny z dźwignią - płyn powinien być w pozycji supportHit (pajęczyny), nie targetPos (dźwigni)
        BlockPos fluidPos = supportHit.getBlockPos();
        BlockHitResult fluidPlaceHit = createFluidPickupHit(fluidPos);
        BlockHitResult fluidPickupHit = createFluidPickupHit(fluidPos);
        
        return new PlacementPlan(targetPos, fluidPlaceHit, fluidPickupHit, supportHit, 
            heldItem, expectedBlock, false, requiresSneak);
    }
    
    private static LeverHoldPlan resolveLeverHoldPlan(MinecraftClient client, BlockHitResult hitResult) {
        BlockPos clickedPos = hitResult.getBlockPos();
        BlockState clickedState = client.world.getBlockState(clickedPos);
        
        if (clickedState.getBlock() instanceof LeverBlock) {
            BlockPos baseBelowLever = clickedPos.down();
            if (isValidLeverHoldBase(client, baseBelowLever)) {
                return new LeverHoldPlan(baseBelowLever);
            }
        }
        
        if (isValidLeverHoldBase(client, clickedPos)) {
            return new LeverHoldPlan(clickedPos);
        }
        
        return null;
    }
    
    private static boolean isValidLeverHoldBase(MinecraftClient client, BlockPos basePos) {
        if (!hasLeverSupport(client.world.getBlockState(basePos))) {
            return false;
        }
        
        if (!hasLeverSupport(client.world.getBlockState(basePos.up(2)))) {
            return false;
        }
        
        BlockState leverSlotState = client.world.getBlockState(basePos.up());
        if (!(leverSlotState.isAir() || leverSlotState.isLiquid() || 
              leverSlotState.getBlock() instanceof LeverBlock)) {
            return false;
        }
        
        // Sprawdź czy jest wsparcie z boku
        for (Direction direction : Direction.Type.HORIZONTAL) {
            if (hasLeverSupport(client.world.getBlockState(basePos.offset(direction).up()))) {
                return true;
            }
        }
        
        return false;
    }
    
    private static boolean hasLeverSupport(BlockState state) {
        return !state.isAir() && !state.isLiquid();
    }
    
    private static BlockHitResult createSupportHitForLever(MinecraftClient client, BlockHitResult leverHit) {
        BlockPos leverPos = leverHit.getBlockPos();
        Direction supportFace = leverHit.getSide().getOpposite();
        BlockPos supportPos = leverPos.offset(supportFace);
        
        if (hasLeverSupport(client.world.getBlockState(supportPos))) {
            return createPlacementHit(supportPos, supportFace);
        }
        
        // Fallback - znajdź inne wsparcie
        for (Direction direction : Direction.values()) {
            BlockPos fallbackSupportPos = leverPos.offset(direction);
            if (hasLeverSupport(client.world.getBlockState(fallbackSupportPos))) {
                return createPlacementHit(fallbackSupportPos, direction.getOpposite());
            }
        }
        
        return null;
    }
    
    private static BlockHitResult resolveLeverAutomationHit(MinecraftClient client, BlockHitResult hitResult) {
        if (client == null || client.world == null || hitResult == null) {
            return null;
        }
        
        BlockPos clickedPos = hitResult.getBlockPos();
        BlockState clickedState = client.world.getBlockState(clickedPos);
        
        if (clickedState.getBlock() instanceof LeverBlock) {
            return createSupportHitForLever(client, hitResult);
        }
        
        Direction preferredSide = hitResult.getSide();
        BlockPos targetPos = clickedPos.offset(preferredSide);
        
        // Sprawdź TYLKO bezpośrednio za pajęczyną w kierunku kliknięcia
        if (client.world.getBlockState(targetPos).getBlock() instanceof LeverBlock) {
            return createPlacementHit(clickedPos, preferredSide);
        }
        
        // Nie szukaj w innych miejscach - tylko bezpośrednio za pajęczyną
        return null;
    }
    
    private static BlockHitResult findAdjacentLeverSupportHit(MinecraftClient client, BlockPos supportPos, Direction preferredSide) {
        if (!hasLeverSupport(client.world.getBlockState(supportPos))) {
            return null;
        }
        
        if (preferredSide != null && client.world.getBlockState(supportPos.offset(preferredSide)).getBlock() instanceof LeverBlock) {
            return createPlacementHit(supportPos, preferredSide);
        }
        
        for (Direction direction : Direction.values()) {
            if (direction == preferredSide) continue;
            if (client.world.getBlockState(supportPos.offset(direction)).getBlock() instanceof LeverBlock) {
                return createPlacementHit(supportPos, direction);
            }
        }
        
        return null;
    }
    
    private static BlockHitResult createPlacementHit(BlockPos supportPos, Direction supportFace) {
        Vec3d hitPos = Vec3d.ofCenter(supportPos).add(
            supportFace.getOffsetX() * 0.5,
            supportFace.getOffsetY() * 0.5, 
            supportFace.getOffsetZ() * 0.5
        );
        return new BlockHitResult(hitPos, supportFace, supportPos, false);
    }
    
    private static BlockHitResult createFluidPickupHit(BlockPos targetPos) {
        return new BlockHitResult(Vec3d.ofCenter(targetPos), Direction.UP, targetPos, false);
    }
    
    private static boolean shouldWaitForStableUse(MinecraftClient client, PendingSequence sequence) {
        if (client == null || client.player == null) {
            return false;
        }
        
        if (!isInteractionStage(sequence.stage())) {
            return false;
        }
        
        // Error Killer pomija sprawdzenia stabilności dla maksymalnej prędkości
        if (speedProfile == SpeedProfile.ERROR_KILLER) {
            return false;
        }
        
        if (sequence.stabilityWaitedTicks() >= MAX_STABILITY_WAIT_TICKS) {
            return false;
        }
        
        Vec3d velocity = client.player.getVelocity();
        double horizontalSpeedSq = velocity.x * velocity.x + velocity.z * velocity.z;
        
        if (isPlayerInCobweb(client)) {
            return Math.abs(velocity.y) > 0.3 || horizontalSpeedSq > 0.18;
        }
        
        boolean airborne = !client.player.isOnGround();
        boolean verticalUnstable = Math.abs(velocity.y) > 0.28;
        boolean horizontalUnstable = horizontalSpeedSq > 0.16;
        
        return airborne && (verticalUnstable || horizontalUnstable);
    }
    
    private static boolean isPlayerInCobweb(MinecraftClient client) {
        if (client.world == null) {
            return false;
        }
        
        BlockPos feet = client.player.getBlockPos();
        if (client.world.getBlockState(feet).isOf(Blocks.COBWEB)) {
            return true;
        }
        
        BlockPos eye = BlockPos.ofFloored(client.player.getEyePos());
        return client.world.getBlockState(eye).isOf(Blocks.COBWEB);
    }
    
    private static boolean isInteractionStage(SequenceStage stage) {
        return stage == SequenceStage.PLACE_BLOCK;
    }
    
    // === INVENTORY MANAGEMENT ===
    
    private static FluidSource findPreferredFluidSource(PlayerInventory inventory) {
        int waterSlot = findInventorySlot(inventory, Items.WATER_BUCKET);
        if (waterSlot >= 0) {
            return new FluidSource(waterSlot, Items.WATER_BUCKET);
        }
        
        int lavaSlot = findInventorySlot(inventory, Items.LAVA_BUCKET);
        if (lavaSlot >= 0) {
            return new FluidSource(lavaSlot, Items.LAVA_BUCKET);
        }
        
        return null;
    }
    
    private static int findInventorySlot(PlayerInventory inventory, Item item) {
        for (int slot = 0; slot < 36; slot++) {
            if (inventory.getStack(slot).isOf(item)) {
                return slot;
            }
        }
        return -1;
    }
    
    private static boolean selectBestSword(MinecraftClient client, int fallbackHotbarSlot) {
        PlayerInventory inventory = client.player.getInventory();
        int bestSwordSlot = -1;
        float bestSwordScore = Float.NEGATIVE_INFINITY;
        
        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = inventory.getStack(slot);
            float swordScore = swordScore(stack);
            if (swordScore > bestSwordScore) {
                bestSwordScore = swordScore;
                bestSwordSlot = slot;
            }
        }
        
        if (bestSwordSlot < 0) {
            return false;
        }
        
        if (bestSwordSlot < 9) {
            selectHotbarSlot(client, bestSwordSlot);
            return true;
        }
        
        swapInventorySlotIntoHotbar(client, bestSwordSlot, fallbackHotbarSlot);
        selectHotbarSlot(client, fallbackHotbarSlot);
        return true;
    }
    
    private static float swordScore(ItemStack stack) {
        if (stack.isOf(Items.NETHERITE_SWORD)) return 7.0f;
        if (stack.isOf(Items.DIAMOND_SWORD)) return 6.0f;
        if (stack.isOf(Items.IRON_SWORD)) return 5.0f;
        if (stack.isOf(Items.GOLDEN_SWORD)) return 4.0f;
        if (stack.isOf(Items.STONE_SWORD)) return 3.0f;
        if (stack.isOf(Items.WOODEN_SWORD)) return 2.0f;
        return Float.NEGATIVE_INFINITY;
    }
    
    private static void selectHotbarSlot(MinecraftClient client, int hotbarSlot) {
        if (!PlayerInventory.isValidHotbarIndex(hotbarSlot)) {
            return;
        }
        
        client.player.getInventory().selectedSlot = hotbarSlot;
        if (client.getNetworkHandler() != null) {
            client.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(hotbarSlot));
        }
    }
    
    private static void swapInventorySlotIntoHotbar(MinecraftClient client, int inventorySlot, int hotbarSlot) {
        int screenSlot = inventoryIndexToScreenSlot(inventorySlot);
        if (screenSlot < 0 || !PlayerInventory.isValidHotbarIndex(hotbarSlot)) {
            return;
        }
        
        client.interactionManager.clickSlot(
            client.player.playerScreenHandler.syncId,
            screenSlot, hotbarSlot, 
            net.minecraft.screen.slot.SlotActionType.SWAP,
            client.player
        );
    }
    
    private static int inventoryIndexToScreenSlot(int inventorySlot) {
        if (inventorySlot >= 0 && inventorySlot < 9) {
            return 36 + inventorySlot; // Hotbar
        }
        if (inventorySlot >= 9 && inventorySlot < 36) {
            return inventorySlot; // Main inventory
        }
        return -1;
    }
    
    // === SNEAK MANAGEMENT ===
    
    private static void ensureLeverSneak(MinecraftClient client) {
        if (leverSneakSpoofed || client.player.isSneaking()) {
            return;
        }
        
        client.player.setSneaking(true);
        leverSneakSpoofed = true;
    }
    
    private static void stopLeverHoldSneak(MinecraftClient client) {
        if (!leverSneakSpoofed || client == null || client.player == null) {
            leverSneakSpoofed = false;
            return;
        }
        
        client.player.setSneaking(false);
        leverSneakSpoofed = false;
    }
    
    private static void stopLeverHold(MinecraftClient client) {
        leverHoldState = null;
        stopLeverHoldSneak(client);
    }
    
    // === INTERACTION ===
    
    private static ActionResult interactStoredHit(MinecraftClient client, BlockHitResult hitResult, boolean forceSneak) {
        if (client == null || client.player == null || client.interactionManager == null || hitResult == null) {
            return ActionResult.PASS;
        }
        
        boolean spoofSneak = forceSneak && !client.player.isSneaking();
        if (spoofSneak && client.getNetworkHandler() != null) {
            client.player.setSneaking(true);
        }
        
        try {
            internalBlockInteraction = true;
            return client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, hitResult);
        } finally {
            internalBlockInteraction = false;
            if (spoofSneak && client.getNetworkHandler() != null) {
                client.player.setSneaking(false);
            }
        }
    }
    
    // === RESELECTION HELPERS ===
    
    private static boolean reselectFluidForSequence(MinecraftClient client, PendingSequence sequence) {
        if (sequence.fluidAccessMode() == FluidAccessMode.HOTBAR) {
            if (!PlayerInventory.isValidHotbarIndex(sequence.fluidInventorySlot())) {
                return false;
            }
            selectHotbarSlot(client, sequence.fluidInventorySlot());
            return true;
        }
        
        if (!sequence.temporarySwapActive()) {
            swapInventorySlotIntoHotbar(client, sequence.fluidInventorySlot(), sequence.originalHotbarSlot());
        }
        selectHotbarSlot(client, sequence.originalHotbarSlot());
        return true;
    }
    
    private static boolean reselectPlacementItem(MinecraftClient client, PendingSequence sequence) {
        if (client == null || client.player == null) {
            return false;
        }
        
        ItemStack originalStack = client.player.getInventory().getStack(sequence.originalHotbarSlot());
        if (!sequence.plan().matchesHeldStack(originalStack)) {
            return false;
        }
        
        selectHotbarSlot(client, sequence.originalHotbarSlot());
        return true;
    }
    
    // === RECORD CLASSES ===
    
    private record PendingSequence(
        PlacementPlan plan,
        int originalHotbarSlot,
        int fluidInventorySlot,
        Item fluidBucketItem,
        FluidAccessMode fluidAccessMode,
        SequenceStage stage,
        int delayTicks,
        int placeRetriesLeft,
        int fluidPlaceAttemptsLeft,
        int fluidPickupAttemptsLeft,
        boolean temporarySwapActive,
        int stabilityWaitedTicks
    ) {
        private PendingSequence advanceTo(SequenceStage nextStage, int nextDelayTicks) {
            return new PendingSequence(plan, originalHotbarSlot, fluidInventorySlot, fluidBucketItem,
                fluidAccessMode, nextStage, nextDelayTicks, placeRetriesLeft, fluidPlaceAttemptsLeft,
                fluidPickupAttemptsLeft, temporarySwapActive, 0);
        }
        
        private PendingSequence withDelay(int nextDelayTicks) {
            return new PendingSequence(plan, originalHotbarSlot, fluidInventorySlot, fluidBucketItem,
                fluidAccessMode, stage, nextDelayTicks, placeRetriesLeft, fluidPlaceAttemptsLeft,
                fluidPickupAttemptsLeft, temporarySwapActive, stabilityWaitedTicks);
        }
        
        private PendingSequence withPlaceRetriesLeft(int nextPlaceRetriesLeft) {
            return new PendingSequence(plan, originalHotbarSlot, fluidInventorySlot, fluidBucketItem,
                fluidAccessMode, stage, delayTicks, nextPlaceRetriesLeft, fluidPlaceAttemptsLeft,
                fluidPickupAttemptsLeft, temporarySwapActive, stabilityWaitedTicks);
        }
        
        private PendingSequence withFluidPlaceAttemptsLeft(int nextFluidPlaceAttemptsLeft) {
            return new PendingSequence(plan, originalHotbarSlot, fluidInventorySlot, fluidBucketItem,
                fluidAccessMode, stage, delayTicks, placeRetriesLeft, nextFluidPlaceAttemptsLeft,
                fluidPickupAttemptsLeft, temporarySwapActive, stabilityWaitedTicks);
        }
        
        private PendingSequence withFluidPickupAttemptsLeft(int nextFluidPickupAttemptsLeft) {
            return new PendingSequence(plan, originalHotbarSlot, fluidInventorySlot, fluidBucketItem,
                fluidAccessMode, stage, delayTicks, placeRetriesLeft, fluidPlaceAttemptsLeft,
                nextFluidPickupAttemptsLeft, temporarySwapActive, stabilityWaitedTicks);
        }
        
        private PendingSequence withTemporarySwapActive(boolean nextTemporarySwapActive) {
            return new PendingSequence(plan, originalHotbarSlot, fluidInventorySlot, fluidBucketItem,
                fluidAccessMode, stage, delayTicks, placeRetriesLeft, fluidPlaceAttemptsLeft,
                fluidPickupAttemptsLeft, nextTemporarySwapActive, stabilityWaitedTicks);
        }
        
        private PendingSequence withStabilityWaitedTicks(int ticks) {
            return new PendingSequence(plan, originalHotbarSlot, fluidInventorySlot, fluidBucketItem,
                fluidAccessMode, stage, delayTicks, placeRetriesLeft, fluidPlaceAttemptsLeft,
                fluidPickupAttemptsLeft, temporarySwapActive, ticks);
        }
    }
    
    private record LeverHoldState(BlockPos basePos, int delayTicks) {
        private LeverHoldState withDelay(int nextDelayTicks) {
            return new LeverHoldState(basePos, nextDelayTicks);
        }
    }
    
    private record PlacementPlan(
        BlockPos targetPos,
        BlockHitResult fluidPlaceHit,
        BlockHitResult fluidPickupHit,
        BlockHitResult placementHit,
        Item placementItem,
        Block expectedBlock,
        boolean refreshExistingTarget,
        boolean requiresSneak
    ) {
        private boolean matchesHeldStack(ItemStack stack) {
            return stack.isOf(placementItem);
        }
        
        private boolean isPlacementSatisfied(BlockState state) {
            return state.isOf(expectedBlock);
        }
    }
    
    private record FluidSource(int inventorySlot, Item bucketItem) {}
    
    private record LeverHoldPlan(BlockPos basePos) {}
    
    private enum FluidAccessMode {
        HOTBAR,
        SWAP_WITH_ORIGINAL_SLOT
    }
    
    private enum SequenceStage {
        SELECT_FLUID,
        USE_FLUID,
        WAIT_FOR_EMPTY_BUCKET,
        PICKUP_FLUID,
        WAIT_FOR_FILLED_BUCKET,
        RESTORE_PLACEMENT_ITEM,
        PLACE_BLOCK
    }
    
    // === CONFIGURATION HELPERS ===
    
    private static int configuredRetryDelayTicks() {
        return speedProfile.retryDelayTicks;
    }
    
    private static int configuredSettleDelayTicks() {
        return speedProfile.settleDelayTicks;
    }
    
    private static int configuredPostPickupDelayTicks() {
        return speedProfile.postPickupDelayTicks;
    }
    
    private static int getStagesPerTick() {
        return speedProfile.stagesPerTick;
    }
}