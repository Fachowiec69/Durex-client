package pl.durex.client.module;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;
import org.lwjgl.glfw.GLFW;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class FriendModule {

    public static final int GLOW_COLOR = 0xFFFF69B4;

    private InputUtil.Key addKey = InputUtil.UNKNOWN_KEY;
    private boolean enabled = true;
    private final Set<UUID> friends = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private boolean wasKeyDown = false;

    public void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!pl.durex.client.license.LicenseManager.getInstance().isValid()) {
                wasKeyDown = false;
                return;
            }
            if (client.currentScreen == null && client.player != null) {
                long handle = client.getWindow().getHandle();
                boolean isDown = addKey.getCode() != -1 && InputUtil.isKeyPressed(handle, addKey.getCode());
                if (isDown && !wasKeyDown) onAdd(client);
                wasKeyDown = isDown;
            } else {
                wasKeyDown = false;
            }
        });
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Set<UUID> getFriends() { return friends; }

    public boolean isTracked(Entity entity) {
        return enabled && friends.contains(entity.getUuid());
    }

    public boolean toggle(Entity entity) {
        UUID uuid = entity.getUuid();
        if (friends.contains(uuid)) { friends.remove(uuid); return false; }
        else { friends.add(uuid); return true; }
    }

    public void clearAll() { friends.clear(); }

    public String getAddKeyName() {
        return addKey.getLocalizedText().getString();
    }

    public InputUtil.Key getAddKey() { return addKey; }

    public void setAddKey(InputUtil.Key key) {
        addKey = key;
    }

    public void onAdd(MinecraftClient client) {
        if (!enabled || client.player == null || client.world == null || client.crosshairTarget == null) return;
        if (client.crosshairTarget instanceof EntityHitResult entityHit) {
            Entity entity = entityHit.getEntity();
            if (!(entity instanceof LivingEntity)) return;
            boolean added = toggle(entity);
            String name = entity.getName().getString();
            String type = entity instanceof PlayerEntity ? "gracza" : "NPC";
            client.player.sendMessage(Text.literal(added
                    ? "§a[Durex] Dodales " + type + " §d" + name + "§a do configu"
                    : "§c[Durex] Usunales " + type + " §d" + name + "§c z configu"), false);
        }
    }
}
