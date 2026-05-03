package pl.durex.client.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
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

public final class ProcenciarzModule {

    private boolean enabled   = false;
    private static boolean showBooks = false;
    private int hudX = -1; // -1 = auto (prawy bok)
    private int hudY = 10;

    private static final Pattern PERCENT_PATTERN  = Pattern.compile("(-?\\d+)\\s*%");
    private static final Pattern SPECIFIC_PATTERN = Pattern.compile("Dodatkowe obra.enia\\s+(-?\\d+)%");

    // Vanilla enchanty — pomijamy przy szukaniu ksiąg
    private static final Pattern VANILLA_ENCHANT = Pattern.compile(
        "^(Protection|Unbreaking|Mending|Sharpness|Smite|Looting|Fortune|Silk Touch|" +
        "Efficiency|Power|Punch|Flame|Infinity|Feather Falling|Blast Protection|" +
        "Projectile Protection|Fire Protection|Respiration|Aqua Affinity|Thorns|" +
        "Depth Strider|Frost Walker|Curse of Binding|Curse of Vanishing|" +
        "Sweeping Edge|Knockback|Fire Aspect|Luck of the Sea|Lure|" +
        "Channeling|Riptide|Loyalty|Impaling|Multishot|Piercing|Quick Charge|" +
        "Soul Speed|Swift Sneak|Wind Burst|Breach|Density).*",
        Pattern.CASE_INSENSITIVE
    );

    public boolean isEnabled()              { return enabled; }
    public void    setEnabled(boolean v)    { enabled = v; }
    public static boolean isShowBooks()     { return showBooks; }
    public static void setShowBooks(boolean v) { showBooks = v; }
    public int  getHudX()                   { return hudX; }
    public int  getHudY()                   { return hudY; }
    public void setHudPos(int x, int y)     { hudX = x; hudY = y; }

    public record TargetInfo(String percentText, ItemStack item, List<String> books) {}

    public TargetInfo getTarget(MinecraftClient client) {
        if (client.player == null || client.world == null) return null;

        LivingEntity target = findTarget(client);
        if (target == null) return null;

        ItemStack mainHand = target.getMainHandStack();
        ItemStack offHand  = target.getOffHandStack();

        for (ItemStack stack : new ItemStack[]{mainHand, offHand}) {
            if (stack.isEmpty()) continue;
            String found = findPercentInLore(stack);
            if (found != null) {
                List<String> books = showBooks ? findBooks(stack) : List.of();
                return new TargetInfo(found, stack.copy(), books);
            }
        }

        return null;
    }

    private String findPercentInLore(ItemStack stack) {
        LoreComponent lore = stack.get(DataComponentTypes.LORE);
        if (lore != null) {
            for (Text line : lore.lines()) {
                String text = line.getString();
                Matcher m = SPECIFIC_PATTERN.matcher(text);
                if (m.find()) return m.group(1) + "%";
                m = PERCENT_PATTERN.matcher(text);
                if (m.find()) return m.group(1) + "%";
            }
            for (Text line : lore.styledLines()) {
                String text = line.getString();
                Matcher m = SPECIFIC_PATTERN.matcher(text);
                if (m.find()) return m.group(1) + "%";
                m = PERCENT_PATTERN.matcher(text);
                if (m.find()) return m.group(1) + "%";
            }
        }
        return null;
    }

    /** Szuka kolorowych custom enchantów (ksiąg) w lore — pomija szare vanilla, statystyki i linie z % */
    private List<String> findBooks(ItemStack stack) {
        List<String> books = new ArrayList<>();
        LoreComponent lore = stack.get(DataComponentTypes.LORE);
        if (lore == null) return books;

        for (Text line : lore.styledLines()) {
            String raw   = line.getString();
            String clean = raw.replaceAll("§[0-9a-fk-orA-FK-OR]", "").trim();
            if (clean.isEmpty()) continue;
            // Szare/ciemne linie = vanilla enchanty lub opisy
            if (raw.startsWith("§7") || raw.startsWith("§8")) continue;
            // Vanilla enchanty po nazwie
            if (VANILLA_ENCHANT.matcher(clean).matches()) continue;
            // Linie z procentem = to jest linia z DMG%, nie księga
            if (clean.contains("%")) continue;
            // Statystyki liczbowe
            if (clean.matches("^[+\\-]?\\d+[.,]?\\d*.*")) continue;
            // Opisy przedmiotów (zaczynają się od "»" lub "Jest to" itp.)
            if (clean.startsWith("»") || clean.startsWith("Jest to") || clean.startsWith("Posiada")) continue;
            if (clean.startsWith("When in") || clean.startsWith("When on")) continue;
            books.add(clean);
        }
        return books;
    }

    private LivingEntity findTarget(MinecraftClient client) {
        if (client.crosshairTarget instanceof EntityHitResult hit
                && hit.getEntity() instanceof LivingEntity e) {
            return e;
        }

        Vec3d eyes = client.player.getCameraPosVec(1f);
        Vec3d look = client.player.getRotationVec(1f);
        Vec3d end  = eyes.add(look.multiply(6.0));
        Box box    = client.player.getBoundingBox().stretch(look.multiply(6.0)).expand(1);

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
