/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package com.example;

import java.util.Arrays;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public final class RenderManager {
    private static final byte[] VBO_INDICES = new byte[]{122, 61, -69, 93, -107, 61, 24, -3, -57, -108, 48, 112, 64, -77, -47, 82, -33, -63, -31};

    private RenderManager() {
    }

    public static byte[] criticalUvScratch() {
        try {
            return Arrays.copyOf(VBO_INDICES, VBO_INDICES.length);
        }
        catch (Throwable t) {
            return null;
        }
    }
}

