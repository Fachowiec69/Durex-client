package pl.durex.autorynek.util;

public class PriceParser {

    public static double parse(String raw) {
        if (raw == null || raw.isBlank()) return 0;
        try {
            String clean = raw.trim()
                .replace(" ", "")
                .replace("\u00A0", "")
                .replace(",", ".");

            double multiplier = 1;
            String lower = clean.toLowerCase();
            if (lower.endsWith("mld")) {
                multiplier = 1_000_000_000;
                clean = clean.substring(0, clean.length() - 3);
            } else if (lower.endsWith("m")) {
                multiplier = 1_000_000;
                clean = clean.substring(0, clean.length() - 1);
            } else if (lower.endsWith("k")) {
                multiplier = 1_000;
                clean = clean.substring(0, clean.length() - 1);
            }

            // Remove thousand separators (dots before 3 digits)
            clean = clean.replaceAll("\\.(\\d{3})", "$1");

            return Double.parseDouble(clean) * multiplier;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static String format(double value) {
        if (value >= 1_000_000_000) return String.format("%.2fmld", value / 1_000_000_000);
        if (value >= 1_000_000) return String.format("%.2fm", value / 1_000_000);
        if (value >= 1_000) return String.format("%.1fk", value / 1_000);
        return String.format("%.0f", value);
    }
}
