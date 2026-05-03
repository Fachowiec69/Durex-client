package pl.durex.client.module;

/**
 * Tracers — linie od środka ekranu do graczy.
 * Opcje: kolor, grubość, max dystans, tryb (od dołu ekranu / od środka).
 */
public class TracerModule {

    private static boolean enabled     = false;
    private static float   maxDistance = 128f;
    // Kolor: 0=fioletowy, 1=czerwony, 2=zielony, 3=żółty, 4=biały
    private static int     colorIdx    = 0;

    public static final float[][] COLORS = {
        {0.53f, 0f,    0.93f, 0.85f}, // fioletowy
        {1f,    0.27f, 0.27f, 0.85f}, // czerwony
        {0.33f, 1f,    0.53f, 0.85f}, // zielony
        {1f,    0.8f,  0.27f, 0.85f}, // żółty
        {1f,    1f,    1f,    0.85f}, // biały
    };
    public static final String[] COLOR_NAMES = {"Fioletowy", "Czerwony", "Zielony", "Żółty", "Biały"};

    public static boolean isEnabled()             { return enabled; }
    public static void    setEnabled(boolean v)   { enabled = v; }
    public static float   getMaxDistance()        { return maxDistance; }
    public static void    setMaxDistance(float v) { maxDistance = Math.max(8f, Math.min(512f, v)); }
    public static int     getColorIdx()           { return colorIdx; }
    public static void    setColorIdx(int v)      { colorIdx = ((v % COLORS.length) + COLORS.length) % COLORS.length; }
    public static float[] getColor()              { return COLORS[colorIdx]; }
}
