package pl.durex.client.gui.click;

import java.util.List;

public abstract class ClickSetting {
    private final String name;

    protected ClickSetting(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static final class ToggleSetting extends ClickSetting {
        private boolean enabled;

        public ToggleSetting(String name, boolean enabled) {
            super(name);
            this.enabled = enabled;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void toggle() {
            enabled = !enabled;
        }
    }

    public static final class SliderSetting extends ClickSetting {
        private final double min;
        private final double max;
        private double value;

        public SliderSetting(String name, double min, double max, double value) {
            super(name);
            this.min = min;
            this.max = max;
            this.value = value;
        }

        public double getMin() {
            return min;
        }

        public double getMax() {
            return max;
        }

        public double getValue() {
            return value;
        }

        public void setValue(double value) {
            this.value = Math.max(min, Math.min(max, value));
        }

        public double getNormalized() {
            return (value - min) / (max - min);
        }
    }

    public static final class DropdownSetting extends ClickSetting {
        private final List<String> options;
        private int index;
        private boolean expanded;

        public DropdownSetting(String name, List<String> options, int index) {
            super(name);
            this.options = options;
            this.index = Math.max(0, Math.min(index, options.size() - 1));
        }

        public List<String> getOptions() {
            return options;
        }

        public String getCurrent() {
            return options.get(index);
        }

        public void next() {
            index = (index + 1) % options.size();
        }

        public void setIndex(int index) {
            this.index = Math.max(0, Math.min(index, options.size() - 1));
        }

        public boolean isExpanded() {
            return expanded;
        }

        public void setExpanded(boolean expanded) {
            this.expanded = expanded;
        }
    }
}
