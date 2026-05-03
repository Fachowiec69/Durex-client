package com.example.msgbot;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

import java.util.ArrayList;
import java.util.List;

public class AutoChHandler {

    public static volatile boolean enabled = false;
    private static volatile int currentSlotIndex = 0;
    private static final List<Integer> channelSlots = new ArrayList<>();

    public static void onChannelScreenOpen(HandledScreen<?> screen) {
        if (!enabled) return;

        MsgBot.LOGGER.info("[MsgBot] AutoCH: wykryto GUI kanalow '{}'", screen.getTitle().getString());

        Thread t = new Thread(() -> {
            try {
                Thread.sleep(150);
                MinecraftClient.getInstance().execute(() -> clickNextChannel(screen));
            } catch (InterruptedException ignored) {}
        });
        t.setDaemon(true);
        t.start();
    }

    private static void clickNextChannel(HandledScreen<?> screen) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        var handler = screen.getScreenHandler();

        // Znajdz wszystkie niepuste sloty (kanaly)
        channelSlots.clear();
        for (int i = 0; i < Math.min(handler.slots.size(), 54); i++) {
            Slot slot = handler.slots.get(i);
            ItemStack stack = slot.getStack();
            if (!stack.isEmpty()) {
                channelSlots.add(i);
                MsgBot.LOGGER.info("[MsgBot] AutoCH: kanal w slocie {} - {}", i, stack.getItem());
            }
        }

        if (channelSlots.isEmpty()) {
            MsgBot.LOGGER.warn("[MsgBot] AutoCH: brak kanalow w GUI");
            mc.player.closeHandledScreen();
            return;
        }

        int slotToClick = channelSlots.get(currentSlotIndex % channelSlots.size());
        currentSlotIndex = (currentSlotIndex + 1) % channelSlots.size();

        MsgBot.LOGGER.info("[MsgBot] AutoCH: klikam slot {} ({}/{})",
                slotToClick, currentSlotIndex, channelSlots.size());

        mc.interactionManager.clickSlot(
                handler.syncId,
                slotToClick,
                0,
                SlotActionType.PICKUP,
                mc.player
        );

        // Reset listy - nowy spawn = nowi ludzie
        SpamScreen.onSectorChanged();
    }

    public static void reset() {
        currentSlotIndex = 0;
        channelSlots.clear();
    }
}
