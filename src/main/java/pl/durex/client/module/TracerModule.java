package pl.durex.client.module;

import java.util.ArrayList;
import java.util.List;

/**
 * Tracers — linie od crosshaira do graczy.
 */
public class TracerModule {

    private static boolean enabled     = false;
    private static float   maxDistance = 256f;
    private static int     colorIdx    = 0;
    private static int     styleIdx    = 0;

    // Custom style'y dodane przez użytkownika
    private static final List<CustomTracerStyle> customStyles = new ArrayList<>();
    // Indeks w custom styles (gdy styleIdx >= Style.values().length)
    private static int customStyleIdx = -1; // -1 = nie używamy custom

    public static final float[][] COLORS = {
        {0.53f, 0f,    0.93f, 0.9f},
        {1f,    0.27f, 0.27f, 0.9f},
        {0.33f, 1f,    0.53f, 0.9f},
        {1f,    0.8f,  0.27f, 0.9f},
        {1f,    1f,    1f,    0.9f},
        {0.27f, 0.8f,  1f,    0.9f},
        {1f,    0.4f,  0.8f,  0.9f},
        {1f,    0.5f,  0f,    0.9f},
        {0f,    1f,    1f,    0.9f},
    };
    public static final String[] COLOR_NAMES = {
        "Fioletowy", "Czerwony", "Zielony", "Żółty", "Biały",
        "Niebieski", "Różowy", "Pomarańczowy", "Cyjan"
    };

    public enum Style {
        LINE("Linia"), DASHED("Przerywana"), HACKER("Hacker 010101"),
        HEART("Serduszkowa"), RAINBOW("Tęczowa"), DOUBLE("Podwójna"),
        ARROW("Strzałkowa"), ZIGZAG("Zygzak"), DOTTED("Kropkowana"),
        THICK("Gruba"), WAVE("Falista"), PULSE("Pulsująca"),
        STAR("Gwiazdkowa"), CROSS("Krzyżykowa"), NEON("Neonowa"),
        FIRE("Ognista"), ICE("Lodowa"), MATRIX("Matrix"), ELECTRIC("Elektryczna"),
        CLEAN("Clean");

        public final String name;
        Style(String name) { this.name = name; }
    }

    public static boolean isEnabled()             { return enabled; }
    public static void    setEnabled(boolean v)   { enabled = v; }
    public static float   getMaxDistance()        { return maxDistance; }
    public static void    setMaxDistance(float v) { maxDistance = Math.max(8f, Math.min(512f, v)); }
    public static int     getColorIdx()           { return colorIdx; }
    public static void    setColorIdx(int v)      { colorIdx = ((v % COLORS.length) + COLORS.length) % COLORS.length; }
    public static float[] getColor()              { return COLORS[colorIdx]; }

    /** Łączna liczba stylów = wbudowane + custom */
    public static int getTotalStyles() {
        return Style.values().length + customStyles.size();
    }

    public static int getStyleIdx()  { return styleIdx; }
    public static void setStyleIdx(int v) {
        int total = getTotalStyles();
        styleIdx = ((v % total) + total) % total;
    }

    /** Czy aktualny styl to custom? */
    public static boolean isCustomStyle() {
        return styleIdx >= Style.values().length;
    }

    /** Zwraca wbudowany styl (gdy nie custom) */
    public static Style getStyle() {
        if (isCustomStyle()) return Style.LINE; // fallback
        return Style.values()[styleIdx];
    }

    /** Zwraca custom styl (gdy custom) */
    public static CustomTracerStyle getCustomStyle() {
        if (!isCustomStyle()) return null;
        int idx = styleIdx - Style.values().length;
        if (idx < 0 || idx >= customStyles.size()) return null;
        return customStyles.get(idx);
    }

    /** Nazwa aktualnego stylu (wbudowany lub custom) */
    public static String getStyleName() {
        if (isCustomStyle()) {
            CustomTracerStyle cs = getCustomStyle();
            return cs != null ? "✏ " + cs.name : "Custom";
        }
        return getStyle().name;
    }

    /** Dodaj custom styl i przełącz na niego */
    public static void addCustomStyle(CustomTracerStyle style) {
        customStyles.add(style);
        styleIdx = Style.values().length + customStyles.size() - 1;
    }

    /** Usuń custom styl po indeksie (w liście custom) */
    public static void removeCustomStyle(int customIdx) {
        if (customIdx < 0 || customIdx >= customStyles.size()) return;
        customStyles.remove(customIdx);
        // Napraw styleIdx jeśli wskazywał na usuniętą lub dalszą pozycję
        int builtIn = Style.values().length;
        int total = getTotalStyles();
        if (total == 0) { styleIdx = 0; return; }
        if (styleIdx >= builtIn + customIdx) {
            styleIdx = Math.max(0, styleIdx - 1);
        }
        styleIdx = Math.min(styleIdx, total - 1);
    }

    /** Usuń aktualnie wybrany custom styl */
    public static void removeCurrentCustomStyle() {
        if (!isCustomStyle()) return;
        removeCustomStyle(styleIdx - Style.values().length);
    }

    public static List<CustomTracerStyle> getCustomStyles() { return customStyles; }
}
