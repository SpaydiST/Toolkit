package org.example1.toolkit;

import org.bukkit.Bukkit;
import org.bukkit.GameRules;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;

public final class F3Hider implements Listener {

    public static void applyAll() {
        if (!Config.HIDE_F3_COORDINATES) return;
        for (World w : Bukkit.getWorlds()) {
            w.setGameRule(GameRules.REDUCED_DEBUG_INFO, true);
        }
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        if (!Config.HIDE_F3_COORDINATES) return;
        event.getWorld().setGameRule(GameRules.REDUCED_DEBUG_INFO, true);
    }
}
