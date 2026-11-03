package org.example1.toolkit;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.UUID;

public final class FlectonePulseCompat implements Listener {

    private static boolean available;
    private static JavaPlugin plugin;

    private FlectonePulseCompat() {}

    public static void init(JavaPlugin hostPlugin) {
        plugin = hostPlugin;
        checkAvailability();
        Bukkit.getPluginManager().registerEvents(new FlectonePulseCompat(), hostPlugin);
        if (available) {
            hostPlugin.getLogger().info("FlectonePulse обнаружен: включена интеграция статусов и TAB");
        }
    }

    private static void checkAvailability() {
        Plugin fp = Bukkit.getPluginManager().getPlugin("FlectonePulse");
        available = fp != null && fp.isEnabled();
    }

    @EventHandler
    public void onPluginEnable(PluginEnableEvent event) {
        if ("FlectonePulse".equalsIgnoreCase(event.getPlugin().getName())) {
            available = true;
            if (plugin != null) {
                plugin.getLogger().info("FlectonePulse подключен: включена интеграция статусов и TAB");
            }
        }
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        if ("FlectonePulse".equalsIgnoreCase(event.getPlugin().getName())) {
            available = false;
            if (plugin != null) {
                plugin.getLogger().info("FlectonePulse отключен");
            }
        }
    }

    public static void onDisable() {
        available = false;
    }

    public static boolean isAvailable() {
        return available && Config.FLECTONEPULSE_COMPAT;
    }

    /**
     * Уведомляет FlectonePulse о необходимости обновить строку игрока в TAB-листе,
     * чтобы мгновенно отобразить изменившийся префикс статуса.
     */
    public static void refreshPlayerTab(Player player) {
        if (!isAvailable() || player == null) return;

        try {
            Plugin fp = Bukkit.getPluginManager().getPlugin("FlectonePulse");
            if (fp == null || !fp.isEnabled()) return;
            ClassLoader pulseClassLoader = fp.getClass().getClassLoader();

            Class<?> apiClass = loadPulseClass(pulseClassLoader, "net.flectone.pulse.FlectonePulseAPI");
            Method getInstance = apiClass.getMethod("getInstance");
            Object pulse = getInstance.invoke(null);
            if (pulse == null) return;

            Method isReady = pulse.getClass().getMethod("isReady");
            if (!Boolean.TRUE.equals(isReady.invoke(pulse))) return;

            Method getMethod = pulse.getClass().getMethod("get", Class.class);

            Class<?> fPlayerServiceClass = loadPulseClass(
                    pulseClassLoader, "net.flectone.pulse.service.FPlayerService");
            Object playerService = getMethod.invoke(pulse, fPlayerServiceClass);
            if (playerService == null) return;

            Method getFPlayer = fPlayerServiceClass.getMethod("getFPlayer", UUID.class);
            Object fPlayer = getFPlayer.invoke(playerService, player.getUniqueId());
            if (fPlayer == null) return;

            Class<?> playerlistClass = loadPulseClass(pulseClassLoader,
                    "net.flectone.pulse.module.message.tab.playerlist.MinecraftPlayerlistnameModule");
            Object playerlistModule = getMethod.invoke(pulse, playerlistClass);
            if (playerlistModule == null) return;

            Class<?> fPlayerClass = loadPulseClass(pulseClassLoader, "net.flectone.pulse.model.entity.FPlayer");
            Method updateMethod = playerlistClass.getMethod("update", fPlayerClass);
            updateMethod.invoke(playerlistModule, fPlayer);
        } catch (Throwable ignored) {
            // FlectonePulse может быть другой версии или модуль отключен, игнорируем
        }
    }

    private static Class<?> loadPulseClass(ClassLoader classLoader, String name) throws ClassNotFoundException {
        return Class.forName(name, true, classLoader);
    }
}
