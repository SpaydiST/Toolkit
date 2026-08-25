package org.example1.toolkit;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class FlectonePulseCompat {

    private static boolean available;

    private FlectonePulseCompat() {}

    public static void init(JavaPlugin plugin) {
        Plugin fp = Bukkit.getPluginManager().getPlugin("FlectonePulse");
        available = fp != null && fp.isEnabled();
        if (available) {
            plugin.getLogger().info("FlectonePulse обнаружен — TAB/action bar переданы FlectonePulse");
        }
    }

    public static boolean isAvailable() {
        return available;
    }
}
