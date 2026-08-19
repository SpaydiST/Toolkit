package org.example1.toolkit.shield;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.util.Vector;
import org.example1.toolkit.Config;

import java.util.Map;

public final class ShieldEnchantListener implements Listener {

    private static final Enchantment[] SHIELD_EXTRA_ENCHANTS = {
            Enchantment.THORNS,
            Enchantment.KNOCKBACK
    };

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!Config.SHIELD_ENCHANT_ENABLED) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (!player.isBlocking()) return;

        ItemStack shield = getShield(player);
        if (shield == null) return;
        if (shield.getType() != Material.SHIELD) return;

        Map<Enchantment, Integer> enchants = shield.getEnchantments();
        Entity damager = event.getDamager();

        if (enchants.containsKey(Enchantment.THORNS)) {
            int level = enchants.get(Enchantment.THORNS);
            if (damager instanceof LivingEntity living) {
                double damage = 1.0 + (level - 1) * 0.5;
                living.damage(damage, player);
            }
        }

        if (enchants.containsKey(Enchantment.KNOCKBACK) && damager instanceof LivingEntity) {
            int level = enchants.get(Enchantment.KNOCKBACK);
            Vector dir = damager.getLocation().toVector()
                    .subtract(player.getLocation().toVector()).normalize();
            dir.multiply(0.6 + level * 0.4);
            dir.setY(0.35 + level * 0.1);
            damager.setVelocity(damager.getVelocity().add(dir));
        }

    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockProjectile(EntityDamageByEntityEvent event) {
        if (!Config.SHIELD_ENCHANT_ENABLED) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (!player.isBlocking()) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.PROJECTILE) return;

        ItemStack shield = getShield(player);
        if (shield == null) return;

        Map<Enchantment, Integer> enchants = shield.getEnchantments();
        if (enchants.containsKey(Enchantment.KNOCKBACK)) {
            int level = enchants.get(Enchantment.KNOCKBACK);
            Entity damager = event.getDamager();
            if (damager instanceof Projectile proj) {
                Vector vel = proj.getVelocity().multiply(-0.5 * level);
                vel.setY(Math.abs(vel.getY()) * 0.5 + 0.1);
                proj.setVelocity(vel);
            } else if (damager instanceof LivingEntity living) {
                Vector dir = living.getLocation().toVector()
                        .subtract(player.getLocation().toVector()).normalize();
                dir.multiply(0.8 * level);
                dir.setY(0.3);
                living.setVelocity(living.getVelocity().add(dir));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onAnvil(PrepareAnvilEvent event) {
        if (!Config.SHIELD_ENCHANT_ENABLED) return;
        AnvilInventory inv = event.getInventory();
        ItemStack left = inv.getItem(0);
        ItemStack right = inv.getItem(1);
        if (left == null || right == null) return;
        if (left.getType() != Material.SHIELD) return;
        if (right.getType() != Material.ENCHANTED_BOOK) return;

        EnchantmentStorageMeta bookMeta = (EnchantmentStorageMeta) right.getItemMeta();
        if (bookMeta == null) return;

        ItemStack result = left.clone();
        boolean changed = false;
        for (Map.Entry<Enchantment, Integer> entry : bookMeta.getStoredEnchants().entrySet()) {
            for (Enchantment allowed : SHIELD_EXTRA_ENCHANTS) {
                if (entry.getKey().equals(allowed)) {
                    int current = result.getEnchantmentLevel(entry.getKey());
                    if (entry.getValue() > current) {
                        result.addUnsafeEnchantment(entry.getKey(), entry.getValue());
                        changed = true;
                    }
                }
            }
        }
        if (changed) {
            event.setResult(result);
        }
    }

    private static ItemStack getShield(Player player) {
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (offhand.getType() == Material.SHIELD) return offhand;
        ItemStack mainhand = player.getInventory().getItemInMainHand();
        if (mainhand.getType() == Material.SHIELD) return mainhand;
        return null;
    }
}
