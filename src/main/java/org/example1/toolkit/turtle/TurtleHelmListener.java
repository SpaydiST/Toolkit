package org.example1.toolkit.turtle;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.example1.toolkit.Config;

public final class TurtleHelmListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!Config.TURTLE_HELM_TRIDENT_ENABLED) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getFinalDamage() <= 0) return;

        Entity damager = event.getDamager();
        // Урон наносится трезубцем (брошенным или в руке) либо булавой в руке атакующего.
        boolean trident = damager instanceof Trident;
        boolean mace = false;
        if (!trident && damager instanceof LivingEntity attacker && attacker.getEquipment() != null) {
            Material weapon = attacker.getEquipment().getItemInMainHand().getType();
            mace = weapon == Material.MACE;
            trident = weapon == Material.TRIDENT;
        }
        if (!mace && !trident) return;

        ItemStack checkHelmet = player.getInventory().getHelmet();
        if (checkHelmet == null || checkHelmet.getType() == Material.AIR) return;

        boolean turtle = checkHelmet.getType() == Material.TURTLE_HELMET;
        boolean anyHelmet = turtle || checkHelmet.getType().name().endsWith("_HELMET");
        if (!anyHelmet) return;

        double originalDamage = event.getDamage();
        // Черепаший шлем поглощает 50% урона, все остальные шлемы — 25%.
        double ratio = turtle ? 0.50 : 0.25;
        double absorbed = originalDamage * ratio;
        event.setDamage(Math.max(0, originalDamage - absorbed));
        // Перечитываем шлем после пересчёта модификаторов события и сохраняем обновлённую прочность обратно в слот
        ItemStack helmet = player.getInventory().getHelmet();
        if (helmet != null && helmet.getItemMeta() instanceof Damageable damageable && !damageable.isUnbreakable()) {
            int unbreaking = helmet.getEnchantmentLevel(Enchantment.UNBREAKING);
            double durabilityMult = (turtle ? 0.5 : 1.0) / (1.0 + unbreaking * 0.5);
            int durabilityLoss = Math.max(1, (int) Math.ceil(absorbed * durabilityMult * 1.5));
            int newDamage = damageable.getDamage() + durabilityLoss;
            int maxDur = helmet.getType().getMaxDurability();
            if (maxDur > 0 && newDamage >= maxDur) {
                player.getInventory().setHelmet(null);
                player.broadcastSlotBreak(EquipmentSlot.HEAD);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
            } else {
                damageable.setDamage(newDamage);
                helmet.setItemMeta(damageable);
                player.getInventory().setHelmet(helmet);
                player.getWorld().playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_TURTLE, 0.8f, 1.2f);
            }
        }
    }
}
