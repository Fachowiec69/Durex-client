package pl.durex.client.gui.click;

import java.util.ArrayList;
import java.util.List;

public final class ClickModuleRepository {
    private ClickModuleRepository() {
    }

    public static List<ClickModule> createModules() {
        List<ClickModule> modules = new ArrayList<>();

        modules.add(module("Velocity", "Knockback tuning shell.", ClickCategory.COMBAT,
                new ClickSetting.ToggleSetting("Preserve Sprint", true),
                new ClickSetting.SliderSetting("Horizontal", 0.0, 100.0, 82.0),
                new ClickSetting.DropdownSetting("Mode", List.of("Smart", "Linear", "Adaptive"), 0)));
        modules.add(module("Auto Pot", "Utility combat helper layout.", ClickCategory.COMBAT,
                new ClickSetting.ToggleSetting("Safe Throw", true),
                new ClickSetting.SliderSetting("Health Trigger", 1.0, 20.0, 8.0),
                new ClickSetting.DropdownSetting("Priority", List.of("Healing", "Speed", "Mixed"), 2)));
        modules.add(module("Backtrack", "Visualized timing compensation.", ClickCategory.COMBAT,
                new ClickSetting.ToggleSetting("Render Ghost", true),
                new ClickSetting.SliderSetting("Delay", 10.0, 250.0, 95.0),
                new ClickSetting.DropdownSetting("Coloring", List.of("Accent", "Health", "Distance"), 0)));

        modules.add(module("Sprint Assist", "Movement helper shell.", ClickCategory.MOVEMENT,
                new ClickSetting.ToggleSetting("Omni Sprint", true),
                new ClickSetting.SliderSetting("Acceleration", 0.1, 2.0, 1.1),
                new ClickSetting.DropdownSetting("Style", List.of("Legit", "Smooth", "Aggressive"), 1)));
        modules.add(module("Strafe", "Responsive movement tuning.", ClickCategory.MOVEMENT,
                new ClickSetting.ToggleSetting("Stop On Hit", false),
                new ClickSetting.SliderSetting("Strength", 0.2, 3.0, 1.6),
                new ClickSetting.DropdownSetting("Curve", List.of("Soft", "Balanced", "Sharp"), 1)));
        modules.add(module("Step", "Step profile editor.", ClickCategory.MOVEMENT,
                new ClickSetting.ToggleSetting("Reverse Step", true),
                new ClickSetting.SliderSetting("Height", 1.0, 4.0, 1.5),
                new ClickSetting.DropdownSetting("Mode", List.of("Fluid", "Static"), 0)));

        modules.add(module("HUD", "Main heads-up overlay card.", ClickCategory.VISUAL,
                new ClickSetting.ToggleSetting("Watermark", true),
                new ClickSetting.SliderSetting("Opacity", 0.1, 1.0, 0.82),
                new ClickSetting.DropdownSetting("Theme", List.of("Glass", "Midnight", "Aurora"), 0)));
        modules.add(module("ESP", "Entity overlay shell.", ClickCategory.VISUAL,
                new ClickSetting.ToggleSetting("Outline", true),
                new ClickSetting.SliderSetting("Glow", 0.0, 1.0, 0.55),
                new ClickSetting.DropdownSetting("Palette", List.of("Blue", "Purple", "Adaptive"), 2)));
        modules.add(module("Blur", "Scene defocus control.", ClickCategory.VISUAL,
                new ClickSetting.ToggleSetting("Panel Blur", true),
                new ClickSetting.SliderSetting("Radius", 2.0, 20.0, 10.0),
                new ClickSetting.DropdownSetting("Algorithm", List.of("Kawase", "Gaussian"), 0)));

        modules.add(module("Inventory", "Inventory QoL shell.", ClickCategory.PLAYER,
                new ClickSetting.ToggleSetting("Quick Move", true),
                new ClickSetting.SliderSetting("Delay", 0.0, 8.0, 2.0),
                new ClickSetting.DropdownSetting("Sorting", List.of("Priority", "Armor", "Adaptive"), 1)));
        modules.add(module("Nametags", "Player label layout.", ClickCategory.PLAYER,
                new ClickSetting.ToggleSetting("Show Ping", true),
                new ClickSetting.SliderSetting("Scale", 0.6, 2.0, 1.1),
                new ClickSetting.DropdownSetting("Detail", List.of("Compact", "Default", "Full"), 0)));
        modules.add(module("Camera", "View tuning shell.", ClickCategory.PLAYER,
                new ClickSetting.ToggleSetting("Smooth Zoom", true),
                new ClickSetting.SliderSetting("Zoom", 10.0, 70.0, 32.0),
                new ClickSetting.DropdownSetting("Easing", List.of("Out Quart", "Expo", "Sine"), 0)));

        modules.add(module("Waypoints", "World marker manager.", ClickCategory.WORLD,
                new ClickSetting.ToggleSetting("Distance Label", true),
                new ClickSetting.SliderSetting("Render Range", 32.0, 512.0, 192.0),
                new ClickSetting.DropdownSetting("Marker", List.of("Dot", "Diamond", "Pin"), 2)));
        modules.add(module("Minimap", "Compact navigation shell.", ClickCategory.WORLD,
                new ClickSetting.ToggleSetting("Rotate", false),
                new ClickSetting.SliderSetting("Scale", 0.8, 1.6, 1.0),
                new ClickSetting.DropdownSetting("Corners", List.of("Rounded", "Circle", "Square"), 0)));
        modules.add(module("Atmosphere", "Ambient world presentation.", ClickCategory.WORLD,
                new ClickSetting.ToggleSetting("Dynamic Fog", true),
                new ClickSetting.SliderSetting("Density", 0.0, 1.0, 0.18),
                new ClickSetting.DropdownSetting("Preset", List.of("Clear", "Night", "Rain"), 0)));

        modules.add(module("Interface", "Global UI profile.", ClickCategory.MISC,
                new ClickSetting.ToggleSetting("Rounded Panels", true),
                new ClickSetting.SliderSetting("Spacing", 6.0, 18.0, 12.0),
                new ClickSetting.DropdownSetting("Font", List.of("Inter", "Inter Display"), 0)));
        modules.add(module("Notifications", "Popup notification skin.", ClickCategory.MISC,
                new ClickSetting.ToggleSetting("Stack", true),
                new ClickSetting.SliderSetting("Duration", 1.0, 8.0, 3.5),
                new ClickSetting.DropdownSetting("Position", List.of("Top Right", "Top Left", "Bottom Right"), 0)));

        return modules;
    }

    private static ClickModule module(String name, String description, ClickCategory category, ClickSetting... settings) {
        return new ClickModule(name, description, category, List.of(settings));
    }
}
