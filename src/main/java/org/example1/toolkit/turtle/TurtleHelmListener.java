package org.example1.toolkit.turtle;

import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.example1.toolkit.Config;

public final class TurtleHelmListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!Config.TURTLE_HELM_TRIDENT_ENABLED) return;
        if (!(event.getEntity() instanceof Player player)) return;

        Entity damager = event.getDamager();
        if (!(damager instanceof Trident)) return;

        ItemStack helmet = player.getInventory().getHelmet();
        if (helmet == null || helmet.getType() == Material.AIR) return;

        boolean turtle = helmet.getType() == Material.TURTLE_HELMET;
        boolean anyHelmet = turtle || helmet.getType().name().endsWith("_HELMET");
        if (!anyHelmet) return;

        double originalDamage = event.getDamage();
        double absorbed;

        if (turtle) {
            absorbed = originalDamage;
            event.setCancelled(true);
        } else {
            absorbed = originalDamage * 0.25;
            event.setDamage(originalDamage - absorbed);
        }

        if (helmet.getItemMeta() instanceof Damageable damageable) {
            double durabilityMult = turtle ? 0.5 : 1.0;
            int durabilityLoss = (int) Math.ceil(absorbed * durabilityMult * 2.0);
            int newDamage = damageable.getDamage() + durabilityLoss;
            int maxDur = helmet.getType().getMaxDurability();
            if (newDamage >= maxDur) {
                player.getInventory().setHelmet(null);
            } else {
                damageable.setDamage(newDamage);
                helmet.setItemMeta(damageable);
            }
        }
    }
}
