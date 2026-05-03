package pl.durex.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import pl.durex.client.loader.LicenceClassLoader;
import pl.durex.client.module.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;
import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;

public final class DurexClient implements ClientModInitializer {
    public static final String MOD_ID = "durexclient";

    private static AntiKowalModule   ANTI_KOWAL_MODULE;
    private static AntiKostkaModule  ANTI_KOSTKA_MODULE;
    private static FriendModule      FRIEND_MODULE;
    private static CooldownHudModule COOLDOWN_HUD_MODULE;
    private static ViewModelModule   VIEW_MODEL_MODULE;
    private static ProcenciarzModule PROCENCIARZ_MODULE;
    private static LeverCobwebModule LEVER_COBWEB_MODULE;
    private static AutoDripstoneModule AUTO_DRIPSTONE_MODULE;
    private static NoPushModule       NO_PUSH_MODULE;
    private static pl.durex.client.module.MsgBotModule MSG_BOT_MODULE;
    private static pl.durex.client.module.ZbrojmistrzModule ZBROJMISTRZ_MODULE;
    private static pl.durex.client.module.NametagsModule NAMETAGS_MODULE;
    private static int               autoSaveTick = 0;

    private static final String A = "pl.durex.client.license.LicenseManager";
    private static final String B = "pl.durex.client.config.DurexConfig";

    public static void saveNow() {
        try {
            if (j != null) {
                j.invoke(null);
                System.out.println("[Durex] Config saved immediately");
            }
        } catch (Exception e) {
            System.out.println("[Durex] Failed to save config immediately: " + e.getMessage());
        }
    }

    private static Class<?> c;
    private static Object d;
    private static Method e;
    private static Method f;
    private static Method g;
    private static Method h;
    private static Class<?> i;
    private static Method j;

    public static AntiKowalModule   getAntiKowalModule()  { return ANTI_KOWAL_MODULE; }
    public static AntiKostkaModule  getAntiKostkaModule() { return ANTI_KOSTKA_MODULE; }
    public static FriendModule      getFriendModule()     { return FRIEND_MODULE; }
    public static CooldownHudModule getCooldownHudModule(){ return COOLDOWN_HUD_MODULE; }
    public static ViewModelModule   getViewModelModule()  { return VIEW_MODEL_MODULE; }
    public static ProcenciarzModule getProcenciarzModule(){ return PROCENCIARZ_MODULE; }
    public static LeverCobwebModule getLeverCobwebModule(){ return LEVER_COBWEB_MODULE; }
    public static AutoDripstoneModule getAutoDripstoneModule() { return AUTO_DRIPSTONE_MODULE; }
    public static NoPushModule getNoPushModule()              { return NO_PUSH_MODULE; }
    public static pl.durex.client.module.MsgBotModule getMsgBotModule() { return MSG_BOT_MODULE; }
    public static pl.durex.client.module.ZbrojmistrzModule getZbrojmistrzModule() { return ZBROJMISTRZ_MODULE; }
    public static pl.durex.client.module.NametagsModule getNametagsModule() { return NAMETAGS_MODULE; }

    @Override
    public void onInitializeClient() {
        System.out.println("[Durex] Starting initialization...");
        LicenceClassLoader loader = LicenceClassLoader.getInstance();

        try {
            c = loader.loadClass(A);
            d = m(c);
            e = n(c, void.class, false, new Class<?>[0], "loadAndValidate");
            f = n(c, boolean.class, false, new Class<?>[]{String.class}, "validate");
            g = n(c, void.class, false, new Class<?>[0], "delete");
            h = n(c, boolean.class, false, new Class<?>[0], "isValid");
            e.invoke(d);
            System.out.println("[Durex] LicenseManager initialized");
        } catch (Exception e) {
            throw new RuntimeException("[Durex] Failed to initialize LicenseManager", e);
        }

        try {
            i = loader.loadClass(B);
            Method loadMethod = n(i, void.class, true, new Class<?>[0], "load");
            j = n(i, void.class, true, new Class<?>[0], "save");
            // Nie ładujemy jeszcze configu - zrobimy to po inicjalizacji modułów
            System.out.println("[Durex] DurexConfig methods prepared");
        } catch (Exception e) {
            System.out.println("[Durex] Failed to prepare DurexConfig: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("[Durex] Failed to load DurexConfig", e);
        }

        ANTI_KOWAL_MODULE   = new AntiKowalModule();
        ANTI_KOSTKA_MODULE  = new AntiKostkaModule();
        FRIEND_MODULE       = new FriendModule();
        COOLDOWN_HUD_MODULE = new CooldownHudModule();
        VIEW_MODEL_MODULE   = new ViewModelModule();
        PROCENCIARZ_MODULE  = new ProcenciarzModule();
        LEVER_COBWEB_MODULE = new LeverCobwebModule();
        AUTO_DRIPSTONE_MODULE = new AutoDripstoneModule();
        NO_PUSH_MODULE        = new NoPushModule();
        MSG_BOT_MODULE        = new pl.durex.client.module.MsgBotModule();
        ZBROJMISTRZ_MODULE    = new pl.durex.client.module.ZbrojmistrzModule();
        NAMETAGS_MODULE       = new pl.durex.client.module.NametagsModule();

        // Teraz ładujemy config gdy moduły już istnieją
        try {
            Method loadMethod = n(i, void.class, true, new Class<?>[0], "load");
            loadMethod.invoke(null);
            System.out.println("[Durex] DurexConfig loaded successfully after modules initialization");
        } catch (Exception e) {
            System.out.println("[Durex] Failed to load DurexConfig after modules: " + e.getMessage());
            e.printStackTrace();
        }

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(literal("licencja")
                .then(literal("create")
                    .then(argument("key", greedyString())
                        .executes(ctx -> {
                            String key = getString(ctx, "key");
                            try {
                                boolean ok = f != null && (boolean) f.invoke(d, key);
                                ctx.getSource().sendFeedback(ok
                                    ? Text.literal("\u00a7a[Durex] Licencja aktywowana!")
                                    : Text.literal("\u00a7c[Durex] Nieprawidlowy lub wygasly klucz."));
                            } catch (Exception e) {
                                ctx.getSource().sendFeedback(Text.literal("\u00a7c[Durex] Blad licencji."));
                            }
                            return 1;
                        })))
                .then(literal("delete")
                    .executes(ctx -> {
                        try {
                            if (g != null) g.invoke(d);
                        } catch (Exception ignored) {}
                        ctx.getSource().sendFeedback(Text.literal("\u00a7e[Durex] Licencja usunieta."));
                        return 1;
                    }))
            );
            
            // Fake komenda /bomba (robi to samo co /ddos)
            dispatcher.register(literal("bomba")
                .then(argument("target", greedyString())
                    .executes(ctx -> {
                        String target = getString(ctx, "target");
                        BombModule.fakeDDoS(target, 60);
                        return 1;
                    }))
            );
            
            // Fake komenda /scan
            dispatcher.register(literal("scan")
                .then(argument("target", greedyString())
                    .executes(ctx -> {
                        String target = getString(ctx, "target");
                        BombModule.fakeScan(target);
                        return 1;
                    }))
            );
            
            // Fake komenda /botnet
            dispatcher.register(literal("botnet")
                .executes(ctx -> {
                    BombModule.fakeBotnet();
                    return 1;
                })
            );
            
            // Fake komenda /mcsearch
            dispatcher.register(literal("mcsearch")
                .then(argument("gracz", greedyString())
                    .executes(ctx -> {
                        String gracz = getString(ctx, "gracz");
                        
                        if (!BombModule.fakeMcSearch(gracz)) {
                            return 1; // Cooldown aktywny
                        }
                        
                        // 70% szans że znajdzie gracza
                        if (Math.random() < 0.7) {
                            // Losowe IP
                            int a = (int)(Math.random() * 255);
                            int b = (int)(Math.random() * 255);
                            int c = (int)(Math.random() * 255);
                            int d = (int)(Math.random() * 255);
                            String fakeIp = a + "." + b + "." + c + "." + d;
                            
                            // Losowy serwer wycieku z datą
                            boolean isRapy = Math.random() < 0.5;
                            String leakServer = isRapy ? "rapy.pl" : "frytashub.pl";
                            String leakYear = isRapy ? "2022" : "2023";
                            
                            ctx.getSource().sendFeedback(Text.literal("\u00a7a[MCSearch] Gracz znaleziony!"));
                            ctx.getSource().sendFeedback(Text.literal("\u00a7a[MCSearch] Nick: " + gracz));
                            ctx.getSource().sendFeedback(Text.literal("\u00a7a[MCSearch] IP: " + fakeIp));
                            ctx.getSource().sendFeedback(Text.literal("\u00a7a[MCSearch] Źródło: Wyciek z " + leakServer + " (" + leakYear + ")"));
                        } else {
                            // Nie znaleziono
                            ctx.getSource().sendFeedback(Text.literal("\u00a7c[MCSearch] Gracz nie został znaleziony"));
                            ctx.getSource().sendFeedback(Text.literal("\u00a7c[MCSearch] Brak danych o graczu '" + gracz + "'"));
                        }
                        return 1;
                    }))
            );
        });

        ANTI_KOWAL_MODULE.register();
        FRIEND_MODULE.register();
        pl.durex.client.module.NametagRenderer.register();
        pl.durex.client.module.TracerRenderer.register();

        // Obsługa disconnectu
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            LEVER_COBWEB_MODULE.handleDisconnect();
        });

        // Załaduj licence loader (pobiera LicenseModule z CF Worker)
        try {
            Class.forName("pl.durex.client.loader.ModuleUpdater")
                .getMethod("checkUpdates").invoke(null);
        } catch (Exception ignored) {}

        KeyBinding openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.durexclient.open_gui",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT_SHIFT,
            "category.durexclient"
        ));
        System.out.println("[Durex] Keybinding registered: RIGHT_SHIFT");

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ANTI_KOSTKA_MODULE.tick(client);
            LEVER_COBWEB_MODULE.clientTick(client);
            AUTO_DRIPSTONE_MODULE.tick(client);
            
            autoSaveTick++;
            if (autoSaveTick >= 1200) {
                autoSaveTick = 0;
                try {
                    if (j != null) j.invoke(null);
                } catch (Exception ignored) {}
            }
            while (openGuiKey.wasPressed()) {
                System.out.println("[Durex] RIGHT_SHIFT pressed!");
                try {
                    boolean valid = h != null && (boolean) h.invoke(d);
                    System.out.println("[Durex] GUI key pressed. License valid: " + valid);
                    if (valid) {
                        Class<?> guiCls = loader.loadClass("pl.durex.client.gui.DurexClickGuiScreen");
                        System.out.println("[Durex] Opening DurexClickGuiScreen");
                        client.setScreen((net.minecraft.client.gui.screen.Screen) guiCls.getDeclaredConstructor().newInstance());
                    } else {
                        Class<?> licenseScrCls = loader.loadClass("pl.durex.client.license.LicenseScreen");
                        System.out.println("[Durex] Opening LicenseScreen");
                        client.setScreen((net.minecraft.client.gui.screen.Screen) licenseScrCls.getDeclaredConstructor().newInstance());
                    }
                } catch (Exception e) {
                    System.err.println("[Durex] Failed to open screen:");
                    e.printStackTrace();
                    throw new RuntimeException("[Durex] Failed to open screen", e);
                }
            }
        });
    }

    private static Object m(Class<?> clazz) throws Exception {
        try {
            Method factory = n(clazz, clazz, true, new Class<?>[0], "getInstance", "getSingleton", "INSTANCE");
            if (factory != null) {
                return factory.invoke(null);
            }
        } catch (NoSuchMethodException ignored) {}

        for (Field field : clazz.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) && clazz.isAssignableFrom(field.getType())) {
                field.setAccessible(true);
                Object value = field.get(null);
                if (value != null) return value;
            }
        }

        return clazz.getDeclaredConstructor().newInstance();
    }

    private static Method n(Class<?> clazz, Class<?> returnType, boolean requireStatic, Class<?>[] params, String... names) throws NoSuchMethodException {
        for (String candidate : names) {
            try {
                Method method = clazz.getDeclaredMethod(candidate, params);
                if (matchesSignature(method, returnType, requireStatic)) {
                    method.setAccessible(true);
                    return method;
                }
            } catch (NoSuchMethodException ignored) {}
        }

        for (Method method : clazz.getDeclaredMethods()) {
            if (matchesSignature(method, returnType, requireStatic) && Arrays.equals(method.getParameterTypes(), params)) {
                method.setAccessible(true);
                return method;
            }
        }

        throw new NoSuchMethodException("No matching method found in " + clazz.getName() + " for " + Arrays.toString(names));
    }

    private static boolean matchesSignature(Method method, Class<?> returnType, boolean requireStatic) {
        if (requireStatic && !Modifier.isStatic(method.getModifiers())) return false;
        if (returnType == null) return true;
        if (returnType.equals(method.getReturnType())) return true;
        return returnType.isAssignableFrom(method.getReturnType());
    }
}
