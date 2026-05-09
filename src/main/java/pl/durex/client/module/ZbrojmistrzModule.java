package pl.durex.client.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ZbrojmistrzModule {

    private static boolean enabled    = false;
    private static boolean showBooks  = false;
    private static int hudX = 10;
    private static int hudY = 120;

    private static final Pattern PLUS_PATTERN = Pattern.compile("\\+(\\d+)");

    // Vanilla enchanty — szare §7, pomijamy
    private static final Pattern VANILLA_ENCHANT_PATTERN = Pattern.compile(
        "^(Protection|Unbreaking|Mending|Sharpness|Smite|Looting|Fortune|Silk Touch|" +
        "Efficiency|Power|Punch|Flame|Infinity|Feather Falling|Blast Protection|" +
        "Projectile Protection|Fire Protection|Respiration|Aqua Affinity|Thorns|" +
        "Depth Strider|Frost Walker|Curse of Binding|Curse of Vanishing|" +
        "Sweeping Edge|Knockback|Fire Aspect|Luck of the Sea|Lure|" +
        "Channeling|Riptide|Loyalty|Impaling|Multishot|Piercing|Quick Charge|" +
        "Soul Speed|Swift Sneak|Wind Burst|Breach|Density).*",
        Pattern.CASE_INSENSITIVE
    );

    // Statystyki — linie zaczynające się od cyfry lub znaku + z liczbą dziesiętną
    // np. "+5.0 odporności na obrazenia", "15% wolniejsze zużycia"
    private static final Pattern STAT_PATTERN = Pattern.compile(
        "^[+\\-]?\\d+[.,]?\\d*\\s*%?\\s+\\S|^\\d+%"
    );

    public static boolean isEnabled()             { return enabled; }
    public static void    setEnabled(boolean v)   { enabled = v; }
    public static boolean isShowBooks()           { return showBooks; }
    public static void    setShowBooks(boolean v) { showBooks = v; }
    public static int     getHudX()               { return hudX; }
    public static int     getHudY()               { return hudY; }
    public static void    setHudPos(int x, int y) { hudX = x; hudY = y; }

    /** Dane jednego elementu zbroi */
    public record ItemData(
        String name,           // np. "Anarchiczna klata II"
        String plus,           // np. "+7" lub null
        List<String> books     // custom enchanty (kolorowe linie z lore), max 2
    ) {}

    /** Pełne dane gracza */
    public record ArmorInfo(
        String playerName,
        ItemData helmet,
        ItemData chest,
        ItemData legs,
        ItemData boots
    ) {}

    public static ArmorInfo getTarget(MinecraftClient client) {
        if (!enabled || client.player == null || client.world == null) return null;

        LivingEntity target = findTarget(client);
        if (target == null) return null;

        String name = target.getName().getString();
        ItemData helmet = parseItem(target.getEquippedStack(EquipmentSlot.HEAD));
        ItemData chest  = parseItem(target.getEquippedStack(EquipmentSlot.CHEST));
        ItemData legs   = parseItem(target.getEquippedStack(EquipmentSlot.LEGS));
        ItemData boots  = parseItem(target.getEquippedStack(EquipmentSlot.FEET));

        boolean anyArmor = helmet != null || chest != null || legs != null || boots != null;
        if (!anyArmor) return null;

        return new ArmorInfo(name, helmet, chest, legs, boots);
    }

    private static ItemData parseItem(ItemStack stack) {
        if (stack.isEmpty()) return null;

        // Nazwa itemu
        String itemName;
        Text customName = stack.get(DataComponentTypes.CUSTOM_NAME);
        if (customName != null) {
            itemName = stripColor(customName.getString());
        } else {
            itemName = stripColor(stack.getName().getString());
        }

        // Plus z nazwy (np. "+7") — szukaj też w lore jeśli nie ma w nazwie
        String plus = findPlus(itemName);
        if (plus == null) {
            LoreComponent loreCheck = stack.get(DataComponentTypes.LORE);
            if (loreCheck != null) {
                for (Text line : loreCheck.lines()) {
                    String lineText = stripColor(line.getString());
                    plus = findPlus(lineText);
                    if (plus != null) break;
                }
            }
        }
        String cleanName = itemName.replaceAll("\\s*\\+\\d+", "").trim();

        // Księgi = kolorowe custom enchanty z lore
        List<String> books = new ArrayList<>();
        if (showBooks) {
            LoreComponent lore = stack.get(DataComponentTypes.LORE);
            if (lore != null) {
                for (Text line : lore.styledLines()) {
                    String raw = line.getString();
                    String clean = stripColor(raw).trim();

                    if (clean.isEmpty()) continue;

                    // Pomiń szare linie (vanilla enchanty §7)
                    if (raw.startsWith("§7")) continue;

                    // Pomiń statystyki (linie z liczbami dziesiętnymi jak "+5.0 odporności")
                    if (STAT_PATTERN.matcher(clean).find()) continue;

                    // Pomiń vanilla enchanty po nazwie
                    if (VANILLA_ENCHANT_PATTERN.matcher(clean).matches()) continue;

                    // Pomiń "When in main/off hand:" itp.
                    if (clean.startsWith("When in") || clean.startsWith("When on")) continue;

                    // To jest "księga" — kolorowy custom enchant
                    books.add(clean);
                }
            }
        }

        return new ItemData(cleanName, plus, books);
    }

    private static String findPlus(String text) {
        if (text == null) return null;
        Matcher m = PLUS_PATTERN.matcher(text);
        return m.find() ? "+" + m.group(1) : null;
    }

    private static String stripColor(String text) {
        if (text == null) return "";
        return text.replaceAll("§[0-9a-fk-orA-FK-OR]", "");
    }

    private static LivingEntity findTarget(MinecraftClient client) {
        if (client.crosshairTarget instanceof EntityHitResult hit
                && hit.getEntity() instanceof LivingEntity e) {
            return e;
        }

        Vec3d eyes = client.player.getCameraPosVec(1f);
        Vec3d look = client.player.getRotationVec(1f);
        Vec3d end  = eyes.add(look.multiply(8.0));
        Box box    = client.player.getBoundingBox().stretch(look.multiply(8.0)).expand(1);

        LivingEntity closest = null;
        double closestDist   = Double.MAX_VALUE;

        for (var entity : client.world.getOtherEntities(client.player, box)) {
            if (!(entity instanceof LivingEntity living) || !living.isAlive()) continue;
            var result = living.getBoundingBox().expand(0.3).raycast(eyes, end);
            if (result.isEmpty()) continue;
            double dist = eyes.squaredDistanceTo(result.get());
            if (dist < closestDist) { closestDist = dist; closest = living; }
        }

        return closest;
    }
}
