package org.example1.toolkit;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.example1.toolkit.clock.ClockHud;
import org.example1.toolkit.clock.TimeSpeedHook;
import org.example1.toolkit.compass.CompassHud;

public final class Hud {

    private final JavaPlugin plugin;
    private final TimeSpeedHook hook;
    private org.bukkit.scheduler.BukkitTask task;

    public Hud(JavaPlugin plugin, TimeSpeedHook hook) {
        this.plugin = plugin;
        this.hook = hook;
    }

    public void start() {
        stop();
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 5L, Config.HUD_INTERVAL_TICKS);
    }

    public void stop() {
        if (task != null) task.cancel();
        task = null;
    }

    private void tick() {
        if (!Config.COMPASS_ENABLED && !Config.CLOCK_ENABLED) return;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.isDead() || !p.isOnline()) continue;
            if (p.isSleeping()) continue;
            render(p);
        }
    }

    private void render(Player p) {
        ItemStack main = p.getInventory().getItemInMainHand();
        ItemStack off = p.getInventory().getItemInOffHand();
        if (Config.COMPASS_ENABLED && main.getType() == Material.COMPASS) {
            CompassHud.render(p, main);
            return;
        }
        if (Config.CLOCK_ENABLED && main.getType() == Material.CLOCK) {
            ClockHud.render(p, main, hook);
            return;
        }
        if (Config.COMPASS_ENABLED && off.getType() == Material.COMPASS) {
            CompassHud.render(p, off);
            return;
        }
        if (Config.CLOCK_ENABLED && off.getType() == Material.CLOCK) {
            ClockHud.render(p, off, hook);
            return;
        }
    }
}
