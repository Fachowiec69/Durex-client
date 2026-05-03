package pl.durex.client.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Nametags — własne tagi nad głowami graczy.
 * Opcje: HP, dystans, zbroja (+poziom), kolor nicku
 */
public class NametagsModule {

    private static boolean enabled      = false;
    private static boolean showHp       = true;
    private static boolean showDistance = true;
    private static boolean showArmor    = true;
    private static float   maxDistance  = 64f;

    // Kolory nicku: 0=biały, 1=żółty, 2=zielony, 3=czerwony, 4=fioletowy
    private static int nickColorIdx = 0;
    public static final String[] NICK_COLORS     = {"§f", "§e", "§a", "§c", "§d"};
    public static final String[] NICK_COLOR_NAMES = {"Biały", "Żółty", "Zielony", "Czerwony", "Fioletowy"};

    private static final Pattern PLUS_PATTERN = Pattern.compile("\\+(\\d+)");

    public static boolean isEnabled()              { return enabled; }
    public static void    setEnabled(boolean v)    { enabled = v; }
    public static boolean isShowHp()               { return showHp; }
    public static void    setShowHp(boolean v)     { showHp = v; }
    public static boolean isShowDistance()         { return showDistance; }
    public static void    setShowDistance(boolean v){ showDistance = v; }
    public static boolean isShowArmor()            { return showArmor; }
    public static void    setShowArmor(boolean v)  { showArmor = v; }
    public static float   getMaxDistance()         { return maxDistance; }
    public static void    setMaxDistance(float v)  { maxDistance = Math.max(8f, Math.min(256f, v)); }
    public static int     getNickColorIdx()        { return nickColorIdx; }
    public static void    setNickColorIdx(int v)   { nickColorIdx = ((v % NICK_COLORS.length) + NICK_COLORS.length) % NICK_COLORS.length; }
    public static String  getNickColor()           { return NICK_COLORS[nickColorIdx]; }

    /** Dane nametaga dla jednego gracza */
    public record NametagData(
        String name,
        float hp,
        float maxHp,
        float distance,
        String[] armorPlus   // [helm, chest, legs, boots] lub null
    ) {}

    public static NametagData getData(PlayerEntity player, MinecraftClient client) {
        if (client.player == null) return null;

        String name = player.getName().getString();
        float hp    = player.getHealth();
        float maxHp = player.getMaxHealth();
        float dist  = (float) client.player.distanceTo(player);

        String[] armorPlus = null;
        if (showArmor) {
            armorPlus = new String[4];
            EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
            for (int i = 0; i < 4; i++) {
                ItemStack stack = player.getEquippedStack(slots[i]);
                armorPlus[i] = stack.isEmpty() ? null : findPlus(stack);
            }
        }

        return new NametagData(name, hp, maxHp, dist, armorPlus);
    }

    private static String findPlus(ItemStack stack) {
        // Szukaj w nazwie
        Text customName = stack.get(DataComponentTypes.CUSTOM_NAME);
        String itemName = customName != null
            ? stripColor(customName.getString())
            : stripColor(stack.getName().getString());

        Matcher m = PLUS_PATTERN.matcher(itemName);
        if (m.find()) return "+" + m.group(1);

        // Szukaj w lore
        LoreComponent lore = stack.get(DataComponentTypes.LORE);
        if (lore != null) {
            for (Text line : lore.lines()) {
                String text = stripColor(line.getString());
                m = PLUS_PATTERN.matcher(text);
                if (m.find()) return "+" + m.group(1);
            }
        }
        return null;
    }

    private static String stripColor(String text) {
        if (text == null) return "";
        return text.replaceAll("§[0-9a-fk-orA-FK-OR]", "");
    }

    /** Kolor HP: zielony > żółty > czerwony */
    public static int hpColor(float hp, float maxHp) {
        float pct = maxHp > 0 ? hp / maxHp : 0;
        if (pct > 0.6f) return 0xFF55FF88;
        if (pct > 0.3f) return 0xFFFFCC44;
        return 0xFFFF4455;
    }

    /** Kolor plusa zbroi */
    public static String plusColor(String plus) {
        if (plus == null) return "§8";
        try {
            int lvl = Integer.parseInt(plus.replace("+", "").trim());
            if (lvl >= 7) return "§c";
            if (lvl >= 5) return "§6";
            if (lvl >= 3) return "§e";
            return "§f";
        } catch (NumberFormatException e) { return "§f"; }
    }
}
