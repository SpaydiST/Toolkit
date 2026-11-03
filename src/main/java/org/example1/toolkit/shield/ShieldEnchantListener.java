package org.example1.toolkit.shield;

import io.papermc.paper.event.player.PlayerShieldDisableEvent;
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
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.entity.HumanEntity;
import org.bukkit.util.Vector;
import org.example1.toolkit.AnvilCosts;
import org.example1.toolkit.Config;
import org.example1.toolkit.progression.AchievementProgression;

import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ShieldEnchantListener implements Listener {

    private static final Enchantment[] SHIELD_EXTRA_ENCHANTS = {
            Enchantment.THORNS,
            Enchantment.KNOCKBACK,
            Enchantment.PUNCH,
            Enchantment.QUICK_CHARGE
    };

    /** На сколько тиков Быстрая перезарядка ускоряет «перезарядку» щита после удара топором (18 тиков/ур -> на III ур. остаётся 46 тиков = 2.3 сек). */
    private static final int QUICK_CHARGE_TICKS_PER_LEVEL = 18;
    private static final long KNOCKBACK_COOLDOWN_MS = 750L;
    private final Map<java.util.UUID, Long> lastKnockbackTime = new java.util.HashMap<>();
    /** Игроки, чью попытку наложить Починку на булаву наковальня отклонила (достижение — по клику на пустой результат). */
    private final Set<UUID> maceRejected = new HashSet<>();
    /** Защита от рекурсии: шипы щита, нанесённые игроку со щитом на шипах. */
    private boolean inThorns;

    /** Цена за уровень зачарования щита (множитель редкости из ванили; книга — вдвое дешевле). */
    private static int enchantCost(Enchantment enchantment, boolean fromBook) {
        int base = enchantment.equals(Enchantment.THORNS) ? 8 : 4;
        return fromBook ? base / 2 : base;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        lastKnockbackTime.remove(event.getPlayer().getUniqueId());
        maceRejected.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!Config.SHIELD_ENCHANT_ENABLED) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (!player.isBlocking()) return;

        Entity damager = event.getDamager();
        if (!wasBlocked(event, player, damager)) return;

        EquipmentSlot slot = shieldSlot(player);
        if (slot == null) return;
        ItemStack shield = player.getInventory().getItem(slot);
        if (shield == null || shield.getType() != Material.SHIELD) return;

        Map<Enchantment, Integer> enchants = shield.getEnchantments();

        if (enchants.containsKey(Enchantment.THORNS)) {
            int level = enchants.get(Enchantment.THORNS);
            if (!inThorns && damager instanceof LivingEntity living
                    && java.util.concurrent.ThreadLocalRandom.current().nextDouble() < (0.25 * level)) {
                double damage = 1.0 + (level - 1) * 0.5;
                inThorns = true;
                try {
                    living.damage(damage, player);
                } finally {
                    inThorns = false;
                }
                damageShield(player, slot, shield, 1);
                AchievementProgression.award(player, "toolkit_shield");
            }
        }

        int pushLevel = Math.max(enchants.getOrDefault(Enchantment.KNOCKBACK, 0),
                enchants.getOrDefault(Enchantment.PUNCH, 0));
        if (pushLevel > 0) {
            int level = pushLevel;
            long now = System.currentTimeMillis();
            long last = lastKnockbackTime.getOrDefault(player.getUniqueId(), 0L);
            if (now - last >= KNOCKBACK_COOLDOWN_MS) {
                lastKnockbackTime.put(player.getUniqueId(), now);
                if (damager instanceof Projectile proj) {
                    Vector vel = proj.getVelocity().multiply(-0.35 * level);
                    vel.setY(Math.abs(vel.getY()) * 0.4 + 0.08);
                    proj.setVelocity(vel);
                    AchievementProgression.award(player, "toolkit_shield");
                } else if (damager instanceof LivingEntity living) {
                    Vector toDamager = living.getLocation().toVector()
                            .subtract(player.getLocation().toVector());
                    if (toDamager.lengthSquared() > 1e-4) {
                        Vector dir = toDamager.normalize().multiply(0.35 + level * 0.25);
                        dir.setY(0.20 + level * 0.08);
                        living.setVelocity(living.getVelocity().add(dir));
                        AchievementProgression.award(player, "toolkit_shield");
                    }
                }
            }
        }
    }

    private static void damageShield(Player player, EquipmentSlot slot, ItemStack shield, int amount) {
        if (!(shield.getItemMeta() instanceof org.bukkit.inventory.meta.Damageable damageable) || damageable.isUnbreakable()) {
            return;
        }
        PlayerItemDamageEvent damageEvent = new PlayerItemDamageEvent(player, shield, amount, amount);
        if (!damageEvent.callEvent()) return;
        amount = damageEvent.getDamage();
        if (amount <= 0) return;
        int unbreaking = shield.getEnchantmentLevel(Enchantment.UNBREAKING);
        if (unbreaking > 0 && java.util.concurrent.ThreadLocalRandom.current().nextInt(unbreaking + 1) > 0) {
            return;
        }
        int newDamage = damageable.getDamage() + amount;
        int maxDur = shield.getType().getMaxDurability();
        if (maxDur > 0 && newDamage >= maxDur) {
            player.getInventory().setItem(slot, null);
            player.broadcastSlotBreak(slot);
        } else {
            damageable.setDamage(newDamage);
            shield.setItemMeta(damageable);
            player.getInventory().setItem(slot, shield);
        }
    }

    /** Урон действительно заблокирован щитом (модификатор BLOCKING), а если ванильных данных нет — по углу. */
    @SuppressWarnings("deprecation")
    private static boolean wasBlocked(EntityDamageByEntityEvent event, Player player, Entity damager) {
        if (event.isApplicable(EntityDamageEvent.DamageModifier.BLOCKING)) {
            return event.getDamage(EntityDamageEvent.DamageModifier.BLOCKING) < 0;
        }
        return isBlockedFromFront(player, damager);
    }

    private static boolean isBlockedFromFront(Player player, Entity damager) {
        Vector toSource = damager.getLocation().toVector().subtract(player.getLocation().toVector());
        toSource.setY(0);
        if (toSource.lengthSquared() < 1e-4) return true;
        Vector look = player.getLocation().getDirection();
        look.setY(0);
        if (look.lengthSquared() < 1e-4) return true;
        return toSource.normalize().dot(look.normalize()) > 0.0;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onShieldDisable(PlayerShieldDisableEvent event) {
        if (!Config.SHIELD_ENCHANT_ENABLED) return;

        Player player = event.getPlayer();
        ItemStack shield = getShield(player);
        if (shield == null) return;

        int level = shield.getEnchantmentLevel(Enchantment.QUICK_CHARGE);
        if (level <= 0) return;

        int reduced = Math.max(20, event.getCooldown() - QUICK_CHARGE_TICKS_PER_LEVEL * level);
        if (reduced < event.getCooldown()) {
            event.setCooldown(reduced);
            AchievementProgression.award(player, "toolkit_shield");
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

        Map<Enchantment, Integer> incomingEnchants;
        if (right.getType() == Material.ENCHANTED_BOOK) {
            EnchantmentStorageMeta bookMeta = (EnchantmentStorageMeta) right.getItemMeta();
            if (bookMeta == null) return;
            incomingEnchants = bookMeta.getStoredEnchants();
        } else if (right.getType() == Material.SHIELD) {
            incomingEnchants = right.getEnchantments();
        } else {
            return;
        }

        ItemStack currentResult = event.getResult();
        ItemStack result = (currentResult != null && currentResult.getType() == Material.SHIELD)
                ? currentResult.clone()
                : left.clone();

        Enchantment existingExtra = getExtraShieldEnchant(left);
        boolean changed = false;
        boolean fromBook = right.getType() == Material.ENCHANTED_BOOK;
        int addedCost = 0;

        for (Map.Entry<Enchantment, Integer> entry : incomingEnchants.entrySet()) {
            Enchantment ench = entry.getKey();
            if (!isExtraShieldEnchant(ench)) continue;

            // Отдача и Отбрасывание — два названия одного эффекта щита.
            if (existingExtra != null && !existingExtra.equals(ench)
                    && !(isPushEnchant(existingExtra) && isPushEnchant(ench))) {
                continue;
            }

            Enchantment target = existingExtra != null && isPushEnchant(existingExtra) && isPushEnchant(ench)
                    ? existingExtra : ench;
            int current = isPushEnchant(ench)
                    ? Math.max(left.getEnchantmentLevel(Enchantment.KNOCKBACK),
                            left.getEnchantmentLevel(Enchantment.PUNCH))
                    : left.getEnchantmentLevel(ench);
            int incoming = entry.getValue();
            int newLevel = isPushEnchant(ench)
                    ? ShieldAnvilLevels.combinePush(current, incoming)
                    : (current == incoming && current < ench.getMaxLevel())
                            ? current + 1 : Math.max(current, incoming);

            if (newLevel > current) {
                if (isPushEnchant(target)) {
                    result.removeEnchantment(Enchantment.KNOCKBACK);
                    result.removeEnchantment(Enchantment.PUNCH);
                }
                result.addUnsafeEnchantment(target, newLevel);
                addedCost += newLevel * enchantCost(target, fromBook);
                existingExtra = target;
                changed = true;
            }
        }
        if (changed) {
            // Если ваниль результата не дала (щит + книга шипов), считаем штрафы и переименование сами.
            boolean vanilla = currentResult != null && currentResult.getType() == Material.SHIELD;
            int base = vanilla
                    ? event.getView().getRepairCost()
                    : AnvilCosts.penalty(left) + AnvilCosts.penalty(right)
                            + (AnvilCosts.applyRename(event, result) ? 1 : 0);
            AnvilCosts.finish(event, result, left, base + addedCost);
        }
    }

    private static boolean isExtraShieldEnchant(Enchantment enchantment) {
        for (Enchantment allowed : SHIELD_EXTRA_ENCHANTS) {
            if (allowed.equals(enchantment)) return true;
        }
        return false;
    }

    private static boolean isPushEnchant(Enchantment enchantment) {
        return enchantment.equals(Enchantment.KNOCKBACK) || enchantment.equals(Enchantment.PUNCH);
    }

    private static Enchantment getExtraShieldEnchant(ItemStack shield) {
        for (Enchantment allowed : SHIELD_EXTRA_ENCHANTS) {
            if (shield.getEnchantmentLevel(allowed) > 0) {
                return allowed;
            }
        }
        return null;
    }

    /** Запретить накладывать Починку на булаву через наковальню. */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAnvilMaceNoMending(PrepareAnvilEvent event) {
        if (!Config.MACE_NO_MENDING_ENABLED) return;
        HumanEntity viewer = event.getView().getPlayer();
        maceRejected.remove(viewer.getUniqueId());
        AnvilInventory inv = event.getInventory();
        ItemStack left = inv.getItem(0);
        ItemStack result = event.getResult();
        if (left == null || result == null) return;
        if (left.getType() != Material.MACE) return;
        // Preserve Mending already on the mace when repairing it. If the
        // prepared result would add Mending to a mace that did not have it,
        // reject the anvil result so the book/material is not consumed.
        if (left.getEnchantmentLevel(Enchantment.MENDING) > 0) return;
        if (result.getEnchantmentLevel(Enchantment.MENDING) <= 0) return;
        event.setResult(null);
        maceRejected.add(viewer.getUniqueId());
    }

    /** Достижение за булаву — когда игрок реально пытается забрать отклонённый результат, а не просто положил книгу. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onMaceRejectedClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getInventory() instanceof AnvilInventory) || event.getRawSlot() != 2) return;
        if (maceRejected.remove(player.getUniqueId())) {
            AchievementProgression.award(player, "toolkit_mace");
        }
    }

    @EventHandler
    public void onAnvilClose(InventoryCloseEvent event) {
        maceRejected.remove(event.getPlayer().getUniqueId());
    }

    private static ItemStack getShield(Player player) {
        EquipmentSlot slot = shieldSlot(player);
        if (slot == null) return null;
        return player.getInventory().getItem(slot);
    }

    private static EquipmentSlot shieldSlot(Player player) {
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (offhand.getType() == Material.SHIELD) return EquipmentSlot.OFF_HAND;
        ItemStack mainhand = player.getInventory().getItemInMainHand();
        if (mainhand.getType() == Material.SHIELD) return EquipmentSlot.HAND;
        return null;
    }
}
