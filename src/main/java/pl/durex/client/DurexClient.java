package pl.durex.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import pl.durex.client.config.DurexConfig;
import pl.durex.client.gui.DurexClickGuiScreen;
import pl.durex.client.module.*;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;
import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;

public final class DurexClient implements ClientModInitializer {
    public static final String MOD_ID = "durexclient";

    private static AntiKowalModule    ANTI_KOWAL_MODULE;
    private static AntiKostkaModule   ANTI_KOSTKA_MODULE;
    private static FriendModule       FRIEND_MODULE;
    private static CooldownHudModule  COOLDOWN_HUD_MODULE;
    private static ViewModelModule    VIEW_MODEL_MODULE;
    private static ProcenciarzModule  PROCENCIARZ_MODULE;
    private static LeverCobwebModule  LEVER_COBWEB_MODULE;
    private static AutoDripstoneModule AUTO_DRIPSTONE_MODULE;
    private static NoPushModule       NO_PUSH_MODULE;
    private static MsgBotModule       MSG_BOT_MODULE;
    private static ZbrojmistrzModule  ZBROJMISTRZ_MODULE;
    private static NametagsModule     NAMETAGS_MODULE;
    private static int                autoSaveTick = 0;

    public static void saveNow() { DurexConfig.save(); }

    public static AntiKowalModule    getAntiKowalModule()    { return ANTI_KOWAL_MODULE; }
    public static AntiKostkaModule   getAntiKostkaModule()   { return ANTI_KOSTKA_MODULE; }
    public static FriendModule       getFriendModule()       { return FRIEND_MODULE; }
    public static CooldownHudModule  getCooldownHudModule()  { return COOLDOWN_HUD_MODULE; }
    public static ViewModelModule    getViewModelModule()    { return VIEW_MODEL_MODULE; }
    public static ProcenciarzModule  getProcenciarzModule()  { return PROCENCIARZ_MODULE; }
    public static LeverCobwebModule  getLeverCobwebModule()  { return LEVER_COBWEB_MODULE; }
    public static AutoDripstoneModule getAutoDripstoneModule() { return AUTO_DRIPSTONE_MODULE; }
    public static NoPushModule       getNoPushModule()       { return NO_PUSH_MODULE; }
    public static MsgBotModule       getMsgBotModule()       { return MSG_BOT_MODULE; }
    public static ZbrojmistrzModule  getZbrojmistrzModule()  { return ZBROJMISTRZ_MODULE; }
    public static NametagsModule     getNametagsModule()     { return NAMETAGS_MODULE; }

    @Override
    public void onInitializeClient() {
        DurexConfig.load();

        ANTI_KOWAL_MODULE    = new AntiKowalModule();
        ANTI_KOSTKA_MODULE   = new AntiKostkaModule();
        FRIEND_MODULE        = new FriendModule();
        COOLDOWN_HUD_MODULE  = new CooldownHudModule();
        VIEW_MODEL_MODULE    = new ViewModelModule();
        PROCENCIARZ_MODULE   = new ProcenciarzModule();
        LEVER_COBWEB_MODULE  = new LeverCobwebModule();
        AUTO_DRIPSTONE_MODULE = new AutoDripstoneModule();
        NO_PUSH_MODULE       = new NoPushModule();
        MSG_BOT_MODULE       = new MsgBotModule();
        ZBROJMISTRZ_MODULE   = new ZbrojmistrzModule();
        NAMETAGS_MODULE      = new NametagsModule();

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(literal("bomba")
                .then(argument("target", greedyString())
                    .executes(ctx -> {
                        BombModule.fakeDDoS(getString(ctx, "target"), 60);
                        return 1;
                    })));
            dispatcher.register(literal("scan")
                .then(argument("target", greedyString())
                    .executes(ctx -> {
                        BombModule.fakeScan(getString(ctx, "target"));
                        return 1;
                    })));
            dispatcher.register(literal("botnet")
                .executes(ctx -> { BombModule.fakeBotnet(); return 1; }));
        });

        ANTI_KOWAL_MODULE.register();
        FRIEND_MODULE.register();
        NametagRenderer.register();
        TracerRenderer.register();

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
            LEVER_COBWEB_MODULE.handleDisconnect());

        KeyBinding openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.durexclient.open_gui",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT_SHIFT,
            "category.durexclient"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ANTI_KOSTKA_MODULE.tick(client);
            LEVER_COBWEB_MODULE.clientTick(client);
            AUTO_DRIPSTONE_MODULE.tick(client);
            AutoShieldBreakModule.onTick();
            FullBrightModule.onTick();

            autoSaveTick++;
            if (autoSaveTick >= 1200) {
                autoSaveTick = 0;
                DurexConfig.save();
            }

            while (openGuiKey.wasPressed()) {
                client.setScreen(new DurexClickGuiScreen());
            }
        });
    }
}
