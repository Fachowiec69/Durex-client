package pl.durex.client.module;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

public class NametagRenderer {

    private static Matrix4f cachedProj = null;
    private static Matrix4f cachedView = null;
    private static int cachedScreenW = 0, cachedScreenH = 0;
    private static long lastCacheFrame = -1;

    public static void register() {
        HudRenderCallback.EVENT.register(NametagRenderer::onHudRender);
    }

    private static void onHudRender(DrawContext ctx, RenderTickCounter tickCounter) {
        if (!NametagsModule.isEnabled()) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null || client.gameRenderer == null) return;

        Camera camera = client.gameRenderer.getCamera();
        Vec3d camPos = camera.getPos();

        long frame = client.getRenderTime();
        if (frame != lastCacheFrame) {
            lastCacheFrame = frame;
            cachedScreenW = client.getWindow().getScaledWidth();
            cachedScreenH = client.getWindow().getScaledHeight();
            cachedProj = client.gameRenderer.getBasicProjectionMatrix(client.options.getFov().getValue());
            cachedView = new Matrix4f();
            cachedView.rotate(camera.getRotation().conjugate(new Quaternionf()));
        }

        List<AbstractClientPlayerEntity> visible = new ArrayList<>();
        for (AbstractClientPlayerEntity player : client.world.getPlayers()) {
            if (player == client.player) continue;
            if (!player.isAlive()) continue;
            double dist = camPos.distanceTo(player.getPos());
            if (dist > NametagsModule.getMaxDistance()) continue;
            visible.add(player);
        }

        for (AbstractClientPlayerEntity player : visible) {
            double dist = camPos.distanceTo(player.getPos());
            NametagsModule.NametagData data = NametagsModule.getData(player, client);
            if (data == null) continue;

            float td = tickCounter.getTickDelta(true);
            double wx = player.prevX + (player.getX() - player.prevX) * td;
            double wy = player.getY() + player.getHeight() + 0.25;
            double wz = player.prevZ + (player.getZ() - player.prevZ) * td;

            int[] screen = worldToScreen(wx - camPos.x, wy - camPos.y, wz - camPos.z);
            if (screen == null) continue;

            // Ping
            int ping = -1;
            if (client.getNetworkHandler() != null) {
                var entry = client.getNetworkHandler().getPlayerListEntry(player.getUuid());
                if (entry != null) ping = entry.getLatency();
            }

            renderTag(ctx, client, screen[0], screen[1], data, (float) dist, ping);
        }
    }

    private static void renderTag(DrawContext ctx, MinecraftClient client,
            int sx, int sy, NametagsModule.NametagData data, float dist, int ping) {

        TextRenderer tr = client.textRenderer;

        // Nick
        String nickLine = NametagsModule.getNickColor() + data.name();

        // HP
        String hpLine = null;
        if (NametagsModule.isShowHp()) {
            int hpInt    = (int) Math.ceil(data.hp());
            int maxHpInt = (int) data.maxHp();
            hpLine = hpColor(data.hp(), data.maxHp()) + hpInt + "§7/" + maxHpInt;
        }

        // Dystans
        String distLine = null;
        if (NametagsModule.isShowDistance()) {
            distLine = "§8" + String.format("%.1f", dist) + "m";
        }

        // Ping
        String pingLine = null;
        if (NametagsModule.isShowPing() && ping >= 0) {
            String pc = ping < 80 ? "§a" : ping < 150 ? "§e" : ping < 300 ? "§6" : "§c";
            pingLine = pc + ping + "§7ms";
        }

        // Zbroja
        ItemStack[] armorStacks = null;
        String[] armorPlus = null;
        int filledCount = 0;
        if (NametagsModule.isShowArmor() && data.armorStacks() != null) {
            armorStacks = data.armorStacks();
            armorPlus = data.armorPlus();
            for (ItemStack s : armorStacks) {
                if (s != null && !s.isEmpty()) filledCount++;
            }
        }

        // Itemy w rękach
        ItemStack mainHand = null;
        ItemStack offHand = null;
        int handItemCount = 0;
        if (NametagsModule.isShowItems()) {
            mainHand = data.player().getMainHandStack();
            offHand = data.player().getOffHandStack();
            if (mainHand != null && !mainHand.isEmpty()) handItemCount++;
            if (offHand != null && !offHand.isEmpty()) handItemCount++;
        }

        // Wymiary
        int padX = 5, padY = 4, lh = 9;
        int iconSize = 12, iconGap = 2;

        String[] textLines = {nickLine, hpLine, distLine, pingLine};
        int maxTextW = 0, textLineCount = 0;
        for (String line : textLines) {
            if (line == null) continue;
            maxTextW = Math.max(maxTextW, tr.getWidth(line));
            textLineCount++;
        }

        int armorRowW = filledCount > 0 ? filledCount * (iconSize + iconGap) - iconGap : 0;
        int handRowW = handItemCount > 0 ? handItemCount * (iconSize + iconGap) - iconGap : 0;
        int contentW = Math.max(maxTextW, Math.max(armorRowW, handRowW));
        int boxW = contentW + padX * 2;
        int textH = textLineCount * lh;
        int armorH = filledCount > 0 ? iconSize + padY : 0;
        int handH = handItemCount > 0 ? iconSize + padY : 0;
        int boxH = padY + textH + armorH + handH + padY;

        int bx = sx - boxW / 2;
        int by = sy - boxH;

        // Tło
        int bg = 0xCC050010;
        int border = 0xAA8800EE;
        ctx.fill(bx + 2, by,     bx + boxW - 2, by + boxH,     bg);
        ctx.fill(bx,     by + 2, bx + boxW,     by + boxH - 2, bg);
        // Ramka
        ctx.fill(bx + 2, by,           bx + boxW - 2, by + 1,           border);
        ctx.fill(bx + 2, by + boxH - 1,bx + boxW - 2, by + boxH,        border);
        ctx.fill(bx,     by + 2,       bx + 1,        by + boxH - 2,    border);
        ctx.fill(bx + boxW - 1, by + 2,bx + boxW,     by + boxH - 2,    border);
        // Narożniki
        ctx.fill(bx + 1, by + 1,             bx + 2, by + 2,             border);
        ctx.fill(bx + boxW - 2, by + 1,      bx + boxW - 1, by + 2,      border);
        ctx.fill(bx + 1, by + boxH - 2,      bx + 2, by + boxH - 1,      border);
        ctx.fill(bx + boxW - 2, by + boxH - 2, bx + boxW - 1, by + boxH - 1, border);

        // Tekst
        int ty = by + padY;
        for (String line : textLines) {
            if (line == null) continue;
            int tw = tr.getWidth(line);
            ctx.drawText(tr, line, sx - tw / 2, ty, 0xFFFFFFFF, false);
            ty += lh;
        }

        // Ikonki zbroi
        if (filledCount > 0 && armorStacks != null) {
            int totalW = filledCount * (iconSize + iconGap) - iconGap;
            int armorStartX = sx - totalW / 2;
            int armorY = ty + 2;
            float scale = (float) iconSize / 16f;
            int drawIdx = 0;
            for (int i = 0; i < 4; i++) {
                if (armorStacks[i] == null || armorStacks[i].isEmpty()) continue;
                int ix = armorStartX + drawIdx * (iconSize + iconGap);
                ctx.getMatrices().push();
                ctx.getMatrices().translate(ix, armorY, 0);
                ctx.getMatrices().scale(scale, scale, 1f);
                ctx.drawItem(armorStacks[i], 0, 0);
                ctx.getMatrices().pop();
                String plusStr = armorPlus != null ? armorPlus[i] : null;
                if (plusStr != null) {
                    ctx.getMatrices().push();
                    ctx.getMatrices().translate(ix + iconSize - 2, armorY + iconSize - 5, 200);
                    ctx.getMatrices().scale(0.5f, 0.5f, 1f);
                    ctx.drawText(tr, NametagsModule.plusColor(plusStr) + plusStr, 0, 0, 0xFFFFFFFF, false);
                    ctx.getMatrices().pop();
                }
                drawIdx++;
            }
            ty = armorY + iconSize;
        }

        // Ikonki itemów w rękach
        if (handItemCount > 0) {
            int totalW = handItemCount * (iconSize + iconGap) - iconGap;
            int handStartX = sx - totalW / 2;
            int handY = ty + 2;
            float scale = (float) iconSize / 16f;
            int drawIdx = 0;

            // Main hand (prawa ręka)
            if (mainHand != null && !mainHand.isEmpty()) {
                int ix = handStartX + drawIdx * (iconSize + iconGap);
                ctx.getMatrices().push();
                ctx.getMatrices().translate(ix, handY, 0);
                ctx.getMatrices().scale(scale, scale, 1f);
                ctx.drawItem(mainHand, 0, 0);
                ctx.getMatrices().pop();
                
                // Liczba itemów
                if (mainHand.getCount() > 1) {
                    ctx.getMatrices().push();
                    ctx.getMatrices().translate(ix + iconSize - 2, handY + iconSize - 5, 200);
                    ctx.getMatrices().scale(0.5f, 0.5f, 1f);
                    ctx.drawText(tr, "§e" + mainHand.getCount(), 0, 0, 0xFFFFFFFF, false);
                    ctx.getMatrices().pop();
                }
                drawIdx++;
            }

            // Off hand (lewa ręka)
            if (offHand != null && !offHand.isEmpty()) {
                int ix = handStartX + drawIdx * (iconSize + iconGap);
                ctx.getMatrices().push();
                ctx.getMatrices().translate(ix, handY, 0);
                ctx.getMatrices().scale(scale, scale, 1f);
                ctx.drawItem(offHand, 0, 0);
                ctx.getMatrices().pop();
                
                // Liczba itemów
                if (offHand.getCount() > 1) {
                    ctx.getMatrices().push();
                    ctx.getMatrices().translate(ix + iconSize - 2, handY + iconSize - 5, 200);
                    ctx.getMatrices().scale(0.5f, 0.5f, 1f);
                    ctx.drawText(tr, "§b" + offHand.getCount(), 0, 0, 0xFFFFFFFF, false);
                    ctx.getMatrices().pop();
                }
                drawIdx++;
            }
        }
    }

    private static int[] worldToScreen(double dx, double dy, double dz) {
        if (cachedProj == null || cachedView == null) return null;
        Vector4f pos = new Vector4f((float)dx, (float)dy, (float)dz, 1.0f);
        cachedView.transform(pos);
        cachedProj.transform(pos);
        if (pos.w <= 0) return null;
        float ndcX = pos.x / pos.w;
        float ndcY = pos.y / pos.w;
        if (ndcX < -1.5f || ndcX > 1.5f || ndcY < -1.5f || ndcY > 1.5f) return null;
        int sx = (int)((ndcX + 1f) / 2f * cachedScreenW);
        int sy = (int)((1f - ndcY) / 2f * cachedScreenH);
        return new int[]{sx, sy};
    }

    private static String hpColor(float hp, float maxHp) {
        float pct = maxHp > 0 ? hp / maxHp : 0;
        if (pct > 0.6f) return "§a";
        if (pct > 0.3f) return "§e";
        return "§c";
    }
}
