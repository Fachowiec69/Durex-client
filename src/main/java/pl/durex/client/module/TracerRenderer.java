package pl.durex.client.module;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public class TracerRenderer {

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(TracerRenderer::render);
    }

    private static void render(WorldRenderContext ctx) {
        if (!TracerModule.isEnabled()) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        Camera camera = ctx.camera();
        Vec3d camPos = camera.getPos();

        // Zbierz graczy w zasięgu
        java.util.List<AbstractClientPlayerEntity> targets = new java.util.ArrayList<>();
        for (AbstractClientPlayerEntity player : client.world.getPlayers()) {
            if (player == client.player) continue;
            if (!player.isAlive()) continue;
            double dist = camPos.distanceTo(player.getPos());
            if (dist > TracerModule.getMaxDistance()) continue;
            targets.add(player);
        }
        if (targets.isEmpty()) return;

        float[] col = TracerModule.getColor();
        float r = col[0], g = col[1], b = col[2], a = col[3];

        // Wyłącz depth test — linie widoczne przez ściany
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.lineWidth(1.5f);

        MatrixStack matrices = new MatrixStack();
        // Kierunek patrzenia kamery — punkt startowy linii (0,0,0 w przestrzeni kamery)
        // Obracamy macierz tak jak kamera
        matrices.multiply(camera.getRotation());

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        Matrix4f mat = matrices.peek().getPositionMatrix();

        for (AbstractClientPlayerEntity player : targets) {
            // Punkt docelowy — środek gracza względem kamery
            double tx = player.getX() - camPos.x;
            double ty = player.getY() + player.getHeight() / 2.0 - camPos.y;
            double tz = player.getZ() - camPos.z;

            // Punkt startowy — (0,0,0) = pozycja kamery (środek ekranu w przestrzeni świata)
            buf.vertex(mat, 0f, 0f, 0f).color(r, g, b, a);
            buf.vertex(mat, (float) tx, (float) ty, (float) tz).color(r, g, b, a);
        }

        BufferRenderer.drawWithGlobalProgram(buf.end());

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }
}
