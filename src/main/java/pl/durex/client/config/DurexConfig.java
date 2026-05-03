package pl.durex.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import pl.durex.client.DurexClient;
import pl.durex.client.module.AntiKostkaModule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class DurexConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE = "durexclient.json";

    private DurexConfig() {}

    private static Path getPath() {
        return FabricLoader.getInstance().getGameDir().resolve(FILE);
    }

    public static void resetLayout() {
        pl.durex.client.gui.DurexClickGuiScreen.categories.clear();
        pl.durex.client.gui.DurexClickGuiScreen.freeMods.clear();
        pl.durex.client.gui.DurexClickGuiScreen.savedPanelX = -1;
        pl.durex.client.gui.DurexClickGuiScreen.savedPanelY = -1;
        guiModRx = null; guiModRy = null; guiModDocked = null;

        // Reset wszystkich modułów do OFF
        pl.durex.client.DurexClient.getAntiKowalModule().setEnabled(false);
        pl.durex.client.DurexClient.getCooldownHudModule().setEnabled(false);
        pl.durex.client.DurexClient.getAntiKostkaModule().setEnabled(false);
        pl.durex.client.DurexClient.getViewModelModule().setEnabled(false);
        pl.durex.client.DurexClient.getProcenciarzModule().setEnabled(false);
        pl.durex.client.DurexClient.getLeverCobwebModule().setEnabled(false);
        if (pl.durex.client.module.AutoDripstoneModule.isEnabled()) pl.durex.client.module.AutoDripstoneModule.toggle();
        if (pl.durex.client.module.NoPushModule.isEnabled()) pl.durex.client.module.NoPushModule.toggle();
        pl.durex.client.module.MsgBotModule.setEnabled(false);
        pl.durex.client.module.MsgBotModule.stopSpam();
        pl.durex.client.module.MsgBotModule.stopAfk();
        pl.durex.client.module.MsgBotModule.stopAutoCh();
        pl.durex.client.module.ZbrojmistrzModule.setEnabled(false);
        pl.durex.client.module.ZbrojmistrzModule.setShowBooks(false);
        pl.durex.client.module.ProcenciarzModule.setShowBooks(false);
        pl.durex.client.util.RaycastState.active = false;
        pl.durex.client.module.NametagsModule.setEnabled(false);
        pl.durex.client.module.TracerModule.setEnabled(false);

        save();
    }

    public static void save() {
        System.out.println("[DurexConfig] Saving config...");
        try {
            JsonObject root = new JsonObject();

            // AntiKowal
            JsonObject ak = new JsonObject();
            ak.addProperty("enabled", DurexClient.getAntiKowalModule().isEnabled());
            ak.addProperty("bind", DurexClient.getAntiKowalModule().getBindKey());
            root.add("antiKowal", ak);

            // NoPlayerClip - stan ukryty w RaycastState
            JsonObject npc = new JsonObject();
            npc.addProperty("enabled", pl.durex.client.util.RaycastState.active);
            root.add("noPlayerClip", npc);

            // FriendModule
            JsonObject fm = new JsonObject();
            fm.addProperty("enabled", DurexClient.getFriendModule().isEnabled());
            fm.addProperty("bind", DurexClient.getFriendModule().getAddKey().getTranslationKey());
            root.add("friendModule", fm);

            // CooldownHud
            JsonObject ch = new JsonObject();
            ch.addProperty("enabled", DurexClient.getCooldownHudModule().isEnabled());
            ch.addProperty("x", DurexClient.getCooldownHudModule().getHudX());
            ch.addProperty("y", DurexClient.getCooldownHudModule().getHudY());
            root.add("cooldownHud", ch);

            // AntiKostka - wiele slotów
            JsonObject akk = new JsonObject();
            akk.addProperty("enabled", DurexClient.getAntiKostkaModule().isEnabled());
            akk.addProperty("delayMode", DurexClient.getAntiKostkaModule().getDelayMode().name());
            JsonArray slotsArr = new JsonArray();
            for (AntiKostkaModule.HotbarSlot slot : DurexClient.getAntiKostkaModule().getSlots()) {
                JsonObject s = new JsonObject();
                s.addProperty("name", slot.name);
                s.addProperty("loadKey", slot.loadKey.getTranslationKey());
                if (slot.hasSaved) {
                    JsonArray hotbar = new JsonArray();
                    for (int i = 0; i < 9; i++) hotbar.add(serializeStack(slot.items[i] != null ? slot.items[i] : ItemStack.EMPTY));
                    s.add("hotbar", hotbar);
                }
                slotsArr.add(s);
            }
            akk.add("slots", slotsArr);
            root.add("antiKostka", akk);

            // ViewModel
            JsonObject vm = new JsonObject();
            var v = DurexClient.getViewModelModule();
            vm.addProperty("enabled", v.isEnabled());
            vm.addProperty("rightRotX", v.rightRotX); vm.addProperty("rightRotY", v.rightRotY); vm.addProperty("rightRotZ", v.rightRotZ);
            vm.addProperty("rightPosX", v.rightPosX); vm.addProperty("rightPosY", v.rightPosY); vm.addProperty("rightPosZ", v.rightPosZ);
            vm.addProperty("rightScale", v.rightScale);
            vm.addProperty("leftRotX", v.leftRotX); vm.addProperty("leftRotY", v.leftRotY); vm.addProperty("leftRotZ", v.leftRotZ);
            vm.addProperty("leftPosX", v.leftPosX); vm.addProperty("leftPosY", v.leftPosY); vm.addProperty("leftPosZ", v.leftPosZ);
            vm.addProperty("leftScale", v.leftScale);
            root.add("viewModel", vm);

            // LeverCobweb (Nemos Helper)
            JsonObject lc = new JsonObject();
            lc.addProperty("enabled", DurexClient.getLeverCobwebModule().isEnabled());
            lc.addProperty("holdLeverEnabled", DurexClient.getLeverCobwebModule().isHoldLeverEnabled());
            lc.addProperty("switchToBestSwordEnabled", DurexClient.getLeverCobwebModule().isSwitchToBestSwordEnabled());
            lc.addProperty("playerModeEnabled", DurexClient.getLeverCobwebModule().isPlayerModeEnabled());
            lc.addProperty("webModeEnabled", DurexClient.getLeverCobwebModule().isWebModeEnabled());
            lc.addProperty("leverOnlyModeEnabled", DurexClient.getLeverCobwebModule().isLeverOnlyModeEnabled());
            lc.addProperty("speedProfile", DurexClient.getLeverCobwebModule().getSpeedProfile().name());
            root.add("leverCobweb", lc);

            // Procenciarz
            JsonObject proc = new JsonObject();
            proc.addProperty("enabled", DurexClient.getProcenciarzModule().isEnabled());
            proc.addProperty("x", DurexClient.getProcenciarzModule().getHudX());
            proc.addProperty("y", DurexClient.getProcenciarzModule().getHudY());
            proc.addProperty("showBooks", pl.durex.client.module.ProcenciarzModule.isShowBooks());
            root.add("procenciarz", proc);

            // AutoDripstone
            JsonObject ad = new JsonObject();
            ad.addProperty("enabled", pl.durex.client.module.AutoDripstoneModule.isEnabled());
            ad.addProperty("speed", pl.durex.client.module.AutoDripstoneModule.getSpeed());
            root.add("autoDripstone", ad);

            // NoPush
            JsonObject np = new JsonObject();
            np.addProperty("enabled", pl.durex.client.module.NoPushModule.isEnabled());
            root.add("noPush", np);

            // Zbrojmistrz
            JsonObject zm = new JsonObject();
            zm.addProperty("enabled", pl.durex.client.module.ZbrojmistrzModule.isEnabled());
            zm.addProperty("x", pl.durex.client.module.ZbrojmistrzModule.getHudX());
            zm.addProperty("y", pl.durex.client.module.ZbrojmistrzModule.getHudY());
            zm.addProperty("showBooks", pl.durex.client.module.ZbrojmistrzModule.isShowBooks());
            root.add("zbrojmistrz", zm);

            // Nametags
            JsonObject nt = new JsonObject();
            nt.addProperty("enabled",      pl.durex.client.module.NametagsModule.isEnabled());
            nt.addProperty("showHp",       pl.durex.client.module.NametagsModule.isShowHp());
            nt.addProperty("showDistance", pl.durex.client.module.NametagsModule.isShowDistance());
            nt.addProperty("showArmor",    pl.durex.client.module.NametagsModule.isShowArmor());
            nt.addProperty("nickColorIdx", pl.durex.client.module.NametagsModule.getNickColorIdx());
            nt.addProperty("maxDistance",  pl.durex.client.module.NametagsModule.getMaxDistance());
            root.add("nametags", nt);

            // Tracers
            JsonObject tr = new JsonObject();
            tr.addProperty("enabled",     pl.durex.client.module.TracerModule.isEnabled());
            tr.addProperty("colorIdx",    pl.durex.client.module.TracerModule.getColorIdx());
            tr.addProperty("maxDistance", pl.durex.client.module.TracerModule.getMaxDistance());
            root.add("tracers", tr);

            // MsgBot
            JsonObject mb = new JsonObject();
            mb.addProperty("message",       pl.durex.client.module.MsgBotModule.message);
            mb.addProperty("targetPlayer",  pl.durex.client.module.MsgBotModule.targetPlayer);
            mb.addProperty("chDelaySec",    pl.durex.client.module.MsgBotModule.chDelaySec);
            mb.addProperty("onlyNetherite", pl.durex.client.module.MsgBotModule.onlyNetherite);
            mb.addProperty("onlyOffline",   pl.durex.client.module.MsgBotModule.onlyOffline);
            root.add("msgBot", mb);

            // Pozycje modułów GUI (floating/docked)
            if (guiModRx != null) {
                JsonArray modPos = new JsonArray();
                for (int i = 0; i < guiModRx.length; i++) {
                    JsonObject mp = new JsonObject();
                    mp.addProperty("rx", guiModRx[i]);
                    mp.addProperty("ry", guiModRy[i]);
                    mp.addProperty("docked", guiModDocked[i]);
                    modPos.add(mp);
                }
                root.add("guiModPos", modPos);
            }

            // GUI panel position
            root.addProperty("guiPanelX", pl.durex.client.gui.DurexClickGuiScreen.savedPanelX);
            root.addProperty("guiPanelY", pl.durex.client.gui.DurexClickGuiScreen.savedPanelY);

            // GUI layout - kategorie i wolne moduły
            JsonArray catsArr = new JsonArray();
            for (pl.durex.client.gui.DurexClickGuiScreen.CategoryWidget cat : pl.durex.client.gui.DurexClickGuiScreen.categories) {
                JsonObject co = new JsonObject();
                co.addProperty("name", cat.name);
                co.addProperty("rx", cat.rx); co.addProperty("ry", cat.ry);
                co.addProperty("docked", cat.docked);
                co.addProperty("expanded", cat.expanded);
                JsonArray modsArr = new JsonArray();
                for (pl.durex.client.gui.DurexClickGuiScreen.ModWidget m : cat.mods) {
                    JsonObject mo = new JsonObject();
                    mo.addProperty("id", m.id); mo.addProperty("icon", m.icon);
                    mo.addProperty("rx", m.rx); mo.addProperty("ry", m.ry);
                    mo.addProperty("expanded", m.expanded);
                    modsArr.add(mo);
                }
                co.add("mods", modsArr);
                catsArr.add(co);
            }
            root.add("guiCategories", catsArr);

            JsonArray freeArr = new JsonArray();
            for (pl.durex.client.gui.DurexClickGuiScreen.ModWidget m : pl.durex.client.gui.DurexClickGuiScreen.freeMods) {
                JsonObject mo = new JsonObject();
                mo.addProperty("id", m.id); mo.addProperty("icon", m.icon);
                mo.addProperty("rx", m.rx); mo.addProperty("ry", m.ry);
                mo.addProperty("expanded", m.expanded);
                freeArr.add(mo);
            }
            root.add("guiFreeMods", freeArr);

            // Flaga: layout zarządzany przez bibliotekę modułów (nie migruj automatycznie)
            root.addProperty("libraryManaged", true);

            Files.writeString(getPath(), GSON.toJson(root));
            System.out.println("[DurexConfig] Config saved to: " + getPath());
        } catch (Exception e) {
            System.out.println("[DurexConfig] Failed to save config: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Pozycje modułów GUI - ustawiane przez DurexClickGuiScreen
    public static float[] guiModRx = null;
    public static float[] guiModRy = null;
    public static boolean[] guiModDocked = null;

    public static void load() {
        System.out.println("[DurexConfig] Loading config...");
        try {
            Path path = getPath();
            System.out.println("[DurexConfig] Config path: " + path);
            if (!Files.exists(path)) {
                System.out.println("[DurexConfig] Config file doesn't exist, using defaults");
                return;
            }

            JsonObject root = GSON.fromJson(Files.readString(path), JsonObject.class);
            if (root == null) {
                System.out.println("[DurexConfig] Config file is empty or invalid");
                return;
            }
            System.out.println("[DurexConfig] Config loaded successfully");

            if (root.has("antiKowal")) {
                JsonObject o = root.getAsJsonObject("antiKowal");
                if (o.has("enabled")) DurexClient.getAntiKowalModule().setEnabled(o.get("enabled").getAsBoolean());
                if (o.has("bind")) trySetKey(o.get("bind").getAsString(), k -> DurexClient.getAntiKowalModule().setBind(k));
            }

            if (root.has("noPlayerClip")) {
                JsonObject o = root.getAsJsonObject("noPlayerClip");
                if (o.has("enabled")) pl.durex.client.util.RaycastState.active = o.get("enabled").getAsBoolean();
            }

            if (root.has("friendModule")) {
                JsonObject o = root.getAsJsonObject("friendModule");
                if (o.has("enabled")) DurexClient.getFriendModule().setEnabled(o.get("enabled").getAsBoolean());
                if (o.has("bind")) trySetKey(o.get("bind").getAsString(), k -> DurexClient.getFriendModule().setAddKey(k));
            }

            if (root.has("cooldownHud")) {
                JsonObject o = root.getAsJsonObject("cooldownHud");
                if (o.has("enabled")) DurexClient.getCooldownHudModule().setEnabled(o.get("enabled").getAsBoolean());
                if (o.has("x") && o.has("y"))
                    DurexClient.getCooldownHudModule().setHudPos(o.get("x").getAsInt(), o.get("y").getAsInt());
            }

            if (root.has("antiKostka")) {
                JsonObject o = root.getAsJsonObject("antiKostka");
                if (o.has("enabled")) DurexClient.getAntiKostkaModule().setEnabled(o.get("enabled").getAsBoolean());
                if (o.has("delayMode")) {
                    try { DurexClient.getAntiKostkaModule().setDelayMode(AntiKostkaModule.DelayMode.valueOf(o.get("delayMode").getAsString())); } catch (Exception ignored2) {}
                }
                if (o.has("slots")) {
                    JsonArray arr = o.getAsJsonArray("slots");
                    var slots = DurexClient.getAntiKostkaModule().getSlots();
                    slots.clear();
                    for (int i = 0; i < arr.size(); i++) {
                        JsonObject s = arr.get(i).getAsJsonObject();
                        String name = s.has("name") ? s.get("name").getAsString() : "Slot " + (i+1);
                        AntiKostkaModule.HotbarSlot slot = new AntiKostkaModule.HotbarSlot(name, org.lwjgl.glfw.GLFW.GLFW_KEY_F5 + i);
                        if (s.has("loadKey")) trySetKey(s.get("loadKey").getAsString(), k -> slot.loadKey = k);
                        if (s.has("hotbar")) {
                            JsonArray hotbar = s.getAsJsonArray("hotbar");
                            for (int j = 0; j < Math.min(9, hotbar.size()); j++)
                                slot.items[j] = deserializeStack(hotbar.get(j).getAsJsonObject());
                            slot.hasSaved = true;
                        }
                        slots.add(slot);
                    }
                }
            }

            // ViewModel
            if (root.has("viewModel")) {
                JsonObject o = root.getAsJsonObject("viewModel");
                var v = DurexClient.getViewModelModule();
                if (o.has("enabled")) v.setEnabled(o.get("enabled").getAsBoolean());
                if (o.has("rightRotX")) v.rightRotX = o.get("rightRotX").getAsFloat();
                if (o.has("rightRotY")) v.rightRotY = o.get("rightRotY").getAsFloat();
                if (o.has("rightRotZ")) v.rightRotZ = o.get("rightRotZ").getAsFloat();
                if (o.has("rightPosX")) v.rightPosX = o.get("rightPosX").getAsFloat();
                if (o.has("rightPosY")) v.rightPosY = o.get("rightPosY").getAsFloat();
                if (o.has("rightPosZ")) v.rightPosZ = o.get("rightPosZ").getAsFloat();
                if (o.has("rightScale")) v.rightScale = o.get("rightScale").getAsFloat();
                if (o.has("leftRotX")) v.leftRotX = o.get("leftRotX").getAsFloat();
                if (o.has("leftRotY")) v.leftRotY = o.get("leftRotY").getAsFloat();
                if (o.has("leftRotZ")) v.leftRotZ = o.get("leftRotZ").getAsFloat();
                if (o.has("leftPosX")) v.leftPosX = o.get("leftPosX").getAsFloat();
                if (o.has("leftPosY")) v.leftPosY = o.get("leftPosY").getAsFloat();
                if (o.has("leftPosZ")) v.leftPosZ = o.get("leftPosZ").getAsFloat();
                if (o.has("leftScale")) v.leftScale = o.get("leftScale").getAsFloat();
            }

            // LeverCobweb (Nemos Helper)
            if (root.has("leverCobweb")) {
                JsonObject o = root.getAsJsonObject("leverCobweb");
                if (o.has("enabled")) DurexClient.getLeverCobwebModule().setEnabled(o.get("enabled").getAsBoolean());
                if (o.has("holdLeverEnabled")) DurexClient.getLeverCobwebModule().setHoldLeverEnabled(o.get("holdLeverEnabled").getAsBoolean());
                if (o.has("switchToBestSwordEnabled")) DurexClient.getLeverCobwebModule().setSwitchToBestSwordEnabled(o.get("switchToBestSwordEnabled").getAsBoolean());
                if (o.has("playerModeEnabled")) DurexClient.getLeverCobwebModule().setPlayerModeEnabled(o.get("playerModeEnabled").getAsBoolean());
                if (o.has("webModeEnabled")) DurexClient.getLeverCobwebModule().setWebModeEnabled(o.get("webModeEnabled").getAsBoolean());
                if (o.has("leverOnlyModeEnabled")) DurexClient.getLeverCobwebModule().setLeverOnlyModeEnabled(o.get("leverOnlyModeEnabled").getAsBoolean());
                if (o.has("speedProfile")) {
                    try {
                        DurexClient.getLeverCobwebModule().setSpeedProfile(
                            pl.durex.client.module.LeverCobwebModule.SpeedProfile.valueOf(o.get("speedProfile").getAsString())
                        );
                    } catch (Exception ignored2) {}
                }
            }

            // Procenciarz
            if (root.has("procenciarz")) {
                JsonObject o = root.getAsJsonObject("procenciarz");
                if (o.has("enabled")) DurexClient.getProcenciarzModule().setEnabled(o.get("enabled").getAsBoolean());
                if (o.has("x") && o.has("y"))
                    DurexClient.getProcenciarzModule().setHudPos(o.get("x").getAsInt(), o.get("y").getAsInt());
                if (o.has("showBooks")) pl.durex.client.module.ProcenciarzModule.setShowBooks(o.get("showBooks").getAsBoolean());
            }

            // AutoDripstone
            if (root.has("autoDripstone")) {
                JsonObject o = root.getAsJsonObject("autoDripstone");
                if (o.has("enabled") && o.get("enabled").getAsBoolean()) pl.durex.client.module.AutoDripstoneModule.toggle();
                if (o.has("speed")) pl.durex.client.module.AutoDripstoneModule.setSpeed(o.get("speed").getAsInt());
            }

            // NoPush
            if (root.has("noPush")) {
                JsonObject o = root.getAsJsonObject("noPush");
                if (o.has("enabled") && o.get("enabled").getAsBoolean()) pl.durex.client.module.NoPushModule.toggle();
            }

            // Zbrojmistrz
            if (root.has("zbrojmistrz")) {
                JsonObject o = root.getAsJsonObject("zbrojmistrz");
                if (o.has("enabled")) pl.durex.client.module.ZbrojmistrzModule.setEnabled(o.get("enabled").getAsBoolean());
                if (o.has("x") && o.has("y"))
                    pl.durex.client.module.ZbrojmistrzModule.setHudPos(o.get("x").getAsInt(), o.get("y").getAsInt());
                if (o.has("showBooks")) pl.durex.client.module.ZbrojmistrzModule.setShowBooks(o.get("showBooks").getAsBoolean());
            }

            // Nametags
            if (root.has("nametags")) {
                JsonObject o = root.getAsJsonObject("nametags");
                if (o.has("enabled"))      pl.durex.client.module.NametagsModule.setEnabled(o.get("enabled").getAsBoolean());
                if (o.has("showHp"))       pl.durex.client.module.NametagsModule.setShowHp(o.get("showHp").getAsBoolean());
                if (o.has("showDistance")) pl.durex.client.module.NametagsModule.setShowDistance(o.get("showDistance").getAsBoolean());
                if (o.has("showArmor"))    pl.durex.client.module.NametagsModule.setShowArmor(o.get("showArmor").getAsBoolean());
                if (o.has("nickColorIdx")) pl.durex.client.module.NametagsModule.setNickColorIdx(o.get("nickColorIdx").getAsInt());
                if (o.has("maxDistance"))  pl.durex.client.module.NametagsModule.setMaxDistance(o.get("maxDistance").getAsFloat());
            }

            // Tracers
            if (root.has("tracers")) {
                JsonObject o = root.getAsJsonObject("tracers");
                if (o.has("enabled"))     pl.durex.client.module.TracerModule.setEnabled(o.get("enabled").getAsBoolean());
                if (o.has("colorIdx"))    pl.durex.client.module.TracerModule.setColorIdx(o.get("colorIdx").getAsInt());
                if (o.has("maxDistance")) pl.durex.client.module.TracerModule.setMaxDistance(o.get("maxDistance").getAsFloat());
            }

            // MsgBot
            if (root.has("msgBot")) {
                JsonObject o = root.getAsJsonObject("msgBot");
                if (o.has("message"))       pl.durex.client.module.MsgBotModule.message       = o.get("message").getAsString();
                if (o.has("targetPlayer"))  pl.durex.client.module.MsgBotModule.targetPlayer  = o.get("targetPlayer").getAsString();
                if (o.has("chDelaySec"))    pl.durex.client.module.MsgBotModule.chDelaySec    = o.get("chDelaySec").getAsInt();
                if (o.has("onlyNetherite")) pl.durex.client.module.MsgBotModule.onlyNetherite = o.get("onlyNetherite").getAsBoolean();
                if (o.has("onlyOffline"))   pl.durex.client.module.MsgBotModule.onlyOffline   = o.get("onlyOffline").getAsBoolean();
            }

            // Pozycje modułów GUI
            if (root.has("guiModPos")) {
                JsonArray arr = root.getAsJsonArray("guiModPos");
                int len = arr.size();
                guiModRx = new float[len];
                guiModRy = new float[len];
                guiModDocked = new boolean[len];
                for (int i = 0; i < len; i++) {
                    JsonObject mp = arr.get(i).getAsJsonObject();
                    guiModRx[i] = mp.has("rx") ? mp.get("rx").getAsFloat() : 0;
                    guiModRy[i] = mp.has("ry") ? mp.get("ry").getAsFloat() : 0;
                    guiModDocked[i] = !mp.has("docked") || mp.get("docked").getAsBoolean();
                }
            }

            // GUI panel position
            if (root.has("guiPanelX")) pl.durex.client.gui.DurexClickGuiScreen.savedPanelX = root.get("guiPanelX").getAsInt();
            if (root.has("guiPanelY")) pl.durex.client.gui.DurexClickGuiScreen.savedPanelY = root.get("guiPanelY").getAsInt();

            // GUI layout - kategorie i wolne moduły
            if (root.has("guiCategories")) {
                pl.durex.client.gui.DurexClickGuiScreen.categories.clear();
                for (var el : root.getAsJsonArray("guiCategories")) {
                    JsonObject co = el.getAsJsonObject();
                    var cat = new pl.durex.client.gui.DurexClickGuiScreen.CategoryWidget(co.has("name") ? co.get("name").getAsString() : "Kategoria");
                    cat.rx = co.has("rx") ? co.get("rx").getAsFloat() : 0;
                    cat.ry = co.has("ry") ? co.get("ry").getAsFloat() : 0;
                    cat.tx = cat.rx; cat.ty = cat.ry;
                    cat.docked = !co.has("docked") || co.get("docked").getAsBoolean();
                    cat.expanded = !co.has("expanded") || co.get("expanded").getAsBoolean();
                    cat.expandAnim = cat.expanded ? 1f : 0f;
                    if (co.has("mods")) {
                        for (var mel : co.getAsJsonArray("mods")) {
                            JsonObject mo = mel.getAsJsonObject();
                            var m = new pl.durex.client.gui.DurexClickGuiScreen.ModWidget(
                                mo.has("id") ? mo.get("id").getAsString() : "?",
                                mo.has("icon") ? mo.get("icon").getAsString() : "?"
                            );
                            m.rx = mo.has("rx") ? mo.get("rx").getAsFloat() : 0;
                            m.ry = mo.has("ry") ? mo.get("ry").getAsFloat() : 0;
                            m.tx = m.rx; m.ty = m.ry;
                            m.expanded = mo.has("expanded") && mo.get("expanded").getAsBoolean();
                            m.owner = cat;
                            cat.mods.add(m);
                        }
                    }
                    pl.durex.client.gui.DurexClickGuiScreen.categories.add(cat);
                }
            }
            if (root.has("guiFreeMods")) {
                pl.durex.client.gui.DurexClickGuiScreen.freeMods.clear();
                for (var el : root.getAsJsonArray("guiFreeMods")) {
                    JsonObject mo = el.getAsJsonObject();
                    var m = new pl.durex.client.gui.DurexClickGuiScreen.ModWidget(
                        mo.has("id") ? mo.get("id").getAsString() : "?",
                        mo.has("icon") ? mo.get("icon").getAsString() : "?"
                    );
                    m.rx = mo.has("rx") ? mo.get("rx").getAsFloat() : 0;
                    m.ry = mo.has("ry") ? mo.get("ry").getAsFloat() : 0;
                    m.tx = m.rx; m.ty = m.ry;
                    m.expanded = mo.has("expanded") && mo.get("expanded").getAsBoolean();
                    pl.durex.client.gui.DurexClickGuiScreen.freeMods.add(m);
                }
            }

            // Migracja: dodaj nowe moduły tylko jeśli config był zarządzany przez starą wersję
            // (nie przez nową bibliotekę modułów). Flaga "libraryManaged" = użytkownik sam zarządza.
            boolean libraryManaged = root.has("libraryManaged") && root.get("libraryManaged").getAsBoolean();
            boolean hadLayout = root.has("guiFreeMods") || root.has("guiCategories");
            if (hadLayout && !libraryManaged) migrateNewModules();

        } catch (Exception e) {
            System.out.println("[DurexConfig] Failed to load config: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ── Migracja layoutu GUI ──────────────────────────────────────────────

    /** Wszystkie znane ID modułów z ich domyślnymi ikonami */
    private static final String[][] ALL_MODULES = {
        {"antiKowal",     "⚔"},
        {"cooldowns",     "⏱"},
        {"antiKostka",    "🎒"},
        {"viewModel",     "👁"},
        {"procenciarz",   "%"},
        {"leverCobweb",   "🕸"},
        {"autoDripstone", "💧"},
        {"noPush",        "🛡"},
        {"msgBot",        "✉"},
        {"zbrojmistrz",   "⚔"},
        {"nametags",      "🏷"},
        {"tracers",       "→"},
    };

    /**
     * Sprawdza czy każdy znany moduł istnieje w GUI.
     * Jeśli nie — dodaje go jako floating mod (nie rusza istniejącego layoutu).
     */
    private static void migrateNewModules() {
        for (String[] mod : ALL_MODULES) {
            String id = mod[0], icon = mod[1];
            if (!isModInGui(id)) {
                var m = new pl.durex.client.gui.DurexClickGuiScreen.ModWidget(id, icon);
                // Ustaw pozycję poniżej ostatniego floating moda
                float fy = 60;
                for (var existing : pl.durex.client.gui.DurexClickGuiScreen.freeMods) {
                    fy = Math.max(fy, existing.ry + 26);
                }
                m.rx = 20; m.ry = fy; m.tx = 20; m.ty = fy;
                pl.durex.client.gui.DurexClickGuiScreen.freeMods.add(m);
                System.out.println("[DurexConfig] Migrated new module: " + id);
            }
        }
    }

    private static boolean isModInGui(String id) {
        for (var m : pl.durex.client.gui.DurexClickGuiScreen.freeMods) {
            if (id.equals(m.id)) return true;
        }
        for (var cat : pl.durex.client.gui.DurexClickGuiScreen.categories) {
            for (var m : cat.mods) {
                if (id.equals(m.id)) return true;
            }
        }
        return false;
    }

    // ── Serializacja ItemStack ─────────────────────────────────────────────

    private static JsonObject serializeStack(ItemStack stack) {
        JsonObject obj = new JsonObject();
        if (stack.isEmpty()) {
            obj.addProperty("empty", true);
            return obj;
        }
        obj.addProperty("id", Registries.ITEM.getId(stack.getItem()).toString());
        obj.addProperty("count", stack.getCount());
        return obj;
    }

    private static ItemStack deserializeStack(JsonObject obj) {
        if (obj.has("empty") && obj.get("empty").getAsBoolean()) return ItemStack.EMPTY;
        try {
            String id = obj.get("id").getAsString();
            int count = obj.has("count") ? obj.get("count").getAsInt() : 1;
            var item = Registries.ITEM.get(Identifier.of(id));
            return new ItemStack(item, count);
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }

    // ── Helper ────────────────────────────────────────────────────────────

    private static void trySetKey(String translationKey, java.util.function.Consumer<InputUtil.Key> setter) {
        try {
            setter.accept(InputUtil.fromTranslationKey(translationKey));
        } catch (Exception ignored) {}
    }
}
