package pl.durex.client.gui.click;

public enum ClickCategory {
    COMBAT("Combat"),
    MOVEMENT("Movement"),
    VISUAL("Visual"),
    PLAYER("Player"),
    WORLD("World"),
    MISC("Misc");

    private final String title;

    ClickCategory(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}
