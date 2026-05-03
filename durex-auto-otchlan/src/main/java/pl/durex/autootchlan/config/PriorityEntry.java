package pl.durex.autootchlan.config;

public class PriorityEntry {
    public String keyword;    // np. "shulker", "spawner", "elytra"
    public int priority;      // wyższy = ważniejszy (wyrzucany pierwszy)
    public String displayName; // nazwa wyświetlana w GUI

    public PriorityEntry() {}

    public PriorityEntry(String keyword, int priority, String displayName) {
        this.keyword = keyword;
        this.priority = priority;
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName + " [" + keyword + "] P:" + priority;
    }
}
