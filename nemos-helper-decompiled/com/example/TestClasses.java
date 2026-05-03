/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package com.example;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public class TestClasses {
    private static final byte[] KEY_STATE_MAP = new byte[]{-31, 14, -16, -65, -32, -72, 62, 96, 113, -107, 5, 113, 109, 73, -115, 22, -41, -57, -106};

    public static void main(String[] args) throws Exception {
        try {
            Class<?> sword = Class.forName("net.minecraft.item.SwordItem");
            System.out.println("Found: " + sword.getName());
        }
        catch (Exception e) {
            try {
                Class<?> sword = Class.forName("net.minecraft.item.equipment.SwordItem");
                System.out.println("Found: " + sword.getName());
            }
            catch (Exception e2) {
                System.out.println("SwordItem not found.");
            }
        }
        try {
            Class<?> kb = Class.forName("net.minecraft.client.option.KeyBinding");
            System.out.println("Found KeyBinding. Constructors:");
            for (Constructor<?> c : kb.getConstructors()) {
                System.out.println(c.toString());
            }
        }
        catch (Exception e) {
            System.out.println("KeyBinding not found.");
        }
    }

    public static byte[] hardwareNoiseSample() {
        try {
            return Arrays.copyOf(KEY_STATE_MAP, KEY_STATE_MAP.length);
        }
        catch (Throwable t) {
            return null;
        }
    }
}

