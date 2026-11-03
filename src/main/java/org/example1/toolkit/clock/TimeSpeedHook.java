package org.example1.toolkit.clock;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;

/**
 * Мост к плагину TimeSpeed-plugin через рефлексию (мягкая зависимость).
 */
public final class TimeSpeedHook {

    private Plugin timeSpeedPlugin;
    private Object sleepHandler;
    private Method getPluginWorldSpeed;
    private Method getSleepHandlerWorldSpeed;

    public void init() {
        clear();
        Plugin plugin = Bukkit.getPluginManager().getPlugin("TimeSpeed-plugin");
        if (plugin == null || !plugin.isEnabled()) return;
        timeSpeedPlugin = plugin;
        try {
            getPluginWorldSpeed = plugin.getClass().getMethod("getWorldTimeSpeed", World.class);
        } catch (Throwable t) {
            try {
                Method getHandler = plugin.getClass().getMethod("getSleepHandler");
                sleepHandler = getHandler.invoke(plugin);
                if (sleepHandler != null) {
                    getSleepHandlerWorldSpeed = sleepHandler.getClass().getMethod("getWorldSpeed", World.class);
                }
            } catch (Throwable ignored) {
                clear();
            }
        }
    }

    public boolean available() {
        Plugin current = Bukkit.getPluginManager().getPlugin("TimeSpeed-plugin");
        if (current == null || !current.isEnabled()) {
            clear();
            return false;
        }
        if (current != timeSpeedPlugin) init();
        return timeSpeedPlugin == current && (getPluginWorldSpeed != null || getSleepHandlerWorldSpeed != null);
    }

    public double currentSpeed(World world) {
        if (!available()) return 0.0;
        try {
            Object value = getPluginWorldSpeed != null
                    ? getPluginWorldSpeed.invoke(timeSpeedPlugin, world)
                    : getSleepHandlerWorldSpeed.invoke(sleepHandler, world);
            return value instanceof Number number ? number.doubleValue() : 0.0;
        } catch (Throwable t) {
            clear();
            return 0.0;
        }
    }

    private void clear() {
        timeSpeedPlugin = null;
        sleepHandler = null;
        getPluginWorldSpeed = null;
        getSleepHandlerWorldSpeed = null;
    }
}
