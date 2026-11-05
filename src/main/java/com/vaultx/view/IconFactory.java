package com.vaultx.view;

import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;

import java.util.Map;

public final class IconFactory {
    private static final Map<String, String> PATHS = Map.ofEntries(
            Map.entry("dashboard", "M3 3h8v8H3z M13 3h8v5h-8z M13 10h8v11h-8z M3 13h8v8H3z"),
            Map.entry("key", "M14 3a7 7 0 0 0-6.7 9L2 17.3V22h4.7l2-2H10v-2h2l2-2v-.3A7 7 0 1 0 14 3zm0 4a2 2 0 1 1 0 4 2 2 0 0 1 0-4z"),
            Map.entry("generator", "M13 2 4 14h6l-1 8 11-13h-7z"),
            Map.entry("shield", "M12 2 3.5 5.5v5.3c0 5.2 3.6 9.8 8.5 11.2 4.9-1.4 8.5-6 8.5-11.2V5.5z M10.5 15.2 7 11.7l1.4-1.4 2.1 2.1 5.1-5.1 1.4 1.4z"),
            Map.entry("settings", "M19.4 13a7.8 7.8 0 0 0 0-2l2-1.5-2-3.5-2.4 1a8 8 0 0 0-1.7-1L15 3h-4l-.4 2.8a8 8 0 0 0-1.7 1l-2.4-1-2 3.5 2 1.5a7.8 7.8 0 0 0 0 2l-2 1.5 2 3.5 2.4-1a8 8 0 0 0 1.7 1L11 21h4l.4-2.8a8 8 0 0 0 1.7-1l2.4 1 2-3.5z M13 15.5a3.5 3.5 0 1 1 0-7 3.5 3.5 0 0 1 0 7z"),
            Map.entry("search", "M10.5 3a7.5 7.5 0 1 0 4.6 13.4l5.2 5.2 1.4-1.4-5.2-5.2A7.5 7.5 0 0 0 10.5 3zm0 2a5.5 5.5 0 1 1 0 11 5.5 5.5 0 0 1 0-11z"),
            Map.entry("lock", "M6 10V7a6 6 0 0 1 12 0v3h2v12H4V10zm2 0h8V7a4 4 0 0 0-8 0zm3 4v4h2v-4z"),
            Map.entry("plus", "M11 3h2v8h8v2h-8v8h-2v-8H3v-2h8z"),
            Map.entry("copy", "M8 3h12a2 2 0 0 1 2 2v13h-2V5H8z M4 7h12a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V9a2 2 0 0 1 2-2zm0 2v12h12V9z"),
            Map.entry("eye", "M12 5c-5 0-9.2 3.1-11 7 1.8 3.9 6 7 11 7s9.2-3.1 11-7c-1.8-3.9-6-7-11-7zm0 12a5 5 0 1 1 0-10 5 5 0 0 1 0 10zm0-2a3 3 0 1 0 0-6 3 3 0 0 0 0 6z"),
            Map.entry("edit", "M3 17.3V21h3.7L18.8 8.9l-3.7-3.7z M20.7 7a1 1 0 0 0 0-1.4l-2.3-2.3a1 1 0 0 0-1.4 0l-1.8 1.8 3.7 3.7z"),
            Map.entry("trash", "M6 7h12l-1 14H7z M9 3h6l1 2h4v2H4V5h4z M9 10v8h2v-8zm4 0v8h2v-8z"),
            Map.entry("star", "M12 2 15 9l7 1-5 5 1.2 7L12 18.5 5.8 22 7 15l-5-5 7-1z"),
            Map.entry("check", "M9 16.2 4.8 12 3.4 13.4 9 19l12-12-1.4-1.4z"),
            Map.entry("close", "M5.6 4.2 12 10.6l6.4-6.4 1.4 1.4-6.4 6.4 6.4 6.4-1.4 1.4-6.4-6.4-6.4 6.4-1.4-1.4 6.4-6.4-6.4-6.4z"),
            Map.entry("globe", "M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20zm6.9 9h-3a15.6 15.6 0 0 0-1.3-5.5A8 8 0 0 1 18.9 11zM12 4c1 1.4 1.7 4 1.9 7h-3.8c.2-3 1-5.6 1.9-7zM9.4 5.5A15.6 15.6 0 0 0 8.1 11h-3a8 8 0 0 1 4.3-5.5zM5.1 13h3a15.6 15.6 0 0 0 1.3 5.5A8 8 0 0 1 5.1 13zm6.9 7c-1-1.4-1.7-4-1.9-7h3.8c-.2 3-1 5.6-1.9 7zm2.6-1.5a15.6 15.6 0 0 0 1.3-5.5h3a8 8 0 0 1-4.3 5.5z"),
            Map.entry("user", "M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8zm0 2c-4.4 0-8 2.2-8 5v2h16v-2c0-2.8-3.6-5-8-5z")
    );

    private IconFactory() { }

    public static Node create(String name, double size, Color color) {
        SVGPath icon = new SVGPath();
        icon.setContent(PATHS.getOrDefault(name, PATHS.get("shield")));
        icon.setFill(color);
        icon.setScaleX(size / 24.0);
        icon.setScaleY(size / 24.0);
        icon.setMouseTransparent(true);
        return icon;
    }
}
