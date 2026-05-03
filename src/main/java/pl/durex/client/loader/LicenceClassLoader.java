package pl.durex.client.loader;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Runtime class loader - reads ProGuard mapping from META-INF/durex-map.properties
 * and translates original class names to their obfuscated counterparts,
 * then delegates loading to the parent classloader.
 */
public final class LicenceClassLoader extends ClassLoader {

    private static LicenceClassLoader INSTANCE;
    private final Map<String, String> mapping = new HashMap<>();

    private LicenceClassLoader(ClassLoader parent) {
        super(parent);
        loadMapping();
    }
    
    // Konstruktor publiczny dla LicenceLoader
    public LicenceClassLoader(ClassLoader parent, boolean skipMapping) {
        super(parent);
        if (!skipMapping) {
            loadMapping();
        }
    }

    public static synchronized LicenceClassLoader getInstance() {
        if (INSTANCE == null)
            INSTANCE = new LicenceClassLoader(LicenceClassLoader.class.getClassLoader());
        return INSTANCE;
    }

    private void loadMapping() {
        try (InputStream is = getParent().getResourceAsStream("META-INF/durex-map.properties")) {
            if (is == null) return;
            Properties props = new Properties();
            props.load(is);
            for (String key : props.stringPropertyNames()) {
                mapping.put(key, props.getProperty(key));
            }
        } catch (Exception ignored) {}
    }

    /**
     * Load a class by its original (pre-obfuscation) name.
     * The mapping translates it to the renamed class name used at runtime.
     */
    @Override
    public Class<?> loadClass(String originalName) throws ClassNotFoundException {
        // Nie próbuj ładować klas Minecrafta/Fabric przez mapping
        if (originalName.startsWith("net.minecraft.") || 
            originalName.startsWith("net.fabricmc.") ||
            originalName.startsWith("java.") ||
            originalName.startsWith("javax.")) {
            return super.loadClass(originalName);
        }
        
        String renamed = mapping.getOrDefault(originalName, originalName);
        return super.loadClass(renamed);
    }
    
    /**
     * Define a class from raw bytecode (for module loader)
     * Używa natywnej metody defineClass z ClassLoader
     */
    public Class<?> defineClass(String name, byte[] bytes) {
        return defineClass(name, bytes, 0, bytes.length);
    }
}
