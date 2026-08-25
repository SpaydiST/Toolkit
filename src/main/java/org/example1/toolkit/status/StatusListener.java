package org.example1.toolkit.status;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class StatusListener implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        StatusManager.onJoin(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        StatusManager.onQuit(event.getPlayer());
    }
}
