import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import java.util.regex.*;

/**
 * Gradle task: reads ProGuard mapping file and injects
 * META-INF/durex-map.properties into the output jar.
 *
 * Properties format:
 *   pl.durex.client.license.LicenseManager=x.II
 */
public abstract class InjectMappingTask extends DefaultTask {

    private File inputJar;
    private File outputJar;
    private File mappingFile;

    @InputFile
    public File getInputJar() { return inputJar; }
    public void setInputJar(File f) { this.inputJar = f; }

    @OutputFile
    public File getOutputJar() { return outputJar; }
    public void setOutputJar(File f) { this.outputJar = f; }

    @InputFile
    public File getMappingFile() { return mappingFile; }
    public void setMappingFile(File f) { this.mappingFile = f; }

    @TaskAction
    public void inject() throws Exception {
        // Parse ProGuard mapping file
        Properties props = new Properties();
        Pattern classLine = Pattern.compile("^(\\S+) -> (\\S+):$");

        for (String line : Files.readAllLines(mappingFile.toPath(), StandardCharsets.UTF_8)) {
            Matcher m = classLine.matcher(line.trim());
            if (m.matches()) {
                String original = m.group(1);
                String renamed  = m.group(2);
                props.setProperty(original, renamed);
            }
        }

        // Serialize properties to bytes
        StringWriter sw = new StringWriter();
        props.store(sw, "Durex class mapping");
        byte[] mappingBytes = sw.toString().getBytes(StandardCharsets.UTF_8);

        // Copy jar and inject mapping entry
        try (JarFile jar = new JarFile(inputJar);
             JarOutputStream out = new JarOutputStream(new FileOutputStream(outputJar))) {

            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.isDirectory()) continue;
                // Skip existing mapping file — we'll write a fresh one below
                if (entry.getName().equals("META-INF/durex-map.properties")) continue;
                out.putNextEntry(new JarEntry(entry.getName()));
                out.write(jar.getInputStream(entry).readAllBytes());
                out.closeEntry();
            }

            // Inject mapping
            out.putNextEntry(new JarEntry("META-INF/durex-map.properties"));
            out.write(mappingBytes);
            out.closeEntry();
        }

        getLogger().lifecycle("[InjectMapping] Injected {} class mappings into {}",
            props.size(), outputJar.getName());
    }
}
