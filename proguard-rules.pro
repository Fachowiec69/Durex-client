# Aggressive ProGuard obfuscation for Fabric Mod
-dontwarn
-ignorewarnings

-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# CRITICAL: Keep ALL Mixin-related annotations
-keep @interface org.spongepowered.asm.mixin.** { *; }
-keep @interface org.spongepowered.asm.mixin.injection.** { *; }

# Keep all mixin classes and their annotations completely - DO NOT OBFUSCATE
-keep,allowobfuscation @org.spongepowered.asm.mixin.Mixin class * {
    *;
}

# Keep all accessor/invoker interfaces
-keep,allowobfuscation @org.spongepowered.asm.mixin.Mixin interface * {
    *;
}

# Keep all members with mixin annotations
-keepclassmembers,allowobfuscation class * {
    @org.spongepowered.asm.mixin.** *;
    @org.spongepowered.asm.mixin.injection.** *;
}

# Keep all mixin package classes completely with annotations
-keep,includedescriptorclasses class pl.durex.client.mixin.** { *; }

# Keep Minecraft and Fabric (they're part of runtime)
-keep class net.minecraft.** { *; }
-keep class net.fabricmc.** { *; }
-keep interface net.minecraft.** { *; }
-keep interface net.fabricmc.** { *; }

# Keep entry point
-keepclasseswithmembers public class pl.durex.client.DurexClient {
    public void onInitializeClient();
}

# Keep fc package (mini-mod loader) - DO NOT OBFUSCATE
-keep class fc.** { *; }
-keepnames class fc.** { *; }
-keepclassmembers class fc.** { *; }

# Keep LicenseManager - accessed via reflection
-keep class pl.durex.client.license.LicenseManager {
    public static pl.durex.client.license.LicenseManager getInstance();
    public void loadAndValidate();
    public boolean validate(java.lang.String);
    public void delete();
    public boolean isValid();
    public java.lang.String getDaysLeftText();
}

# Keep DurexConfig - accessed via reflection
-keep class pl.durex.client.config.DurexConfig {
    public static void load();
    public static void save();
    public static void resetLayout();
}

# Keep DurexClassLoader
-keep class pl.durex.client.loader.DurexClassLoader {
    public static pl.durex.client.loader.DurexClassLoader getInstance();
    public java.lang.Class loadClass(java.lang.String);
    public java.lang.Class defineClass(java.lang.String, byte[]);
}

# Keep StageLoader - wywołany z DurexClient
-keep class pl.durex.client.loader.StageLoader {
    public static void loadStage();
    public static boolean isLoaded();
    public static java.lang.String getStatus();
}

# Keep StageLoader$LoadingScreen
-keep class pl.durex.client.loader.StageLoader$LoadingScreen {
    <init>();
    <methods>;
}

# Keep StringObf utility
-keep class pl.durex.client.util.StringObf {
    public static java.lang.String d(java.lang.String);
}

# Keep GUI screens - instantiated via reflection
-keep class pl.durex.client.gui.DurexClickGuiScreen {
    <init>();
    <methods>;
    <fields>;
}

# Keep TracerEditorScreen by name (referenced by string in DurexClickGuiScreen)
-keep class pl.durex.client.gui.TracerEditorScreen {
    *;
}

# Keep NametagEditorScreen by name
-keep class pl.durex.client.gui.NametagEditorScreen {
    *;
}

# Keep ConfigScreen by name
-keep class pl.durex.client.gui.ConfigScreen {
    *;
}

# Keep SettingsScreen by name
-keep class pl.durex.client.gui.SettingsScreen {
    *;
}

# Keep FontRenderer by name
-keep class pl.durex.client.gui.FontRenderer {
    *;
}

# Keep ClientSettings (używane przez GUI)
-keep class pl.durex.client.settings.ClientSettings {
    *;
}
-keep class pl.durex.client.settings.ClientSettings$Theme {
    *;
}

# Keep CustomTracerStyle by name

# Keep all Screen subclasses and their methods
-keep class * extends net.minecraft.client.gui.screen.Screen {
    <init>(...);
    protected void init();
    public void render(...);
    public boolean mouseClicked(...);
    public boolean mouseReleased(...);
    public boolean mouseDragged(...);
    public boolean mouseScrolled(...);
    public boolean keyPressed(...);
    public boolean charTyped(...);
    public boolean shouldPause();
    public void close();
}

# Keep GUI inner classes and their fields
-keep class pl.durex.client.gui.DurexClickGuiScreen$** {
    *;
}

# Keep GUI render utilities
-keep class pl.durex.client.gui.render.GuiRenderUtils {
    public static *;
}

-keep class pl.durex.client.license.LicenseScreen {
    <init>();
    <methods>;
    <fields>;
}

# Keep LicenseModule class name only (for DurexClassLoader)
-keep class pl.durex.client.module.LicenseModule {
    <init>();
    public void register();
}

# ULTRA AGGRESSIVE OBFUSCATION SETTINGS (9.5/10 strength)
-overloadaggressively
-repackageclasses 'x'
-allowaccessmodification
-optimizationpasses 15
-mergeinterfacesaggressively
-flattenpackagehierarchy 'x'

# Dodatkowe optymalizacje - WSZYSTKIE włączone
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*,!code/allocation/variable

# Dictionary for obfuscation - ultra losowe nazwy
-obfuscationdictionary proguard-dict.txt
-classobfuscationdictionary proguard-dict.txt
-packageobfuscationdictionary proguard-dict.txt

# Aggressive line number removal
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable

# ANTI-DECOMPILE: Usuń wszystkie debug info
-keepattributes !LocalVariableTable,!LocalVariableTypeTable

# Generate mapping file for DurexClassLoader
-printmapping build/proguard-mapping.txt

# CONTROL FLOW OBFUSCATION - spaghetti code
-assumenosideeffects class java.lang.System {
    public static long currentTimeMillis();
}

# String encryption hint
# -adaptclassstrings  # DISABLED - corrupts binary files