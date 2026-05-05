package pl.durex.client.module;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public class TracerRenderer {

    private static long tick = 0;
    private static Matrix4f cachedProj = null;
    private static Matrix4f cachedView = null;
    private static int cachedW = 0, cachedH = 0;
    private static long lastFrame = -1;

    public static void register() {
        HudRenderCallback.EVENT.register(TracerRenderer::onHud);
    }

    private static void onHud(DrawContext ctx, RenderTickCounter tc) {
        if (!TracerModule.isEnabled()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || mc.gameRenderer == null) return;

        tick++;
        Camera cam = mc.gameRenderer.getCamera();
        Vec3d cp = cam.getPos();

        long frame = mc.getRenderTime();
        if (frame != lastFrame) {
            lastFrame = frame;
            cachedW = mc.getWindow().getScaledWidth();
            cachedH = mc.getWindow().getScaledHeight();
            cachedProj = mc.gameRenderer.getBasicProjectionMatrix(mc.options.getFov().getValue());
            cachedView = new Matrix4f().rotate(cam.getRotation().conjugate(new org.joml.Quaternionf()));
        }

        int sx = cachedW / 2, sy = cachedH / 2;
        float[] col = TracerModule.getColor();
        int color = argb(col[0], col[1], col[2], col[3]);
        TracerModule.Style style = TracerModule.getStyle();
        float td = tc.getTickDelta(true);

        for (AbstractClientPlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player || !p.isAlive()) continue;
            if (cp.distanceTo(p.getPos()) > TracerModule.getMaxDistance()) continue;

            // Interpolowana pozycja
            double wx = p.prevX + (p.getX()-p.prevX)*td;
            double wy = p.prevY + (p.getY()-p.prevY)*td + p.getHeight()/2.0;
            double wz = p.prevZ + (p.getZ()-p.prevZ)*td;

            int[] sc = project(wx-cp.x, wy-cp.y, wz-cp.z);
            if (sc == null) {
                // Gracz za plecami — strzałka wokół crosshaira
                drawBehindArrow(ctx, sx, sy, wx-cp.x, wy-cp.y, wz-cp.z, color);
                continue;
            }

            // Custom styl?
            if (TracerModule.isCustomStyle()) {
                pl.durex.client.module.CustomTracerStyle cs = TracerModule.getCustomStyle();
                if (cs != null) cs.draw(ctx, sx, sy, sc[0], sc[1]);
            } else {
                drawStyle(ctx, mc, sx, sy, sc[0], sc[1], color, col, style);
            }
        }
    }

    private static void drawStyle(DrawContext ctx, MinecraftClient mc,
            int x1, int y1, int x2, int y2, int color, float[] col, TracerModule.Style style) {
        switch (style) {
            case LINE     -> line(ctx, x1, y1, x2, y2, color, 3);
            case DASHED   -> dashed(ctx, x1, y1, x2, y2, color);
            case HACKER   -> hacker(ctx, mc, x1, y1, x2, y2);
            case HEART    -> heart(ctx, x1, y1, x2, y2, color);
            case RAINBOW  -> rainbow(ctx, x1, y1, x2, y2);
            case DOUBLE   -> doubleLine(ctx, x1, y1, x2, y2, color);
            case ARROW    -> arrow(ctx, x1, y1, x2, y2, color);
            case ZIGZAG   -> zigzag(ctx, x1, y1, x2, y2, color);
            case DOTTED   -> dotted(ctx, x1, y1, x2, y2, color);
            case THICK    -> line(ctx, x1, y1, x2, y2, color, 1); // thick = krok 1 ale 2px
            case WAVE     -> wave(ctx, x1, y1, x2, y2, color);
            case PULSE    -> line(ctx, x1, y1, x2, y2,
                argb(col[0],col[1],col[2], 0.4f+0.5f*(float)Math.abs(Math.sin(tick*0.08))), 3);
            case STAR     -> star(ctx, x1, y1, x2, y2, color);
            case CROSS    -> cross(ctx, x1, y1, x2, y2, color);
            case NEON     -> neon(ctx, x1, y1, x2, y2, col);
            case FIRE     -> fire(ctx, x1, y1, x2, y2);
            case ICE      -> ice(ctx, x1, y1, x2, y2);
            case MATRIX   -> matrix(ctx, mc, x1, y1, x2, y2);
            case ELECTRIC -> electric(ctx, x1, y1, x2, y2, color);
            case CLEAN    -> clean(ctx, x1, y1, x2, y2, col);
        }
    }

    // ── Strzałka dla gracza za plecami ────────────────────────────────────
    private static void drawBehindArrow(DrawContext ctx, int cx, int cy,
            double dx, double dy, double dz, int color) {
        if (cachedView == null) return;

        Vector4f v = new Vector4f((float)dx, (float)dy, (float)dz, 0f);
        cachedView.transform(v);

        float vx = v.x, vy = -v.y;
        float len = (float) Math.sqrt(vx*vx + vy*vy);
        if (len < 0.001f) return;
        vx /= len; vy /= len;

        // Mała strzałka 20px od crosshaira
        int r = 20;
        int ax = cx + (int)(vx * r);
        int ay = cy + (int)(vy * r);

        // Prostopadły
        float px = -vy * 3, py = vx * 3;

        // Wierzchołek strzałki
        int tx = ax + (int)(vx * 5);
        int ty = ay + (int)(vy * 5);
        // Podstawa
        int b1x = ax - (int)(vx * 4) + (int)px;
        int b1y = ay - (int)(vy * 4) + (int)py;
        int b2x = ax - (int)(vx * 4) - (int)px;
        int b2y = ay - (int)(vy * 4) - (int)py;

        // Tylko 3 linie — czysta strzałka
        line(ctx, tx, ty, b1x, b1y, color, 1);
        line(ctx, tx, ty, b2x, b2y, color, 1);
        line(ctx, b1x, b1y, b2x, b2y, color, 1);
    }

    // ── LINE — Bresenham krok 3 ───────────────────────────────────────────
    private static void line(DrawContext ctx, int x1, int y1, int x2, int y2, int color, int step) {
        int dx = Math.abs(x2-x1), dy = Math.abs(y2-y1);
        if (dy==0){ctx.fill(Math.min(x1,x2),y1,Math.max(x1,x2)+1,y1+1,color);return;}
        if (dx==0){ctx.fill(x1,Math.min(y1,y2),x1+1,Math.max(y1,y2)+1,color);return;}
        int sx=x1<x2?1:-1,sy=y1<y2?1:-1,err=dx-dy,x=x1,y=y1,c=0;
        while(true){
            if(c%step==0)ctx.fill(x,y,x+1,y+1,color);
            c++;
            if(x==x2&&y==y2)break;
            int e2=2*err;
            if(e2>-dy){err-=dy;x+=sx;}
            if(e2<dx){err+=dx;y+=sy;}
        }
    }

    // ── DASHED ────────────────────────────────────────────────────────────
    private static void dashed(DrawContext ctx, int x1, int y1, int x2, int y2, int color) {
        int dx=Math.abs(x2-x1),dy=Math.abs(y2-y1);
        int sx=x1<x2?1:-1,sy=y1<y2?1:-1,err=dx-dy,x=x1,y=y1,c=0;
        while(true){
            if(c%10<6)ctx.fill(x,y,x+1,y+1,color);
            c++;
            if(x==x2&&y==y2)break;
            int e2=2*err;
            if(e2>-dy){err-=dy;x+=sx;}
            if(e2<dx){err+=dx;y+=sy;}
        }
    }

    // ── HACKER 010101 ─────────────────────────────────────────────────────
    private static void hacker(DrawContext ctx, MinecraftClient mc, int x1, int y1, int x2, int y2) {
        // Rysuj jako dashed z zielonym kolorem zamiast tekstu (unika UV2 crash)
        int dx=x2-x1,dy=y2-y1,steps=Math.max(Math.abs(dx),Math.abs(dy));
        if(steps==0)return;
        float xs=(float)dx/steps,ys=(float)dy/steps;
        for(int i=0;i<=steps;i+=6){
            int px=(int)(x1+xs*i),py=(int)(y1+ys*i);
            int c=(i/6%2==0)?0xFF00FF44:0xFF007722;
            ctx.fill(px,py,px+2,py+2,c);
        }
    }

    // ── HEART ─────────────────────────────────────────────────────────────
    private static void heart(DrawContext ctx, int x1, int y1, int x2, int y2, int color) {
        line(ctx,x1,y1,x2,y2,color,4);
        int dx=x2-x1,dy=y2-y1,steps=Math.max(Math.abs(dx),Math.abs(dy));
        if(steps==0)return;
        float xs=(float)dx/steps,ys=(float)dy/steps;
        int[][]h={{0,1,0,1,0},{1,1,1,1,1},{0,1,1,1,0},{0,0,1,0,0}};
        for(int i=12;i<steps;i+=25){
            int cx=(int)(x1+xs*i),cy=(int)(y1+ys*i);
            for(int r=0;r<h.length;r++)
                for(int c=0;c<h[r].length;c++)
                    if(h[r][c]==1)ctx.fill(cx-2+c,cy-2+r,cx-1+c,cy-1+r,color);
        }
    }

    // ── RAINBOW ───────────────────────────────────────────────────────────
    private static void rainbow(DrawContext ctx, int x1, int y1, int x2, int y2) {
        int dx=x2-x1,dy=y2-y1,steps=Math.max(Math.abs(dx),Math.abs(dy));
        if(steps==0)return;
        int segs=Math.min(16,steps);
        float xs=(float)dx/segs,ys=(float)dy/segs;
        for(int i=0;i<segs;i++){
            float hue=((float)i/segs+(tick%100)/100f)%1f;
            int c=hsv(hue,1f,1f,0.9f);
            line(ctx,(int)(x1+xs*i),(int)(y1+ys*i),(int)(x1+xs*(i+1)),(int)(y1+ys*(i+1)),c,1);
        }
    }

    // ── DOUBLE ────────────────────────────────────────────────────────────
    private static void doubleLine(DrawContext ctx, int x1, int y1, int x2, int y2, int color) {
        int dx=x2-x1,dy=y2-y1;
        double len=Math.sqrt(dx*dx+dy*dy);
        if(len==0)return;
        int ox=(int)(-dy/len*2),oy=(int)(dx/len*2);
        line(ctx,x1+ox,y1+oy,x2+ox,y2+oy,color,3);
        line(ctx,x1-ox,y1-oy,x2-ox,y2-oy,color,3);
    }

    // ── ARROW ─────────────────────────────────────────────────────────────
    private static void arrow(DrawContext ctx, int x1, int y1, int x2, int y2, int color) {
        line(ctx,x1,y1,x2,y2,color,3);
        int dx=x2-x1,dy=y2-y1;
        double len=Math.sqrt(dx*dx+dy*dy);
        if(len<10)return;
        float nx=dx/(float)len,ny=dy/(float)len;
        int ox=(int)(-ny*5),oy=(int)(nx*5);
        line(ctx,x2,y2,(int)(x2-nx*10+ox),(int)(y2-ny*10+oy),color,1);
        line(ctx,x2,y2,(int)(x2-nx*10-ox),(int)(y2-ny*10-oy),color,1);
    }

    // ── ZIGZAG ────────────────────────────────────────────────────────────
    private static void zigzag(DrawContext ctx, int x1, int y1, int x2, int y2, int color) {
        int dx=x2-x1,dy=y2-y1;
        double len=Math.sqrt(dx*dx+dy*dy);
        if(len==0)return;
        float nx=dx/(float)len,ny=dy/(float)len,ox=-ny*4,oy=nx*4;
        int segs=Math.max(1,Math.min(16,(int)(len/12)));
        float sl=(float)len/segs;
        for(int i=0;i<segs;i++){
            float ax=x1+nx*i*sl+(i%2==0?ox:-ox),ay=y1+ny*i*sl+(i%2==0?oy:-oy);
            float bx=x1+nx*(i+1)*sl+((i+1)%2==0?ox:-ox),by=y1+ny*(i+1)*sl+((i+1)%2==0?oy:-oy);
            line(ctx,(int)ax,(int)ay,(int)bx,(int)by,color,1);
        }
    }

    // ── DOTTED ────────────────────────────────────────────────────────────
    private static void dotted(DrawContext ctx, int x1, int y1, int x2, int y2, int color) {
        int dx=x2-x1,dy=y2-y1,steps=Math.max(Math.abs(dx),Math.abs(dy));
        if(steps==0)return;
        float xs=(float)dx/steps,ys=(float)dy/steps;
        for(int i=0;i<=steps;i+=8)
            ctx.fill((int)(x1+xs*i)-1,(int)(y1+ys*i)-1,(int)(x1+xs*i)+2,(int)(y1+ys*i)+2,color);
    }

    // ── WAVE ──────────────────────────────────────────────────────────────
    private static void wave(DrawContext ctx, int x1, int y1, int x2, int y2, int color) {
        int dx=x2-x1,dy=y2-y1;
        double len=Math.sqrt(dx*dx+dy*dy);
        if(len==0)return;
        float nx=dx/(float)len,ny=dy/(float)len,ox=-ny,oy=nx;
        int segs=Math.min(40,(int)(len/4));
        if(segs==0)return;
        float sl=(float)len/segs;
        for(int i=0;i<segs;i++){
            float w1=(float)Math.sin((i+tick*0.1)*0.4)*4;
            float w2=(float)Math.sin(((i+1)+tick*0.1)*0.4)*4;
            line(ctx,(int)(x1+nx*i*sl+ox*w1),(int)(y1+ny*i*sl+oy*w1),
                     (int)(x1+nx*(i+1)*sl+ox*w2),(int)(y1+ny*(i+1)*sl+oy*w2),color,1);
        }
    }

    // ── STAR ──────────────────────────────────────────────────────────────
    private static void star(DrawContext ctx, int x1, int y1, int x2, int y2, int color) {
        line(ctx,x1,y1,x2,y2,color,4);
        int dx=x2-x1,dy=y2-y1,steps=Math.max(Math.abs(dx),Math.abs(dy));
        if(steps==0)return;
        float xs=(float)dx/steps,ys=(float)dy/steps;
        for(int i=10;i<steps;i+=20){
            int cx=(int)(x1+xs*i),cy=(int)(y1+ys*i);
            ctx.fill(cx-3,cy,cx+4,cy+1,color);
            ctx.fill(cx,cy-3,cx+1,cy+4,color);
            ctx.fill(cx-2,cy-2,cx-1,cy-1,color);
            ctx.fill(cx+2,cy-2,cx+3,cy-1,color);
        }
    }

    // ── CROSS ─────────────────────────────────────────────────────────────
    private static void cross(DrawContext ctx, int x1, int y1, int x2, int y2, int color) {
        line(ctx,x1,y1,x2,y2,color,4);
        int dx=x2-x1,dy=y2-y1,steps=Math.max(Math.abs(dx),Math.abs(dy));
        if(steps==0)return;
        float xs=(float)dx/steps,ys=(float)dy/steps;
        for(int i=8;i<steps;i+=15){
            int cx=(int)(x1+xs*i),cy=(int)(y1+ys*i);
            ctx.fill(cx-3,cy,cx+4,cy+1,color);
            ctx.fill(cx,cy-3,cx+1,cy+4,color);
        }
    }

    // ── NEON — 3 warstwy ──────────────────────────────────────────────────
    private static void neon(DrawContext ctx, int x1, int y1, int x2, int y2, float[] col) {
        line(ctx,x1,y1,x2,y2,argb(col[0],col[1],col[2],0.2f),1);
        line(ctx,x1,y1,x2,y2,argb(col[0],col[1],col[2],0.5f),2);
        line(ctx,x1,y1,x2,y2,argb(col[0],col[1],col[2],0.95f),3);
    }

    // ── FIRE ──────────────────────────────────────────────────────────────
    private static void fire(DrawContext ctx, int x1, int y1, int x2, int y2) {
        int dx=x2-x1,dy=y2-y1,steps=Math.max(Math.abs(dx),Math.abs(dy));
        if(steps==0)return;
        int segs=Math.min(12,steps);
        float xs=(float)dx/segs,ys=(float)dy/segs;
        for(int i=0;i<segs;i++){
            float t=(float)i/segs;
            line(ctx,(int)(x1+xs*i),(int)(y1+ys*i),(int)(x1+xs*(i+1)),(int)(y1+ys*(i+1)),
                argb(1f,t*0.8f,0f,0.9f),1);
        }
    }

    // ── ICE ───────────────────────────────────────────────────────────────
    private static void ice(DrawContext ctx, int x1, int y1, int x2, int y2) {
        int dx=x2-x1,dy=y2-y1,steps=Math.max(Math.abs(dx),Math.abs(dy));
        if(steps==0)return;
        int segs=Math.min(12,steps);
        float xs=(float)dx/segs,ys=(float)dy/segs;
        for(int i=0;i<segs;i++){
            float t=(float)i/segs;
            line(ctx,(int)(x1+xs*i),(int)(y1+ys*i),(int)(x1+xs*(i+1)),(int)(y1+ys*(i+1)),
                argb(t*0.3f,t*0.7f+0.3f,1f,0.9f),1);
        }
    }

    // ── MATRIX — cyfry ────────────────────────────────────────────────────
    private static void matrix(DrawContext ctx, MinecraftClient mc, int x1, int y1, int x2, int y2) {
        // Rysuj jako zielone kwadraty zamiast tekstu (unika UV2 crash)
        int dx=x2-x1,dy=y2-y1,steps=Math.max(Math.abs(dx),Math.abs(dy));
        if(steps==0)return;
        float xs=(float)dx/steps,ys=(float)dy/steps;
        int idx=0;
        for(int i=0;i<=steps;i+=8){
            int px=(int)(x1+xs*i),py=(int)(y1+ys*i);
            float bright=0.4f+0.6f*(float)Math.abs(Math.sin(idx*1.7+tick*0.05));
            int c=argb(0f,bright,0f,0.9f);
            ctx.fill(px,py,px+3,py+3,c);
            idx++;
        }
    }

    // ── ELECTRIC ──────────────────────────────────────────────────────────
    private static void electric(DrawContext ctx, int x1, int y1, int x2, int y2, int color) {
        int dx=x2-x1,dy=y2-y1;
        double len=Math.sqrt(dx*dx+dy*dy);
        if(len==0)return;
        float nx=dx/(float)len,ny=dy/(float)len,ox=-ny,oy=nx;
        int segs=Math.max(2,Math.min(14,(int)(len/10)));
        float sl=(float)len/segs;
        int px=x1,py=y1;
        for(int i=1;i<=segs;i++){
            float noise=(i==segs)?0:(float)(Math.sin(i*3.7+tick*0.3)*6);
            int nx2=(int)(x1+nx*i*sl+ox*noise),ny2=(int)(y1+ny*i*sl+oy*noise);
            line(ctx,px,py,nx2,ny2,color,1);
            px=nx2;py=ny2;
        }
    }

    // ── CLEAN — gładka linia z glow i strzałką ───────────────────────────
    private static void clean(DrawContext ctx, int x1, int y1, int x2, int y2, float[] col) {
        int dx = x2 - x1, dy = y2 - y1;
        double len = Math.sqrt(dx * dx + dy * dy);
        if (len < 1) return;

        float nx = dx / (float)len, ny = dy / (float)len; // jednostkowy kierunek
        float px = -ny, py = nx;                           // prostopadły

        float r = col[0], g = col[1], b = col[2], a = col[3];

        // ── Glow — 3 warstwy coraz szersze i bardziej przezroczyste ──────
        // Warstwa 3 (najszersza, najbledszy glow)
        int glow3 = argb(r, g, b, a * 0.08f);
        int glow2 = argb(r, g, b, a * 0.18f);
        int glow1 = argb(r, g, b, a * 0.35f);
        int core  = argb(r, g, b, a * 0.92f);

        // Rysuj linie równoległe — symulacja smooth/glow
        // ±3px
        smoothLine(ctx, x1 + (int)(px*3), y1 + (int)(py*3), x2 + (int)(px*3), y2 + (int)(py*3), glow3);
        smoothLine(ctx, x1 - (int)(px*3), y1 - (int)(py*3), x2 - (int)(px*3), y2 - (int)(py*3), glow3);
        // ±2px
        smoothLine(ctx, x1 + (int)(px*2), y1 + (int)(py*2), x2 + (int)(px*2), y2 + (int)(py*2), glow2);
        smoothLine(ctx, x1 - (int)(px*2), y1 - (int)(py*2), x2 - (int)(px*2), y2 - (int)(py*2), glow2);
        // ±1px
        smoothLine(ctx, x1 + (int)(px), y1 + (int)(py), x2 + (int)(px), y2 + (int)(py), glow1);
        smoothLine(ctx, x1 - (int)(px), y1 - (int)(py), x2 - (int)(px), y2 - (int)(py), glow1);
        // Rdzeń (0px)
        smoothLine(ctx, x1, y1, x2, y2, core);

        // ── Strzałka na końcu ─────────────────────────────────────────────
        float arrowLen = 7f, arrowW = 3.5f;
        // Cofnij punkt strzałki od końca
        int ax = (int)(x2 - nx * arrowLen);
        int ay = (int)(y2 - ny * arrowLen);
        // Dwa ramiona strzałki
        int b1x = (int)(ax + px * arrowW), b1y = (int)(ay + py * arrowW);
        int b2x = (int)(ax - px * arrowW), b2y = (int)(ay - py * arrowW);
        smoothLine(ctx, x2, y2, b1x, b1y, core);
        smoothLine(ctx, x2, y2, b2x, b2y, core);
        // Glow strzałki
        smoothLine(ctx, x2, y2, b1x, b1y, glow1);
        smoothLine(ctx, x2, y2, b2x, b2y, glow1);

        // ── Punkt startowy — mały kółko (crosshair dot) ───────────────────
        ctx.fill(x1 - 1, y1 - 1, x1 + 2, y1 + 2, glow1);
        ctx.fill(x1,     y1,     x1 + 1, y1 + 1, core);
    }

    /** Linia bez przerw — każdy piksel */
    private static void smoothLine(DrawContext ctx, int x1, int y1, int x2, int y2, int color) {
        int dx = Math.abs(x2-x1), dy = Math.abs(y2-y1);
        if (dx == 0 && dy == 0) { ctx.fill(x1, y1, x1+1, y1+1, color); return; }
        if (dy == 0) { ctx.fill(Math.min(x1,x2), y1, Math.max(x1,x2)+1, y1+1, color); return; }
        if (dx == 0) { ctx.fill(x1, Math.min(y1,y2), x1+1, Math.max(y1,y2)+1, color); return; }
        int sx = x1<x2?1:-1, sy = y1<y2?1:-1, err = dx-dy, x = x1, y = y1;
        while (true) {
            ctx.fill(x, y, x+1, y+1, color);
            if (x == x2 && y == y2) break;
            int e2 = 2*err;
            if (e2 > -dy) { err -= dy; x += sx; }
            if (e2 <  dx) { err += dx; y += sy; }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────
    private static int[] project(double dx, double dy, double dz) {
        if(cachedProj==null||cachedView==null)return null;
        Vector4f p=new Vector4f((float)dx,(float)dy,(float)dz,1f);
        cachedView.transform(p);
        cachedProj.transform(p);
        if(p.w<=0)return null;
        float nx=p.x/p.w,ny=p.y/p.w;
        if(nx<-2||nx>2||ny<-2||ny>2)return null;
        return new int[]{(int)((nx+1f)/2f*cachedW),(int)((1f-ny)/2f*cachedH)};
    }

    private static int argb(float r,float g,float b,float a){
        return ((int)(a*255)&0xFF)<<24|((int)(r*255)&0xFF)<<16|((int)(g*255)&0xFF)<<8|((int)(b*255)&0xFF);
    }

    private static int hsv(float h,float s,float v,float a){
        int i=(int)(h*6);float f=h*6-i;
        float p=v*(1-s),q=v*(1-f*s),t=v*(1-(1-f)*s);
        float r,g,b;
        switch(i%6){case 0->{r=v;g=t;b=p;}case 1->{r=q;g=v;b=p;}case 2->{r=p;g=v;b=t;}
            case 3->{r=p;g=q;b=v;}case 4->{r=t;g=p;b=v;}default->{r=v;g=p;b=q;}}
        return argb(r,g,b,a);
    }
}
