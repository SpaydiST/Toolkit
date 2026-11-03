package org.example1.toolkit.bundlevanish;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.example1.toolkit.AnvilCosts;
import org.example1.toolkit.Config;
import org.example1.toolkit.progression.AchievementProgression;

public final class BundleVanishingListener implements Listener {

    private static final int VANISHING_BOOK_COST = 4;

    private final JavaPlugin plugin;

    public BundleVanishingListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTakeAnvilResult(InventoryClickEvent event) {
        if (!Config.BUNDLE_VANISHING_ENABLED) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getInventory() instanceof AnvilInventory) || event.getRawSlot() != 2) return;
        if (event.getAction() == InventoryAction.NOTHING) return;
        ItemStack result = event.getCurrentItem();
        if (result == null || !isBundle(result.getType())
                || result.getEnchantmentLevel(Enchantment.VANISHING_CURSE) <= 0) return;

        ItemStack expected = result.clone();
        int countBefore = countSimilar(player, expected);
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline() && countSimilar(player, expected) > countBefore) {
                AchievementProgression.award(player, "toolkit_bundle");
            }
        });
    }

    @EventHandler
    public void onAnvil(PrepareAnvilEvent event) {
        if (!Config.BUNDLE_VANISHING_ENABLED) return;
        AnvilInventory inv = event.getInventory();
        ItemStack left = inv.getItem(0);
        ItemStack right = inv.getItem(1);
        if (left == null || right == null) return;
        if (!isBundle(left.getType())) return;
        if (right.getType() != Material.ENCHANTED_BOOK) return;

        EnchantmentStorageMeta bookMeta = (EnchantmentStorageMeta) right.getItemMeta();
        if (bookMeta == null) return;

        if (!bookMeta.hasStoredEnchant(Enchantment.VANISHING_CURSE)) return;

        if (left.getEnchantmentLevel(Enchantment.VANISHING_CURSE) > 0) return;

        int bookLevel = bookMeta.getStoredEnchantLevel(Enchantment.VANISHING_CURSE);
        ItemStack current = event.getResult();
        ItemStack result = current != null && current.getType() == left.getType() ? current.clone() : left.clone();
        result.addUnsafeEnchantment(Enchantment.VANISHING_CURSE, bookLevel);
        boolean renamed = AnvilCosts.applyRename(event, result);

        // Проклятие исчезновения: множитель книги 4 за уровень + штрафы предыдущих работ + 1 за переименование.
        int cost = bookLevel * VANISHING_BOOK_COST + AnvilCosts.penalty(left) + AnvilCosts.penalty(right)
                + (renamed ? 1 : 0);
        AnvilCosts.finish(event, result, left, cost);
    }

    private static boolean isBundle(Material material) {
        return material == Material.BUNDLE || material.name().endsWith("_BUNDLE");
    }

    private static int countSimilar(Player player, ItemStack item) {
        int count = 0;
        for (ItemStack inventoryItem : player.getInventory().getContents()) {
            if (inventoryItem != null && inventoryItem.isSimilar(item)) {
                count += inventoryItem.getAmount();
            }
        }
        ItemStack cursor = player.getItemOnCursor();
        if (cursor != null && cursor.isSimilar(item)) {
            count += cursor.getAmount();
        }
        return count;
    }
}
