package pl.durex.client.util;

public final class TimerUtil {
    private long lastMs = System.currentTimeMillis();

    public boolean hasTimePassed(long ms) {
        return System.currentTimeMillis() - lastMs >= ms;
    }

    public void reset() {
        lastMs = System.currentTimeMillis();
    }
}
