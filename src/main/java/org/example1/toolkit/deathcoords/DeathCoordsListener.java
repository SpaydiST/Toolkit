package org.example1.toolkit.deathcoords;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public final class DeathCoordsListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (!DeathCoords.isEnabled(player)) {
            return;
        }
        DeathCoords.recordDeath(player);
        player.sendMessage(DeathCoords.deathComponent(player));
    }
}
