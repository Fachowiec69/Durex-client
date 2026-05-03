package pl.durex.client.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public class RaycastHelper {
    
    /**
     * Raycast przez graczy - ignoruje graczy i trafia w bloki za nimi
     */
    public static BlockHitResult raycastThrough(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null) return null;
        
        Vec3d eyes = player.getCameraPosVec(1.0F);
        Vec3d look = player.getRotationVec(1.0F).normalize();
        Vec3d end = eyes.add(look.multiply(4.5));
        
        return client.world.raycast(new RaycastContext(
            eyes, end,
            RaycastContext.ShapeType.OUTLINE,
            RaycastContext.FluidHandling.NONE,
            player
        ));
    }
}
