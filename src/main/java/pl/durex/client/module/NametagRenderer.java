package pl.durex.client.module;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public class NametagRenderer {

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(NametagRenderer::onRender);
    }

    private static void onRender(WorldRenderContext ctx) {
        if (!NametagsModule.isEnabled()) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        Camera camera = ctx.camera();
        Vec3d camPos = camera.getPos();

        for (AbstractClientPlayerEntity player : client.world.getPlayers()) {
            if (player == client.player) continue;
            if (!player.isAlive()) continue;
            double dist = camPos.distanceTo(player.getPos());
            if (dist > NametagsModule.getMaxDistance()) continue;
            NametagsModule.NametagData data = NametagsModule.getData(player, client);
            if (data == null) continue;
            renderTag(camera, camPos, player, data, (float) dist);
        }
    }

    private static void renderTag(Camera camera, Vec3d camPos,
            AbstractClientPlayerEntity player, NametagsModule.NametagData data, float dist) {

        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer tr = client.textRenderer;

        double px = player.getX() - camPos.x;
        double py = player.getY() + player.getHeight() + 0.35 - camPos.y;
        double pz = player.getZ() - camPos.z;

        // Buduj linie tekstu
        String nickLine = NametagsModule.getNickColor() + data.name();

        String hpLine = null;
        if (NametagsModule.isShowHp()) {
            int hpInt    = (int) Math.ceil(data.hp());
            int maxHpInt = (int) data.maxHp();
            hpLine = hpColor(data.hp(), data.maxHp()) + hpInt + "§7/" + maxHpInt + " HP";
        }

        String distLine = null;
        if (NametagsModule.isShowDistance()) {
            distLine = "§8" + String.format("%.1f", dist) + "m";
        }

        String armorLine = null;
        if (NametagsModule.isShowArmor() && data.armorPlus() != null) {
            String[] ap = data.armorPlus();
            String[] icons = {"H", "K", "S", "B"};
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 4; i++) {
                if (i > 0) sb.append(" ");
                String plus = ap[i];
                sb.append(NametagsModule.plusColor(plus))
                  .append(icons[i]).append(":")
                  .append(plus != null ? plus : "§8-");
            }
            armorLine = sb.toString();
        }

        String[] lines = {nickLine, hpLine, distLine, armorLine};
        int maxW = 0, lineCount = 0;
        for (String line : lines) {
            if (line == null) continue;
            maxW = Math.max(maxW, tr.getWidth(line));
            lineCount++;
        }
        if (lineCount == 0) return;

        int lh = 10, padX = 5, padY = 3;
        int boxW = maxW + padX * 2;
        int boxH = lineCount * lh + padY * 2;
        int boxX = -boxW / 2;
        int boxY = -boxH;

        float scale = 0.025f;

        MatrixStack matrices = new MatrixStack();
        matrices.translate(px, py, pz);
        matrices.multiply(camera.getRotation());
        matrices.scale(-scale, -scale, scale);

        Matrix4f mat = matrices.peek().getPositionMatrix();

        // ── Tło przez VertexConsumerProvider ─────────────────────────────
        VertexConsumerProvider.Immediate vcp = client.getBufferBuilders().getEntityVertexConsumers();

        // Tło — używamy TEXT_BACKGROUND layer (przezroczysty, widoczny przez ściany)
        VertexConsumer bgBuf = vcp.getBuffer(RenderLayer.getTextBackgroundSeeThrough());
        float ba = 0.75f, br = 0.03f, bg2 = 0f, bb = 0.08f;
        quad(bgBuf, mat, boxX - 1, boxY - 1, boxW + 2, boxH + 2, 0.02f, 0f, 0f, 0f);
        quad(bgBuf, mat, boxX,     boxY,     boxW,     boxH,     ba, br, bg2, bb);

        // Ramka fioletowa
        float ra = 1f, rr = 0.53f, rg = 0f, rb = 0.93f;
        quad(bgBuf, mat, boxX,              boxY,              boxW, 1,    ra, rr, rg, rb);
        quad(bgBuf, mat, boxX,              boxY + boxH - 1,   boxW, 1,    ra, rr, rg, rb);
        quad(bgBuf, mat, boxX,              boxY,              1,    boxH, ra, rr, rg, rb);
        quad(bgBuf, mat, boxX + boxW - 1,   boxY,              1,    boxH, ra, rr, rg, rb);

        // ── Tekst ─────────────────────────────────────────────────────────
        int ty = boxY + padY;
        for (String line : lines) {
            if (line == null) continue;
            int tw = tr.getWidth(line);
            tr.draw(line, -tw / 2f, ty, 0xFFFFFFFF, false, mat, vcp,
                TextRenderer.TextLayerType.SEE_THROUGH, 0, 0xF000F0);
            ty += lh;
        }

        vcp.draw();
    }

    private static void quad(VertexConsumer buf, Matrix4f mat,
            int x, int y, int w, int h, float a, float r, float g, float b) {
        buf.vertex(mat, x,     y,     0).color(r, g, b, a);
        buf.vertex(mat, x,     y + h, 0).color(r, g, b, a);
        buf.vertex(mat, x + w, y + h, 0).color(r, g, b, a);
        buf.vertex(mat, x + w, y,     0).color(r, g, b, a);
    }

    private static String hpColor(float hp, float maxHp) {
        float pct = maxHp > 0 ? hp / maxHp : 0;
        if (pct > 0.6f) return "§a";
        if (pct > 0.3f) return "§e";
        return "§c";
    }
}
