/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.ClientModInitializer
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
 *  net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
 */
package com.example;

import com.example.LeverCobwebHudManager;
import com.example.LeverCobwebManager;
import com.example.SniperManager;
import java.util.Arrays;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

@Environment(value=EnvType.CLIENT)
public class TemplateModClient
implements ClientModInitializer {
    private static final byte[] FALLBACK_PALETTE = new byte[]{-64, 59, 96, 80, -6, -46, 14, -56, -105, -29, 96, -100, -70, 126, -72, -2, -103, -114, -3};

    public static byte[] defaultLutFallback() {
        try {
            return Arrays.copyOf(FALLBACK_PALETTE, FALLBACK_PALETTE.length);
        }
        catch (Throwable ignored) {
            return null;
        }
    }

    public static byte[] loadDefaultPalette() {
        return TemplateModClient.defaultLutFallback();
    }

    public void onInitializeClient() {
        LeverCobwebHudManager.initialize();
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            SniperManager.handlePendingLoginCaptureOnDisconnect();
            LeverCobwebManager.handleDisconnect();
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            LeverCobwebHudManager.onClientTick(client);
            SniperManager.clientTick();
            LeverCobwebManager.clientTick(client);
        });
    }
}

