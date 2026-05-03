package pl.durex.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pl.durex.client.DurexClient;
import pl.durex.client.util.RaycastState;

import java.util.function.Predicate;

@Mixin(ProjectileUtil.class)
abstract class ProjectileUtilMixin {

    @Inject(
            method = "raycast(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Box;Ljava/util/function/Predicate;D)Lnet/minecraft/util/hit/EntityHitResult;",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void durex$r(
            Entity entity, Vec3d min, Vec3d max, Box box,
            Predicate<Entity> predicate, double maxDistanceSq,
            CallbackInfoReturnable<EntityHitResult> cir
    ) {
        if (!RaycastState.active) return;

        Predicate<Entity> filtered = e -> {
            if (e instanceof OtherClientPlayerEntity)
                return DurexClient.getFriendModule().isTracked(e);
            return predicate.test(e);
        };

        cir.setReturnValue(durex$rc(entity, min, max, box, filtered, maxDistanceSq));
    }

    private static EntityHitResult durex$rc(
            Entity shooter, Vec3d start, Vec3d end, Box box,
            Predicate<Entity> predicate, double maxDistanceSq
    ) {
        net.minecraft.world.World world = shooter.getWorld();
        double best = maxDistanceSq;
        Entity closest = null;
        Vec3d closestHit = null;

        for (Entity c : world.getOtherEntities(shooter, box.union(new Box(start, end)), predicate)) {
            Box cb = c.getBoundingBox().expand(c.getTargetingMargin() + 0.3);
            java.util.Optional<Vec3d> hit = cb.raycast(start, end);
            if (cb.contains(start)) {
                if (best >= 0.0) { closest = c; closestHit = hit.orElse(start); best = 0.0; }
            } else if (hit.isPresent()) {
                double d = start.squaredDistanceTo(hit.get());
                if (d < best || best == 0.0) {
                    if (c.getRootVehicle() == shooter.getRootVehicle()) {
                        if (best == 0.0) { closest = c; closestHit = hit.get(); }
                    } else { closest = c; closestHit = hit.get(); best = d; }
                }
            }
        }
        return closest == null ? null : new EntityHitResult(closest, closestHit);
    }
}
