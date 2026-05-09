package pl.durex.client.module;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.lwjgl.glfw.GLFW;
import pl.durex.client.util.TimerUtil;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public final class AntiKowalModule {
    private static final double INTERACT_REACH = 6.0;
    private static final double SEARCH_EXPAND = 1.5;
    private static final long INTERACT_DELAY_MS = 110L;
    private static final Set<String> BLACKSMITH_NAMES = Set.of("kowal", "blacksmith");

    private InputUtil.Key toggleKey = InputUtil.UNKNOWN_KEY;
    private final TimerUtil interactTimer = new TimerUtil();
    private boolean wasKeyDown = false;

    private boolean enabled;
    private boolean hidePlayers = false;

    public void register() {
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean v) { enabled = v; }

    public void toggle() {
        enabled = !enabled;
        interactTimer.reset();
    }

    public boolean isHidePlayers() {
        return hidePlayers;
    }

    public void setHidePlayers(boolean hidePlayers) {
        this.hidePlayers = hidePlayers;
    }

    public String getBindName() {
        return toggleKey.getLocalizedText().getString();
    }

    public String getBindKey() {
        return toggleKey.getTranslationKey();
    }

    public void setBind(InputUtil.Key key) {
        toggleKey = key;
    }

    private void onClientTick(MinecraftClient client) {
        

        // Toggle AntiKowal po naciśnięciu klawisza (tylko gdy nie ma GUI)
        if (client.currentScreen == null) {
            long handle = client.getWindow().getHandle();
            boolean isDown = toggleKey.getCode() != -1 && InputUtil.isKeyPressed(handle, toggleKey.getCode());
            if (isDown && !wasKeyDown) {
                toggle();
                sendStateMessage(client);
            }
            wasKeyDown = isDown;
        } else {
            wasKeyDown = false;
        }

        if (!enabled || client.player == null || client.world == null) {
            return;
        }

        if (client.options.useKey.isPressed() && interactTimer.hasTimePassed(INTERACT_DELAY_MS)) {
            if (tryInteract(client)) {
                interactTimer.reset();
            }
        }
    }

    private void sendStateMessage(MinecraftClient client) {
        if (client.player != null) {
            client.player.sendMessage(Text.literal("[Durex] AntiKowal " + (enabled ? "§aON" : "§cOFF")), false);
        }
    }

    // 🔥 GŁÓWNA LOGIKA KLIKANIA
    private boolean tryInteract(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null || client.interactionManager == null) {
            return false;
        }

        Entity target = findPreferredTarget(client);
        if (target == null) {
            return false;
        }

        client.interactionManager.interactEntity(
                player,
                target,
                Hand.MAIN_HAND
        );

        player.swingHand(Hand.MAIN_HAND);
        return true;
    }

    public Entity findPreferredTarget(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null) {
            return null;
        }

        Vec3d eyes = player.getCameraPosVec(1.0F);
        Vec3d look = player.getRotationVec(1.0F).normalize();
        Vec3d maxReach = eyes.add(look.multiply(INTERACT_REACH));

        Box searchBox = player.getBoundingBox().stretch(look.multiply(INTERACT_REACH)).expand(SEARCH_EXPAND);

        Entity best = null;
        double bestScore = Double.MAX_VALUE;

        for (Entity entity : client.world.getOtherEntities(player, searchBox)) {
            if (!isNPC(entity)) {
                continue;
            }

            Optional<Vec3d> hitPos = entity.getBoundingBox().expand(entity.getTargetingMargin() + 0.15F).raycast(eyes, maxReach);
            if (hitPos.isEmpty()) {
                continue;
            }

            Vec3d entityHitPos = hitPos.get();
            double entityDistanceSq = eyes.squaredDistanceTo(entityHitPos);
            if (entityDistanceSq > INTERACT_REACH * INTERACT_REACH) {
                continue;
            }

            if (isBlockedByWorld(client, eyes, entityHitPos, entityDistanceSq)) {
                continue;
            }

            double score = entityDistanceSq;

            if (score < bestScore) {
                bestScore = score;
                best = entity;
            }
        }

        return best;
    }

    public boolean shouldIgnoreForTargeting(Entity entity) {
        return enabled && entity instanceof OtherClientPlayerEntity;
    }

    private boolean isNPC(Entity entity) {
        if (!(entity instanceof LivingEntity livingEntity)) {
            return false;
        }
        if (entity instanceof OtherClientPlayerEntity || !livingEntity.isAlive() || livingEntity.isRemoved()) {
            return false;
        }

        String name = entity.getName().getString().toLowerCase(Locale.ROOT);
        for (String s : BLACKSMITH_NAMES) {
            if (name.contains(s)) {
                return true;
            }
        }

        return false;
    }

    public boolean shouldHideEntity(Entity entity) {
        return enabled && hidePlayers && entity instanceof OtherClientPlayerEntity;
    }

    private boolean isBlockedByWorld(MinecraftClient client, Vec3d eyes, Vec3d hitPos, double entityDistanceSq) {
        BlockHitResult blockHit = client.world.raycast(new RaycastContext(
                eyes,
                hitPos,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                client.player
        ));

        if (blockHit.getType() != HitResult.Type.BLOCK) {
            return false;
        }

        return eyes.squaredDistanceTo(blockHit.getPos()) + 1.0E-4 < entityDistanceSq;
    }
}
