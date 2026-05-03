/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.minecraft.class_1657
 *  net.minecraft.class_1661
 *  net.minecraft.class_1707
 *  net.minecraft.class_1713
 *  net.minecraft.class_1735
 *  net.minecraft.class_1792
 *  net.minecraft.class_1799
 *  net.minecraft.class_1802
 *  net.minecraft.class_1935
 *  net.minecraft.class_2561
 *  net.minecraft.class_310
 *  net.minecraft.class_476
 */
package com.example;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1657;
import net.minecraft.class_1661;
import net.minecraft.class_1707;
import net.minecraft.class_1713;
import net.minecraft.class_1735;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_1935;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_476;

@Environment(value=EnvType.CLIENT)
public class AutomationHandler {
    private static State currentState = State.IDLE;
    private static final int DELAY_TICKS = 20;
    private static int stateTimer = 0;

    public static void tick(class_310 client) {
        if (client.field_1724 == null || client.field_1687 == null) {
            return;
        }
        ++stateTimer;
        switch (currentState.ordinal()) {
            case 0: {
                AutomationHandler.checkConditions(client);
                break;
            }
            case 1: {
                if (stateTimer <= 20) break;
                client.field_1724.field_3944.method_45729("#stop");
                client.field_1724.field_3944.method_45730("kit");
                currentState = State.REFILL_WAIT_FOR_KIT_GUI;
                stateTimer = 0;
                break;
            }
            case 2: {
                if (!(client.field_1755 instanceof class_476)) break;
                AutomationHandler.clickSlot(client, 20);
                currentState = State.REFILL_CLICK_ITEMS;
                stateTimer = 0;
                break;
            }
            case 3: {
                if (stateTimer <= 10) break;
                AutomationHandler.clickSlot(client, 43);
                currentState = State.REFILL_WAIT_FOR_PICKUP_GUI;
                stateTimer = 0;
                break;
            }
            case 4: {
                class_476 screen;
                class_2561 title;
                if (!(client.field_1755 instanceof class_476) || !(title = (screen = (class_476)client.field_1755).method_25440()).getString().toLowerCase().contains("odbierz")) break;
                currentState = State.REFILL_PICKUP_ITEMS;
                stateTimer = 0;
                break;
            }
            case 5: {
                if (stateTimer <= 10) break;
                class_476 screen = (class_476)client.field_1755;
                int steakSlot = -1;
                for (int i = 0; i < ((class_1707)screen.method_17577()).field_7761.size(); ++i) {
                    class_1799 stack = ((class_1735)((class_1707)screen.method_17577()).field_7761.get(i)).method_7677();
                    if (stack.method_7909() != class_1802.field_8176) continue;
                    steakSlot = i;
                    break;
                }
                if (steakSlot != -1) {
                    AutomationHandler.clickSlot(client, steakSlot);
                }
                client.field_1724.method_7346();
                currentState = State.REFILL_EQUIP_OFFHAND;
                stateTimer = 0;
                break;
            }
            case 6: {
                if (stateTimer <= 20) break;
                for (int i = 9; i < 45; ++i) {
                    class_1799 stack = ((class_1735)client.field_1724.field_7498.field_7761.get(i)).method_7677();
                    if (stack.method_7909() != class_1802.field_8176) continue;
                    client.field_1761.method_2906(client.field_1724.field_7498.field_7763, i, 0, class_1713.field_7790, (class_1657)client.field_1724);
                    client.field_1761.method_2906(client.field_1724.field_7498.field_7763, 45, 0, class_1713.field_7790, (class_1657)client.field_1724);
                }
                currentState = State.REFILL_FINISH;
                stateTimer = 0;
                break;
            }
            case 7: {
                if (stateTimer <= 20) break;
                client.field_1724.field_3944.method_45729("#farm");
                currentState = State.IDLE;
                break;
            }
            case 8: {
                if (stateTimer <= 20) break;
                client.field_1724.field_3944.method_45729("#stop");
                client.field_1724.field_3944.method_45730("kosz");
                currentState = State.DEPOSIT_WAIT_FOR_GUI;
                stateTimer = 0;
                break;
            }
            case 9: {
                if (!(client.field_1755 instanceof class_476)) break;
                currentState = State.DEPOSIT_DUMP_ITEMS;
                stateTimer = 0;
                break;
            }
            case 10: {
                if (stateTimer <= 10) break;
                class_476 screen = (class_476)client.field_1755;
                int totalSlots = ((class_1707)screen.method_17577()).field_7761.size();
                for (int i = 0; i < totalSlots; ++i) {
                    class_1799 stack;
                    class_1735 slot = (class_1735)((class_1707)screen.method_17577()).field_7761.get(i);
                    if (!(slot.field_7871 instanceof class_1661) || (stack = slot.method_7677()).method_7960() || AutomationHandler.isWhitelisted(stack.method_7909())) continue;
                    client.field_1761.method_2906(((class_1707)screen.method_17577()).field_7763, i, 0, class_1713.field_7794, (class_1657)client.field_1724);
                }
                client.field_1724.method_7346();
                currentState = State.DEPOSIT_FINISH;
                stateTimer = 0;
                break;
            }
            case 11: {
                if (stateTimer <= 20) break;
                client.field_1724.field_3944.method_45729("#farm");
                currentState = State.IDLE;
            }
        }
    }

    private static void checkConditions(class_310 client) {
        if (client.field_1724 == null) {
            return;
        }
        boolean hasSteak = client.field_1724.method_31548().method_7379(new class_1799((class_1935)class_1802.field_8176));
        if (!hasSteak && client.field_1724.method_6079().method_7909() != class_1802.field_8176) {
            currentState = State.REFILL_START;
            stateTimer = 0;
            return;
        }
        if (client.field_1724.method_31548().method_7376() == -1) {
            currentState = State.DEPOSIT_START;
            stateTimer = 0;
            return;
        }
    }

    private static boolean isWhitelisted(class_1792 item) {
        return item == class_1802.field_8176 || item == class_1802.field_8527 || item == class_1802.field_8250;
    }

    private static void clickSlot(class_310 client, int slotId) {
        if (client.field_1755 instanceof class_476) {
            class_476 screen = (class_476)client.field_1755;
            client.field_1761.method_2906(((class_1707)screen.method_17577()).field_7763, slotId, 0, class_1713.field_7790, (class_1657)client.field_1724);
        }
    }

    @Environment(value=EnvType.CLIENT)
    private static enum State {
        IDLE,
        REFILL_START,
        REFILL_WAIT_FOR_KIT_GUI,
        REFILL_CLICK_ITEMS,
        REFILL_WAIT_FOR_PICKUP_GUI,
        REFILL_PICKUP_ITEMS,
        REFILL_EQUIP_OFFHAND,
        REFILL_FINISH,
        DEPOSIT_START,
        DEPOSIT_WAIT_FOR_GUI,
        DEPOSIT_DUMP_ITEMS,
        DEPOSIT_FINISH;

    }
}

