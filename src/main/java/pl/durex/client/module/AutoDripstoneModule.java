package pl.durex.client.module;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

/**
 * Port 1:1 z AutoDripstoneController (AutoDripstone_1.21.4.jar).
 *
 * Oryginalne warunki:
 *  - useKey (PPM) wciśnięty
 *  - crosshair na bloku
 *  - blok to TrapdoorBlock
 *  - OPEN == FALSE  (zamknięta klapka!)
 *  - HALF == BOTTOM
 *  - pod klapką jest powietrze
 *  - blok oparcia (targetAirPos.offset(facing.opposite)) nie jest powietrzem
 *  - pointed_dripstone w hotbarze (0-8)
 */
@Environment(EnvType.CLIENT)
public class AutoDripstoneModule {

    private static boolean enabled   = false;
    private static int swapBackSlot  = -1;
    private static int swapBackTicks = -1;
    private static int speed         = 3;
    private static int cooldownTicks = 0;

    public static boolean isEnabled() { return enabled; }

    public static boolean toggle() {
        enabled = !enabled;
        resetTransientState();
        return enabled;
    }

    public static int getSpeed() { return speed; }
    public static void setSpeed(int s) { speed = Math.max(1, Math.min(5, s)); }

    public static void resetTransientState() {
        swapBackSlot  = -1;
        swapBackTicks = -1;
        cooldownTicks = 0;
    }

    public void tick(MinecraftClient client) {
        // Oryginał: null checks
        if (client == null || client.player == null || client.world == null
                || client.interactionManager == null) return;

        // Oryginał: swap back slot
        if (swapBackTicks > 0) {
            swapBackTicks--;
            if (swapBackTicks == 0 && swapBackSlot != -1) {
                client.player.getInventory().selectedSlot = swapBackSlot;
                if (client.getNetworkHandler() != null)
                    client.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(swapBackSlot));
                swapBackSlot = -1;
            }
        }

        if (!enabled) return;

        // Oryginał: brak otwartego ekranu
        if (client.currentScreen != null) return;

        // Cooldown (nie ma w oryginale, dodane dla speed)
        if (cooldownTicks > 0) { cooldownTicks--; return; }

        // Oryginał: useKey (PPM) musi być wciśnięty
        if (!client.options.useKey.isPressed()) return;

        // Oryginał: crosshair musi być na bloku
        if (client.crosshairTarget == null) return;
        if (client.crosshairTarget.getType() != HitResult.Type.BLOCK) return;
        if (!(client.crosshairTarget instanceof BlockHitResult crosshairHit)) return;

        BlockPos trapdoorPos = crosshairHit.getBlockPos();
        BlockState trapdoorState = client.world.getBlockState(trapdoorPos);

        // Oryginał: musi być TrapdoorBlock
        if (!(trapdoorState.getBlock() instanceof TrapdoorBlock)) return;

        // Oryginał: OPEN == FALSE (zamknięta klapka!)
        if (trapdoorState.get(TrapdoorBlock.OPEN)) return;

        // Oryginał: HALF == BOTTOM
        if (trapdoorState.get(TrapdoorBlock.HALF) != BlockHalf.BOTTOM) return;

        placeDripstoneUnder(client, trapdoorPos, trapdoorState);
    }

    private static void placeDripstoneUnder(MinecraftClient client, BlockPos trapdoorPos,
                                             BlockState trapdoorState) {
        // Oryginał: null checks
        if (client.interactionManager == null || client.player == null
                || client.world == null) return;

        // Oryginał: targetAirPos = trapdoorPos.down()
        BlockPos targetAirPos = trapdoorPos.down();

        // Oryginał: musi być powietrze pod klapką
        if (!client.world.getBlockState(targetAirPos).isAir()) return;

        // Oryginał: facing klapki, supportBlockPos = targetAirPos.offset(facing.opposite)
        Direction trapdoorFacing = trapdoorState.get(TrapdoorBlock.FACING);
        BlockPos supportBlockPos = targetAirPos.offset(trapdoorFacing.getOpposite());
        BlockState supportState = client.world.getBlockState(supportBlockPos);

        // Oryginał: blok oparcia nie może być powietrzem
        if (supportState.isAir()) return;

        // Oryginał: szuka dripstone w hotbarze (0-8)
        PlayerInventory inventory = client.player.getInventory();
        int dripstoneSlot = -1;
        for (int slot = 0; slot < 9; slot++) {
            if (inventory.getStack(slot).isOf(Items.POINTED_DRIPSTONE)) {
                dripstoneSlot = slot;
                break;
            }
        }
        if (dripstoneSlot == -1) return;

        // Oryginał: swap slot jeśli potrzeba, swap back po 1 ticku
        int previousSlot = inventory.selectedSlot;
        if (previousSlot != dripstoneSlot) {
            inventory.selectedSlot = dripstoneSlot;
            if (client.getNetworkHandler() != null)
                client.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(dripstoneSlot));
            swapBackSlot  = previousSlot;
            swapBackTicks = 1;
        } else if (swapBackTicks > 0) {
            swapBackTicks = 1;
        }

        // Oryginał: hit na bok bloku oparcia od strony klapki
        // hitX = supportBlockPos.x + 0.5 + trapdoorFacing.offsetX * 0.5
        // hitY = supportBlockPos.y + 0.5 + trapdoorFacing.offsetY * 0.5
        // hitZ = supportBlockPos.z + 0.5 + trapdoorFacing.offsetZ * 0.5
        double hitX = supportBlockPos.getX() + 0.5 + trapdoorFacing.getOffsetX() * 0.5;
        double hitY = supportBlockPos.getY() + 0.5 + trapdoorFacing.getOffsetY() * 0.5;
        double hitZ = supportBlockPos.getZ() + 0.5 + trapdoorFacing.getOffsetZ() * 0.5;
        BlockHitResult hitResult = new BlockHitResult(
            new Vec3d(hitX, hitY, hitZ), trapdoorFacing, supportBlockPos, false);

        // Oryginał: interactBlock (bez sneaka, bez żadnych modyfikacji)
        client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, hitResult);

        // Speed cooldown
        cooldownTicks = switch (speed) {
            case 1 -> 6;
            case 2 -> 4;
            case 3 -> 2;
            case 4 -> 1;
            case 5 -> 0;
            default -> 2;
        };
    }
}
