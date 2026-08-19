package org.example1.toolkit.compass;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.example1.toolkit.Config;
import org.example1.toolkit.Lang;

import java.util.UUID;

public final class CompassBindListener implements Listener {

    @EventHandler(priority = EventPriority.LOW)
    public void onRightClickLodestone(PlayerInteractEvent event) {
        if (!Config.COMPASS_ENABLED) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.LODESTONE) return;
        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.COMPASS) return;
        if (CompassTracker.isPlayerBound(item)) {
            event.setCancelled(true);
            CompassTracker.unbind(item);
            CompassHud.hint(event.getPlayer(), Lang.get(event.getPlayer(), "unbound"));
            return;
        }
        if (CompassTracker.isLodestoneBound(item)) {
            event.setCancelled(true);
            CompassTracker.unbind(item);
            CompassHud.hint(event.getPlayer(), Lang.get(event.getPlayer(), "unbound"));
        }
    }

    @EventHandler
    public void onRightClickPlayer(PlayerInteractEntityEvent event) {
        if (!Config.COMPASS_ENABLED) return;
        if (!(event.getRightClicked() instanceof Player target)) return;
        Player player = event.getPlayer();
        if (target.equals(player)) return;

        EquipmentSlot hand = event.getHand();
        ItemStack held = hand == EquipmentSlot.HAND
                ? player.getInventory().getItemInMainHand()
                : player.getInventory().getItemInOffHand();
        if (held == null || held.getType() != Material.COMPASS) return;

        event.setCancelled(true);
        if (player.isSneaking()) {
            CompassTracker.unbind(held);
            CompassHud.hint(player, Lang.get(player, "unbound"));
            return;
        }
        if (CompassTracker.isOnCooldown(held)) return;

        if (!CompassTracker.isLodestoneBound(held)) {
            CompassHud.hint(player, Lang.get(player, "need-magnet"));
            return;
        }

        if (target.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
            CompassHud.hint(player, Lang.get(player, "target-invisible"));
            return;
        }

        UUID tracked = CompassTracker.trackedPlayer(held);
        if (tracked != null && tracked.equals(target.getUniqueId())) {
            CompassTracker.refresh(held, player);
        } else {
            if (!CompassTracker.anchorAlive(held)) {
                CompassTracker.unbind(held);
                CompassHud.hint(player, Lang.get(player, "magnet-broken"));
                return;
            }
            CompassTracker.bind(held, player, target);
        }
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        if (!Config.COMPASS_ENABLED) return;
        Player player = event.getPlayer();
        for (ItemStack s : player.getInventory().getContents()) {
            if (s != null && s.getType() == Material.COMPASS && CompassTracker.isPlayerBound(s)) {
                CompassTracker.update(s, player);
            }
        }
        ItemStack off = player.getInventory().getItemInOffHand();
        if (off != null && off.getType() == Material.COMPASS && CompassTracker.isPlayerBound(off)) {
            CompassTracker.update(off, player);
        }
    }

    @EventHandler
    public void onRightClickAir(PlayerInteractEvent event) {
        if (!Config.COMPASS_ENABLED) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR) return;
        Player player = event.getPlayer();

        ItemStack main = player.getInventory().getItemInMainHand();
        ItemStack off = player.getInventory().getItemInOffHand();
        ItemStack held = null;
        if (main.getType() == Material.COMPASS) {
            held = main;
        } else if (off.getType() == Material.COMPASS) {
            held = off;
        }
        if (held == null || !CompassTracker.isPlayerBound(held)) return;

        event.setCancelled(true);
        if (player.isSneaking()) {
            CompassTracker.unbind(held);
            CompassHud.hint(player, Lang.get(player, "unbound"));
            return;
        }
        if (CompassTracker.isOnCooldown(held)) return;

        UUID id = CompassTracker.trackedPlayer(held);
        Player target = id == null ? null : Bukkit.getPlayer(id);
        if (target == null || !target.isOnline()) return;
        CompassTracker.refresh(held, player);
    }
}
