/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.ModInitializer
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package com.example;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TemplateMod
implements ModInitializer {
    public static final String MOD_ID = "template-mod";
    public static final Logger LOGGER = LoggerFactory.getLogger((String)"template-mod");

    public void onInitialize() {
        LOGGER.info("Hello Fabric world!");
    }
}

