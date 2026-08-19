package org.example1.toolkit.clock;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;

/**
 * Мост к плагину TimeSpeed-plugin через рефлексию (мягкая зависимость).
 */
public final class TimeSpeedHook {

    private Object sleepHandler;
    private Method getWorldSpeed;

    public void init() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("TimeSpeed-plugin");
        if (plugin == null) return;
        try {
            Method getHandler = plugin.getClass().getMethod("getSleepHandler");
            sleepHandler = getHandler.invoke(plugin);
            if (sleepHandler == null) return;
            getWorldSpeed = sleepHandler.getClass().getMethod("getWorldSpeed", World.class);
        } catch (Throwable t) {
            sleepHandler = null;
            getWorldSpeed = null;
        }
    }

    public boolean available() {
        return getWorldSpeed != null;
    }

    public double currentSpeed(World world) {
        if (getWorldSpeed == null) return 0.0;
        try {
            Object value = getWorldSpeed.invoke(sleepHandler, world);
            return value instanceof Number number ? number.doubleValue() : 0.0;
        } catch (Throwable t) {
            return 0.0;
        }
    }
}
