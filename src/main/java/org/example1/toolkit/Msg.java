package org.example1.toolkit;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

import static net.kyori.adventure.text.format.NamedTextColor.*;

public final class Msg {

    public static TextColor EMERALD = Config.COLOR_ACCENT;

    private Msg() {}

    public static Component sep() { return Component.text(" ", Config.COLOR_SEP); }

    public static Component of(Player player, Component... parts) {
        Component msg = Component.empty();
        for (Component part : parts) msg = msg.append(part);
        ActionBarManager.show(player, "toolkit:feedback", msg, 40, 50);
        return msg;
    }

    public static Component prefix(String text, TextColor color) {
        return Component.text(text, color);
    }

    public static Component error(String text) {
        return Component.text(text, Config.COLOR_ERROR);
    }

    public static Component action(String text) {
        return Component.text(text, WHITE);
    }

    public static Component dim(String text) {
        return Component.text(text, Config.COLOR_DIM);
    }

    public static Component val(String text) {
        return Component.text(text, Config.COLOR_ACCENT);
    }

    public static Component accent(String text, TextColor color) {
        return Component.text(text, color);
    }

    public static TextColor parseColor(String hex, TextColor fallback) {
        if (hex == null) return fallback;
        try {
            TextColor color = TextColor.fromHexString(hex.startsWith("#") ? hex : "#" + hex);
            return color != null ? color : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }

    public static Component eq() { return Component.text(" = ", Config.COLOR_SEP); }

    public static Component level(int current, int max) {
        return Component.text(current + "/" + max, WHITE);
    }

    public static Component count(int n, String unit) {
        return Component.text(n + " " + unit, WHITE);
    }
}
