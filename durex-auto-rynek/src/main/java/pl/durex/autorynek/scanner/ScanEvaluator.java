package pl.durex.autorynek.scanner;

import pl.durex.autorynek.config.PriceEntry;
import pl.durex.autorynek.config.ServerProfile;
import pl.durex.autorynek.util.PriceParser;

import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Inteligentna ocena oferty rynkowej.
 *
 * Logika wzorowana na BK-Rynek ScanEvaluator:
 *  - cache skompilowanych patternów (brak re-kompilacji co slot)
 *  - scoring dopasowania (material > name > lore > enchants)
 *  - obsługa enchantments, excludedEnchants, lore, componentCount, customModelData
 *  - parsowanie daty wygaśnięcia oferty (minDaysLeft)
 *  - obsługa stackSize (cena jednostkowa vs. sumaryczna)
 */
public final class ScanEvaluator {

    private static final Map<String, Pattern> PATTERN_CACHE = new ConcurrentHashMap<>();

    private ScanEvaluator() {}

    private static Pattern getPattern(String regex) {
        return PATTERN_CACHE.computeIfAbsent(regex, r -> Pattern.compile(r, Pattern.CASE_INSENSITIVE));
    }

    /**
     * Ocenia slot rynku.
     *
     * @param input   dane slotu
     * @param profile profil serwera
     * @param sumMode true = porównuj cenę całego stacka, false = cenę jednostkową
     * @return wynik oceny
     */
    public static ScanResult evaluate(ScanInput input, ServerProfile profile, boolean sumMode) {
        if (input == null || profile == null) return ScanResult.noHighlight();

        // 1. Wyciągnij cenę z lore
        double foundPrice = extractPriceFromLore(input.loreLines, profile.loreRegex);
        if (foundPrice < 0) {
            return ScanResult.noHighlight();
        }

        // 2. Cena jednostkowa vs. sumaryczna
        boolean isStack = input.stackSize > 1;
        double unitPrice = isStack ? foundPrice / input.stackSize : foundPrice;
        double comparisonPrice = sumMode ? foundPrice : unitPrice;

        // 3. Parsuj datę wygaśnięcia (jeśli jest w lore)
        Integer daysLeft = extractDaysLeft(input.loreLines);

        // 4. Znajdź najlepiej pasujący wpis cenowy (scoring)
        PriceEntry entry = findBestMatch(input, profile, daysLeft);
        if (entry == null) {
            return ScanResult.noHighlight();
        }

        // 5. Sprawdź czy cena mieści się w limicie
        double maxPrice = entry.maxPrice;
        if (maxPrice <= 0) return ScanResult.noHighlight();

        if (comparisonPrice <= maxPrice + 1e-4) {
            // Dla sumMode sprawdź też requiredCount
            if (sumMode && entry.requiredCount != null && entry.requiredCount > 0
                    && input.stackSize != entry.requiredCount) {
                return ScanResult.noHighlight();
            }
            return new ScanResult(true, foundPrice, maxPrice, entry);
        }

        return ScanResult.noHighlight();
    }

    // ── Wyciąganie ceny z lore ────────────────────────────────────────────────

    private static double extractPriceFromLore(List<String> loreLines, String loreRegex) {
        if (loreRegex == null || loreRegex.isEmpty()) return -1;
        Pattern p;
        try { p = getPattern(loreRegex); }
        catch (Exception e) { return -1; }

        for (String line : loreLines) {
            Matcher m = p.matcher(line);
            if (m.find()) {
                try {
                    double price = PriceParser.parse(m.group(1));
                    if (price >= 0) return price;
                } catch (Exception ignored) {}
            }
        }
        return -1;
    }

    // ── Parsowanie daty wygaśnięcia ───────────────────────────────────────────

    private static Integer extractDaysLeft(List<String> loreLines) {
        for (String line : loreLines) {
            String lower = line.toLowerCase();
            if (!lower.contains("wygaśnie:") && !lower.contains("wygasnie:")) continue;
            try {
                String dateStr = line.substring(line.indexOf(":") + 1).trim();
                if (dateStr.contains(",")) dateStr = dateStr.substring(0, dateStr.indexOf(",")).trim();
                String[] parts = dateStr.split("-");
                if (parts.length != 3) break;
                int day   = Integer.parseInt(parts[0].trim());
                int month = Integer.parseInt(parts[1].trim());
                int year  = Integer.parseInt(parts[2].trim());
                Calendar cal = Calendar.getInstance();
                long now = cal.getTimeInMillis();
                cal.set(year, month - 1, day, 23, 59, 59);
                long expiry = cal.getTimeInMillis();
                return (int) ((expiry - now) / 86_400_000L);
            } catch (Exception ignored) {}
            break;
        }
        return null;
    }

    // ── Scoring dopasowania ───────────────────────────────────────────────────

    /**
     * Scoring (wyższy = lepsze dopasowanie):
     *  material match      = +1000
     *  name exact match    = +500 + len + 200
     *  name material match = +500 + len
     *  lore match          = +50 per part
     *  enchants match      = +25 per enchant
     *  customModelData     = +2000
     *  componentCount      = wymagane minimum
     */
    private static PriceEntry findBestMatch(ScanInput input, ServerProfile profile, Integer daysLeft) {
        if (profile.prices == null || profile.prices.isEmpty()) return null;

        PriceEntry best = null;
        int bestScore = -1;

        String lowerMaterial    = input.materialId.toLowerCase();
        String lowerEnchantments = input.enchantments.toLowerCase();

        for (PriceEntry pe : profile.prices) {
            if (!Boolean.TRUE.equals(pe.buyEnabled)) continue;

            // Filtr minDaysLeft
            if (daysLeft != null && pe.minDaysLeft != null && daysLeft < pe.minDaysLeft) continue;

            // Filtr material
            if (pe.material != null && !pe.material.isEmpty()) {
                String m1 = lowerMaterial.startsWith("minecraft:") ? lowerMaterial.substring(10) : lowerMaterial;
                String m2 = pe.material.toLowerCase();
                if (m2.startsWith("minecraft:")) m2 = m2.substring(10);
                if (!m1.equals(m2)) continue;
            }

            // Filtr customModelData
            if (pe.customModelData != null && !pe.customModelData.equals(input.customModelData)) continue;

            // Filtr componentCount
            if (pe.componentCount > 0 && input.componentCount < pe.componentCount) continue;

            int score = 0;

            // Material score
            if (pe.material != null && !pe.material.isEmpty()) score += 1000;

            // Name score — strip color codes from pe.name before comparing
            if (pe.name != null && !pe.name.isEmpty()) {
                // Usuń kody kolorów z nazwy w configu (§5§lBombarda maxima -> Bombarda maxima)
                String cleanPeName = pe.name.replaceAll("§[0-9a-fk-orA-FK-OR]", "").trim();
                if (input.noColorName.equalsIgnoreCase(cleanPeName)) {
                    score += Math.min(500, cleanPeName.length()) + 200;
                } else {
                    // Sprawdź czy nazwa pasuje do material ID
                    String lowerEntryName = cleanPeName.toLowerCase();
                    String normalizedName = lowerEntryName.replace(" ", "_");
                    if (lowerMaterial.contains(lowerEntryName) || lowerMaterial.contains(normalizedName)) {
                        score += Math.min(500, cleanPeName.length());
                    } else {
                        continue; // name zdefiniowany ale nie pasuje
                    }
                }
            }

            // Lore score
            if (pe.lore != null && !pe.lore.isEmpty()) {
                String[] loreParts = pe.lore.split(";");
                boolean allFound = true;
                for (String part : loreParts) {
                    if (part.trim().isEmpty()) continue;
                    boolean found = false;
                    for (String line : input.loreLines) {
                        if (line.toLowerCase().contains(part.toLowerCase())) { found = true; break; }
                    }
                    if (!found) { allFound = false; break; }
                }
                if (!allFound) continue;
                score += 50;
            }

            // Enchants score
            if (pe.enchants != null && !pe.enchants.isEmpty()) {
                String[] required = pe.enchants.toLowerCase().split(",");
                boolean allFound = true;
                for (String req : required) {
                    if (req.trim().isEmpty()) continue;
                    if (!lowerEnchantments.contains(req.trim())) { allFound = false; break; }
                }
                if (!allFound) continue;
                score += 25;
            }

            // Excluded enchants — jeśli przedmiot MA wykluczone zaklęcie, pomiń
            if (pe.excludedEnchants != null && !pe.excludedEnchants.isEmpty()) {
                String[] excluded = pe.excludedEnchants.toLowerCase().split(",");
                boolean foundExcluded = false;
                for (String ex : excluded) {
                    if (ex.trim().isEmpty()) continue;
                    if (lowerEnchantments.contains(ex.trim())) { foundExcluded = true; break; }
                }
                if (foundExcluded) continue;
            }

            // customModelData bonus
            if (pe.customModelData != null) score += 2000;

            if (score > bestScore) {
                bestScore = score;
                best = pe;
            }
        }

        return best;
    }
}
