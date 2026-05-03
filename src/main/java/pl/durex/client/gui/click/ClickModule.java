package pl.durex.client.gui.click;

import java.util.List;

public final class ClickModule {
    private final String name;
    private final String description;
    private final ClickCategory category;
    private final List<ClickSetting> settings;
    private boolean enabled;

    public ClickModule(String name, String description, ClickCategory category, List<ClickSetting> settings) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.settings = settings;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public ClickCategory getCategory() {
        return category;
    }

    public List<ClickSetting> getSettings() {
        return settings;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void toggle() {
        enabled = !enabled;
    }
}
