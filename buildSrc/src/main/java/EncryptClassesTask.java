import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Enumeration;
import java.util.jar.*;

/**
 * Gradle task: szyfruje klasy AES-CBC w jarze.
 *
 * Pipeline:
 *   input.jar  →  [szyfrowanie klas]  →  output.jar
 *
 * Klasy exempt (DurexClient, LicenceClassLoader, Mixiny) zostają plaintext.
 * Wszystkie inne klasy lądują jako encrypted/<path>.class.enc
 * Klucz AES + IV zapisywane jako META-INF/.durex (XOR z maską wbudowaną w loader)
 */
public abstract class EncryptClassesTask extends DefaultTask {

    // Maska XOR - musi być identyczna jak w LicenceClassLoader.java
    private static final long M0 = 0x4A3F2E1D0C9B8A7FL;
    private static final long M1 = 0x1E2D3C4B5A6F7E8DL;
    private static final long M2 = 0x9F8E7D6C5B4A3F2EL;
    private static final long M3 = 0x2B3C4D5E6F7A8B9CL;

    // Klasy które MUSZĄ pozostać plaintext
    private static final String[] EXEMPT_PREFIXES = {
        "pl/durex/client/DurexClient.class",
        "pl/durex/client/loader/LicenceClassLoader.class",
        "pl/durex/client/util/RaycastState.class",
        "pl/durex/client/mixin/",
        // Moduły używane przez Mixiny
        "pl/durex/client/module/AntiKowalModule.class",
        "pl/durex/client/module/AntiKostkaModule.class",
        "pl/durex/client/module/FriendModule.class",
        "pl/durex/client/module/CooldownHudModule.class",
        "pl/durex/client/module/ViewModelModule.class",
        "pl/durex/client/module/ProcenciarzModule.class",
        // Zasoby i metadane
        "META-INF/",
        "fabric.mod.json",
        "durexclient.mixins.json",
    };

    private File inputJar;
    private File outputJar;

    @InputFile
    public File getInputJar() { return inputJar; }
    public void setInputJar(File f) { this.inputJar = f; }

    @OutputFile
    public File getOutputJar() { return outputJar; }
    public void setOutputJar(File f) { this.outputJar = f; }

    @TaskAction
    public void encrypt() throws Exception {
        // Generuj losowy klucz AES-128 i IV
        SecureRandom rng = new SecureRandom();
        byte[] key = new byte[16];
        byte[] iv  = new byte[16];
        rng.nextBytes(key);
        rng.nextBytes(iv);

        // Zaszyfruj klucz+IV przez maskę XOR (tę samą co w LicenceClassLoader)
        byte[] mask = buildMask();
        byte[] keyMaterial = new byte[32];
        System.arraycopy(key, 0, keyMaterial, 0,  16);
        System.arraycopy(iv,  0, keyMaterial, 16, 16);
        byte[] encKeyMaterial = new byte[32];
        for (int i = 0; i < 32; i++) encKeyMaterial[i] = (byte)(keyMaterial[i] ^ mask[i]);

        int encrypted = 0, skipped = 0;

        try (JarFile jar = new JarFile(inputJar);
             JarOutputStream out = new JarOutputStream(new FileOutputStream(outputJar))) {

            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.isDirectory()) continue;

                byte[] data = jar.getInputStream(entry).readAllBytes();
                String name = entry.getName();

                if (name.endsWith(".class") && !isExempt(name)) {
                    // Zaszyfruj i zapisz jako encrypted/<name>.enc
                    byte[] enc = aesEncrypt(data, key, iv);
                    out.putNextEntry(new JarEntry("encrypted/" + name + ".enc"));
                    out.write(enc);
                    out.closeEntry();
                    encrypted++;
                } else {
                    // Przepisz bez zmian
                    out.putNextEntry(new JarEntry(name));
                    out.write(data);
                    out.closeEntry();
                    skipped++;
                }
            }

            // Dodaj zaszyfrowany materiał klucza
            out.putNextEntry(new JarEntry("META-INF/.durex"));
            out.write(encKeyMaterial);
            out.closeEntry();
        }

        getLogger().lifecycle("[EncryptClasses] Zaszyfrowano {} klas, pominięto {} (exempt/zasoby)",
            encrypted, skipped);
        getLogger().lifecycle("[EncryptClasses] Output: {}", outputJar.getName());
    }

    private boolean isExempt(String name) {
        for (String prefix : EXEMPT_PREFIXES) {
            if (name.startsWith(prefix) || name.equals(prefix)) return true;
        }
        return false;
    }

    private byte[] aesEncrypt(byte[] data, byte[] key, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
        return cipher.doFinal(data);
    }

    private static byte[] buildMask() {
        byte[] mask = new byte[32];
        ByteBuffer.wrap(mask, 0,  8).putLong(M0);
        ByteBuffer.wrap(mask, 8,  8).putLong(M1);
        ByteBuffer.wrap(mask, 16, 8).putLong(M2);
        ByteBuffer.wrap(mask, 24, 8).putLong(M3);
        return mask;
    }
}
