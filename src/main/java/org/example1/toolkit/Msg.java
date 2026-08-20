package org.example1.toolkit;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

import static net.kyori.adventure.text.format.NamedTextColor.*;

public final class Msg {

    private static final Component SEP = Component.text(" \u00b7 ", DARK_GRAY);
    private static final Component EQ = Component.text(" = ", DARK_GRAY);
    private static final Component SPACE = Component.text(" ", WHITE);

    private Msg() {}

    public static Component sep() { return SEP; }

    public static Component of(Player player, Component... parts) {
        Component msg = Component.empty();
        for (Component part : parts) msg = msg.append(part);
        player.sendActionBar(msg);
        return msg;
    }

    public static Component prefix(String text, TextColor color) {
        return Component.text(text, color);
    }

    public static Component action(String text) {
        return Component.text(text, WHITE);
    }

    public static Component dim(String text) {
        return Component.text(text, GRAY);
    }

    public static Component val(String text) {
        return Component.text(text, YELLOW);
    }

    public static Component accent(String text, TextColor color) {
        return Component.text(text, color);
    }

    public static Component eq() { return EQ; }

    public static Component level(int current, int max) {
        return Component.text(current + "/" + max, WHITE);
    }

    public static Component count(int n, String unit) {
        return Component.text(n + " " + unit, WHITE);
    }
}
